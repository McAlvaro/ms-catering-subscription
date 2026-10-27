package com.mcalvaro.mscatering.domain.subscription;

import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.mcalvaro.mscatering.domain.core.AggregateRoot;
import com.mcalvaro.mscatering.domain.subscription.entity.BiweeklyEvaluation;
import com.mcalvaro.mscatering.domain.subscription.entity.DeliveryCalendar;
import com.mcalvaro.mscatering.domain.subscription.entity.DeliveryDay;
import com.mcalvaro.mscatering.domain.subscription.entity.PauseRequest;
import com.mcalvaro.mscatering.domain.subscription.enums.DeliveryDayStatus;
import com.mcalvaro.mscatering.domain.subscription.enums.EvaluationStatus;
import com.mcalvaro.mscatering.domain.subscription.enums.SubscriptionStatus;
import com.mcalvaro.mscatering.domain.subscription.event.BiweeklyEvaluationCompleted;
import com.mcalvaro.mscatering.domain.subscription.event.DeliveryDayCancelled;
import com.mcalvaro.mscatering.domain.subscription.event.DeliveryDayModified;
import com.mcalvaro.mscatering.domain.subscription.event.DeliveryConfirmed;
import com.mcalvaro.mscatering.domain.subscription.event.DeliveryFailed;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionCancelled;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionCompleted;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionCreated;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionPaused;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionReactivated;
import com.mcalvaro.mscatering.domain.subscription.vo.ContractCode;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryPreferences;
import com.mcalvaro.mscatering.domain.subscription.vo.PauseRange;
import com.mcalvaro.mscatering.domain.subscription.vo.ServiceContract;
import com.mcalvaro.mscatering.domain.subscription.vo.TimeWindow;
import com.mcalvaro.mscatering.domain.subscription.vo.ValidityPeriod;

/**
 * Aggregate Root — Subscription (Suscripcion).
 * <p>
 * Manages the full lifecycle of a catering subscription:
 * creation, pause/reactivation, delivery-day modifications,
 * completion and cancellation.
 */
public class Subscription extends AggregateRoot {

    private final UUID patientId;
    private final UUID dietPlanId;
    private final ContractCode contractCode;
    private ServiceContract contract;
    private DeliveryPreferences preferences;
    private final DeliveryCalendar deliveryCalendar;
    private SubscriptionStatus status;
    private final List<PauseRequest> pauseRequests = new ArrayList<>();
    private final List<BiweeklyEvaluation> evaluations = new ArrayList<>();

    private Subscription(UUID id, UUID patientId, UUID dietPlanId,
            ContractCode contractCode, ServiceContract contract,
            DeliveryPreferences preferences, DeliveryCalendar deliveryCalendar) {
        super(id);
        this.patientId = patientId;
        this.dietPlanId = dietPlanId;
        this.contractCode = contractCode;
        this.contract = contract;
        this.preferences = preferences;
        this.deliveryCalendar = deliveryCalendar;
        this.status = SubscriptionStatus.ACTIVE;
    }

    /**
     * Creates a new active subscription.
     * <ul>
     * <li>Generates a {@link ContractCode} using {@code contractSequence} (must be
     * unique within the year — the caller is responsible for providing it).</li>
     * <li>Populates the {@link DeliveryCalendar} with one {@link DeliveryDay}
     * per day in the validity period, using the patient's default preferences.</li>
     * <li>Schedules biweekly evaluations at the end of each
     * {@value #BIWEEKLY_PERIOD_DAYS}-day
     * period: 1 evaluation for 15-day plans (on {@code endDate}),
     * 2 evaluations for 30-day plans (on day 15 and on {@code endDate}).</li>
     * <li>Emits {@link SubscriptionCreated}.</li>
     * </ul>
     *
     * @param contractSequence unique sequence number for the contract code within
     *                         the current year,
     *                         provided by the application layer after querying the
     *                         repository.
     */
    public static Subscription create(UUID id, UUID patientId, UUID dietPlanId,
            ServiceContract contract,
            DeliveryPreferences preferences,
            int contractSequence) {
        ContractCode code = ContractCode.generate(Year.now().getValue(), contractSequence);

        ValidityPeriod period = contract.period();

        DeliveryCalendar calendar = new DeliveryCalendar(UUID.randomUUID(), id, period);
        for (LocalDate day : period.allDays()) {
            calendar.addDay(new DeliveryDay(
                    UUID.randomUUID(),
                    day,
                    preferences.primaryAddress(),
                    preferences.timeWindow(),
                    preferences.specialInstructions()));
        }

        Subscription subscription = new Subscription(
                id, patientId, dietPlanId, code, contract, preferences, calendar);

        subscription.addDomainEvent(new SubscriptionCreated(id, patientId, code));
        return subscription;
    }

    /**
     * Inject evaluations generated by the Domain Service.
     */
    public void scheduleEvaluations(List<BiweeklyEvaluation> generatedEvaluations) {
        this.evaluations.addAll(generatedEvaluations);
    }

    /**
     * Pauses the subscription for the given range.
     * Validates that the current status is {@code ACTIVE}.
     */
    public void pause(PauseRange range, String reason) {
        pause(range, reason, UUID.randomUUID().toString());
    }

    public void pause(PauseRange range, String reason, String correlationId) {
        if (status != SubscriptionStatus.ACTIVE) {
            throw SubscriptionErrors.subscriptionNotActive();
        }
        requireCorrelationId(correlationId);
        if (!contract.period().contains(range.startDate()) || !contract.period().contains(range.endDate())) {
            throw SubscriptionErrors.pauseRangeOutsideValidity();
        }
        if (deliveryCalendar.hasProcessedDays(range)) {
            throw SubscriptionErrors.deliveryDayBlocked();
        }
        int extensionDays = range.durationDays();
        LocalDate newEndDate = contract.period().endDate().plusDays(extensionDays);
        extendEffectiveValidity(newEndDate);
        deliveryCalendar.pauseRange(range);
        PauseRequest request = new PauseRequest(UUID.randomUUID(), range, reason, correlationId, extensionDays);
        pauseRequests.add(request);
        status = SubscriptionStatus.PAUSED;
        addDomainEvent(new SubscriptionPaused(getId(), patientId, range, reason, LocalDate.now(), newEndDate, correlationId));
    }

    /**
     * Reactivates the subscription early, ending the active pause request.
     * Validates that the current status is {@code PAUSED}.
     */
    public void reactivate(LocalDate reactivationDate) {
        if (status != SubscriptionStatus.PAUSED) {
            throw SubscriptionErrors.subscriptionNotPaused();
        }
        PauseRequest active = pauseRequests.stream()
                .filter(PauseRequest::isActive)
                .findFirst()
                .orElseThrow(SubscriptionErrors::noPauseRequestActive);

        ensureAtLeast48Hours(reactivationDate);
        if (reactivationDate.isBefore(active.getRange().startDate())
                || reactivationDate.isAfter(active.getRange().endDate())) {
            throw SubscriptionErrors.reactivationOutsidePauseRange();
        }
        int restoredDays = (int) java.time.temporal.ChronoUnit.DAYS
                .between(reactivationDate, active.getRange().endDate()) + 1;
        LocalDate newEndDate = contract.period().endDate().minusDays(restoredDays);
        if (deliveryCalendar.hasProcessedDaysAfter(newEndDate)) {
            throw SubscriptionErrors.deliveryDayBlocked();
        }
        active.earlyReactivate(reactivationDate);
        active.reduceExtensionDays(restoredDays);
        deliveryCalendar.reactivateRange(new PauseRange(reactivationDate, active.getRange().endDate()));
        contract = withPeriod(new ValidityPeriod(contract.period().startDate(), newEndDate));
        deliveryCalendar.removeDaysAfter(newEndDate);
        status = SubscriptionStatus.ACTIVE;
        addDomainEvent(new SubscriptionReactivated(getId(), patientId, reactivationDate, newEndDate,
                active.getCorrelationId()));
    }

    /**
     * Modifies a single delivery day's details.
     * The 48-hour rule is enforced inside {@link DeliveryDay#modify}.
     */
    public void modifyDeliveryDay(UUID dayId, DeliveryAddress address,
            TimeWindow timeWindow, String instructions) {
        DeliveryDay day = deliveryCalendar.findDayById(dayId)
                .orElseThrow(() -> SubscriptionErrors.deliveryDayNotFound(dayId));
        day.modify(address, timeWindow, instructions);
        addDomainEvent(new DeliveryDayModified(getId(), dayId, day.getDate()));
    }

    public void markNoDelivery(UUID dayId) {
        if (!deliveryCalendar.hasActiveDaysAfterExcluding(dayId)) {
            throw SubscriptionErrors.calendarNoActiveDays();
        }
        DeliveryDay day = deliveryCalendar.findDayById(dayId)
                .orElseThrow(() -> SubscriptionErrors.deliveryDayNotFound(dayId));
        day.markAsNoDelivery();
        addDomainEvent(new DeliveryDayCancelled(getId(), dayId));
    }

    public void updateDeliveryPreferences(DeliveryPreferences newPreferences) {
        this.preferences = newPreferences;
    }

    public void confirmDelivery(UUID dayId) {
        DeliveryDay day = deliveryCalendar.findDayById(dayId)
                .orElseThrow(() -> SubscriptionErrors.deliveryDayNotFound(dayId));
        day.markAsDelivered();
        addDomainEvent(new DeliveryConfirmed(getId(), dayId));
    }

    public void registerFailedDelivery(UUID dayId, String reason) {
        DeliveryDay day = deliveryCalendar.findDayById(dayId)
                .orElseThrow(() -> SubscriptionErrors.deliveryDayNotFound(dayId));
        day.markAsFailed(reason);
        addDomainEvent(new DeliveryFailed(getId(), dayId, reason));
    }

    public void markEvaluationCompleted(UUID evaluationId, LocalDate completedAt) {
        BiweeklyEvaluation evaluation = evaluations.stream()
                .filter(e -> e.getId().equals(evaluationId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Evaluation not found"));
        evaluation.markCompleted(completedAt);
        addDomainEvent(new BiweeklyEvaluationCompleted(getId(), evaluationId, completedAt));
    }

    /**
     * Completes the subscription. Only allowed after the contract period has ended.
     */
    public void complete() {
        complete(UUID.randomUUID().toString());
    }

    public void complete(String correlationId) {
        if (status != SubscriptionStatus.ACTIVE) {
            throw SubscriptionErrors.subscriptionNotActive();
        }
        if (!LocalDate.now().isAfter(contract.period().endDate())) {
            throw SubscriptionErrors.subscriptionPeriodNotEnded();
        }
        requireCorrelationId(correlationId);
        LocalDate today = LocalDate.now();
        int cancelledPendingDeliveries = Math.toIntExact(deliveryCalendar.countFuturePendingDeliveries(today));
        deliveryCalendar.cancelFuturePendingDeliveries(today);
        status = SubscriptionStatus.COMPLETED;
        addDomainEvent(new SubscriptionCompleted(getId(), patientId, dietPlanId, contract.period().endDate(),
                cancelledPendingDeliveries, correlationId));
    }

    /**
     * Cancels the subscription and all its pending/scheduled deliveries and
     * evaluations.
     */
    public void cancel(String reason) {
        cancel(reason, UUID.randomUUID().toString());
    }

    public void cancel(String reason, String correlationId) {
        if (reason == null || reason.isBlank()) {
            throw SubscriptionErrors.blankCancellationReason();
        }
        requireCorrelationId(correlationId);
        LocalDate today = LocalDate.now();
        int cancelledPendingDeliveries = Math.toIntExact(deliveryCalendar.countFuturePendingDeliveries(today));
        status = SubscriptionStatus.CANCELLED;
        deliveryCalendar.cancelFuturePendingDeliveries(today);

        evaluations.stream()
                .filter(e -> e.getStatus() == EvaluationStatus.PENDING)
                .forEach(BiweeklyEvaluation::cancel);

        addDomainEvent(new SubscriptionCancelled(getId(), patientId, dietPlanId, reason, today,
                cancelledPendingDeliveries, correlationId));
    }

    private void extendEffectiveValidity(LocalDate newEndDate) {
        ValidityPeriod extendedPeriod = new ValidityPeriod(contract.period().startDate(), newEndDate);
        contract = withPeriod(extendedPeriod);
        deliveryCalendar.extendTo(extendedPeriod, preferences.primaryAddress(), preferences.timeWindow(),
                preferences.specialInstructions());
    }

    private ServiceContract withPeriod(ValidityPeriod period) {
        return new ServiceContract(contract.dietPlanId(), period, contract.serviceType(), contract.totalPrice(),
                contract.acceptedConditions(), contract.signedAt());
    }

    private void ensureAtLeast48Hours(LocalDate date) {
        if (!date.isAfter(LocalDate.now().plusDays(1))) {
            throw SubscriptionErrors.pauseRequires48HoursAnticipation();
        }
    }

    private void requireCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            throw SubscriptionErrors.correlationIdBlank();
        }
    }

    public UUID getPatientId() {
        return patientId;
    }

    public UUID getDietPlanId() {
        return dietPlanId;
    }

    public ContractCode getContractCode() {
        return contractCode;
    }

    public ServiceContract getContract() {
        return contract;
    }

    public DeliveryPreferences getPreferences() {
        return preferences;
    }

    public DeliveryCalendar getDeliveryCalendar() {
        return deliveryCalendar;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public List<PauseRequest> getPauseRequests() {
        return Collections.unmodifiableList(pauseRequests);
    }

    public List<BiweeklyEvaluation> getEvaluations() {
        return Collections.unmodifiableList(evaluations);
    }
}

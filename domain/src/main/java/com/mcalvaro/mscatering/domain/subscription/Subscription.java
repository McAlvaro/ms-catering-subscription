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
import com.mcalvaro.mscatering.domain.subscription.event.DeliveryDayModified;
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
    private final ServiceContract contract;
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
        if (status != SubscriptionStatus.ACTIVE) {
            throw SubscriptionErrors.subscriptionNotActive();
        }
        deliveryCalendar.pauseRange(range);
        pauseRequests.add(new PauseRequest(UUID.randomUUID(), range, reason));
        status = SubscriptionStatus.PAUSED;
        addDomainEvent(new SubscriptionPaused(getId(), range));
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

        active.earlyReactivate(reactivationDate);
        deliveryCalendar.reactivateRange(active.getRange());
        status = SubscriptionStatus.ACTIVE;
        addDomainEvent(new SubscriptionReactivated(getId(), reactivationDate));
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
    }

    public void updateDeliveryPreferences(DeliveryPreferences newPreferences) {
        this.preferences = newPreferences;
    }

    public void confirmDelivery(UUID dayId) {
        DeliveryDay day = deliveryCalendar.findDayById(dayId)
                .orElseThrow(() -> SubscriptionErrors.deliveryDayNotFound(dayId));
        day.markAsDelivered();
    }

    public void registerFailedDelivery(UUID dayId, String reason) {
        DeliveryDay day = deliveryCalendar.findDayById(dayId)
                .orElseThrow(() -> SubscriptionErrors.deliveryDayNotFound(dayId));
        day.markAsFailed(reason);
    }

    public void markEvaluationCompleted(UUID evaluationId, LocalDate completedAt) {
        BiweeklyEvaluation evaluation = evaluations.stream()
                .filter(e -> e.getId().equals(evaluationId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Evaluation not found"));
        evaluation.markCompleted(completedAt);
    }

    /**
     * Completes the subscription. Only allowed after the contract period has ended.
     */
    public void complete() {
        if (status != SubscriptionStatus.ACTIVE) {
            throw SubscriptionErrors.subscriptionNotActive();
        }
        if (!LocalDate.now().isAfter(contract.period().endDate())) {
            throw SubscriptionErrors.subscriptionPeriodNotEnded();
        }
        status = SubscriptionStatus.COMPLETED;
        addDomainEvent(new SubscriptionCompleted(getId()));
    }

    /**
     * Cancels the subscription and all its pending/scheduled deliveries and
     * evaluations.
     */
    public void cancel(String reason) {
        status = SubscriptionStatus.CANCELLED;
        deliveryCalendar.getDeliveryDays().stream()
                .filter(d -> d.getStatus() == DeliveryDayStatus.SCHEDULED
                        || d.getStatus() == DeliveryDayStatus.PAUSED)
                .forEach(DeliveryDay::cancel);
        evaluations.stream()
                .filter(e -> e.getStatus() == EvaluationStatus.PENDING)
                .forEach(BiweeklyEvaluation::cancel);
        addDomainEvent(new SubscriptionCancelled(getId(), reason));
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

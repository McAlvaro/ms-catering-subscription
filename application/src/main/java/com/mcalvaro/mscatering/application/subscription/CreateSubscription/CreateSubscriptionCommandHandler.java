package com.mcalvaro.mscatering.application.subscription.CreateSubscription;

import an.awesome.pipelinr.Command;

import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;
import com.mcalvaro.mscatering.domain.subscription.entity.BiweeklyEvaluation;
import com.mcalvaro.mscatering.domain.subscription.enums.PlanDuration;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.domain.subscription.service.BiweeklyEvaluationGenerator;
import com.mcalvaro.mscatering.domain.subscription.service.SubscriptionDuplicationValidator;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryPreferences;
import com.mcalvaro.mscatering.domain.subscription.vo.ServiceContract;
import com.mcalvaro.mscatering.domain.subscription.vo.TimeWindow;
import com.mcalvaro.mscatering.domain.subscription.vo.ValidityPeriod;
import com.mcalvaro.mscatering.application.patient.address.DeliveryAddressResolver;
import com.mcalvaro.mscatering.application.dietplan.DietPlanResolver;
import com.mcalvaro.mscatering.domain.dietplan.DietPlanProjection;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Handler for {@link CreateSubscriptionCommand}.
 * <p>
 * Orchestrates the creation of a new subscription by coordinating
 * domain services, the aggregate factory method, and the unit of work.
 * <p>
 * Business rules enforced:
 * <ul>
 * <li>INV-01: A patient can only have one ACTIVE or PAUSED subscription at a
 * time.</li>
 * <li>RN-11: Biweekly evaluations are generated automatically on creation.</li>
 * <li>RN-12: Evaluation dates are adjusted to the next business day if
 * needed.</li>
 * </ul>
 */
public class CreateSubscriptionCommandHandler implements Command.Handler<CreateSubscriptionCommand, UUID> {

    private final ISubscriptionRepository subscriptionRepository;
    private final SubscriptionDuplicationValidator duplicationValidator;
    private final BiweeklyEvaluationGenerator evaluationGenerator;
    private final DeliveryAddressResolver deliveryAddressResolver;
    private final DietPlanResolver dietPlanResolver;

    public CreateSubscriptionCommandHandler(ISubscriptionRepository subscriptionRepository,
            SubscriptionDuplicationValidator duplicationValidator,
            BiweeklyEvaluationGenerator evaluationGenerator, DeliveryAddressResolver deliveryAddressResolver,
            DietPlanResolver dietPlanResolver) {
        this.subscriptionRepository = subscriptionRepository;
        this.duplicationValidator = duplicationValidator;
        this.evaluationGenerator = evaluationGenerator;
        this.deliveryAddressResolver = deliveryAddressResolver;
        this.dietPlanResolver = dietPlanResolver;
    }

    @Override
    public UUID handle(CreateSubscriptionCommand command) {
        duplicationValidator.validate(command.patientId());
        // Build domain Value Objects from primitive command fields
        ValidityPeriod period = new ValidityPeriod(command.startDate(), command.endDate());
        DietPlanProjection dietPlan = dietPlanResolver.resolve(command.dietPlanId(), command.patientId(),
                command.startDate(), command.endDate());
        ServiceContract contract = new ServiceContract(
                command.dietPlanId(),
                dietPlan.type(),
                period,
                ServiceType.valueOf(command.serviceType()),
                command.totalPrice(),
                command.acceptedConditions(),
                Instant.now());

        TimeWindow window = new TimeWindow(command.prefTimeStart(), command.prefTimeEnd());
        DeliveryAddress address = deliveryAddressResolver.resolve(command.patientId(), command.deliveryAddressId());
        DeliveryPreferences preferences = new DeliveryPreferences(address, window, command.prefSpecialInstructions());

        UUID subscriptionId = UUID.randomUUID();
        int nextSequence = subscriptionRepository.getNextContractSequenceOfYear();

        PlanDuration duration = PlanDuration.fromDays(period.durationDays());
        List<BiweeklyEvaluation> evaluations = evaluationGenerator.generate(
                command.patientId(),
                period.startDate(),
                duration);
        Subscription subscription = Subscription.create(
                subscriptionId,
                command.patientId(),
                command.dietPlanId(),
                contract,
                preferences,
                nextSequence,
                evaluations,
                UUID.randomUUID().toString());

        subscriptionRepository.save(subscription);

        return subscription.getId();
    }
}

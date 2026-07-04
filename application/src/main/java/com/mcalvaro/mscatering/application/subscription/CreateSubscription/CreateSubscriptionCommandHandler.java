package com.mcalvaro.mscatering.application.subscription.CreateSubscription;

import an.awesome.pipelinr.Command;

import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;
import com.mcalvaro.mscatering.domain.subscription.entity.BiweeklyEvaluation;
import com.mcalvaro.mscatering.domain.subscription.enums.PlanDuration;
import com.mcalvaro.mscatering.domain.subscription.service.BiweeklyEvaluationGenerator;
import com.mcalvaro.mscatering.domain.subscription.service.SubscriptionDuplicationValidator;

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

    public CreateSubscriptionCommandHandler(ISubscriptionRepository subscriptionRepository,
            SubscriptionDuplicationValidator duplicationValidator,
            BiweeklyEvaluationGenerator evaluationGenerator) {
        this.subscriptionRepository = subscriptionRepository;
        this.duplicationValidator = duplicationValidator;
        this.evaluationGenerator = evaluationGenerator;
    }

    @Override
    public UUID handle(CreateSubscriptionCommand command) {
        duplicationValidator.validate(command.patientId());

        int nextSequence = subscriptionRepository.getNextContractSequenceOfYear();

        Subscription subscription = Subscription.create(
                command.id(),
                command.patientId(),
                command.dietPlanId(),
                command.contract(),
                command.preferences(),
                nextSequence);

        PlanDuration duration = PlanDuration.fromDays(command.contract().period().durationDays());
        List<BiweeklyEvaluation> evaluations = evaluationGenerator.generate(
                command.patientId(),
                command.contract().period().startDate(),
                duration);
        subscription.scheduleEvaluations(evaluations);

        subscriptionRepository.save(subscription);

        return subscription.getId();
    }
}

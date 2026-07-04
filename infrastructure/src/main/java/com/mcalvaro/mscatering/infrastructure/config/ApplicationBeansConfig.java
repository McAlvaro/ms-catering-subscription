package com.mcalvaro.mscatering.infrastructure.config;

import com.mcalvaro.mscatering.application.abstractions.DomainEventDispatcher;

import com.mcalvaro.mscatering.application.consolidatedcalendar.CloseConsolidatedCalendar.CloseConsolidatedCalendarCommandHandler;
import com.mcalvaro.mscatering.application.consolidatedcalendar.GetConsolidatedCalendarByDate.GetConsolidatedCalendarByDateQueryHandler;
import com.mcalvaro.mscatering.application.consolidatedcalendar.GetConsolidatedCalendarByDate.IConsolidatedCalendarQueryService;

import com.mcalvaro.mscatering.application.subscription.CancelSubscription.CancelSubscriptionCommandHandler;
import com.mcalvaro.mscatering.application.subscription.CompleteSubscription.CompleteSubscriptionCommandHandler;
import com.mcalvaro.mscatering.application.subscription.ConfirmDelivery.ConfirmDeliveryCommandHandler;
import com.mcalvaro.mscatering.application.subscription.CreateSubscription.CreateSubscriptionCommandHandler;
import com.mcalvaro.mscatering.application.subscription.GetSubscriptionDetails.GetSubscriptionDetailsQueryHandler;
import com.mcalvaro.mscatering.application.subscription.GetSubscriptionDetails.ISubscriptionQueryService;
import com.mcalvaro.mscatering.application.subscription.MarkEvaluationCompleted.MarkEvaluationCompletedCommandHandler;
import com.mcalvaro.mscatering.application.subscription.MarkNoDelivery.MarkNoDeliveryCommandHandler;
import com.mcalvaro.mscatering.application.subscription.ModifyDeliveryDay.ModifyDeliveryDayCommandHandler;
import com.mcalvaro.mscatering.application.subscription.PauseSubscription.PauseSubscriptionCommandHandler;
import com.mcalvaro.mscatering.application.subscription.ReactivateSubscription.ReactivateSubscriptionCommandHandler;
import com.mcalvaro.mscatering.application.subscription.RegisterFailedDelivery.RegisterFailedDeliveryCommandHandler;
import com.mcalvaro.mscatering.application.subscription.UpdateDeliveryPreferences.UpdateDeliveryPreferencesCommandHandler;
import com.mcalvaro.mscatering.application.patient.SavePatientReferenceCommandHandler;

import com.mcalvaro.mscatering.domain.consolidatedcalendar.IConsolidatedCalendarRepository;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.service.DailyConsolidator;
import com.mcalvaro.mscatering.domain.patient.IPatientReferenceRepository;
import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.service.BiweeklyEvaluationGenerator;
import com.mcalvaro.mscatering.domain.subscription.service.SubscriptionDuplicationValidator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra todos los Handlers de la capa {@code application} como beans de
 * Spring.
 * <p>
 * Esta clase es el punto de ensamblaje donde la Inversión de Dependencias (DIP)
 * se materializa: los Handlers son POJOs puros que reciben interfaces del
 * dominio
 * por constructor. Spring, a través de esta configuración, inyecta las
 * implementaciones concretas que viven en la capa de infraestructura.
 * <p>
 * Patrón aplicado: <b>Composition Root</b> — el único lugar donde se decide
 * qué implementación concreta satisface cada abstracción.
 */
@Configuration
public class ApplicationBeansConfig {

    // ========================================================================
    // Patient Command Handlers
    // ========================================================================

    @Bean
    SavePatientReferenceCommandHandler savePatientReferenceCommandHandler(
            IPatientReferenceRepository patientRepository,
            DomainEventDispatcher domainEventDispatcher) {
        return new SavePatientReferenceCommandHandler(patientRepository, domainEventDispatcher);
    }

    // ========================================================================
    // Subscription Command Handlers
    // ========================================================================

    @Bean
    CreateSubscriptionCommandHandler createSubscriptionCommandHandler(
            ISubscriptionRepository subscriptionRepository,
            SubscriptionDuplicationValidator duplicationValidator,
            BiweeklyEvaluationGenerator evaluationGenerator,
            DomainEventDispatcher domainEventDispatcher) {
        return new CreateSubscriptionCommandHandler(
                subscriptionRepository, duplicationValidator, evaluationGenerator, domainEventDispatcher);
    }

    @Bean
    PauseSubscriptionCommandHandler pauseSubscriptionCommandHandler(
            ISubscriptionRepository subscriptionRepository,
            DomainEventDispatcher domainEventDispatcher) {
        return new PauseSubscriptionCommandHandler(subscriptionRepository, domainEventDispatcher);
    }

    @Bean
    ReactivateSubscriptionCommandHandler reactivateSubscriptionCommandHandler(
            ISubscriptionRepository subscriptionRepository,
            DomainEventDispatcher domainEventDispatcher) {
        return new ReactivateSubscriptionCommandHandler(subscriptionRepository, domainEventDispatcher);
    }

    @Bean
    ModifyDeliveryDayCommandHandler modifyDeliveryDayCommandHandler(
            ISubscriptionRepository subscriptionRepository,
            DomainEventDispatcher domainEventDispatcher) {
        return new ModifyDeliveryDayCommandHandler(subscriptionRepository, domainEventDispatcher);
    }

    @Bean
    MarkNoDeliveryCommandHandler markNoDeliveryCommandHandler(
            ISubscriptionRepository subscriptionRepository,
            DomainEventDispatcher domainEventDispatcher) {
        return new MarkNoDeliveryCommandHandler(subscriptionRepository, domainEventDispatcher);
    }

    @Bean
    CancelSubscriptionCommandHandler cancelSubscriptionCommandHandler(
            ISubscriptionRepository subscriptionRepository,
            DomainEventDispatcher domainEventDispatcher) {
        return new CancelSubscriptionCommandHandler(subscriptionRepository, domainEventDispatcher);
    }

    @Bean
    CompleteSubscriptionCommandHandler completeSubscriptionCommandHandler(
            ISubscriptionRepository subscriptionRepository,
            DomainEventDispatcher domainEventDispatcher) {
        return new CompleteSubscriptionCommandHandler(subscriptionRepository, domainEventDispatcher);
    }

    @Bean
    ConfirmDeliveryCommandHandler confirmDeliveryCommandHandler(
            ISubscriptionRepository subscriptionRepository,
            DomainEventDispatcher domainEventDispatcher) {
        return new ConfirmDeliveryCommandHandler(subscriptionRepository, domainEventDispatcher);
    }

    @Bean
    RegisterFailedDeliveryCommandHandler registerFailedDeliveryCommandHandler(
            ISubscriptionRepository subscriptionRepository,
            DomainEventDispatcher domainEventDispatcher) {
        return new RegisterFailedDeliveryCommandHandler(subscriptionRepository, domainEventDispatcher);
    }

    @Bean
    MarkEvaluationCompletedCommandHandler markEvaluationCompletedCommandHandler(
            ISubscriptionRepository subscriptionRepository,
            DomainEventDispatcher domainEventDispatcher) {
        return new MarkEvaluationCompletedCommandHandler(subscriptionRepository, domainEventDispatcher);
    }

    @Bean
    UpdateDeliveryPreferencesCommandHandler updateDeliveryPreferencesCommandHandler(
            ISubscriptionRepository subscriptionRepository,
            DomainEventDispatcher domainEventDispatcher) {
        return new UpdateDeliveryPreferencesCommandHandler(subscriptionRepository, domainEventDispatcher);
    }

    // ========================================================================
    // Subscription Query Handlers
    // ========================================================================

    @Bean
    GetSubscriptionDetailsQueryHandler getSubscriptionDetailsQueryHandler(
            ISubscriptionQueryService queryService) {
        return new GetSubscriptionDetailsQueryHandler(queryService);
    }

    // ========================================================================
    // Consolidated Calendar Command Handlers
    // ========================================================================

    @Bean
    CloseConsolidatedCalendarCommandHandler closeConsolidatedCalendarCommandHandler(
            DailyConsolidator dailyConsolidator,
            IConsolidatedCalendarRepository calendarRepository,
            DomainEventDispatcher domainEventDispatcher) {
        return new CloseConsolidatedCalendarCommandHandler(
                dailyConsolidator, calendarRepository, domainEventDispatcher);
    }

    // ========================================================================
    // Consolidated Calendar Query Handlers
    // ========================================================================

    @Bean
    GetConsolidatedCalendarByDateQueryHandler getConsolidatedCalendarByDateQueryHandler(
            IConsolidatedCalendarQueryService queryService) {
        return new GetConsolidatedCalendarByDateQueryHandler(queryService);
    }
}

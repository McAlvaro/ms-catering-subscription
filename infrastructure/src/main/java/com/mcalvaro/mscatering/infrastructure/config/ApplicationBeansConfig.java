package com.mcalvaro.mscatering.infrastructure.config;

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
            IPatientReferenceRepository patientRepository) {
        return new SavePatientReferenceCommandHandler(patientRepository);
    }

    // ========================================================================
    // Subscription Command Handlers
    // ========================================================================

    @Bean
    CreateSubscriptionCommandHandler createSubscriptionCommandHandler(
            ISubscriptionRepository subscriptionRepository,
            SubscriptionDuplicationValidator duplicationValidator,
            BiweeklyEvaluationGenerator evaluationGenerator) {
        return new CreateSubscriptionCommandHandler(
                subscriptionRepository, duplicationValidator, evaluationGenerator);
    }

    @Bean
    PauseSubscriptionCommandHandler pauseSubscriptionCommandHandler(
            ISubscriptionRepository subscriptionRepository) {
        return new PauseSubscriptionCommandHandler(subscriptionRepository);
    }

    @Bean
    ReactivateSubscriptionCommandHandler reactivateSubscriptionCommandHandler(
            ISubscriptionRepository subscriptionRepository) {
        return new ReactivateSubscriptionCommandHandler(subscriptionRepository);
    }

    @Bean
    ModifyDeliveryDayCommandHandler modifyDeliveryDayCommandHandler(
            ISubscriptionRepository subscriptionRepository) {
        return new ModifyDeliveryDayCommandHandler(subscriptionRepository);
    }

    @Bean
    MarkNoDeliveryCommandHandler markNoDeliveryCommandHandler(
            ISubscriptionRepository subscriptionRepository) {
        return new MarkNoDeliveryCommandHandler(subscriptionRepository);
    }

    @Bean
    CancelSubscriptionCommandHandler cancelSubscriptionCommandHandler(
            ISubscriptionRepository subscriptionRepository) {
        return new CancelSubscriptionCommandHandler(subscriptionRepository);
    }

    @Bean
    CompleteSubscriptionCommandHandler completeSubscriptionCommandHandler(
            ISubscriptionRepository subscriptionRepository) {
        return new CompleteSubscriptionCommandHandler(subscriptionRepository);
    }

    @Bean
    ConfirmDeliveryCommandHandler confirmDeliveryCommandHandler(
            ISubscriptionRepository subscriptionRepository) {
        return new ConfirmDeliveryCommandHandler(subscriptionRepository);
    }

    @Bean
    RegisterFailedDeliveryCommandHandler registerFailedDeliveryCommandHandler(
            ISubscriptionRepository subscriptionRepository) {
        return new RegisterFailedDeliveryCommandHandler(subscriptionRepository);
    }

    @Bean
    MarkEvaluationCompletedCommandHandler markEvaluationCompletedCommandHandler(
            ISubscriptionRepository subscriptionRepository) {
        return new MarkEvaluationCompletedCommandHandler(subscriptionRepository);
    }

    @Bean
    UpdateDeliveryPreferencesCommandHandler updateDeliveryPreferencesCommandHandler(
            ISubscriptionRepository subscriptionRepository) {
        return new UpdateDeliveryPreferencesCommandHandler(subscriptionRepository);
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
            IConsolidatedCalendarRepository calendarRepository) {
        return new CloseConsolidatedCalendarCommandHandler(
                dailyConsolidator, calendarRepository);
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

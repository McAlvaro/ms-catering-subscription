package com.mcalvaro.mscatering.infrastructure.config;

import com.mcalvaro.mscatering.application.abstractions.UnitOfWork;

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
            UnitOfWork unitOfWork) {
        return new SavePatientReferenceCommandHandler(patientRepository, unitOfWork);
    }

    // ========================================================================
    // Subscription Command Handlers
    // ========================================================================

    @Bean
    CreateSubscriptionCommandHandler createSubscriptionCommandHandler(
            ISubscriptionRepository subscriptionRepository,
            SubscriptionDuplicationValidator duplicationValidator,
            BiweeklyEvaluationGenerator evaluationGenerator,
            UnitOfWork unitOfWork) {
        return new CreateSubscriptionCommandHandler(
                subscriptionRepository, duplicationValidator, evaluationGenerator, unitOfWork);
    }

    @Bean
    PauseSubscriptionCommandHandler pauseSubscriptionCommandHandler(
            ISubscriptionRepository subscriptionRepository,
            UnitOfWork unitOfWork) {
        return new PauseSubscriptionCommandHandler(subscriptionRepository, unitOfWork);
    }

    @Bean
    ReactivateSubscriptionCommandHandler reactivateSubscriptionCommandHandler(
            ISubscriptionRepository subscriptionRepository,
            UnitOfWork unitOfWork) {
        return new ReactivateSubscriptionCommandHandler(subscriptionRepository, unitOfWork);
    }

    @Bean
    ModifyDeliveryDayCommandHandler modifyDeliveryDayCommandHandler(
            ISubscriptionRepository subscriptionRepository,
            UnitOfWork unitOfWork) {
        return new ModifyDeliveryDayCommandHandler(subscriptionRepository, unitOfWork);
    }

    @Bean
    MarkNoDeliveryCommandHandler markNoDeliveryCommandHandler(
            ISubscriptionRepository subscriptionRepository,
            UnitOfWork unitOfWork) {
        return new MarkNoDeliveryCommandHandler(subscriptionRepository, unitOfWork);
    }

    @Bean
    CancelSubscriptionCommandHandler cancelSubscriptionCommandHandler(
            ISubscriptionRepository subscriptionRepository,
            UnitOfWork unitOfWork) {
        return new CancelSubscriptionCommandHandler(subscriptionRepository, unitOfWork);
    }

    @Bean
    CompleteSubscriptionCommandHandler completeSubscriptionCommandHandler(
            ISubscriptionRepository subscriptionRepository,
            UnitOfWork unitOfWork) {
        return new CompleteSubscriptionCommandHandler(subscriptionRepository, unitOfWork);
    }

    @Bean
    ConfirmDeliveryCommandHandler confirmDeliveryCommandHandler(
            ISubscriptionRepository subscriptionRepository,
            UnitOfWork unitOfWork) {
        return new ConfirmDeliveryCommandHandler(subscriptionRepository, unitOfWork);
    }

    @Bean
    RegisterFailedDeliveryCommandHandler registerFailedDeliveryCommandHandler(
            ISubscriptionRepository subscriptionRepository,
            UnitOfWork unitOfWork) {
        return new RegisterFailedDeliveryCommandHandler(subscriptionRepository, unitOfWork);
    }

    @Bean
    MarkEvaluationCompletedCommandHandler markEvaluationCompletedCommandHandler(
            ISubscriptionRepository subscriptionRepository,
            UnitOfWork unitOfWork) {
        return new MarkEvaluationCompletedCommandHandler(subscriptionRepository, unitOfWork);
    }

    @Bean
    UpdateDeliveryPreferencesCommandHandler updateDeliveryPreferencesCommandHandler(
            ISubscriptionRepository subscriptionRepository,
            UnitOfWork unitOfWork) {
        return new UpdateDeliveryPreferencesCommandHandler(subscriptionRepository, unitOfWork);
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
            UnitOfWork unitOfWork) {
        return new CloseConsolidatedCalendarCommandHandler(
                dailyConsolidator, calendarRepository, unitOfWork);
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

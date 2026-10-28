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
import com.mcalvaro.mscatering.application.patient.synchronization.PatientReferenceSynchronizationStore;
import com.mcalvaro.mscatering.application.patient.synchronization.SynchronizePatientReferenceCommandHandler;
import com.mcalvaro.mscatering.application.patient.address.DeliveryAddressResolver;
import com.mcalvaro.mscatering.application.dietplan.DietPlanResolver;
import com.mcalvaro.mscatering.application.dietplan.synchronization.DietPlanSynchronizationStore;
import com.mcalvaro.mscatering.application.dietplan.synchronization.SynchronizeDietPlanCommandHandler;
import com.mcalvaro.mscatering.domain.dietplan.IDietPlanProjectionRepository;
import com.mcalvaro.mscatering.application.patient.address.synchronization.PatientAddressSynchronizationStore;
import com.mcalvaro.mscatering.application.patient.address.synchronization.SynchronizePatientAddressCommandHandler;

import com.mcalvaro.mscatering.domain.consolidatedcalendar.IConsolidatedCalendarRepository;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.service.DailyConsolidator;
import com.mcalvaro.mscatering.application.consolidatedcalendar.service.DefaultDailyConsolidator;
import com.mcalvaro.mscatering.domain.patient.IPatientReferenceRepository;
import com.mcalvaro.mscatering.domain.patient.IPatientAddressReferenceRepository;
import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.service.BiweeklyEvaluationGenerator;
import com.mcalvaro.mscatering.domain.subscription.service.DefaultBiweeklyEvaluationGenerator;
import com.mcalvaro.mscatering.domain.subscription.service.SubscriptionDuplicationValidator;
import com.mcalvaro.mscatering.application.subscription.service.DefaultSubscriptionDuplicationValidator;

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
    // Patient Synchronization Handlers
    // ========================================================================

    @Bean
    SynchronizePatientReferenceCommandHandler synchronizePatientReferenceCommandHandler(
            PatientReferenceSynchronizationStore synchronizationStore) {
        return new SynchronizePatientReferenceCommandHandler(synchronizationStore);
    }

    @Bean
    SynchronizePatientAddressCommandHandler synchronizePatientAddressCommandHandler(
            PatientAddressSynchronizationStore synchronizationStore) {
        return new SynchronizePatientAddressCommandHandler(synchronizationStore);
    }

    @Bean
    DeliveryAddressResolver deliveryAddressResolver(IPatientAddressReferenceRepository addressRepository) {
        return new DeliveryAddressResolver(addressRepository);
    }

    @Bean
    DietPlanResolver dietPlanResolver(IDietPlanProjectionRepository dietPlanRepository) {
        return new DietPlanResolver(dietPlanRepository);
    }

    @Bean
    SynchronizeDietPlanCommandHandler synchronizeDietPlanCommandHandler(DietPlanSynchronizationStore synchronizationStore) {
        return new SynchronizeDietPlanCommandHandler(synchronizationStore);
    }

    // ========================================================================
    // Subscription Command Handlers
    // ========================================================================

    @Bean
    CreateSubscriptionCommandHandler createSubscriptionCommandHandler(
            ISubscriptionRepository subscriptionRepository,
            SubscriptionDuplicationValidator duplicationValidator,
            BiweeklyEvaluationGenerator evaluationGenerator, DeliveryAddressResolver deliveryAddressResolver,
            DietPlanResolver dietPlanResolver) {
        return new CreateSubscriptionCommandHandler(
                subscriptionRepository, duplicationValidator, evaluationGenerator, deliveryAddressResolver, dietPlanResolver);
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
            ISubscriptionRepository subscriptionRepository, DeliveryAddressResolver deliveryAddressResolver) {
        return new UpdateDeliveryPreferencesCommandHandler(subscriptionRepository, deliveryAddressResolver);
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

    // ========================================================================
    // Domain & Application Services
    // ========================================================================

    @Bean
    BiweeklyEvaluationGenerator biweeklyEvaluationGenerator() {
        return new DefaultBiweeklyEvaluationGenerator();
    }

    @Bean
    SubscriptionDuplicationValidator subscriptionDuplicationValidator(
            ISubscriptionRepository subscriptionRepository,
            IPatientReferenceRepository patientRepository) {
        return new DefaultSubscriptionDuplicationValidator(subscriptionRepository, patientRepository);
    }

    @Bean
    DailyConsolidator dailyConsolidator(ISubscriptionRepository subscriptionRepository) {
        return new DefaultDailyConsolidator(subscriptionRepository);
    }
}

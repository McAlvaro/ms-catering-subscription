package com.mcalvaro.mscatering.infrastructure.persistence.subscription.mapper;

import com.mcalvaro.mscatering.domain.subscription.Subscription;
import com.mcalvaro.mscatering.domain.subscription.entity.BiweeklyEvaluation;
import com.mcalvaro.mscatering.domain.subscription.entity.DeliveryCalendar;
import com.mcalvaro.mscatering.domain.subscription.entity.DeliveryDay;
import com.mcalvaro.mscatering.domain.subscription.entity.PauseRequest;
import com.mcalvaro.mscatering.domain.subscription.enums.DeliveryDayStatus;
import com.mcalvaro.mscatering.domain.subscription.enums.EvaluationStatus;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.domain.subscription.enums.SubscriptionStatus;
import com.mcalvaro.mscatering.domain.subscription.vo.ContractCode;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryPreferences;
import com.mcalvaro.mscatering.domain.subscription.vo.PauseRange;
import com.mcalvaro.mscatering.domain.subscription.vo.ServiceContract;
import com.mcalvaro.mscatering.domain.subscription.vo.TimeWindow;
import com.mcalvaro.mscatering.domain.subscription.vo.ValidityPeriod;
import com.mcalvaro.mscatering.infrastructure.persistence.subscription.entity.BiweeklyEvaluationJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.subscription.entity.DeliveryCalendarJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.subscription.entity.DeliveryDayJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.subscription.entity.PauseRequestJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.subscription.entity.SubscriptionJpaEntity;

import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SubscriptionMapper {

    public SubscriptionJpaEntity toJpaEntity(Subscription domain) {
        if (domain == null)
            return null;

        SubscriptionJpaEntity jpa = new SubscriptionJpaEntity();
        jpa.setId(domain.getId());
        jpa.setPatientId(domain.getPatientId());
        jpa.setDietPlanId(domain.getDietPlanId());
        jpa.setContractCode(domain.getContractCode().value());
        jpa.setStatus(domain.getStatus().name());

        // Contract
        ServiceContract contract = domain.getContract();
        jpa.setContractStartDate(contract.period().startDate());
        jpa.setContractEndDate(contract.period().endDate());
        jpa.setContractDurationDays(contract.period().durationDays());
        jpa.setServiceType(contract.serviceType().name());
        jpa.setTotalPrice(contract.totalPrice());
        jpa.setAcceptedConditions(contract.acceptedConditions());
        jpa.setSignedAt(contract.signedAt());

        // Preferences
        DeliveryPreferences prefs = domain.getPreferences();
        jpa.setPrefStreet(prefs.primaryAddress().street());
        jpa.setPrefNumber(prefs.primaryAddress().number());
        jpa.setPrefCity(prefs.primaryAddress().city());
        jpa.setPrefReference(prefs.primaryAddress().reference());
        jpa.setPrefLatitude(prefs.primaryAddress().latitude());
        jpa.setPrefLongitude(prefs.primaryAddress().longitude());
        jpa.setPrefPhone(prefs.primaryAddress().phone());
        jpa.setPrefTimeStart(prefs.timeWindow().startTime().toString());
        jpa.setPrefTimeEnd(prefs.timeWindow().endTime().toString());
        jpa.setPrefSpecialInstructions(prefs.specialInstructions());

        // Delivery Calendar
        DeliveryCalendarJpaEntity calJpa = new DeliveryCalendarJpaEntity();
        calJpa.setId(domain.getDeliveryCalendar().getId());
        calJpa.setPeriodStart(domain.getDeliveryCalendar().getPeriod().startDate());
        calJpa.setPeriodEnd(domain.getDeliveryCalendar().getPeriod().endDate());

        List<DeliveryDayJpaEntity> dayJpas = domain.getDeliveryCalendar().getDeliveryDays().stream()
                .map(this::toDeliveryDayJpa)
                .collect(Collectors.toList());
        calJpa.setDeliveryDays(dayJpas);
        jpa.setDeliveryCalendar(calJpa);

        // Pause Requests
        List<PauseRequestJpaEntity> pauseJpas = domain.getPauseRequests().stream()
                .map(this::toPauseRequestJpa)
                .collect(Collectors.toList());
        jpa.setPauseRequests(pauseJpas);

        // Evaluations
        List<BiweeklyEvaluationJpaEntity> evalJpas = domain.getEvaluations().stream()
                .map(this::toEvaluationJpa)
                .collect(Collectors.toList());
        jpa.setEvaluations(evalJpas);

        return jpa;
    }

    public Subscription toDomain(SubscriptionJpaEntity jpa) {
        if (jpa == null)
            return null;

        try {
            ValidityPeriod period = new ValidityPeriod(jpa.getContractStartDate(), jpa.getContractEndDate());
            ServiceContract contract = new ServiceContract(
                    jpa.getDietPlanId(),
                    period,
                    ServiceType.valueOf(jpa.getServiceType()),
                    jpa.getTotalPrice(),
                    jpa.getAcceptedConditions(),
                    jpa.getSignedAt());

            DeliveryAddress address = new DeliveryAddress(
                    jpa.getPrefStreet(), jpa.getPrefNumber(), jpa.getPrefCity(), jpa.getPrefReference(),
                    jpa.getPrefLatitude() != null ? jpa.getPrefLatitude() : 0.0,
                    jpa.getPrefLongitude() != null ? jpa.getPrefLongitude() : 0.0,
                    jpa.getPrefPhone());
            TimeWindow window = new TimeWindow(LocalTime.parse(jpa.getPrefTimeStart()),
                    LocalTime.parse(jpa.getPrefTimeEnd()));
            DeliveryPreferences prefs = new DeliveryPreferences(address, window, jpa.getPrefSpecialInstructions());

            DeliveryCalendar calendar = new DeliveryCalendar(jpa.getDeliveryCalendar().getId(), jpa.getId(), period);
            for (DeliveryDayJpaEntity dayJpa : jpa.getDeliveryCalendar().getDeliveryDays()) {
                DeliveryDay day = toDeliveryDayDomain(dayJpa);
                calendar.addDay(day);
            }

            Subscription subscription = (Subscription) allocateInstance(Subscription.class);
            setField(Subscription.class, subscription, "domainEvents", new java.util.ArrayList<>());
            setField(Subscription.class, subscription, "id", jpa.getId());
            setField(Subscription.class, subscription, "patientId", jpa.getPatientId());
            setField(Subscription.class, subscription, "dietPlanId", jpa.getDietPlanId());
            setField(Subscription.class, subscription, "contractCode", new ContractCode(jpa.getContractCode()));
            setField(Subscription.class, subscription, "contract", contract);
            setField(Subscription.class, subscription, "preferences", prefs);
            setField(Subscription.class, subscription, "deliveryCalendar", calendar);
            setField(Subscription.class, subscription, "status", SubscriptionStatus.valueOf(jpa.getStatus()));

            List<PauseRequest> pauseRequests = jpa.getPauseRequests().stream().map(this::toPauseRequestDomain).toList();
            setField(Subscription.class, subscription, "pauseRequests", new java.util.ArrayList<>(pauseRequests));

            List<BiweeklyEvaluation> evaluations = jpa.getEvaluations().stream().map(this::toEvaluationDomain).toList();
            setField(Subscription.class, subscription, "evaluations", new java.util.ArrayList<>(evaluations));

            return subscription;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map SubscriptionJpaEntity to Domain", e);
        }
    }

    private DeliveryDayJpaEntity toDeliveryDayJpa(DeliveryDay domain) {
        DeliveryDayJpaEntity jpa = new DeliveryDayJpaEntity();
        jpa.setId(domain.getId());
        jpa.setDate(domain.getDate());
        jpa.setStatus(domain.getStatus().name());
        jpa.setAddressStreet(domain.getAddress().street());
        jpa.setAddressNumber(domain.getAddress().number());
        jpa.setAddressCity(domain.getAddress().city());
        jpa.setAddressReference(domain.getAddress().reference());
        jpa.setAddressLatitude(domain.getAddress().latitude());
        jpa.setAddressLongitude(domain.getAddress().longitude());
        jpa.setAddressPhone(domain.getAddress().phone());
        jpa.setTimeStart(domain.getTimeWindow().startTime().toString());
        jpa.setTimeEnd(domain.getTimeWindow().endTime().toString());
        jpa.setInstructions(domain.getInstructions());
        jpa.setConsolidatedAt(domain.getConsolidatedAt());
        jpa.setDeliveredAt(domain.getDeliveredAt());
        jpa.setFailureReason(domain.getFailureReason());
        return jpa;
    }

    private DeliveryDay toDeliveryDayDomain(DeliveryDayJpaEntity jpa) throws Exception {
        DeliveryDay day = (DeliveryDay) allocateInstance(DeliveryDay.class);
        setField(DeliveryDay.class, day, "id", jpa.getId());
        setField(DeliveryDay.class, day, "date", jpa.getDate());
        setField(DeliveryDay.class, day, "status", DeliveryDayStatus.valueOf(jpa.getStatus()));

        DeliveryAddress address = new DeliveryAddress(
                jpa.getAddressStreet(), jpa.getAddressNumber(), jpa.getAddressCity(), jpa.getAddressReference(),
                jpa.getAddressLatitude() != null ? jpa.getAddressLatitude() : 0.0,
                jpa.getAddressLongitude() != null ? jpa.getAddressLongitude() : 0.0,
                jpa.getAddressPhone());
        setField(DeliveryDay.class, day, "address", address);

        TimeWindow window = new TimeWindow(LocalTime.parse(jpa.getTimeStart()), LocalTime.parse(jpa.getTimeEnd()));
        setField(DeliveryDay.class, day, "timeWindow", window);
        setField(DeliveryDay.class, day, "instructions", jpa.getInstructions());
        setField(DeliveryDay.class, day, "consolidatedAt", jpa.getConsolidatedAt());
        setField(DeliveryDay.class, day, "deliveredAt", jpa.getDeliveredAt());
        setField(DeliveryDay.class, day, "failureReason", jpa.getFailureReason());
        return day;
    }

    private PauseRequestJpaEntity toPauseRequestJpa(PauseRequest domain) {
        PauseRequestJpaEntity jpa = new PauseRequestJpaEntity();
        jpa.setId(domain.getId());
        jpa.setRangeStart(domain.getRange().startDate());
        jpa.setRangeEnd(domain.getRange().endDate());
        jpa.setReason(domain.getReason());
        jpa.setActualEndDate(domain.getActualEndDate());
        jpa.setCreatedAt(domain.getCreatedAt());
        jpa.setActive(domain.isActive());
        return jpa;
    }

    private PauseRequest toPauseRequestDomain(PauseRequestJpaEntity jpa) {
        try {
            PauseRequest req = (PauseRequest) allocateInstance(PauseRequest.class);
            setField(PauseRequest.class, req, "id", jpa.getId());
            setField(PauseRequest.class, req, "range", new PauseRange(jpa.getRangeStart(), jpa.getRangeEnd()));
            setField(PauseRequest.class, req, "reason", jpa.getReason());
            setField(PauseRequest.class, req, "actualEndDate", jpa.getActualEndDate());
            setField(PauseRequest.class, req, "createdAt", jpa.getCreatedAt());
            setField(PauseRequest.class, req, "active", jpa.isActive());
            return req;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private BiweeklyEvaluationJpaEntity toEvaluationJpa(BiweeklyEvaluation domain) {
        BiweeklyEvaluationJpaEntity jpa = new BiweeklyEvaluationJpaEntity();
        jpa.setId(domain.getId());
        jpa.setPatientId(domain.getPatientId());
        jpa.setEvaluationNumber(domain.getEvaluationNumber());
        jpa.setScheduledDate(domain.getScheduledDate());
        jpa.setStatus(domain.getStatus().name());
        jpa.setCompletedDate(domain.getCompletedDate());
        return jpa;
    }

    private BiweeklyEvaluation toEvaluationDomain(BiweeklyEvaluationJpaEntity jpa) {
        try {
            BiweeklyEvaluation eval = (BiweeklyEvaluation) allocateInstance(BiweeklyEvaluation.class);
            setField(BiweeklyEvaluation.class, eval, "id", jpa.getId());
            setField(BiweeklyEvaluation.class, eval, "patientId", jpa.getPatientId());
            setField(BiweeklyEvaluation.class, eval, "evaluationNumber", jpa.getEvaluationNumber());
            setField(BiweeklyEvaluation.class, eval, "scheduledDate", jpa.getScheduledDate());
            setField(BiweeklyEvaluation.class, eval, "status", EvaluationStatus.valueOf(jpa.getStatus()));
            setField(BiweeklyEvaluation.class, eval, "completedDate", jpa.getCompletedDate());
            return eval;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Class<?> clazz, Object instance, String fieldName, Object value) throws Exception {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(instance, value);
                return;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field " + fieldName + " not found in " + clazz.getName());
    }

    private Object allocateInstance(Class<?> clazz) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field f = unsafeClass.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Object unsafe = f.get(null);
        java.lang.reflect.Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        return allocateInstance.invoke(unsafe, clazz);
    }
}

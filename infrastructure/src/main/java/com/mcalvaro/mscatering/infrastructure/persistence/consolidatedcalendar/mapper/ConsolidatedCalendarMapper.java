package com.mcalvaro.mscatering.infrastructure.persistence.consolidatedcalendar.mapper;

import com.mcalvaro.mscatering.domain.consolidatedcalendar.ConsolidatedCalendar;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.entity.ConsolidatedLine;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.enums.ConsolidateStatus;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.domain.subscription.vo.TimeWindow;
import com.mcalvaro.mscatering.infrastructure.persistence.consolidatedcalendar.entity.ConsolidatedCalendarJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.consolidatedcalendar.entity.ConsolidatedLineJpaEntity;

import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ConsolidatedCalendarMapper {

    public ConsolidatedCalendarJpaEntity toJpaEntity(ConsolidatedCalendar domain) {
        if (domain == null)
            return null;

        ConsolidatedCalendarJpaEntity jpa = new ConsolidatedCalendarJpaEntity();
        jpa.setId(domain.getId());
        jpa.setDate(domain.getDate());
        jpa.setStatus(domain.getStatus().name());
        jpa.setTotalDeliveries(domain.getTotalDeliveries());
        jpa.setClosedAt(domain.getClosedAt());
        jpa.setClosedBy(domain.getClosedBy());

        List<ConsolidatedLineJpaEntity> lineJpas = domain.getLines().stream()
                .map(this::toLineJpa)
                .collect(Collectors.toList());
        jpa.setLines(lineJpas);

        return jpa;
    }

    public ConsolidatedCalendar toDomain(ConsolidatedCalendarJpaEntity jpa) {
        if (jpa == null)
            return null;

        try {
            ConsolidatedCalendar calendar = (ConsolidatedCalendar) allocateInstance(ConsolidatedCalendar.class);
            setField(ConsolidatedCalendar.class, calendar, "domainEvents", new java.util.ArrayList<>());
            setField(ConsolidatedCalendar.class, calendar, "id", jpa.getId());
            setField(ConsolidatedCalendar.class, calendar, "date", jpa.getDate());
            setField(ConsolidatedCalendar.class, calendar, "status", ConsolidateStatus.valueOf(jpa.getStatus()));
            setField(ConsolidatedCalendar.class, calendar, "totalDeliveries", jpa.getTotalDeliveries());
            setField(ConsolidatedCalendar.class, calendar, "closedAt", jpa.getClosedAt());
            setField(ConsolidatedCalendar.class, calendar, "closedBy", jpa.getClosedBy());

            List<ConsolidatedLine> lines = jpa.getLines().stream().map(this::toLineDomain).toList();
            setField(ConsolidatedCalendar.class, calendar, "lines", new java.util.ArrayList<>(lines));

            return calendar;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map ConsolidatedCalendarJpaEntity to Domain", e);
        }
    }

    private ConsolidatedLineJpaEntity toLineJpa(ConsolidatedLine domain) {
        ConsolidatedLineJpaEntity jpa = new ConsolidatedLineJpaEntity();
        jpa.setId(domain.getId());
        jpa.setSubscriptionId(domain.getSubscriptionId());
        jpa.setPatientId(domain.getPatientId());
        jpa.setDietPlanId(domain.getDietPlanId());
        jpa.setServiceType(domain.getServiceType().name());

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

        return jpa;
    }

    private ConsolidatedLine toLineDomain(ConsolidatedLineJpaEntity jpa) {
        try {
            ConsolidatedLine line = (ConsolidatedLine) allocateInstance(ConsolidatedLine.class);
            setField(ConsolidatedLine.class, line, "id", jpa.getId());
            setField(ConsolidatedLine.class, line, "consolidatedCalendarId", jpa.getConsolidatedCalendar().getId());
            setField(ConsolidatedLine.class, line, "subscriptionId", jpa.getSubscriptionId());
            setField(ConsolidatedLine.class, line, "patientId", jpa.getPatientId());
            setField(ConsolidatedLine.class, line, "dietPlanId", jpa.getDietPlanId());
            setField(ConsolidatedLine.class, line, "serviceType", ServiceType.valueOf(jpa.getServiceType()));

            DeliveryAddress address = new DeliveryAddress(
                    jpa.getAddressStreet(), jpa.getAddressNumber(), jpa.getAddressCity(), jpa.getAddressReference(),
                    jpa.getAddressLatitude() != null ? jpa.getAddressLatitude() : 0.0,
                    jpa.getAddressLongitude() != null ? jpa.getAddressLongitude() : 0.0,
                    jpa.getAddressPhone());
            setField(ConsolidatedLine.class, line, "address", address);

            TimeWindow window = new TimeWindow(LocalTime.parse(jpa.getTimeStart()), LocalTime.parse(jpa.getTimeEnd()));
            setField(ConsolidatedLine.class, line, "timeWindow", window);
            setField(ConsolidatedLine.class, line, "instructions", jpa.getInstructions());

            return line;
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

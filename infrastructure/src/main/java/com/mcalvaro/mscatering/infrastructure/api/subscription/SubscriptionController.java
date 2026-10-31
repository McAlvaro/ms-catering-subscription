package com.mcalvaro.mscatering.infrastructure.api.subscription;

import an.awesome.pipelinr.Pipeline;
import com.mcalvaro.mscatering.application.subscription.CancelSubscription.CancelSubscriptionCommand;
import com.mcalvaro.mscatering.application.subscription.CompleteSubscription.CompleteSubscriptionCommand;
import com.mcalvaro.mscatering.application.subscription.ConfirmDelivery.ConfirmDeliveryCommand;
import com.mcalvaro.mscatering.application.subscription.CreateSubscription.CreateSubscriptionCommand;
import com.mcalvaro.mscatering.application.subscription.GetSubscriptionDetails.GetSubscriptionDetailsQuery;
import com.mcalvaro.mscatering.application.subscription.GetSubscriptionDetails.SubscriptionDetailsDto;
import com.mcalvaro.mscatering.application.subscription.MarkEvaluationCompleted.MarkEvaluationCompletedCommand;
import com.mcalvaro.mscatering.application.subscription.MarkNoDelivery.MarkNoDeliveryCommand;
import com.mcalvaro.mscatering.application.subscription.ModifyDeliveryDay.ModifyDeliveryDayCommand;
import com.mcalvaro.mscatering.application.subscription.PauseSubscription.PauseSubscriptionCommand;
import com.mcalvaro.mscatering.application.subscription.ReactivateSubscription.ReactivateSubscriptionCommand;
import com.mcalvaro.mscatering.application.subscription.RegisterFailedDelivery.RegisterFailedDeliveryCommand;
import com.mcalvaro.mscatering.application.subscription.UpdateDeliveryPreferences.UpdateDeliveryPreferencesCommand;
import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.core.id.DeliveryDayId;
import com.mcalvaro.mscatering.domain.core.id.EvaluationId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;
import com.mcalvaro.mscatering.infrastructure.api.CorrelationIdFilter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final Pipeline pipeline;

    public SubscriptionController(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    @PostMapping
    public ResponseEntity<SubscriptionId> create(@RequestBody CreateSubscriptionCommand command) {
        if (command.patientId() == null || command.dietPlanId() == null || command.deliveryAddressId() == null) {
            throw new DomainException("INVALID_PAYLOAD", "A patient, plan and delivery address are required");
        }
        SubscriptionId resultId = pipeline.send(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionDetailsDto> getDetails(@PathVariable String id) {
        SubscriptionDetailsDto dto = pipeline.send(new GetSubscriptionDetailsQuery(SubscriptionId.of(id)));
        if (dto == null) {
            throw new IllegalArgumentException("Subscription not found: " + id);
        }
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<Void> pause(@PathVariable String id, @RequestBody PauseSubscriptionCommand body,
            @RequestAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE) String correlationId) {
        pipeline.send(new PauseSubscriptionCommand(SubscriptionId.of(id), body.startDate(), body.endDate(), body.reason(), correlationId));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivate(
            @PathVariable String id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reactivationDate,
            @RequestAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE) String correlationId) {
        pipeline.send(new ReactivateSubscriptionCommand(SubscriptionId.of(id), reactivationDate, correlationId));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/delivery-days/{dayId}")
    public ResponseEntity<Void> modifyDeliveryDay(
            @PathVariable String id,
            @PathVariable String dayId,
            @RequestBody ModifyDeliveryDayCommand body) {
        pipeline.send(new ModifyDeliveryDayCommand(
                SubscriptionId.of(id), DeliveryDayId.of(dayId),
                body.street(), body.number(), body.city(), body.reference(),
                body.latitude(), body.longitude(), body.phone(),
                body.startTime(), body.endTime(), body.instructions()));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/delivery-days/{dayId}/no-delivery")
    public ResponseEntity<Void> markNoDelivery(@PathVariable String id, @PathVariable String dayId) {
        pipeline.send(new MarkNoDeliveryCommand(SubscriptionId.of(id), DeliveryDayId.of(dayId)));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/delivery-days/{dayId}/confirm")
    public ResponseEntity<Void> confirmDelivery(@PathVariable String id, @PathVariable String dayId) {
        pipeline.send(new ConfirmDeliveryCommand(SubscriptionId.of(id), DeliveryDayId.of(dayId)));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/delivery-days/{dayId}/fail")
    public ResponseEntity<Void> registerFailedDelivery(
            @PathVariable String id,
            @PathVariable String dayId,
            @RequestParam String reason) {
        pipeline.send(new RegisterFailedDeliveryCommand(SubscriptionId.of(id), DeliveryDayId.of(dayId), reason));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/evaluations/{evalId}/complete")
    public ResponseEntity<Void> markEvaluationCompleted(
            @PathVariable String id,
            @PathVariable String evalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate completedAt) {
        pipeline.send(new MarkEvaluationCompletedCommand(SubscriptionId.of(id), EvaluationId.of(evalId), completedAt));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Void> complete(@PathVariable String id,
            @RequestAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE) String correlationId) {
        pipeline.send(new CompleteSubscriptionCommand(SubscriptionId.of(id), correlationId));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable String id, @RequestParam String reason,
            @RequestAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE) String correlationId) {
        pipeline.send(new CancelSubscriptionCommand(SubscriptionId.of(id), reason, correlationId));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/preferences")
    public ResponseEntity<Void> updatePreferences(
            @PathVariable String id,
            @RequestBody UpdateDeliveryPreferencesCommand body) {
        if (body.deliveryAddressId() == null) {
            throw new DomainException("INVALID_PAYLOAD", "A delivery address is required");
        }
        pipeline.send(new UpdateDeliveryPreferencesCommand(
                SubscriptionId.of(id),
                body.deliveryAddressId(),
                body.startTime(), body.endTime(), body.specialInstructions()));
        return ResponseEntity.ok().build();
    }
}

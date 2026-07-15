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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final Pipeline pipeline;

    public SubscriptionController(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    @PostMapping
    public ResponseEntity<UUID> create(@RequestBody CreateSubscriptionCommand command) {
        UUID resultId = pipeline.send(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionDetailsDto> getDetails(@PathVariable UUID id) {
        SubscriptionDetailsDto dto = pipeline.send(new GetSubscriptionDetailsQuery(id));
        if (dto == null) {
            throw new IllegalArgumentException("Subscription not found: " + id);
        }
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<Void> pause(@PathVariable UUID id, @RequestBody PauseSubscriptionCommand body) {
        pipeline.send(new PauseSubscriptionCommand(id, body.startDate(), body.endDate(), body.reason()));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivate(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reactivationDate) {
        pipeline.send(new ReactivateSubscriptionCommand(id, reactivationDate));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/delivery-days/{dayId}")
    public ResponseEntity<Void> modifyDeliveryDay(
            @PathVariable UUID id,
            @PathVariable UUID dayId,
            @RequestBody ModifyDeliveryDayCommand body) {
        pipeline.send(new ModifyDeliveryDayCommand(
                id, dayId,
                body.street(), body.number(), body.city(), body.reference(),
                body.latitude(), body.longitude(), body.phone(),
                body.startTime(), body.endTime(), body.instructions()));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/delivery-days/{dayId}/no-delivery")
    public ResponseEntity<Void> markNoDelivery(@PathVariable UUID id, @PathVariable UUID dayId) {
        pipeline.send(new MarkNoDeliveryCommand(id, dayId));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/delivery-days/{dayId}/confirm")
    public ResponseEntity<Void> confirmDelivery(@PathVariable UUID id, @PathVariable UUID dayId) {
        pipeline.send(new ConfirmDeliveryCommand(id, dayId));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/delivery-days/{dayId}/fail")
    public ResponseEntity<Void> registerFailedDelivery(
            @PathVariable UUID id,
            @PathVariable UUID dayId,
            @RequestParam String reason) {
        pipeline.send(new RegisterFailedDeliveryCommand(id, dayId, reason));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/evaluations/{evalId}/complete")
    public ResponseEntity<Void> markEvaluationCompleted(
            @PathVariable UUID id,
            @PathVariable UUID evalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate completedAt) {
        pipeline.send(new MarkEvaluationCompletedCommand(id, evalId, completedAt));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Void> complete(@PathVariable UUID id) {
        pipeline.send(new CompleteSubscriptionCommand(id));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable UUID id, @RequestParam String reason) {
        pipeline.send(new CancelSubscriptionCommand(id, reason));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/preferences")
    public ResponseEntity<Void> updatePreferences(
            @PathVariable UUID id,
            @RequestBody UpdateDeliveryPreferencesCommand body) {
        pipeline.send(new UpdateDeliveryPreferencesCommand(
                id,
                body.street(), body.number(), body.city(), body.reference(),
                body.latitude(), body.longitude(), body.phone(),
                body.startTime(), body.endTime(), body.specialInstructions()));
        return ResponseEntity.ok().build();
    }
}

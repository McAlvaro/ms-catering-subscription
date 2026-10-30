package com.mcalvaro.mscatering.infrastructure.outbox;

import com.mcalvaro.mscatering.application.outbox.OutboxMessage;
import com.mcalvaro.mscatering.application.outbox.OutboxMessageRepository;
import com.mcalvaro.mscatering.infrastructure.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxRelayDisabledIT extends BaseIntegrationTest {
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private OutboxMessageRepository repository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void disabledDefaultHasNoRelayProcessorOrSchedulingAndLeavesPendingRowsUntouched() {
        OutboxMessage message = OutboxMessage.pending(UUID.randomUUID(), "test.disabled-pending", "{\"pending\":true}",
                Instant.now().minusSeconds(1));
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.executeWithoutResult(status -> repository.save(message));

        assertThat(applicationContext.getBeansOfType(OutboxProcessor.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(OutboxRelay.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(ScheduledAnnotationBeanPostProcessor.class)).isEmpty();
        OutboxMessage persisted = transaction.execute(status -> repository.findById(message.id()).orElseThrow());
        assertThat(persisted)
                .extracting(OutboxMessage::processed, OutboxMessage::attemptCount, OutboxMessage::lockedBy,
                        OutboxMessage::lockedUntil)
                .containsExactly(false, 0, null, null);
    }
}

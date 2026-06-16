package dev.confera.shared.outbox;

import dev.confera.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private static final int MAX_ATTEMPTS = 3;

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ApplicationContext ctx;

    @Scheduled(fixedDelay = 5000)
    public void poll() {
        List<OutboxEvent> pending = outboxEventRepository.findUnpublished();
        if (!pending.isEmpty()) {
            log.debug("OutboxPoller: {} event(s) to publish", pending.size());
        }
        for (OutboxEvent event : pending) {
            ctx.getBean(OutboxPoller.class).publish(event);
        }
    }

    @Transactional
    public void publish(OutboxEvent event) {
        try {
            String routingKey = event.getRoutingKey() != null
                ? event.getRoutingKey()
                : event.getEventType().toLowerCase().replace("_", ".");

            Message message = MessageBuilder
                .withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .build();

            rabbitTemplate.send(RabbitConfig.EXCHANGE, routingKey, message);

            event.setPublished(true);
            event.setPublishedAt(LocalDateTime.now());
            log.debug("OutboxPoller: published event {} ({})", event.getId(), event.getEventType());
        } catch (Exception e) {
            int attempts = event.getAttempts() + 1;
            event.setAttempts(attempts);
            log.warn("OutboxPoller: failed to publish event {} (attempt {}/{}): {}",
                event.getId(), attempts, MAX_ATTEMPTS, e.getMessage());
        }
        outboxEventRepository.save(event);
    }
}
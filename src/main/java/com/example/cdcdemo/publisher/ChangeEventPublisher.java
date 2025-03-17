package com.example.cdcdemo.publisher;


import com.example.cdcdemo.config.RabbitMQConfig;
import com.example.cdcdemo.model.DataChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChangeEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishEvent(String entityType, Long entityId, String operation, Object data) {
        DataChangeEvent event = new DataChangeEvent(
                entityType,
                entityId,
                operation,
                data,
                LocalDateTime.now()
        );

        log.info("Publishing data change event: {}", event);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                event
        );
    }
}
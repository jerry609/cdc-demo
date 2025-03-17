package com.example.cdcdemo.listener;

import com.example.cdcdemo.model.Customer;
import com.example.cdcdemo.model.DataChangeEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class DatabaseChangeListener {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String CACHE_KEY_CUSTOMER = "customer:";

    @RabbitListener(queues = "cdc.queue")
    public void processDataChangeEvent(DataChangeEvent event) {
        log.info("Received data change event: {}", event);

        // 处理不同类型的数据变更事件
        if ("customer".equals(event.getEntityType())) {
            switch (event.getOperation()) {
                case "CREATE":
                    log.info("Processing CREATE event for Customer with ID: {}", event.getEntityId());
                    handleCustomerCreated(event);
                    break;
                case "UPDATE":
                    log.info("Processing UPDATE event for Customer with ID: {}", event.getEntityId());
                    handleCustomerUpdated(event);
                    break;
                case "DELETE":
                    log.info("Processing DELETE event for Customer with ID: {}", event.getEntityId());
                    handleCustomerDeleted(event);
                    break;
                default:
                    log.warn("Unknown operation type: {}", event.getOperation());
            }
        } else {
            log.info("Received event for entity type: {}", event.getEntityType());
        }
    }

    private void handleCustomerCreated(DataChangeEvent event) {
        // 从 LinkedHashMap 转换为 Customer 对象
        Customer customer = objectMapper.convertValue(event.getData(), Customer.class);
        log.info("Syncing created customer to other systems: {}", customer);

        // 这里可以添加同步到其他系统的逻辑
        // 例如: 发送通知、更新搜索索引等
    }

    private void handleCustomerUpdated(DataChangeEvent event) {
        // 从 LinkedHashMap 转换为 Customer 对象
        Customer customer = objectMapper.convertValue(event.getData(), Customer.class);
        log.info("Syncing updated customer to other systems: {}", customer);

        // 这里可以添加同步到其他系统的逻辑
        // 例如: 发送通知、更新搜索索引等
    }

    private void handleCustomerDeleted(DataChangeEvent event) {
        // 从 LinkedHashMap 转换为 Customer 对象
        Customer customer = objectMapper.convertValue(event.getData(), Customer.class);
        log.info("Syncing deleted customer to other systems: {}", customer);

        // 确保 Redis 缓存中的数据已删除
        redisTemplate.delete(CACHE_KEY_CUSTOMER + event.getEntityId());

        // 这里可以添加同步到其他系统的逻辑
        // 例如: 发送通知、更新搜索索引等
    }
}
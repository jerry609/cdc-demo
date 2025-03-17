package com.example.cdcdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.example.cdcdemo.mapper.CustomerMapper;
import com.example.cdcdemo.model.Customer;
import com.example.cdcdemo.publisher.ChangeEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService extends ServiceImpl<CustomerMapper, Customer> {

    private final CustomerMapper customerMapper;
    private final ChangeEventPublisher eventPublisher;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY_CUSTOMER = "customer:";
    private static final String CACHE_KEY_ALL_CUSTOMERS = "allCustomers";

    @Cacheable(cacheNames = "customers", key = "'all'")
    public List<Customer> getAllCustomers() {
        log.info("Fetching all customers from database");
        return customerMapper.selectList(null);
    }

    @Cacheable(cacheNames = "customers", key = "#id")
    public Optional<Customer> getCustomerById(Long id) {
        log.info("Fetching customer with id: {} from database", id);
        Customer customer = customerMapper.selectById(id);
        return Optional.ofNullable(customer);
    }

    @Transactional
    @CacheEvict(cacheNames = "customers", key = "'all'")
    public Customer createCustomer(Customer customer) {
        log.info("Creating new customer");
        customerMapper.insert(customer);

        // 保存到 Redis 缓存
        redisTemplate.opsForValue().set(
                CACHE_KEY_CUSTOMER + customer.getId(),
                customer,
                30,
                TimeUnit.MINUTES
        );

        // 清除列表缓存
        redisTemplate.delete(CACHE_KEY_ALL_CUSTOMERS);

        // 发布创建事件
        eventPublisher.publishEvent(
                "customer",
                customer.getId(),
                "CREATE",
                customer
        );

        return customer;
    }

    @Transactional
    @CachePut(cacheNames = "customers", key = "#id")
    @CacheEvict(cacheNames = "customers", key = "'all'")
    public Customer updateCustomer(Long id, Customer customer) {
        log.info("Updating customer with id: {}", id);
        Customer existingCustomer = customerMapper.selectById(id);

        if (existingCustomer != null) {
            customer.setId(id);
            customerMapper.updateById(customer);

            // 更新 Redis 缓存
            redisTemplate.opsForValue().set(
                    CACHE_KEY_CUSTOMER + customer.getId(),
                    customer,
                    30,
                    TimeUnit.MINUTES
            );

            // 清除列表缓存
            redisTemplate.delete(CACHE_KEY_ALL_CUSTOMERS);

            // 发布更新事件
            eventPublisher.publishEvent(
                    "customer",
                    customer.getId(),
                    "UPDATE",
                    customer
            );

            return customer;
        } else {
            throw new RuntimeException("Customer not found with id: " + id);
        }
    }

    @Transactional
    @CacheEvict(cacheNames = "customers", allEntries = true)
    public void deleteCustomer(Long id) {
        log.info("Deleting customer with id: {}", id);
        Customer existingCustomer = customerMapper.selectById(id);

        if (existingCustomer != null) {
            customerMapper.deleteById(id);

            // 从 Redis 缓存中删除
            redisTemplate.delete(CACHE_KEY_CUSTOMER + id);
            redisTemplate.delete(CACHE_KEY_ALL_CUSTOMERS);

            // 发布删除事件
            eventPublisher.publishEvent(
                    "customer",
                    id,
                    "DELETE",
                    existingCustomer
            );
        } else {
            throw new RuntimeException("Customer not found with id: " + id);
        }
    }
}
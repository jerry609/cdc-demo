package com.example.cdcdemo.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.example.cdcdemo.model.Customer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {
}
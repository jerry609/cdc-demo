package com.example.cdcdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.cdcdemo.model.integration.IntegrationJob;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IntegrationJobMapper extends BaseMapper<IntegrationJob> {
}
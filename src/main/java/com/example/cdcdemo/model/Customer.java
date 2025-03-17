package com.example.cdcdemo.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@TableName("customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;
    private String email;
    private String phone;
    private String address;
}

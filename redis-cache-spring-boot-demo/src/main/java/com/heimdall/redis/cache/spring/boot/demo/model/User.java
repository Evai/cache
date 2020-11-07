package com.heimdall.redis.cache.spring.boot.demo.model;

import com.heimdall.redis.cache.spring.boot.starter.IEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author crh
 * @since 2020/10/28
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User implements IEntity {

    private Long id;

    private String name;

}

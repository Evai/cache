package com.heimdall.redis.cache.spring.boot.demo.controller;

import com.heimdall.redis.cache.core.CacheAction;
import com.heimdall.redis.cache.core.annotation.CacheAble;
import com.heimdall.redis.cache.core.annotation.CacheAbleEntity;
import com.heimdall.redis.cache.spring.boot.demo.model.User;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author crh
 * @date 2019-09-20
 * @description
 */
@RestController
@RequestMapping("/test")
@AllArgsConstructor
public class TestController {

    @RequestMapping("/cache")
    @CacheAble(key = "'name:' + #name")
    public User cache(String name) {
       return new User(name);
    }

    @RequestMapping("/entity")
    @CacheAbleEntity(key = "#name", action = CacheAction.SELECT)
    public User entity(Long id) {
        if (id == null) {
            return null;
        }
        return new User("jack");
    }

}

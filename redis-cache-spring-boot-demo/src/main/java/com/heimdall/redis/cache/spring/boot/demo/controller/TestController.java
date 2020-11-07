package com.heimdall.redis.cache.spring.boot.demo.controller;

import com.heimdall.redis.cache.core.annotation.CacheAble;
import com.heimdall.redis.cache.core.annotation.CachePut;
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
    @CacheAble
    public String cache(Long id, String name) {
        return id + name;
    }

    @RequestMapping("/entity")
    @CacheAble
    public User entity(Long id) {
        if (id == null) {
            return null;
        }
        return new User(id, "jack");
    }

    @RequestMapping("/insert")
    @CachePut
    public boolean insert(User user) {
        return true;
    }

}

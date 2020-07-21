package net.commchina.platform.gateway.common;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * @description: platform-gateway
 * @author: hengxiaokang
 * @time 2020/7/21 11:37
 */
@Configuration
public class CacheConfig {

    @Bean
    public Cache<String, Object> cache(){
        Cache<String, Object> build = Caffeine.newBuilder().initialCapacity(10).maximumSize(1000).expireAfterWrite(30, TimeUnit.MINUTES).build();
        return build;
    }
}

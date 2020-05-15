package net.commchina.platform.gateway.handler.captcha.impl;

import lombok.extern.slf4j.Slf4j;
import net.commchina.platform.gateway.handler.captcha.CaptchaCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 对于分布式部署的应用，我们建议应用自己实现CaptchaCacheService，比如用Redis，参考service/spring-boot代码示例。
 * 如果应用是单点的，也没有使用redis，那默认使用内存。
 * 内存缓存只适合单节点部署的应用，否则验证码生产与验证在节点之间信息不同步，导致失败。
 *
 * @Title: 默认使用内存当缓存
 * @date 2020-05-12
 */
@Slf4j
@Service(value = "captchaCacheServiceMemImpl")
public class CaptchaCacheServiceMemImpl implements CaptchaCacheService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public void set(String key, String value, long expiresInSeconds)
    {
        redisTemplate.opsForValue().set(key, value, expiresInSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean exists(String key)
    {
        return redisTemplate.hasKey(key);
    }

    @Override
    public void delete(String key)
    {
        redisTemplate.delete(key);
    }

    @Override
    public String get(String key)
    {
        return redisTemplate.opsForValue().get(key);
    }
}

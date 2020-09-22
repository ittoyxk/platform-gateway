package net.commchina.platform.gateway.handler.captcha.impl;

import cn.hutool.crypto.symmetric.AES;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import net.commchina.platform.gateway.handler.captcha.CaptchaCacheService;
import net.commchina.platform.gateway.handler.captcha.CaptchaVerifyHandler;
import net.commchina.platform.gateway.response.APIResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.awt.*;

/**
 * @description: platform-gateway
 * @author: hengxiaokang
 * @time 2020/5/14 17:35
 */
@Slf4j
@Component
@RefreshScope
public class CaptchaVerifyHandlerImpl implements CaptchaVerifyHandler {

    @Autowired
    private CaptchaCacheService captchaCacheService;

    @Value("${security.encode.key:1234567812345678}")
    private String aesKey;

    @Value("${captcha.slip.offset:5}")
    private String slipOffset;

    @Value("${auth.core.imagecode.timeout:60}")
    private Long timeOut;
    /**
     * check校验坐标
     */
    protected static String REDIS_CAPTCHA_KEY = "gateway:captcha:running:captcha:";
    /**
     * 后台二次校验坐标
     */
    protected static String REDIS_SECOND_CAPTCHA_KEY = "gateway:captcha:running:captcha:second:";

    /**
     * 核对验证码(前端)
     *
     * @param serverRequest
     * @return
     */
    @Override
    public Mono<ServerResponse> handle(ServerRequest serverRequest)
    {
        return ServerResponse
                .status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(check(serverRequest)));
    }

    public APIResponse check(ServerRequest serverRequest)
    {
        String token = serverRequest.queryParam("token").get();
        String pointJson = serverRequest.queryParam("pointJson").get();
        //取坐标信息
        String codeKey = REDIS_CAPTCHA_KEY + token;
        if (!captchaCacheService.exists(codeKey)) {
            return APIResponse.error("验证码已失效，请重新获取");
        }
        String s = captchaCacheService.get(codeKey);
        //验证码只用一次，即刻失效
        captchaCacheService.delete(codeKey);
        Point point = null;
        Point point1 = null;
        try {
            point = JSONObject.parseObject(s, Point.class);
            //aes解密
            AES aes = new AES(aesKey.getBytes());
            pointJson = aes.decryptStr(pointJson);
            point1 = JSONObject.parseObject(pointJson, Point.class);
        } catch (Exception e) {
            log.error("验证码坐标解析失败:{}", e);
            return APIResponse.error("验证码坐标解析失败");
        }
        if (point.x - Integer.parseInt(slipOffset) > point1.x || point1.x > point.x + Integer.parseInt(slipOffset) || point.y != point1.y) {
            return APIResponse.error("验证失败");
        }
        //校验成功，将信息存入redis
        String secondKey = REDIS_SECOND_CAPTCHA_KEY + token;
        captchaCacheService.set(secondKey, pointJson, timeOut);
        return APIResponse.success(true);
    }
}

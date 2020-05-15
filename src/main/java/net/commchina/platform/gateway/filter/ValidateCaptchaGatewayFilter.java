package net.commchina.platform.gateway.filter;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.symmetric.AES;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.commchina.platform.gateway.exception.ValidateCodeException;
import net.commchina.platform.gateway.handler.captcha.CaptchaCacheService;
import net.commchina.platform.gateway.response.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

/**
 * @author: hengxiaokang
 * @time 2019/11/20 15:14
 * 验证码处理
 */
@Slf4j
@Component
public class ValidateCaptchaGatewayFilter extends AbstractGatewayFilterFactory {

    private final CaptchaCacheService captchaCacheService;

    @Value("${security.encode.key:1234567812345678}")
    private String aesKey;
    private static final String REDIS_SECOND_CAPTCHA_KEY = "gateway:captcha:running:captcha:second:";

    public ValidateCaptchaGatewayFilter(CaptchaCacheService captchaCacheService){
        this.captchaCacheService=captchaCacheService;
    }

    @Override
    public GatewayFilter apply(Object config)
    {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // 不是登录请求，直接向下执行
            if (!StrUtil.containsAnyIgnoreCase(request.getURI().getPath(), "/oauth/token")) {
                return chain.filter(exchange);
            }

            // 刷新token，直接向下执行
            String grantType = request.getQueryParams().getFirst("grant_type");
            if (StrUtil.equals("refresh_token", grantType)) {
                return chain.filter(exchange);
            }

            try {
                //校验验证码
                checkCode(request);
            } catch (Exception e) {
                String message = e.getMessage();
                return ResponseEntity.errorResult(exchange.getResponse(), HttpStatus.PRECONDITION_REQUIRED, message);
            }
            return chain.filter(exchange);
        };
    }

    /**
     * 检查code
     *
     * @param request
     */
    @SneakyThrows
    private void checkCode(ServerHttpRequest request)
    {
        String token = request.getQueryParams().getFirst("token");

        if (StrUtil.isBlank(token)) {
            throw new ValidateCodeException("验证坐标token不能为空");
        }

        String pointJson = request.getQueryParams().getFirst("pointJson");
        if (StrUtil.isBlank(pointJson)) {
            throw new ValidateCodeException("验证坐标不能为空");
        }

        //取坐标信息
        String codeKey = REDIS_SECOND_CAPTCHA_KEY + token;
        if (!captchaCacheService.exists(codeKey)) {
            throw new ValidateCodeException("验证码已失效,请重新获取");
        }

        try {
            AES aes = new AES(aesKey.getBytes());
            pointJson = aes.decryptStr(pointJson);
        } catch (Exception e) {
            log.error("验证码坐标解析失败:{}",e);
            throw new ValidateCodeException("验证码坐标解析失败");
        }
        String redisData = captchaCacheService.get(codeKey);
        //二次校验取值后，即刻失效
        captchaCacheService.delete(codeKey);
        if (!pointJson.equals(redisData)) {
            throw new ValidateCodeException("验证失败");
        }
    }

}

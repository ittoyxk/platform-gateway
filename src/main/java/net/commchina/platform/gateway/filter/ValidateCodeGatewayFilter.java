package net.commchina.platform.gateway.filter;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.commchina.platform.gateway.common.HttpUtils;
import net.commchina.platform.gateway.exception.ValidateCodeException;
import net.commchina.platform.gateway.response.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author: hengxiaokang
 * @time 2019/11/20 15:14
 * 验证码处理
 */
@Slf4j
@Component
@RefreshScope
@AllArgsConstructor
public class ValidateCodeGatewayFilter extends AbstractGatewayFilterFactory {

    private final StringRedisTemplate redisTemplate;

    @Value("${client.white.list}")
    private List<String> clientIdWhiteList;

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

            String clientId = HttpUtils.getClientId(request);
            log.info("clientId:{}", clientId);
            if (clientId != null && clientIdWhiteList.contains(clientId)) {
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
        String code = request.getQueryParams().getFirst("code");

        if (StrUtil.isBlank(code)) {
            throw new ValidateCodeException("验证码不能为空");
        }

        String randomStr = request.getQueryParams().getFirst("randomStr");
        if (StrUtil.isBlank(randomStr)) {
            randomStr = request.getQueryParams().getFirst("mobile");
        }

        String key = "auth:core:imagecode:" + randomStr;
        if (!redisTemplate.hasKey(key)) {
            throw new ValidateCodeException("验证码已失效,请重新获取");
        }

        Object codeObj = redisTemplate.opsForValue().get(key);

        if (codeObj == null) {
            throw new ValidateCodeException("验证码已失效,请重新获取");
        }

        String saveCode = codeObj.toString();
        if (StrUtil.isBlank(saveCode)) {
            redisTemplate.delete(key);
            throw new ValidateCodeException("验证码已失效,请重新获取");
        }

        if (!StrUtil.equals(saveCode, code)) {
            redisTemplate.delete(key);
            throw new ValidateCodeException("验证码不正确");
        }

        redisTemplate.delete(key);
    }

}

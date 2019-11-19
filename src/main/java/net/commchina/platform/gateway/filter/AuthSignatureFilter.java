package net.commchina.platform.gateway.filter;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import net.commchina.platform.gateway.remote.AuthUserRemote;
import net.commchina.platform.gateway.remote.http.resp.UserInfo;
import net.commchina.platform.gateway.response.APIResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 全局权限认证
 */
@Slf4j
@Component
public class AuthSignatureFilter implements GlobalFilter, Ordered {

    @Autowired
    private AuthUserRemote authUserRemote;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        String path = exchange.getRequest().getURI().getPath();

        if (StrUtil.containsAnyIgnoreCase(path, "/oauth/token")) {
            return chain.filter(exchange);
        } else if (StrUtil.containsAnyIgnoreCase(path, "/job/")) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        } else if (StrUtil.containsAnyIgnoreCase(path, "/api/")) {
            String authorization = exchange.getRequest().getHeaders().getFirst("Authorization");
            log.debug("authorization:{}", authorization);
            if (authorization == null || authorization.isEmpty()) {
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            } else {
                APIResponse<UserInfo> token = authUserRemote.getTokenPage(authorization);
                if (token.getCode() == 1) {
                    UserInfo data = token.getData();

                    ServerHttpRequest newRequest = exchange.getRequest().mutate()
                            .header("userName", data.getUserName())
                            .header("companyId", Long.toString(data.getCompanyId()))
                            .header("enterpriseId", Long.toString(data.getEnterpriseId()))
                            .header("userId", Long.toString(data.getUserId()))
                            .header("requestId", UUID.randomUUID().toString())
                            .build();

                    return chain.filter(exchange.mutate().request(newRequest.mutate().build()).build());
                } else {
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return response.setComplete();
                }
            }
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder()
    {
        return -1000;
    }
}

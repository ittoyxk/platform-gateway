package net.commchina.platform.gateway.filter;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import net.commchina.platform.gateway.remote.AuthUserRemote;
import net.commchina.platform.gateway.remote.http.resp.UserInfo;
import net.commchina.platform.gateway.response.APIResponse;
import net.commchina.platform.gateway.response.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
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
public class AuthSignatureGlobalFilter implements GlobalFilter, Ordered {

    @Autowired
    private AuthUserRemote authUserRemote;


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String path = request.getURI().getPath();

        //获取token
        if (StrUtil.containsAnyIgnoreCase(path, "/oauth/token")) {
            return chain.filter(exchange);
        }
        //后端服务RPC使用,不暴露外部
        else if (StrUtil.containsAnyIgnoreCase(path, "/job/")) {
            return ResponseEntity.errorResult(response, HttpStatus.UNAUTHORIZED, "没有权限访问");
        }
        //外部前端接口统一认证
        else if (StrUtil.containsAnyIgnoreCase(path, "/api/")) {
            String authorization = request.getHeaders().getFirst("Authorization");
            log.debug("authorization:{}", authorization);
            if (authorization == null || authorization.isEmpty()) {
                return ResponseEntity.errorResult(response, HttpStatus.UNAUTHORIZED, "用户未登陆");
            } else {
                APIResponse<UserInfo> token = authUserRemote.getTokenPage(authorization);
                if (token.getCode() == 1) {
                    UserInfo data = token.getData();

                    ServerHttpRequest newRequest = request.mutate()
                            .header("userName", data.getUserName())
                            .header("companyId", Long.toString(data.getCompanyId()))
                            .header("enterpriseId", Long.toString(data.getEnterpriseId()))
                            .header("userId", Long.toString(data.getUserId()))
                            .header("requestId", UUID.randomUUID().toString())
                            .build();

                    return chain.filter(exchange.mutate().request(newRequest.mutate().build()).build());
                } else {
                    return ResponseEntity.errorResult(response, HttpStatus.UNAUTHORIZED, "没有获取到有效认证");
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

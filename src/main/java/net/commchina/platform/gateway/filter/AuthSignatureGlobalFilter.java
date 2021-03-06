package net.commchina.platform.gateway.filter;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import net.commchina.platform.gateway.common.HttpUtils;
import net.commchina.platform.gateway.remote.AuthUserRemote;
import net.commchina.platform.gateway.remote.http.resp.UserInfo;
import net.commchina.platform.gateway.response.APIResponse;
import net.commchina.platform.gateway.response.ResponseEntity;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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

import java.io.IOException;
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

        //外部前端接口统一认证
        if (StrUtil.containsAnyIgnoreCase(path, "/api/")) {
            String authorization = request.getHeaders().getFirst("Authorization");
            if (StringUtils.isEmpty(authorization)) {
                authorization = request.getQueryParams().getFirst("token");
            }
            log.debug("authorization:{}", authorization);
            if (authorization == null || authorization.isEmpty()) {
                return ResponseEntity.errorResult(response, HttpStatus.UNAUTHORIZED, "用户未登陆");
            } else {
                Response token = authUserRemote.getTokenPage(authorization);
                log.debug("status:{}", token.status());
                try {
                    if (token.status() == HttpStatus.OK.value()) {

                        String bodyStr = IOUtils.toString(token.body().asInputStream(), "UTF-8");
                        APIResponse apiResponse = JSONObject.toJavaObject(JSONObject.parseObject(bodyStr), APIResponse.class);
                        if (apiResponse.getCode() == 1) {
                            UserInfo data = JSONObject.toJavaObject((JSONObject) apiResponse.getData(), UserInfo.class);

                            ServerHttpRequest newRequest = request.mutate()
                                    .header("userName", data.getUserName())
                                    .header("companyId", data.getCompanyId() == null ? "-1" : Long.toString(data.getCompanyId()))
                                    .header("enterpriseId", data.getEnterpriseId() == null ? "-1" : Long.toString(data.getEnterpriseId()))
                                    .header("userId", data.getUserId() == null ? "-1" : Long.toString(data.getUserId()))
                                    .header("requestId", UUID.randomUUID().toString())
                                    .header("deptId", data.getDeptId() == null ? "-1" : Long.toString(data.getDeptId()))
                                    .header("groundId", data.getGroundId() == null ? "-1" : Long.toString(data.getGroundId()))
                                    .header("clientId", data.getClientId())
                                    .build();

                            log.debug("request IP:{}-PATH:{}-USERID:{}-USERNAME:{}-QUERYPARAMS:{}-BODY:{}", HttpUtils.getIpAddress(request), path, data.getUserId(), data.getUserName(), request.getQueryParams());
                            return chain.filter(exchange.mutate().request(newRequest.mutate().build()).build());
                        }
                        return ResponseEntity.errorResult(response, HttpStatus.UNAUTHORIZED, "没有获取到有效认证");
                    } else {
                        log.warn("auth 异常");
                        return ResponseEntity.errorResult(response, HttpStatus.UNAUTHORIZED, "token 已失效,请重新登录");
                    }
                } catch (IOException e) {
                    log.error("apiResponse IOException:{}", e);
                    return ResponseEntity.errorResult(response, HttpStatus.UNAUTHORIZED, "没有获取到有效认证");
                }
            }
        }//后端服务RPC使用,不暴露外部
        else if (StrUtil.containsAnyIgnoreCase(path, "/job/")) {
            return ResponseEntity.errorResult(response, HttpStatus.UNAUTHORIZED, "没有权限访问");
        }
        return chain.filter(exchange);
    }


    @Override
    public int getOrder()
    {
        return -1000;
    }
}

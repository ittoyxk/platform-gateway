package net.commchina.platform.gateway.remote;

import feign.Response;
import net.commchina.platform.gateway.remote.http.req.OpenApiAuthReq;
import net.commchina.platform.gateway.remote.http.resp.UserInfo;
import net.commchina.platform.gateway.response.APIResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * @description: platform-gateway
 * @author: hengxiaokang
 * @time 2019/11/19 13:22
 */
@FeignClient("auth-core")
public interface AuthUserRemote {

    @GetMapping("/token/getUser")
    Response getTokenPage(@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authHeader);

    @PostMapping("/oauth/openapi/auth")
    APIResponse<UserInfo> auth(@RequestBody OpenApiAuthReq req);
}

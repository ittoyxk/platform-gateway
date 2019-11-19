package net.commchina.platform.gateway.remote;

import net.commchina.platform.gateway.remote.http.resp.UserInfo;
import net.commchina.platform.gateway.response.APIResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * @description: platform-gateway
 * @author: hengxiaokang
 * @time 2019/11/19 13:22
 */
@FeignClient("auth-core")
public interface AuthUserRemote {

    @GetMapping("/token/getUser")
    public APIResponse<UserInfo> getTokenPage(@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authHeader);
}

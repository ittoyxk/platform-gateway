package net.commchina.platform.gateway.common;

import com.google.common.base.Strings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.Base64Utils;

/**
 * @description: platform-gateway
 * @author: hengxiaokang
 * @time 2019/11/27 9:49
 */
public class HttpUtils {

    public static String getIpAddress(ServerHttpRequest request)
    {
        HttpHeaders headers = request.getHeaders();
        String ip = headers.getFirst("x-forwarded-for");
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            // 多次反向代理后会有多个ip值，第一个ip才是真实ip
            if (ip.indexOf(",") != -1) {
                ip = ip.split(",")[0];
            }
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = headers.getFirst("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = headers.getFirst("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = headers.getFirst("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = headers.getFirst("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = headers.getFirst("X-Real-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddress().getAddress().getHostAddress();
        }
        return ip;
    }


    public static String getClientId(ServerHttpRequest request){
        String authorization = request.getHeaders().getFirst("Authorization");
        if(!Strings.isNullOrEmpty(authorization)){
            String basic_ = authorization.replace("Basic ", "");
            if(!Strings.isNullOrEmpty(basic_)){
                byte[] bytes = Base64Utils.decodeFromString(basic_);
                String client = new String(bytes);
                String clientId = client.split(":")[0];

                return clientId;
            }
        }
        return null;
    }
}

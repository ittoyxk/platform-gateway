package net.commchina.platform.gateway.response;

import com.alibaba.fastjson.JSON;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @description: platform-gateway
 * @author: hengxiaokang
 * @time 2019/11/20 19:11
 */
public class ResponseEntity {


    public static Mono<Void> errorResult(ServerHttpResponse response, HttpStatus status, String msg)
    {
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
        APIResponse<Object> build = APIResponse.builder().code(-1).msg(msg).build();
        byte[] bytes = JSON.toJSONBytes(build);
        DataBuffer wrap = response.bufferFactory().wrap(bytes);
        return response.writeWith(Flux.just(wrap));
    }

}

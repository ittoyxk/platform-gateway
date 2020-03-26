package net.commchina.platform.gateway.handler;

import lombok.extern.slf4j.Slf4j;
import net.commchina.platform.gateway.response.APIResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * @description: platform-gateway
 * @author: hengxiaokang
 * @time 2020/3/26 13:30
 */
@Slf4j
@Component
public class TestHandler implements HandlerFunction<ServerResponse> {
    @Override
    public Mono<ServerResponse> handle(ServerRequest serverRequest)
    {
        log.info("test...");
        return ServerResponse.ok().contentType(MediaType.APPLICATION_PROBLEM_JSON_UTF8).body(BodyInserters.fromObject(APIResponse.builder().code(1).msg("成功").data("succeed").build()));
    }
}

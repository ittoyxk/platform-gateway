package net.commchina.platform.gateway.response;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.cloud.gateway.support.CachedBodyOutputMessage;
import org.springframework.cloud.gateway.support.DefaultClientResponse;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.ResponseCookie;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

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

    public static Mono<Void> getResponse(ServerWebExchange exchange, String message, ObjectMapper objectMapper) throws JsonProcessingException
    {
        return new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body)
            {
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.add(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
                ResponseAdapter responseAdapter = new ResponseAdapter(body, httpHeaders);
                DefaultClientResponse clientResponse = new DefaultClientResponse(responseAdapter, ExchangeStrategies.withDefaults());
                Mono<String> rawBody = clientResponse.bodyToMono(String.class).map(s -> s);
                BodyInserter<Mono<String>, ReactiveHttpOutputMessage> bodyInserter = BodyInserters.fromPublisher(rawBody, String.class);
                CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(exchange, exchange.getResponse().getHeaders());
                return bodyInserter.insert(outputMessage, new BodyInserterContext())
                        .then(Mono.defer(() -> {
                            Flux<DataBuffer> messageBody = outputMessage.getBody();
                            Flux<DataBuffer> flux = messageBody.map(buffer -> {
                                CharBuffer charBuffer = StandardCharsets.UTF_8.decode(buffer.asByteBuffer());
                                DataBufferUtils.release(buffer);
                                // 将响应信息转化为字符串
                                String responseStr = charBuffer.toString();
                                if (StringUtils.isNotBlank(responseStr)) {
                                    JSONObject result = JSONObject.parseObject(responseStr);
                                    responseStr = result.toJSONString();
                                }
                                return getDelegate().bufferFactory().wrap(responseStr.getBytes(StandardCharsets.UTF_8));
                            });
                            HttpHeaders headers = getDelegate().getHeaders();
                            headers.set(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
                            // 修改响应包的大小，不修改会因为包大小不同被浏览器丢掉
                            flux = flux.doOnNext(data -> headers.setContentLength(data.readableByteCount()));
                            return getDelegate().writeWith(flux);
                        }));
            }
        }.writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(objectMapper.writeValueAsBytes(APIResponse.builder().msg(message).code(-1).build()))));
    }

    private static class ResponseAdapter implements ClientHttpResponse {

        private final Flux<DataBuffer> flux;
        private final HttpHeaders headers;

        @SuppressWarnings("unchecked")
        private ResponseAdapter(Publisher<? extends DataBuffer> body, HttpHeaders headers)
        {
            this.headers = headers;
            if (body instanceof Flux) {
                flux = (Flux) body;
            } else {
                flux = ((Mono) body).flux();
            }
        }

        @Override
        public Flux<DataBuffer> getBody()
        {
            return flux;
        }

        @Override
        public HttpHeaders getHeaders()
        {
            return headers;
        }

        @Override
        public HttpStatus getStatusCode()
        {
            return null;
        }

        @Override
        public int getRawStatusCode()
        {
            return 0;
        }

        @Override
        public MultiValueMap<String, ResponseCookie> getCookies()
        {
            return null;
        }
    }
}

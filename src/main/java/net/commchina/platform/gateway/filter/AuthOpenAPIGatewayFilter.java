package net.commchina.platform.gateway.filter;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import net.commchina.platform.gateway.remote.AuthUserRemote;
import net.commchina.platform.gateway.remote.http.req.OpenApiAuthReq;
import net.commchina.platform.gateway.remote.http.resp.UserInfo;
import net.commchina.platform.gateway.response.APIResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.cloud.gateway.support.CachedBodyOutputMessage;
import org.springframework.cloud.gateway.support.DefaultServerRequest;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * @description: platform-gateway
 * @author: hengxiaokang
 * @time 2020/4/28 18:03
 */
@Slf4j
@Component
public class AuthOpenAPIGatewayFilter extends AbstractGatewayFilterFactory {

    @Autowired
    private AuthUserRemote authUserRemote;


    @Override
    @SuppressWarnings("unchecked")
    public GatewayFilter apply(Object config)
    {
        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
            {
                String path = exchange.getRequest().getURI().getPath();
                if (StrUtil.containsAnyIgnoreCase(path, "/openapi/")) {
                    ServerRequest serverRequest = new DefaultServerRequest(exchange);
                    HttpHeaders headers = new HttpHeaders();
                    headers.putAll(exchange.getRequest().getHeaders());
                    // mediaType
                    MediaType mediaType = exchange.getRequest().getHeaders().getContentType();
                    // read & modify body
                    Mono<String> modifiedBody = serverRequest.bodyToMono(String.class)
                            .flatMap(body -> {
                                if (MediaType.APPLICATION_JSON_UTF8.isCompatibleWith(mediaType)) {
                                    // origin body map
                                    JSONObject json = decodeBody(body);

                                    Object data = json.get("data");
                                    String timestamp = json.getString("timestamp");
                                    String appId = json.getString("appId");
                                    String signature = json.getString("signature");
                                    String signType = json.getString("signType");
                                    OpenApiAuthReq build = OpenApiAuthReq.builder().timestamp(timestamp).signType(signType).signature(signature).appId(appId).reqData(data).build();
                                    //log.debug("build:{}", build.toString());
                                    APIResponse<UserInfo> auth = authUserRemote.auth(build);
                                    headers.set("companyId", auth.getData().getCompanyId() == null ? "-1" : Long.toString(auth.getData().getCompanyId()));
                                    headers.set("enterpriseId", auth.getData().getEnterpriseId() == null ? "-1" : Long.toString(auth.getData().getEnterpriseId()));
                                    headers.set("userId", auth.getData().getUserId() == null ? "-1" : Long.toString(auth.getData().getUserId()));
                                    headers.set("deptId", auth.getData().getDeptId() == null ? "-1" : Long.toString(auth.getData().getDeptId()));
                                    headers.set("groundId", auth.getData().getGroundId() == null ? "" : Long.toString(auth.getData().getGroundId()));
                                    headers.set("requestId", UUID.randomUUID().toString());
                                    // new body map

                                    return Mono.just(encodeBody(data));
                                }
                                return Mono.empty();
                            });

                    BodyInserter bodyInserter = BodyInserters.fromPublisher(modifiedBody, String.class);


                    // the new content type will be computed by bodyInserter
                    // and then set in the request decorator
                    headers.remove(HttpHeaders.CONTENT_LENGTH);

                    CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(exchange, headers);
                    return bodyInserter.insert(outputMessage, new BodyInserterContext())
                            .then(Mono.defer(() -> {
                                ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(
                                        exchange.getRequest()) {
                                    @Override
                                    public HttpHeaders getHeaders()
                                    {
                                        long contentLength = headers.getContentLength();
                                        HttpHeaders httpHeaders = new HttpHeaders();
                                        httpHeaders.putAll(headers);
                                        if (contentLength > 0) {
                                            httpHeaders.setContentLength(contentLength);
                                        } else {
                                            httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
                                        }
                                        return httpHeaders;
                                    }

                                    @Override
                                    public Flux<DataBuffer> getBody()
                                    {
                                        return outputMessage.getBody();
                                    }
                                };
                                return chain.filter(exchange.mutate().request(decorator).build());
                            }));
                } else {
                    return chain.filter(exchange);
                }
            }
        };
    }

    private JSONObject decodeBody(String body)
    {
        return JSONObject.parseObject(body);
    }

    private String encodeBody(Object json)
    {
        return JSONObject.toJSONString(json);
    }
}

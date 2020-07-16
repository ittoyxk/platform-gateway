package net.commchina.platform.gateway.filter;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import net.commchina.platform.gateway.remote.AuthUserRemote;
import net.commchina.platform.gateway.remote.http.req.OpenApiAuthReq;
import net.commchina.platform.gateway.remote.http.resp.UserInfo;
import net.commchina.platform.gateway.response.APIResponse;
import net.commchina.platform.gateway.response.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.cloud.gateway.support.CachedBodyOutputMessage;
import org.springframework.cloud.gateway.support.DefaultServerRequest;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${gateway.auth.cache:false}")
    private boolean authCache;

    @Override
    @SuppressWarnings("unchecked")
    public GatewayFilter apply(Object config)
    {
        return (exchange, chain) ->
        {
            String path = exchange.getRequest().getURI().getPath();
            if (StrUtil.containsAnyIgnoreCase(path, "/openapi/")) {

                HttpHeaders headers = new HttpHeaders();
                headers.putAll(exchange.getRequest().getHeaders());
                // mediaType
                MediaType mediaType = exchange.getRequest().getHeaders().getContentType();
                HttpMethod method = exchange.getRequest().getMethod();
                if (Objects.equals(method, HttpMethod.GET)) {
                    String appId = exchange.getRequest().getHeaders().getFirst("appId");
                    String timestamp = exchange.getRequest().getHeaders().getFirst("timestamp");
                    String signType = exchange.getRequest().getHeaders().getFirst("signType");
                    String signature = exchange.getRequest().getHeaders().getFirst("signature");
                    MultiValueMap<String, String> data = exchange.getRequest().getQueryParams();

                    OpenApiAuthReq build = OpenApiAuthReq.builder().timestamp(timestamp).signType(signType).signature(signature).appId(appId).reqData(HttpUtil.toParams(data)).build();
                    APIResponse<UserInfo> auth = getUserInfo(build);
                    if (auth.getCode() == 1) {
                        setAuthHeader(headers, auth);
                    } else {
                        return ResponseEntity.errorResult(exchange.getResponse(), HttpStatus.UNAUTHORIZED, auth.getMsg());
                    }
                    ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(exchange.getRequest()) {
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
                    };
                    return chain.filter(exchange.mutate().request(decorator).build());
                } else {
                    ServerRequest serverRequest = new DefaultServerRequest(exchange);
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
                                    APIResponse<UserInfo> auth = getUserInfo(build);
                                    if (auth.getCode() == 1) {
                                        setAuthHeader(headers, auth);
                                    } else {
                                        headers.set("authCode", Integer.toString(auth.getCode()));
                                        headers.set("authMsg", auth.getMsg());
                                    }
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
                                ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(exchange.getRequest()) {
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
                                if (Objects.equals(headers.getFirst("authCode"), "1")) {
                                    return chain.filter(exchange.mutate().request(decorator).build());
                                } else {
                                    return ResponseEntity.errorResult(exchange.getResponse(), HttpStatus.UNAUTHORIZED, headers.getFirst("authMsg"));
                                }
                            }));
                }
            } else {
                return chain.filter(exchange);
            }
        };
    }

    private APIResponse<UserInfo> getUserInfo(OpenApiAuthReq build)
    {
        if (authCache) {
            String appId = build.getAppId();
            String cache = redisTemplate.opsForValue().get("auth:core:openapi:userinfo:" + appId);
            if (Strings.isNullOrEmpty(cache)) {
                log.debug("auth init:{}", build.toString());
                APIResponse<UserInfo> auth = authUserRemote.auth(build);
                if (auth != null && auth.getData() != null) {
                    UserInfo data = auth.getData();
                    redisTemplate.opsForValue().set("auth:core:openapi:userinfo:" + appId, JSONObject.toJSONString(data),1800, TimeUnit.SECONDS);
                }
                return auth;
            } else {
                UserInfo userInfo = JSONObject.toJavaObject(JSONObject.parseObject(cache), UserInfo.class);
                return APIResponse.success(userInfo);
            }
        }
        return authUserRemote.auth(build);
    }

    private void setAuthHeader(HttpHeaders headers, APIResponse<UserInfo> auth)
    {
        headers.set("authCode", Integer.toString(auth.getCode()));
        headers.set("companyId", auth.getData().getCompanyId() == null ? "-1" : Long.toString(auth.getData().getCompanyId()));
        headers.set("enterpriseId", auth.getData().getEnterpriseId() == null ? "-1" : Long.toString(auth.getData().getEnterpriseId()));
        headers.set("userId", auth.getData().getUserId() == null ? "-1" : Long.toString(auth.getData().getUserId()));
        headers.set("deptId", auth.getData().getDeptId() == null ? "-1" : Long.toString(auth.getData().getDeptId()));
        headers.set("groundId", auth.getData().getGroundId() == null ? "" : Long.toString(auth.getData().getGroundId()));
        headers.set("requestId", UUID.randomUUID().toString());
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

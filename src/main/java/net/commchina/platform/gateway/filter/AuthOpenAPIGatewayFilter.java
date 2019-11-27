package net.commchina.platform.gateway.filter;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBufAllocator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.commchina.platform.gateway.common.HttpUtils;
import net.commchina.platform.gateway.remote.AuthUserRemote;
import net.commchina.platform.gateway.remote.http.req.OpenApiAuthReq;
import net.commchina.platform.gateway.remote.http.resp.UserInfo;
import net.commchina.platform.gateway.response.APIResponse;
import net.commchina.platform.gateway.response.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description: platform-gateway
 * @author: hengxiaokang
 * @time 2019/11/27 9:31
 */
@Slf4j
@Component
@AllArgsConstructor
public class AuthOpenAPIGatewayFilter extends AbstractGatewayFilterFactory {

    @Autowired
    private AuthUserRemote authUserRemote;

    private final Pattern pattern = Pattern.compile("\\s*|\t|\r|\n");

    @Override
    public GatewayFilter apply(Object config)
    {
        return ((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            String path = request.getURI().getPath();
            //只拦截外部系统请求
            if (StrUtil.containsAnyIgnoreCase(path, "/openapi/")) {
                String hostAddress = HttpUtils.getIpAddress(request);
                log.info("hostAddress:{}", hostAddress);

                String body = resolveBodyFromRequest(request);
                JSONObject jsonObject = JSONObject.parseObject(body);

                JSONObject data = jsonObject.getJSONObject("data");
                String timestamp = jsonObject.getString("timestamp");
                String appId = jsonObject.getString("appId");
                String signature = jsonObject.getString("signature");

                OpenApiAuthReq build = OpenApiAuthReq.builder().remoteIp(hostAddress).timestamp(timestamp).signature(signature).appId(appId).reqData(data).build();
                log.info("build:{}", build.toString());
                APIResponse<UserInfo> auth = authUserRemote.auth(build);
                if (auth != null && auth.getCode() == 1) {
                    DataBuffer bodyDataBuffer = stringBuffer(data.toJSONString());
                    ServerHttpRequest newRequest = new ServerHttpRequestDecorator(request) {
                        @Override
                        public Flux<DataBuffer> getBody()
                        {
                            return Flux.just(bodyDataBuffer);
                        }

                        @Override
                        public HttpHeaders getHeaders()
                        {
                            HttpHeaders httpHeaders = new HttpHeaders();
                            httpHeaders.putAll(super.getHeaders());
                            httpHeaders.setContentLength(bodyDataBuffer.readableByteCount());
                            httpHeaders.set("companyId", Long.toString(auth.getData().getCompanyId()));
                            httpHeaders.set("enterpriseId", Long.toString(auth.getData().getEnterpriseId()));
                            httpHeaders.set("userId", Long.toString(auth.getData().getUserId()));
                            httpHeaders.set("requestId", UUID.randomUUID().toString());
                            return httpHeaders;
                        }
                    };
                    return chain.filter(exchange.mutate().request(newRequest.mutate().build()).build());
                } else {
                    return ResponseEntity.errorResult(response, HttpStatus.UNAUTHORIZED, auth.getMsg());
                }
            }
            return chain.filter(exchange);
        });
    }

    private String resolveBodyFromRequest(ServerHttpRequest serverHttpRequest)
    {
        //获取请求体
        Flux<DataBuffer> body = serverHttpRequest.getBody();
        StringBuilder sb = new StringBuilder();
        body.subscribe(buffer -> {
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            DataBufferUtils.release(buffer);
            String bodyString = new String(bytes, StandardCharsets.UTF_8);
            sb.append(bodyString);
        });
        return formatStr(sb.toString());
    }

    /**
     * 去掉空格,换行和制表符
     *
     * @param str
     * @return
     */
    private String formatStr(String str)
    {
        if (str != null && str.length() > 0) {
            Matcher m = pattern.matcher(str);
            return m.replaceAll("");
        }
        return str;
    }

    private DataBuffer stringBuffer(String value)
    {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        NettyDataBufferFactory nettyDataBufferFactory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
        DataBuffer buffer = nettyDataBufferFactory.allocateBuffer(bytes.length);
        buffer.write(bytes);
        return buffer;
    }
}

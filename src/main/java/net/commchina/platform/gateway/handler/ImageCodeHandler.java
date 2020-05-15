package net.commchina.platform.gateway.handler;

import com.google.code.kaptcha.Producer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author: hengxiaokang
 * @time 2019/11/20 15:14
 * 验证码生成逻辑处理类
 */
@Slf4j
@RefreshScope
@Component
public class ImageCodeHandler implements HandlerFunction<ServerResponse> {
    private final Producer producer;
    private final StringRedisTemplate redisTemplate;

    @Value("${auth.core.imagecode.timeout:60}")
    private Long timeOut;


    public ImageCodeHandler(final Producer producer, final StringRedisTemplate redisTemplate) {
        this.producer = producer;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<ServerResponse> handle(ServerRequest serverRequest)
    {
        //生成验证码
        String text = producer.createText();
        BufferedImage image = producer.createImage(text);

        //保存验证码信息
        String randomStr = serverRequest.queryParam("randomStr").get();
        redisTemplate.opsForValue().set("auth:core:imagecode:" + randomStr, text, timeOut, TimeUnit.SECONDS);

        log.info("text code:{}", text);
        // 转换流信息写出
        FastByteArrayOutputStream os = new FastByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpeg", os);
        } catch (IOException e) {
            log.error("ImageIO write err", e);
            return Mono.error(e);
        }

        return ServerResponse
                .status(HttpStatus.OK)
                .contentType(MediaType.IMAGE_JPEG)
                .body(BodyInserters.fromResource(new ByteArrayResource(os.toByteArray())));
    }
}

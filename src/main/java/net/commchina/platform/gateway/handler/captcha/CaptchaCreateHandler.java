package net.commchina.platform.gateway.handler.captcha;


import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * @author xiaokang
 * @Title: 验证码缓存接口
 * @date 2020-05-12
 */
public interface CaptchaCreateHandler extends HandlerFunction<ServerResponse> {

}

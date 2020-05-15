package net.commchina.platform.gateway.config;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.commchina.platform.gateway.handler.HystrixFallbackHandler;
import net.commchina.platform.gateway.handler.ImageCodeHandler;
import net.commchina.platform.gateway.handler.captcha.CaptchaCreateHandler;
import net.commchina.platform.gateway.handler.captcha.CaptchaVerifyHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;

/**
 * @author: hengxiaokang
 * @time 2019/11/20 15:14
 * 路由配置信息
 */
@Slf4j
@Configuration
@AllArgsConstructor
public class RouterFunctionConfiguration {
    private final HystrixFallbackHandler hystrixFallbackHandler;
    private final ImageCodeHandler imageCodeHandler;
    private final CaptchaCreateHandler captchaHandler;
	private final CaptchaVerifyHandler captchaVerifyHandler;

    @Bean
    public RouterFunction routerFunction()
    {
        return RouterFunctions.route(
                RequestPredicates.path("/fallback")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_PROBLEM_JSON_UTF8)), hystrixFallbackHandler)
                .andRoute(RequestPredicates.GET("/code")
                        .and(RequestPredicates.accept(MediaType.TEXT_PLAIN)), imageCodeHandler)
                .andRoute(RequestPredicates.GET("/captcha/get")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON_UTF8)), captchaHandler)
                .andRoute(RequestPredicates.POST("/captcha/verify")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON_UTF8)), captchaVerifyHandler);

    }

}

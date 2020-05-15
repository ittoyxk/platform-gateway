package net.commchina.platform.gateway.handler.captcha.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class CaptchaVO implements Serializable {


    /**
     * 原生图片base64
     */
    private String originalImageBase64;


    /**
     * 滑块图片base64
     */
    private String jigsawImageBase64;


    /**
     * UUID(每次请求的验证码唯一标识)
     */
    private String token;

}

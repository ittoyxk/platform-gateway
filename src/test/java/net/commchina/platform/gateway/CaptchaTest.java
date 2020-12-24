package net.commchina.platform.gateway;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.ShearCaptcha;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * @description: platform-gateway
 * @author: hengxiaokang
 * @time 2020/12/24 14:38
 */
@Slf4j
public class CaptchaTest {

    @Test
    public void test(){
        ShearCaptcha shearCaptcha = CaptchaUtil.createShearCaptcha(200, 150,4,2);
        String code = shearCaptcha.getCode();
        BufferedImage image = shearCaptcha.getImage();
        shearCaptcha.write(new File("D:/a.png"));
        log.info("code:{}",code);
        log.info("image:{}",image);
    }
}

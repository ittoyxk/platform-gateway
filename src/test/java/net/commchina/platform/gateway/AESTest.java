package net.commchina.platform.gateway;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.symmetric.AES;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * @description: platform-gateway
 * @author: hengxiaokang
 * @time 2020/2/17 10:13
 */
@Slf4j
public class AESTest {

    @Test
    public void test(){
        AES aes = new AES("AESIOSSAABCDEFIF".getBytes());
        String admin = aes.encryptHex("admin");
        log.info("admin:{}",admin);

        String s = aes.decryptStr(HexUtil.decodeHexStr("56434c694842556467696b5a356b49424e2f337169413d3d"));
        log.info("s:{}",s);
        String hexStr = HexUtil.decodeHexStr("56434c694842556467696b5a356b49424e2f337169413d3d");
        log.info("hex:{}",hexStr);


        log.info("p;{}",new BCryptPasswordEncoder().encode("shield@2020_xiaokang"));

        String s1 = aes.encryptBase64("123456");
        log.info("pass:{}",s1);
        log.info("de:{}",HexUtil.encodeHexStr(s1));
    }

    @Test
    public void test2(){
        AES aes = new AES("AESIOSSAABCDEFIF".getBytes());
        String admin = aes.encryptHex("{\"x\":226,\"y\":5}");
        log.info("admin:{}",admin);

        String s = aes.decryptStr(admin);
        log.info("s:{}",s);

    }
}

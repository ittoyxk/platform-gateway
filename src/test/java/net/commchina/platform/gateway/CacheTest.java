package net.commchina.platform.gateway;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @description: platform-gateway
 * @author: hengxiaokang
 * @time 2020/7/21 11:47
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class CacheTest {

    @Autowired
    private Cache<String, Object> cache;

    @Test
    public void test(){
        long l = System.nanoTime();
        Object o = cache.get("HH",k->k + "OK");
        long l2 = System.nanoTime();
        log.info("o:{}",o);
        log.info("time:{}",(l2-l));

        long l3 = System.nanoTime();
        Object hh = cache.get("HH", k -> k + "OK");
        long l4 = System.nanoTime();
        log.info("o2:{}",hh);
        log.info("time:{}",(l4-l3));

        long l5 = System.nanoTime();
        Object hh2 = cache.get("HH", k -> k + "OK");
        long l6 = System.nanoTime();
        log.info("o3:{}",hh2);
        log.info("time:{}",(l6-l5));
    }
}

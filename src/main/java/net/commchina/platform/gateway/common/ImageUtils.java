package net.commchina.platform.gateway.common;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Base64Utils;
import org.springframework.util.FileCopyUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class ImageUtils {

    /**
     * 滑块底图
     */
    private static final Map<String, String> originalCacheMap = new ConcurrentHashMap();
    /**
     * 滑块
     */
    private static final Map<String, String> slidingBlockCacheMap = new ConcurrentHashMap();


    static {
        //滑动拼图
        originalCacheMap.putAll(getResourcesImagesFile("classpath:images/jigsaw/original/*.png"));
        slidingBlockCacheMap.putAll(getResourcesImagesFile("classpath:images/jigsaw/slidingBlock/*.png"));
    }

    public static BufferedImage getOriginal()
    {
        int randomNum = RandomUtils.getRandomInt(1, originalCacheMap.size() + 1);
        String s = originalCacheMap.get("bg".concat(String.valueOf(randomNum)).concat(".png"));
        return getBase64StrToImage(s);
    }

    public static BufferedImage getslidingBlock()
    {
        int randomNum = RandomUtils.getRandomInt(1, slidingBlockCacheMap.size() + 1);
        String s = slidingBlockCacheMap.get(String.valueOf(randomNum).concat(".png"));
        return getBase64StrToImage(s);
    }

    /**
     * base64 字符串转图片
     *
     * @param base64String
     * @return
     */
    public static BufferedImage getBase64StrToImage(String base64String)
    {
        try {
            byte[] bytes = Base64Utils.decodeFromString(base64String);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            return ImageIO.read(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static Map<String, String> getResourcesImagesFile(String path)
    {
        Map<String, String> imgMap = new HashMap<>();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(path);
            for (Resource resource : resources) {
                byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
                String string = Base64Utils.encodeToString(bytes);
                String filename = resource.getFilename();
                imgMap.put(filename, string);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imgMap;
    }

}

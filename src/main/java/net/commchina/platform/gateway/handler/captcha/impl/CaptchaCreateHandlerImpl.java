package net.commchina.platform.gateway.handler.captcha.impl;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import net.commchina.platform.gateway.common.ImageUtils;
import net.commchina.platform.gateway.common.RandomUtils;
import net.commchina.platform.gateway.handler.captcha.CaptchaCacheService;
import net.commchina.platform.gateway.handler.captcha.CaptchaCreateHandler;
import net.commchina.platform.gateway.handler.captcha.model.CaptchaVO;
import net.commchina.platform.gateway.response.APIResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Random;

/**
 * 滑动验证码
 */
@Slf4j
@Component
@RefreshScope
public class CaptchaCreateHandlerImpl implements CaptchaCreateHandler {

    @Autowired
    private CaptchaCacheService captchaCacheService;

    @Value("${captcha.water.mark:通讯卫士}")
    private String waterMark;

    @Value("${captcha.water.font:'宋体'}")
    private String waterMarkFont;

    private static final String IMAGE_TYPE_PNG = "png";
    private static final int HAN_ZI_SIZE = 25;
    /**
     * check校验坐标
     */
    private static final String REDIS_CAPTCHA_KEY = "gateway:captcha:running:captcha:";
    @Value("${auth.core.imagecode.timeout:60}")
    private Long timeOut;

    /**
     * 获取验证码
     *
     * @param serverRequest
     * @return
     */
    @Override
    public Mono<ServerResponse> handle(ServerRequest serverRequest)
    {
        return ServerResponse
                .status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(BodyInserters.fromObject(get(serverRequest)));
    }

    public APIResponse get(ServerRequest serverRequest)
    {
        //原生图片
        BufferedImage originalImage = ImageUtils.getOriginal();
        //设置水印
        Graphics backgroundGraphics = originalImage.getGraphics();
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        Font watermark = new Font(waterMarkFont, Font.BOLD, HAN_ZI_SIZE / 2);
        backgroundGraphics.setFont(watermark);
        backgroundGraphics.setColor(Color.white);
        backgroundGraphics.drawString(waterMark, width - ((HAN_ZI_SIZE / 2) * (waterMark.length())) - 5, height - (HAN_ZI_SIZE / 2) + 7);

        //抠图图片
        BufferedImage jigsawImage = ImageUtils.getslidingBlock();
        CaptchaVO captcha = pictureTemplatesCut(originalImage, jigsawImage);
        if (captcha == null || StringUtils.isBlank(captcha.getJigsawImageBase64()) || StringUtils.isBlank(captcha.getOriginalImageBase64())) {
            return APIResponse.error("获取验证码失败");
        }
        return APIResponse.success(captcha);
    }


    /**
     * 根据模板切图
     *
     * @throws Exception
     */
    public CaptchaVO pictureTemplatesCut(BufferedImage originalImage, BufferedImage jigsawImage)
    {
        try {
            CaptchaVO dataVO = new CaptchaVO();

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            int jigsawWidth = jigsawImage.getWidth();
            int jigsawHeight = jigsawImage.getHeight();

            //随机生成拼图坐标
            Point point = generateJigsawPoint(originalWidth, originalHeight, jigsawWidth, jigsawHeight);
            int x = (int) point.getX();
            int y = (int) point.getY();

            //生成新的拼图图像
            BufferedImage newJigsawImage = new BufferedImage(jigsawWidth, jigsawHeight, jigsawImage.getType());
            Graphics2D graphics = newJigsawImage.createGraphics();
            graphics.setBackground(Color.white);

            int bold = 5;
            BufferedImage subImage = originalImage.getSubimage(x, 0, jigsawWidth, jigsawHeight);

            // 获取拼图区域
            newJigsawImage = DealCutPictureByTemplate(subImage, jigsawImage, newJigsawImage);

            // 设置“抗锯齿”的属性
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setStroke(new BasicStroke(bold, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
            graphics.drawImage(newJigsawImage, 0, 0, null);
            graphics.dispose();

            //新建流。
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            //利用ImageIO类提供的write方法，将bi以png图片的数据模式写入流。
            ImageIO.write(newJigsawImage, IMAGE_TYPE_PNG, os);
            byte[] jigsawImages = os.toByteArray();

            // 源图生成遮罩
            byte[] oriCopyImages = DealOriPictureByTemplate(originalImage, jigsawImage, x, 0);
            BASE64Encoder encoder = new BASE64Encoder();
            dataVO.setOriginalImageBase64(encoder.encode(oriCopyImages).replaceAll("\r|\n", ""));
            //point信息不传到前端，只做后端check校验
            dataVO.setJigsawImageBase64(encoder.encode(jigsawImages).replaceAll("\r|\n", ""));
            dataVO.setToken(RandomUtils.getUUID());

            //将坐标信息存入redis中
            String codeKey = REDIS_CAPTCHA_KEY + dataVO.getToken();
            captchaCacheService.set(codeKey, JSONObject.toJSONString(point), timeOut);

            return dataVO;
        } catch (Exception e) {
            log.error("e:{}", e);
            return null;
        }
    }

    /**
     * 抠图后原图生成
     *
     * @param oriImage
     * @param templateImage
     * @param x
     * @param y
     * @return
     * @throws Exception
     */
    private static byte[] DealOriPictureByTemplate(BufferedImage oriImage, BufferedImage templateImage, int x, int y) throws Exception
    {
        // 源文件备份图像矩阵 支持alpha通道的rgb图像
        BufferedImage ori_copy_image = new BufferedImage(oriImage.getWidth(), oriImage.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        // 源文件图像矩阵
        int[][] oriImageData = getData(oriImage);
        // 模板图像矩阵
        int[][] templateImageData = getData(templateImage);

        //copy 源图做不透明处理
        for (int i = 0; i < oriImageData.length; i++) {
            for (int j = 0; j < oriImageData[0].length; j++) {
                int rgb = oriImage.getRGB(i, j);
                int r = (0xff & rgb);
                int g = (0xff & (rgb >> 8));
                int b = (0xff & (rgb >> 16));
                //无透明处理
                rgb = r + (g << 8) + (b << 16) + (255 << 24);
                ori_copy_image.setRGB(i, j, rgb);
            }
        }

        for (int i = 0; i < templateImageData.length; i++) {
            for (int j = 0; j < templateImageData[0].length - 5; j++) {
                int rgb = templateImage.getRGB(i, j);
                //对源文件备份图像(x+i,y+j)坐标点进行透明处理
                if (rgb != 16777215 && rgb <= 0) {
                    int rgb_ori = ori_copy_image.getRGB(x + i, y + j);
                    int r = (0xff & rgb_ori);
                    int g = (0xff & (rgb_ori >> 8));
                    int b = (0xff & (rgb_ori >> 16));
                    rgb_ori = r + (g << 8) + (b << 16) + (50 << 24);
                    ori_copy_image.setRGB(x + i, y + j, rgb_ori);
                }
            }
        }
        //新建流。
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        //利用ImageIO类提供的write方法，将bi以png图片的数据模式写入流。
        ImageIO.write(ori_copy_image, "png", os);
        //从流中获取数据数组。
        byte b[] = os.toByteArray();
        return b;
    }


    /**
     * 根据模板图片抠图
     *
     * @param oriImage
     * @param templateImage
     * @param targetImage
     * @return
     * @throws Exception
     */
    private static BufferedImage DealCutPictureByTemplate(BufferedImage oriImage, BufferedImage templateImage, BufferedImage targetImage) throws Exception
    {
        // 源文件图像矩阵
        int[][] oriImageData = getData(oriImage);
        // 模板图像矩阵
        int[][] templateImageData = getData(templateImage);
        // 模板图像宽度

        for (int i = 0; i < templateImageData.length; i++) {
            // 模板图片高度
            for (int j = 0; j < templateImageData[0].length; j++) {
                // 如果模板图像当前像素点不是白色 copy源文件信息到目标图片中
                int rgb = templateImageData[i][j];
                if (rgb != 16777215 && rgb <= 0) {
                    targetImage.setRGB(i, j, oriImageData[i][j]);
                    int rgb_ori = targetImage.getRGB(i, j);
                    if (j > 3 && j < templateImageData[0].length - 3) {
                        int rgbBefore = templateImageData[i][j - 1];
                        int rgbAfter = templateImageData[i][j + 1];
                        if (rgbBefore > 0 || rgbAfter > 0) {
                            int rgb1 = new Color(255, 255, 255, 150).getRGB();
                            targetImage.setRGB(i, j, rgb1);
                        }
                    }
                }
            }
        }
        return targetImage;
    }

    /**
     * 生成图像矩阵
     *
     * @param
     * @return
     * @throws Exception
     */
    private static int[][] getData(BufferedImage bimg)
    {
        int[][] data = new int[bimg.getWidth()][bimg.getHeight()];
        for (int i = 0; i < bimg.getWidth(); i++) {
            for (int j = 0; j < bimg.getHeight(); j++) {
                data[i][j] = bimg.getRGB(i, j);
            }
        }
        return data;
    }

    /**
     * 随机生成拼图坐标
     *
     * @param originalWidth
     * @param originalHeight
     * @param jigsawWidth
     * @param jigsawHeight
     * @return
     */
    private static Point generateJigsawPoint(int originalWidth, int originalHeight, int jigsawWidth, int jigsawHeight)
    {
        Random random = new Random();
        int widthDifference = originalWidth - jigsawWidth;
        int heightDifference = originalHeight - jigsawHeight;
        int x, y = 0;
        if (widthDifference <= 0) {
            x = 5;
        } else {
            x = random.nextInt(originalWidth - jigsawWidth - 130) + 100;
        }
        if (heightDifference <= 0) {
            y = 5;
        } else {
            y = random.nextInt(originalHeight - jigsawHeight) + 5;
        }
        return new Point(x, y);
    }
}
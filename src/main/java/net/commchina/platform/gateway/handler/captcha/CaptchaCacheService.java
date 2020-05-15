package net.commchina.platform.gateway.handler.captcha;

/**
 * @author xiaokang
 * @Title: 验证码缓存接口
 * @date 2020-05-12
 */
public interface CaptchaCacheService {

	void set(String key, String value, long expiresInSeconds);

	boolean exists(String key);

	void delete(String key);

	String get(String key);

}

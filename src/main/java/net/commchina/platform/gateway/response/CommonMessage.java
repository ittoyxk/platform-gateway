package net.commchina.platform.gateway.response;

/**
 * @description: platform-gateway
 * @author: hengxiaokang
 * @time 2020/5/14 16:27
 */
public class CommonMessage implements EMessage {

    /**
     * 处理成功状态码
     **/
    public static final int OK = 1;
    public static final String OK_MSG = "成功!";

    /**
     * 处理失败状态码
     **/
    public static final int ERROR = -1;
    public static final String ERROR_MSG = "失败!";

    /**
     * 处理失败状态码
     **/
    public static final int UNDEFINED_EXCEPTION = -1024;
    public static final String UNDEFINED_EXCEPTION_MSG = "系统错误，请稍后再试～";

    /**
     * 验证参数错误
     **/
    public static final int VALIDATE_PARAM_ERROR = -400;
    public static final String VALIDATE_PARAM_ERROR_MSG = "验证参数错误～";
    public static final int MISSING_SERVLET_REQUEST_PARAM = -401;
    public static final String MISSING_SERVLET_REQUEST_PARAM_MSG = "缺少请求参数!";
    public static final int MESSAGE_NOT_READABLE = -402;
    public static final String MESSAGE_NOT_READABLE_MSG = "请求参数解析失败!";
    public static final int METHOD_ARGUMENT_NOT_VALID = -403;
    public static final String METHOD_ARGUMENT_NOT_VALID_MSG = "方法参数无效!";
    public static final int METHOD_NOT_SUPPORTED = -404;
    public static final String METHOD_NOT_SUPPORTED_MSG = "不支持当前请求方法!";
    public static final int BIND_PARAM_EXCEPTION = -405;
    public static final String BIND_PARAM_EXCEPTION_MSG = "参数绑定失败!";
    public static final int VALIDATION_EXCEPTION = -406;
    public static final String VALIDATION_EXCEPTION_MSG = "验证参数错误!";
    public static final int MEDIA_TYPE_NOT_SUPPORTED = -407;
    public static final String MEDIA_TYPE_NOT_SUPPORTED_MSG = "不支持当前媒体类型!";

    /**
     * 基本参数错误
     **/
    public static final int NOT_LOGIN_USER = 7000;
    public static final String NOT_LOGIN_USER_MSG = "用户未登录";

    public static final int QUERY_ID_NOT_NULL = 7001;
    public static final String QUERY_ID_NOT_NULL_MSG = "查询ID不能为空";

    private int code;

    private String msg;

    private String desc;

    private CommonMessage(int code, String msg, String desc)
    {
        this.code = code;
        this.msg = msg;
        this.desc = desc;
    }

    private static CommonMessage of(int code, String msg, String desc)
    {
        return new CommonMessage(code, msg, desc);
    }

    @Override
    public int getCode()
    {
        return this.code;
    }

    @Override
    public String getMsg()
    {
        return this.msg;
    }

    @Override
    public String getDesc()
    {
        return this.desc;
    }
}

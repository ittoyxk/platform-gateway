package net.commchina.platform.gateway.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @description: platform-gateway
 * @author: hengxiaokang
 * @time 2019/11/12 13:55
 */
@Data
@Builder
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class APIResponse<T> {

    private int code;
    private String msg;
    private T data;


    public static <T> APIResponse<T> success()
    {
        return new APIResponse<>(CommonMessage.OK, CommonMessage.OK_MSG, null);
    }

    public static <T> APIResponse<T> success(T data)
    {
        return new APIResponse<>(CommonMessage.OK, CommonMessage.OK_MSG, data);
    }

    public static <T> APIResponse<T> success(String msg, T data)
    {
        return new APIResponse<>(CommonMessage.OK, msg, data);
    }

    public static <T> APIResponse<T> success(int code, String msg, T data)
    {
        return new APIResponse<>(code, msg, data);
    }

    public static <T> APIResponse<T> error(String msg)
    {
        return new APIResponse<>(CommonMessage.ERROR, msg, null);
    }

    public static <T> APIResponse<T> error(String msg, T data)
    {
        return new APIResponse<>(CommonMessage.ERROR, msg, data);
    }

    public static <T> APIResponse<T> error(int code, String msg)
    {
        return new APIResponse<>(code, msg, null);
    }

    public static <T> APIResponse<T> error(int code, String msg, T data)
    {
        return new APIResponse<>(code, msg, data);
    }

    public static <T> APIResponse<T> error(EMessage eMessage)
    {
        return new APIResponse<>(eMessage.getCode(), eMessage.getMsg(), null);
    }

    public static <T> APIResponse<T> error(EMessage eMessage, T data)
    {
        return new APIResponse<>(eMessage.getCode(), eMessage.getMsg(), data);
    }
}

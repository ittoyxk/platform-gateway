package net.commchina.platform.gateway.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
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
public class APIResponse<T> {

    private int code;
    private String msg;
    private T data;
}

package net.commchina.platform.gateway.response;

import java.io.Serializable;

/**
 * @description: platform-gateway
 * @author: hengxiaokang
 * @time 2020/5/14 16:27
 */
public interface EMessage extends Serializable {

    int getCode();

    String getMsg();

    String getDesc();
}

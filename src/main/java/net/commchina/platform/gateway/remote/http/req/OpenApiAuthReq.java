package net.commchina.platform.gateway.remote.http.req;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @description: platform-gateway
 * @author: hengxiaokang
 * @time 2019/11/22 10:41
 */
@Data
@Builder
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class OpenApiAuthReq {
    private String remoteIp;
    private String appId;
    private String timestamp;
    private String signature;
    private String signType;
    private JSONObject reqData;
}

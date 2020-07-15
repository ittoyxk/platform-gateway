package net.commchina.platform.gateway.remote.http.resp;

import lombok.Data;

/**
 * @description: platform-gateway
 * @author: hengxiaokang
 * @time 2019/11/19 13:25
 */
@Data
public class UserInfo {
    private Long userId;
    private String userName;

    private Long companyId;

    private Long enterpriseId;
    private Long deptId;
    private Long groundId;
    private String clientId;
}

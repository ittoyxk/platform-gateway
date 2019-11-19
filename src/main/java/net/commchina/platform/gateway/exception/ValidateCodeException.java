package net.commchina.platform.gateway.exception;

/**
 * @description: platform-gateway
 * @author: hengxiaokang
 * @time 2019/11/12 13:53
 */
public class ValidateCodeException  extends Exception {
    private static final long serialVersionUID = -7285211528095468156L;

    public ValidateCodeException() {
    }

    public ValidateCodeException(String msg) {
        super(msg);
    }
}

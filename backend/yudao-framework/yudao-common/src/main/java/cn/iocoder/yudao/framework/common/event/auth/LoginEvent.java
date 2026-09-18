package cn.iocoder.yudao.framework.common.event.auth;

import cn.iocoder.yudao.framework.common.event.BaseApplicationEvent;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * 用户登录成功事件
 * 
 * @author ruoyi-vue-pro
 */
@Getter
@Setter
public class LoginEvent extends BaseApplicationEvent {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 用户角色集合
     */
    private Set<String> roleCodes;

    /**
     * 登录类型
     */
    private Integer loginType;

    /**
     * 用户类型
     */
    private Integer userType;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * User-Agent
     */
    private String userAgent;

    public LoginEvent(Object source, Long userId, String username, Long deptId, 
                     Long tenantId, Set<String> roleCodes, Integer loginType, Integer userType) {
        super(source);
        this.userId = userId;
        this.username = username;
        this.deptId = deptId;
        this.tenantId = tenantId;
        this.roleCodes = roleCodes;
        this.loginType = loginType;
        this.userType = userType;
    }
} 
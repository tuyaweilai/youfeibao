package cn.iocoder.yudao.module.enterprise.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * e签宝认证流程 DO
 *
 * @author 芋道源码
 */
@TableName("enterprise_esign_auth_flow")
@KeySequence("enterprise_esign_auth_flow_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EsignAuthFlowDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    
    /**
     * e签宝认证流程ID
     */
    private String authFlowId;
    
    /**
     * 企业ID（企业认证时使用）
     */
    private Long enterpriseId;
    
    /**
     * 用户ID（个人认证时使用）
     */
    private Long userId;
    
    /**
     * 认证类型（1:企业认证, 2:个人认证）
     */
    private Integer authType;
    
    /**
     * 认证状态（0:待认证, 1:认证中, 2:认证成功, 3:认证失败）
     */
    private Integer authStatus;
    
    /**
     * 认证链接
     */
    private String authUrl;
    
    /**
     * 认证短链接
     */
    private String authShortUrl;
    
    /**
     * 回调状态（0:未回调, 1:已回调）
     */
    private Integer notifyStatus;
    
    /**
     * 回调时间
     */
    private LocalDateTime notifyTime;
    
    /**
     * 回调原始数据
     */
    private String notifyData;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * e签宝组织ID
     */
    private String orgId;
    
    /**
     * e签宝个人ID
     */
    private String personId;
} 
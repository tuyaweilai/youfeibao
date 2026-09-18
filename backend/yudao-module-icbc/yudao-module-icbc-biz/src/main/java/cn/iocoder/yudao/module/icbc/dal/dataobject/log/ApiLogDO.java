package cn.iocoder.yudao.module.icbc.dal.dataobject.log;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 工行接口调用日志 DO
 *
 * @author 芋道源码
 */
@TableName("icbc_api_log")
@KeySequence("icbc_api_log_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiLogDO extends TenantBaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 消息通讯唯一编号
     */
    private String msgId;
    /**
     * 接口名称
     */
    private String apiName;
    /**
     * 接口URL
     */
    private String apiUrl;
    /**
     * 请求方法
     */
    private String method;
    /**
     * 请求参数（脱敏后）
     */
    private String requestParams;
    /**
     * 响应数据
     */
    private String responseData;
    /**
     * 工行返回码
     */
    private String returnCode;
    /**
     * 工行返回消息
     */
    private String returnMsg;
    /**
     * 调用状态：1-成功，2-失败
     *
     * 枚举 {@link cn.iocoder.yudao.module.icbc.enums.ApiLogStatusEnum}
     */
    private Integer status;
    /**
     * 耗时（毫秒）
     */
    private Integer costTime;
    /**
     * 业务ID（订单号等）
     */
    private String businessId;
    /**
     * 业务类型
     */
    private String businessType;
    /**
     * 错误信息
     */
    private String errorMsg;

} 
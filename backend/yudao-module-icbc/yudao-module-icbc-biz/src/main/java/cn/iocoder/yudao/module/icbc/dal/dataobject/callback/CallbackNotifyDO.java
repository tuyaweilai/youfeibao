package cn.iocoder.yudao.module.icbc.dal.dataobject.callback;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 工行回调通知 DO
 *
 * @author 芋道源码
 */
@TableName("icbc_callback_notify")
@KeySequence("icbc_callback_notify_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallbackNotifyDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 通知ID
     */
    private String notifyId;
    /**
     * 通知类型：01 预下单异常、02 支付、03 开票、04 缴税、05 发票上传、
     * 06 发票取消、07 红票申请、08 红票上传、09 红票撤销
     *
     * 枚举 {@link cn.iocoder.yudao.module.icbc.enums.CallbackNotifyTypeEnum}
     */
    private String notifyType;
    /**
     * 业务ID
     */
    private String businessId;
    /**
     * 通知数据
     */
    private String notifyData;
    /**
     * 签名
     */
    private String sign;
    /**
     * 处理状态：0-待处理，1-处理成功，2-处理失败
     *
     * 枚举 {@link cn.iocoder.yudao.module.icbc.enums.CallbackProcessStatusEnum}
     */
    private Integer processStatus;
    /**
     * 处理结果信息
     */
    private String processMsg;
    /**
     * 处理时间
     */
    private LocalDateTime processTime;
    /**
     * 重试次数
     */
    private Integer retryCount;
    /**
     * 租户ID
     */
    private Long tenantId;

} 
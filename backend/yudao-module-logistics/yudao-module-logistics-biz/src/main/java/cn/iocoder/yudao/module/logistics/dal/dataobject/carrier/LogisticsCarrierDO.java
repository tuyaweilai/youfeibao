package cn.iocoder.yudao.module.logistics.dal.dataobject.carrier;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 承运商档案 DO（V3 #70）。
 *
 * <p>承运商是「运力吃紧时承接运输业务的第三方公司」（CONTEXT.md「承运商」）：它是**运输服务的提供方**，
 * 不是交易对方——既不卖货给回收企业，也不在反向开票链路里。
 *
 * <p>司机通过 {@code carrier_id} 挂到承运商上（来源为承运商时非空），于是「运费该付给谁、出了事
 * 责任归谁」有据可查。承运合同与运价归 V8（#75）。
 */
@TableName("logistics_carrier")
@KeySequence("logistics_carrier_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogisticsCarrierDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 承运商名称（租户内唯一） */
    private String name;

    /** 联系人 */
    private String contactName;

    /** 联系电话 */
    private String contactMobile;

    /**
     * 状态：0-合作中，1-已停用
     *
     * <p>停用而不是删除：历史任务上的司机与运费要留着（与品类的做法一致）。
     */
    private Integer status;

    /** 备注 */
    private String remark;

}

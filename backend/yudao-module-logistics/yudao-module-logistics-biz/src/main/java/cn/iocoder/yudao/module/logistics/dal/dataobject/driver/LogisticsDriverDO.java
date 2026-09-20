package cn.iocoder.yudao.module.logistics.dal.dataobject.driver;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 司机档案 DO（V2a #77）。
 *
 * <p>司机是**回收企业建档的租户内账号**（ADR 0032 第 9 条）：自有司机与承运商司机同构，
 * 差别只在 {@code source} 标记。第三方司机不自主注册，账号由回收企业创建。
 *
 * <p>本票只做最小档案（关联租户内用户、姓名、手机号、来源、状态）。驾驶证与从业资格证号码与到期日、
 * 准驾车型、以及「证件过期不得派出」的门禁归 V3（#70）。
 *
 * <p>{@code userId} 指向租户内的系统用户：司机要登录司机端，登录体系与收货员现场端同一套
 * （`/admin-api` + token），不新建鉴权体系。
 */
@TableName("logistics_driver")
@KeySequence("logistics_driver_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogisticsDriverDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 关联的租户内系统用户编号（司机登录司机端用；租户内唯一） */
    private Long userId;

    /** 司机姓名 */
    private String name;

    /** 手机号 */
    private String mobile;

    /**
     * 司机来源
     *
     * 枚举 {@link cn.iocoder.yudao.module.logistics.enums.LogisticsDriverSourceEnum}
     */
    private Integer source;

    /**
     * 司机状态
     *
     * 枚举 {@link cn.iocoder.yudao.module.logistics.enums.LogisticsDriverStatusEnum}
     */
    private Integer status;

    /** 备注 */
    private String remark;

}

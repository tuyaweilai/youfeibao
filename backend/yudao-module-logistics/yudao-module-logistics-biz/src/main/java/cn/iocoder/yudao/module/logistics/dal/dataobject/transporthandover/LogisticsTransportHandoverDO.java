package cn.iocoder.yudao.module.logistics.dal.dataobject.transporthandover;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交接登记 DO（V6 #73）。
 *
 * <p>司机在**提货点**就一个停靠点登记的交接事实：品类、参考量、参考单价与凭证照片。
 * 它是「现场谈好的事」的留痕，**不是收购单**（ADR 0031）：
 * <ul>
 *   <li>**现场不产生金额**——结算重量回场复磅才定，金额那时才由收购单算出来，所以本表没有金额字段；</li>
 *   <li>{@code referenceQuantity} / {@code referenceUnitPrice} 是**现场约定值**，不是计量事实，
 *       收购时会被复磅结果与收货员的修正覆盖（修正必须留原因）；</li>
 *   <li>缺身份证或银行卡时记 {@code documentStatus = PENDING}（待补档）：事实照记，回场生成的
 *       收购单被付款与开票门禁拦住，不进开票申请、不进台账口径、不计入额度。</li>
 * </ul>
 *
 * <p>出售者与品类只存 icbc 侧编号 + 快照（ADR 0028 / 0032）：{@code payeeId} 是收方档案编号、
 * {@code goodsConfigId} 是品类配置编号，物流不知道它们长什么样，也不引用 icbc 的类。
 *
 * <p>同一个停靠点只登记一次：{@code (tenant_id, stop_id)} 在服务层校验（不建 DB 唯一键的理由同
 * {@code logistics_vehicle}——本表逻辑删除，唯一键会让删掉的行永远占着键值）。
 */
@TableName("logistics_transport_handover")
@KeySequence("logistics_transport_handover_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogisticsTransportHandoverDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 交接登记单号（平台生成，租户内唯一） */
    private String handoverNo;

    /** 运输任务编号 */
    private Long taskId;
    /** 运输任务单号（冗余） */
    private String taskNo;
    /** 停靠点编号（一个停靠点一次交接登记） */
    private Long stopId;
    /**
     * 提货地址快照（上门提货的实际提货地址）
     *
     * <p>它是 icbc 侧收购单**交易地址**的来源：场站是**归属场站**（这一趟由哪个场站派车），
     * 实际提货发生在别处（ADR 0031）。
     */
    private String address;

    /** 出售者编号（icbc 侧收方档案编号） */
    private Long payeeId;
    /** 出售者姓名快照 */
    private String payeeName;
    /** 出售者手机号快照 */
    private String payeeMobile;

    /** 品类配置编号（icbc 侧编号；权威品类，ADR 0028） */
    private Long goodsConfigId;
    /** 品类名称快照 */
    private String categoryName;
    /** 计量单位快照 */
    private String unit;

    /** 参考量（现场约定值，不是计量事实） */
    private BigDecimal referenceQuantity;
    /** 参考单价（现场约定值，收购时可修正并留原因） */
    private BigDecimal referenceUnitPrice;

    /** 凭证照片 URL 列表（JSON 数组文本） */
    private String photos;

    /** 司机编号（引用 + 快照并存，ADR 0032 第 7 条） */
    private Long driverId;
    /** 司机姓名快照 */
    private String driverName;
    /** 司机手机号快照 */
    private String driverMobile;
    /** 车辆编号 */
    private Long vehicleId;
    /** 车牌号快照 */
    private String plateNo;

    /** 交接发生时间 */
    private LocalDateTime occurTime;

    /**
     * 要件状态
     *
     * 枚举 {@link cn.iocoder.yudao.module.logistics.enums.LogisticsHandoverDocumentStatusEnum}
     */
    private String documentStatus;
    /** 缺什么（待补档时说明） */
    private String documentGap;

    /** 客户端请求号（弱网重复提交的幂等键） */
    private String clientRequestId;

    /** 备注 */
    private String remark;

}

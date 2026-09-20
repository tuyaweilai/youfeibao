package cn.iocoder.yudao.module.logistics.api.transport.dto;

import cn.iocoder.yudao.module.logistics.enums.LogisticsHandoverDocumentStatusEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 交接登记响应 DTO（V6 #73）：司机在提货点登记的**现场交接事实**。
 *
 * <p>它是「现场谈好的事」的留痕，**不是收购单**（ADR 0031）：现场不产生金额，收购单要等回场复磅。
 * 因此这里只有**参考量**与**参考单价**，没有金额字段——金额由收购单按有效磅次算出来。
 *
 * <p>出售者与品类存 icbc 侧编号（物流不引用 icbc 的类，ADR 0032）：`payeeId` 是收方档案编号，
 * `goodsConfigId` 是品类配置编号（ADR 0028 的品类权威），两者都配姓名 / 名称快照。
 *
 * <p>icbc 侧的两个用途：
 * <ul>
 *   <li>回场复磅时按本登记建交接批次（把参考值、要件状态、司机与车辆引用搬过去），
 *       并给磅房看**现场参考量与照片凭证**；</li>
 *   <li>追溯时按 {@code id} 取这一趟的运输节点与凭证（见
 *       {@link cn.iocoder.yudao.module.logistics.api.transport.LogisticsTransportApi#getEvidenceListByHandoverId}）。</li>
 * </ul>
 *
 * @author 芋道源码
 */
@Data
public class LogisticsTransportHandoverRespDTO {

    /** 交接登记编号 */
    private Long id;
    /** 交接登记单号 */
    private String handoverNo;
    /** 运输任务编号 */
    private Long taskId;
    /** 运输任务单号 */
    private String taskNo;
    /** 停靠点编号（一个停靠点一次交接登记） */
    private Long stopId;
    /** 提货地址快照（上门提货的实际提货地址；落到收购单的交易地址） */
    private String address;

    /** 出售者编号（icbc 侧收方档案编号，可空：临时散户现场才建档） */
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

    /** 参考量（现场约定值，**不是计量事实**） */
    private BigDecimal referenceQuantity;
    /** 参考单价（现场约定值，收购时可修正并留原因） */
    private BigDecimal referenceUnitPrice;

    /** 凭证照片 URL 列表 */
    private List<String> photos;

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

    /** 要件状态，枚举 {@link LogisticsHandoverDocumentStatusEnum} */
    private String documentStatus;
    /** 要件状态名 */
    private String documentStatusName;
    /** 缺什么（待补档时说明） */
    private String documentGap;

    /** 备注 */
    private String remark;

}

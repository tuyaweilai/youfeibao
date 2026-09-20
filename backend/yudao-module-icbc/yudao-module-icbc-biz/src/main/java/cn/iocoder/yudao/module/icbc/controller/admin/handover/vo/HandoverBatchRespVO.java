package cn.iocoder.yudao.module.icbc.controller.admin.handover.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 / 收货员现场端 - 交接批次 Response VO")
@Data
public class HandoverBatchRespVO {

    @Schema(description = "批次编号")
    private Long id;

    @Schema(description = "批次号")
    private String batchNo;

    @Schema(description = "出售者（交易对方）档案编号")
    private Long payeeId;

    @Schema(description = "出售者姓名")
    private String sellerName;

    @Schema(description = "出售者联系方式")
    private String sellerMobile;

    @Schema(description = "场站编号")
    private Long stationId;

    @Schema(description = "场站名称")
    private String stationName;

    @Schema(description = "上门地址")
    private String visitAddress;

    @Schema(description = "交接时间")
    private LocalDateTime occurTime;

    @Schema(description = "来源方式：APPOINTMENT / WALK_IN / ON_SITE")
    private String sourceType;

    @Schema(description = "来源方式名称")
    private String sourceTypeName;

    @Schema(description = "司机姓名")
    private String driverName;

    @Schema(description = "司机手机号")
    private String driverMobile;

    @Schema(description = "车牌号")
    private String plateNo;

    @Schema(description = "关联的到站预约编号（可空）")
    private Long appointmentId;

    @Schema(description = "关联的采购订单编号（可空）")
    private Long purchaseOrderId;

    @Schema(description = "本批次已产生的收购单张数（未作废的）")
    private Long acquisitionCount;

    @Schema(description = "有效磅次是否已锁定：true-已产生收购单，不能再改有效磅次")
    private Boolean weighingChangeLocked;

    @Schema(description = "有效磅次编号；为空表示尚未指定")
    private Long effectiveWeighingId;

    @Schema(description = "有效磅次是第几次；为空表示尚未指定")
    private Integer effectiveWeighingSeqNo;

    @Schema(description = "磅次明细（含留档不参与的那些）")
    private List<HandoverWeighingRespVO> weighingList;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}

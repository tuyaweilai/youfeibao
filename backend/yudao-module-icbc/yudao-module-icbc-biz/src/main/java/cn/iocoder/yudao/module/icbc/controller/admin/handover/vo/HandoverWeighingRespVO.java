package cn.iocoder.yudao.module.icbc.controller.admin.handover.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 磅次 Response VO")
@Data
public class HandoverWeighingRespVO {

    @Schema(description = "磅次编号")
    private Long id;

    @Schema(description = "交接批次编号")
    private Long batchId;

    @Schema(description = "第几次磅次（计量结果引用它的值与此版本号）")
    private Integer seqNo;

    @Schema(description = "毛重")
    private BigDecimal grossWeight;

    @Schema(description = "皮重")
    private BigDecimal tareWeight;

    @Schema(description = "净重 = 毛重 − 皮重")
    private BigDecimal netWeight;

    @Schema(description = "过磅时间")
    private LocalDateTime weighTime;

    @Schema(description = "磅单号")
    private String weightTicketNo;

    @Schema(description = "磅单照片地址")
    private String weightTicketImageUrl;

    @Schema(description = "磅单上的车牌号")
    private String plateNo;

    @Schema(description = "是否有效磅次：true-参与计量，false-留档不参与")
    private Boolean effective;

    @Schema(description = "有效磅次文案：参与计量 / 留档不参与")
    private String effectiveText;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}

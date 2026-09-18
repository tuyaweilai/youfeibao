package cn.iocoder.yudao.module.icbc.controller.admin.acquisition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 管理后台 - 收购登记识别结果人工修正 Request VO。
 *
 * <p>磅单与车牌的识别结果允许人工修正（见 issue #7）；修正后重新做车牌比对。
 * 只更新显式传入的字段，不传的保持原值。
 */
@Schema(description = "管理后台 - 收购登记识别结果人工修正 Request VO")
@Data
public class AcquisitionCorrectionReqVO {

    @Schema(description = "收购单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "收购单编号不能为空")
    private Long id;

    @Schema(description = "毛重", example = "18000.00")
    private BigDecimal grossWeight;

    @Schema(description = "皮重", example = "5500.00")
    private BigDecimal tareWeight;

    @Schema(description = "净重", example = "12500.00")
    private BigDecimal netWeight;

    @Schema(description = "磅单号", example = "WD20261201001")
    private String weightTicketNo;

    @Schema(description = "磅单识别出的车牌号", example = "京A12345")
    private String weightTicketPlateNo;

    @Schema(description = "车头车尾照片识别出的车牌号", example = "京A12345")
    private String vehiclePlateNo;

    @Schema(description = "修正说明", example = "磅单识别把 3 看成 8，已按实物改正")
    private String remark;

}

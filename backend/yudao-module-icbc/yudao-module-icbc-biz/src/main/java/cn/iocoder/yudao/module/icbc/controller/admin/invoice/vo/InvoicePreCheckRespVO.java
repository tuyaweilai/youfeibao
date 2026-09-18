package cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 开票申请前置校验结果。
 *
 * <p>逐项列出五类校验（租户三层资质 / 出售者状态 / 票种与计税方法 / 品类编码 / 收购要件），
 * 让开票员一次看清所有不满足的条目与补齐方式。
 */
@Schema(description = "管理后台 - 开票申请前置校验结果")
@Data
public class InvoicePreCheckRespVO {

    @Schema(description = "收购单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long acquisitionId;

    @Schema(description = "收购单号", example = "ACQ202601011200001234")
    private String acquisitionNo;

    @Schema(description = "是否全部通过", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean allPassed;

    @Schema(description = "逐项校验结果", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<InvoicePreCheckItemVO> items;
}

package cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 开票申请前置校验的单项结果。
 *
 * <p>校验不通过时不止「失败」这一个信息：{@code message} 说明哪一条不满足，
 * {@code remedy} 说明怎么补齐。开票员据此自助解决，不必找客服。
 */
@Schema(description = "管理后台 - 开票申请前置校验单项")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoicePreCheckItemVO {

    @Schema(description = "校验项编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TENANT_QUALIFICATION")
    private String code;

    @Schema(description = "校验项名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "租户三层资质")
    private String name;

    @Schema(description = "是否通过", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean passed;

    @Schema(description = "说明：不通过时指出具体哪一条不满足", example = "租户三层资质不齐或已失效")
    private String message;

    @Schema(description = "如何补齐", example = "在「租户开票就绪 · 三层资质」补齐税务侧 / 行业侧 / 公安侧资质并核实")
    private String remedy;
}

package cn.iocoder.yudao.module.icbc.controller.admin.tax.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 一条申报缺项：哪里不齐、有多少、怎么补。
 */
@Schema(description = "管理后台 - 申报缺项 Response VO")
@Data
public class TaxMissingDataVO {

    @Schema(description = "缺项类型", example = "INVOICE_NOT_ISSUED")
    private String type;

    @Schema(description = "缺项名称", example = "发票未开出")
    private String typeName;

    @Schema(description = "说明", example = "本月还有 2 张发票未开出，其销售额尚未纳入申报")
    private String message;

    @Schema(description = "涉及数量", example = "2")
    private Integer count;

    @Schema(description = "怎么补")
    private String remedy;

    @Schema(description = "示例（发票号 / 出售者姓名）")
    private List<String> samples;

}

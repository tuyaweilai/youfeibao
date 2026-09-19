package cn.iocoder.yudao.module.icbc.controller.admin.tax.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 代办税费申报明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TaxDeclarationItemPageReqVO extends PageParam {

    @Schema(description = "申报单编号", example = "1024")
    private Long declarationId;

    @Schema(description = "申报月 yyyy-MM", example = "2026-08")
    private String periodMonth;

    @Schema(description = "出售者档案编号", example = "2048")
    private Long payeeId;

    @Schema(description = "出售者姓名", example = "张三")
    private String sellerName;

    @Schema(description = "是否当月销售额超过 10 万元需要单独列出", example = "true")
    private Boolean overExempt;

}

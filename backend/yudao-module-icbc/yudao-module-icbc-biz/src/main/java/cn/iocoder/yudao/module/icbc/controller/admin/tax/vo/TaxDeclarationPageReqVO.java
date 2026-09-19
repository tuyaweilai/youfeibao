package cn.iocoder.yudao.module.icbc.controller.admin.tax.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 代办税费申报单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TaxDeclarationPageReqVO extends PageParam {

    @Schema(description = "申报月 yyyy-MM", example = "2026-08")
    private String periodMonth;

    @Schema(description = "状态：0-待申报，1-已申报待缴款，2-已缴款", example = "0")
    private Integer status;

}

package cn.iocoder.yudao.module.icbc.controller.admin.acquisition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 管理后台 - 收购单补档 Request VO（V6 #73）。
 *
 * <p>缺身份证或银行卡的交接先记为**待补档**（ADR 0030 第 4 条）：付款与开票被门禁拦住。
 * 证件补齐后在这里放行——留办理人与时间，不以「改一个状态」了事。
 */
@Schema(description = "管理后台 - 收购单补档 Request VO")
@Data
public class AcquisitionCompleteDocumentsReqVO {

    @Schema(description = "收购单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "收购单编号不能为空")
    private Long id;

    @Schema(description = "补档说明（补了什么，如「身份证已核验」「银行卡已补登」）", example = "身份证与银行卡已补齐并核验")
    private String remark;

}

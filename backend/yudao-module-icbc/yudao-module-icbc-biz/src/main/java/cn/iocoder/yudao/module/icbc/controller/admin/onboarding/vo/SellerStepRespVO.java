package cn.iocoder.yudao.module.icbc.controller.admin.onboarding.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 出售者建档某一步的返回：工行页面表单 HTML
 */
@Schema(description = "管理后台 - 出售者建档步骤返回")
@Data
public class SellerStepRespVO {

    @Schema(description = "出售者（收方）编号", example = "1024")
    private Long payeeId;

    @Schema(description = "工行页面自动提交表单 HTML，交给前端 openIcbcForm() 打新窗口")
    private String formHtml;

    @Schema(description = "当前步骤", example = "REAL_NAME")
    private String step;

}

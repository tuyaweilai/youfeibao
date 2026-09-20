package cn.iocoder.yudao.module.logistics.controller.admin.driver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 司机新增/修改 Request VO")
@Data
public class LogisticsDriverSaveReqVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "关联的租户内系统用户编号（司机用它登录司机端，租户内唯一）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "关联用户不能为空")
    private Long userId;

    @Schema(description = "司机姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotEmpty(message = "司机姓名不能为空")
    private String name;

    @Schema(description = "手机号", example = "13800138000")
    private String mobile;

    @Schema(description = "司机来源：1-自有，2-承运商", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "司机来源不能为空")
    private Integer source;

    @Schema(description = "司机状态：0-在职，1-离职，2-请假", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "司机状态不能为空")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

}

package cn.iocoder.yudao.module.logistics.controller.admin.driver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

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

    @Schema(description = "所属承运商编号（来源为承运商时必填）", example = "1")
    private Long carrierId;

    @Schema(description = "驾驶证号码", example = "3301...")
    private String drivingLicenseNo;

    @Schema(description = "准驾车型", example = "A2")
    private String drivingLicenseType;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "驾驶证到期日（过期不得派出；可由管理员带原因授权放行）", example = "2030-12-31")
    private LocalDate drivingLicenseExpiryDate;

    @Schema(description = "从业资格证号码")
    private String qualificationCertNo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "从业资格证到期日（同上）", example = "2030-12-31")
    private LocalDate qualificationCertExpiryDate;

    @Schema(description = "司机状态：0-在职，1-离职，2-请假", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "司机状态不能为空")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

}

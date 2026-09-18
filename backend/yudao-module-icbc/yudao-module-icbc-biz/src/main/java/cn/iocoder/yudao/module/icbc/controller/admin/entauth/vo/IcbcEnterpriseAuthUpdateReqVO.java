package cn.iocoder.yudao.module.icbc.controller.admin.entauth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 回填工行企业授权结果 Request VO")
@Data
public class IcbcEnterpriseAuthUpdateReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "编号不能为空")
    private Long id;

    @Schema(description = "授权状态：0-未授权，1-已授权，2-已失效", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "授权状态不能为空")
    private Integer authStatus;

    @Schema(description = "授权时间（不传且状态为已授权时，默认取当前时间）")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime authTime;

    @Schema(description = "授权有效期止")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    @Schema(description = "备注")
    private String remark;

}

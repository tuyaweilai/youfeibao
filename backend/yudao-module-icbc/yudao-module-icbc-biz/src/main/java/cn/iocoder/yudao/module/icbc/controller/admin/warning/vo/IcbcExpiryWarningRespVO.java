package cn.iocoder.yudao.module.icbc.controller.admin.warning.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 资质到期预警 Response VO")
@Data
public class IcbcExpiryWarningRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "资质编号")
    private Long qualificationId;

    @Schema(description = "资质层")
    private String type;

    @Schema(description = "资质名称")
    private String name;

    @Schema(description = "有效期止")
    private LocalDate validTo;

    @Schema(description = "状态：0-待处理，1-已处理")
    private Integer status;

    @Schema(description = "预警时间")
    private LocalDateTime warnedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}

package cn.iocoder.yudao.module.icbc.controller.admin.qualification.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 租户资质 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class IcbcQualificationRespVO extends IcbcQualificationSaveReqVO {

    @Schema(description = "租户编号")
    private Long tenantId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "是否临近到期（30 天内）")
    private Boolean expiringSoon;

}

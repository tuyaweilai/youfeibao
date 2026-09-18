package cn.iocoder.yudao.module.icbc.controller.admin.entauth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 工行企业授权 Response VO")
@Data
public class IcbcEnterpriseAuthRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "付方编号")
    private String outVendorId;

    @Schema(description = "工行入参 siteType")
    private String siteType;

    @Schema(description = "工行入参 userType")
    private String userType;

    @Schema(description = "授权状态：0-未授权，1-已授权，2-已失效")
    private Integer authStatus;

    @Schema(description = "授权时间")
    private LocalDateTime authTime;

    @Schema(description = "授权有效期止")
    private LocalDateTime expireTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}

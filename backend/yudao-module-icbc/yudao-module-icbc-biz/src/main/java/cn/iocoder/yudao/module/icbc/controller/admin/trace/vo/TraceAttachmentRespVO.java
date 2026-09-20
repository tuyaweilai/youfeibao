package cn.iocoder.yudao.module.icbc.controller.admin.trace.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理后台 - 关联单据查询附件（#55 T17，AC3）。
 */
@Schema(description = "管理后台 - 关联单据查询附件")
@Data
public class TraceAttachmentRespVO {

    @Schema(description = "附件名称", example = "磅单照片")
    private String name;

    @Schema(description = "文件地址")
    private String url;

    @Schema(description = "来源单据类型", example = "ACQUISITION")
    private String sourceType;

    @Schema(description = "来源单据编号", example = "ACQ17645472000001234")
    private String sourceNo;

}

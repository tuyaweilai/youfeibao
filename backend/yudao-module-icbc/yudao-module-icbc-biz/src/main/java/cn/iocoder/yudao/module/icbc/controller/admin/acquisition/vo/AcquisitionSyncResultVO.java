package cn.iocoder.yudao.module.icbc.controller.admin.acquisition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理后台 - 收购登记离线补传的单条结果。
 *
 * <p>逐条返回成败：一条失败不影响其他条，现场端据此只重传失败的那几条。
 */
@Schema(description = "管理后台 - 收购登记离线补传结果 VO")
@Data
public class AcquisitionSyncResultVO {

    @Schema(description = "客户端幂等键")
    private String clientRequestId;

    @Schema(description = "收购单编号；失败时为 null", example = "1024")
    private Long id;

    @Schema(description = "收购单号；失败时为 null", example = "ACQ17645472000001234")
    private String acquisitionNo;

    @Schema(description = "是否成功")
    private Boolean success;

    @Schema(description = "是否命中去重（服务端已有同一 clientRequestId 的收购单）")
    private Boolean duplicated;

    @Schema(description = "失败原因；成功时为 null")
    private String errorMsg;

}

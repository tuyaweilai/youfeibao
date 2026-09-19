package cn.iocoder.yudao.module.icbc.controller.admin.callback.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 工行回调通知 Response VO")
@Data
public class CallbackNotifyRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "通知ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "NOTIFY123456")
    private String notifyId;

    @Schema(description = "通知类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "03")
    private String notifyType;

    @Schema(description = "通知类型名", example = "开票")
    private String notifyTypeName;

    @Schema(description = "关联业务类型名", example = "开票")
    private String businessName;

    @Schema(description = "业务ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "ORDER123456")
    private String businessId;

    @Schema(description = "通知数据", requiredMode = Schema.RequiredMode.REQUIRED, example = "{\"status\":\"APPROVED\"}")
    private String notifyData;

    @Schema(description = "签名", example = "ABC123...")
    private String sign;

    @Schema(description = "处理状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer processStatus;

    @Schema(description = "处理状态名", example = "处理成功")
    private String processStatusName;

    @Schema(description = "失败时的下一步动作，便于定位与重放", example = "处理失败，可在「重放」中重新处理")
    private String nextAction;

    @Schema(description = "处理结果信息", example = "处理成功")
    private String processMsg;

    @Schema(description = "处理时间")
    private LocalDateTime processTime;

    @Schema(description = "重试次数", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer retryCount;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

} 
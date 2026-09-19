package cn.iocoder.yudao.module.icbc.controller.admin.callback.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 平台运营 - 工行回调通知处理概览。
 *
 * <p>回答「九类通知各自处理成没成」：总数 / 待处理 / 成功 / 失败，以及按通知类型细分。
 */
@Schema(description = "管理后台 - 工行回调通知处理概览 Response VO")
@Data
public class CallbackNotifySummaryRespVO {

    @Schema(description = "通知总数", requiredMode = Schema.RequiredMode.REQUIRED, example = "120")
    private Long total;

    @Schema(description = "待处理数", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Long pendingCount;

    @Schema(description = "处理成功数", requiredMode = Schema.RequiredMode.REQUIRED, example = "117")
    private Long successCount;

    @Schema(description = "处理失败数", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long failureCount;

    @Schema(description = "按通知类型细分", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<TypeStat> types;

    @Schema(description = "管理后台 - 工行回调通知类型统计")
    @Data
    public static class TypeStat {

        @Schema(description = "通知类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "03")
        private String notifyType;

        @Schema(description = "通知类型名", example = "开票")
        private String notifyTypeName;

        @Schema(description = "关联业务名", example = "开票")
        private String businessName;

        @Schema(description = "该类型通知数", requiredMode = Schema.RequiredMode.REQUIRED, example = "40")
        private Long total;

        @Schema(description = "该类型失败数", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        private Long failureCount;

    }

}

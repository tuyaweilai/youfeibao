package cn.iocoder.yudao.module.logistics.controller.admin.transporthandover.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理后台 / 司机端 - 交接登记 Request VO（V6 #73）。
 *
 * <p>**没有金额字段**：现场不产生金额，收购单在回场复磅后生成（ADR 0031）。
 * 参考量与参考单价是现场约定值，会被复磅结果与收货员的修正覆盖（修正留原因）。
 *
 * <p>出售者与品类都是 icbc 侧编号（ADR 0032：物流只存 icbc 侧编号），名称 / 单位以**快照**一起上送
 * ——物流不读 icbc 的档案，快照让历史登记不被档案改名影响。
 */
@Schema(description = "管理后台 - 交接登记 Request VO")
@Data
public class LogisticsTransportHandoverCreateReqVO {

    @Schema(description = "运输任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "运输任务编号不能为空")
    private Long taskId;

    @Schema(description = "停靠点编号（任务有停靠点时必填：集货时不能把几家混成一次交接）", example = "1")
    private Long stopId;

    @Schema(description = "提货地址（不填取停靠点上的地址；上门提货的实际提货地址会落到收购单的交易地址）",
            example = "某某路 1 号")
    private String address;

    @Schema(description = "出售者编号（icbc 侧收方档案编号；不填时取停靠点上登记的出售者）", example = "1024")
    private Long payeeId;

    @Schema(description = "出售者姓名快照", example = "张三")
    private String payeeName;

    @Schema(description = "出售者手机号快照", example = "13800138000")
    private String payeeMobile;

    @Schema(description = "品类配置编号（icbc 侧编号；权威品类，ADR 0028）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "品类不能为空")
    private Long goodsConfigId;

    @Schema(description = "品类名称快照", example = "废钢铁")
    private String categoryName;

    @Schema(description = "计量单位快照", example = "吨")
    private String unit;

    @Schema(description = "参考量（现场约定值，不是计量事实）", requiredMode = Schema.RequiredMode.REQUIRED, example = "12.5")
    @NotNull(message = "参考量不能为空")
    private BigDecimal referenceQuantity;

    @Schema(description = "参考单价（现场约定值；收购时未修正则作为成交单价）", example = "2600.00")
    private BigDecimal referenceUnitPrice;

    @Schema(description = "凭证照片 URL 列表（现场凭证，至少一张）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "交接凭证照片不能为空")
    private List<String> photos;

    @Schema(description = "要件状态：COMPLETE-已齐（默认），PENDING-待补档（缺身份证或银行卡）", example = "PENDING")
    private String documentStatus;

    @Schema(description = "缺什么（待补档时必填，如「缺身份证」「缺银行卡」）", example = "缺身份证")
    private String documentGap;

    @Schema(description = "交接发生时间；不填取登记时刻")
    private LocalDateTime occurTime;

    @Schema(description = "客户端请求号（弱网重复提交的幂等键）", example = "9f1c2a7e-3b4d-4c5e-8f6a-0b1c2d3e4f50")
    private String clientRequestId;

    @Schema(description = "备注", example = "现场目测含少量杂质")
    private String remark;

}

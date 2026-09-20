package cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理后台 - 采购履约配置 Response VO（#47 T09）。
 *
 * <p>配置项一律带「口径说明」（{@code *Definition}），界面上与选项一起显示——用户看到的每一项都能
 * 追到它是什么、影响哪个动作，这是 ADR 0021 对状态与数字的要求。
 */
@Schema(description = "管理后台 - 采购履约配置 Response VO")
@Data
public class PurchaseOrderSettingRespVO {

    @Schema(description = "完成比例采用的履约口径：ACCEPTED / SETTLED")
    private String performanceBasis;

    @Schema(description = "完成比例口径名")
    private String performanceBasisName;

    @Schema(description = "完成比例口径说明")
    private String performanceBasisDefinition;

    @Schema(description = "超量交货的处理方式：BLOCK / APPROVAL")
    private String overQuantityRule;

    @Schema(description = "超量交货处理方式名")
    private String overQuantityRuleName;

    @Schema(description = "过期交货的处理方式：BLOCK / APPROVAL")
    private String expiredRule;

    @Schema(description = "过期交货处理方式名")
    private String expiredRuleName;

    @Schema(description = "跨场站交货的处理方式：BLOCK / APPROVAL")
    private String crossStationRule;

    @Schema(description = "跨场站交货处理方式名")
    private String crossStationRuleName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "口径说明")
    private String scopeNote;

}

package cn.iocoder.yudao.module.icbc.controller.admin.onboarding.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 框架收购协议新增 / 修改请求
 *
 * <p>名称、数量、规格、回收期次、结算方式为税总 5 号公告第十七条要求的要素，全部必填。
 */
@Schema(description = "管理后台 - 框架收购协议新增/修改请求")
@Data
public class FrameworkAgreementSaveReqVO {

    @Schema(description = "主键（修改时传）", example = "1024")
    private Long id;

    @Schema(description = "出售者（收方）编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "出售者编号不能为空")
    private Long payeeId;

    @Schema(description = "协议编号（留空由后端生成）", example = "FW202609190001")
    private String agreementNo;

    @Schema(description = "货物名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "废钢")
    @NotEmpty(message = "货物名称不能为空")
    @Size(max = 200, message = "货物名称长度不能超过200个字符")
    private String productName;

    @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "5 吨")
    @NotEmpty(message = "数量不能为空")
    @Size(max = 100, message = "数量长度不能超过100个字符")
    private String quantity;

    @Schema(description = "规格", requiredMode = Schema.RequiredMode.REQUIRED, example = "重型废钢")
    @NotEmpty(message = "规格不能为空")
    @Size(max = 100, message = "规格长度不能超过100个字符")
    private String specification;

    @Schema(description = "回收期次", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026 年 9 月第 1 期")
    @NotEmpty(message = "回收期次不能为空")
    @Size(max = 100, message = "回收期次长度不能超过100个字符")
    private String recyclePeriod;

    @Schema(description = "结算方式", requiredMode = Schema.RequiredMode.REQUIRED, example = "银行转账，过磅后 3 日内结清")
    @NotEmpty(message = "结算方式不能为空")
    @Size(max = 200, message = "结算方式长度不能超过200个字符")
    private String settlementMethod;

    @Schema(description = "签署方式：ELECTRONIC-电子签章，PAPER-纸质签署", example = "ELECTRONIC")
    @Size(max = 20, message = "签署方式长度不能超过20个字符")
    private String signMethod;

    @Schema(description = "签署时间")
    private LocalDateTime signedAt;

    @Schema(description = "协议文件地址")
    @Size(max = 500, message = "协议文件地址长度不能超过500个字符")
    private String fileUrl;

    @Schema(description = "状态：0-待签署，1-生效，2-作废", example = "1")
    private Integer status;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

}

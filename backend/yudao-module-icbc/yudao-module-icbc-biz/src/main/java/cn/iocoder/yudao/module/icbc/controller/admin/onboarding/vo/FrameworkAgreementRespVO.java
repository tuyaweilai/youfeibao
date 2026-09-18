package cn.iocoder.yudao.module.icbc.controller.admin.onboarding.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 框架收购协议返回
 */
@Schema(description = "管理后台 - 框架收购协议返回")
@Data
public class FrameworkAgreementRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "出售者（收方）编号")
    private Long payeeId;

    @Schema(description = "协议编号")
    private String agreementNo;

    @Schema(description = "货物名称")
    private String productName;

    @Schema(description = "数量")
    private String quantity;

    @Schema(description = "规格")
    private String specification;

    @Schema(description = "回收期次")
    private String recyclePeriod;

    @Schema(description = "结算方式")
    private String settlementMethod;

    @Schema(description = "签署方式")
    private String signMethod;

    @Schema(description = "签署时间")
    private LocalDateTime signedAt;

    @Schema(description = "协议文件地址")
    private String fileUrl;

    @Schema(description = "状态：0-待签署，1-生效，2-作废")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}

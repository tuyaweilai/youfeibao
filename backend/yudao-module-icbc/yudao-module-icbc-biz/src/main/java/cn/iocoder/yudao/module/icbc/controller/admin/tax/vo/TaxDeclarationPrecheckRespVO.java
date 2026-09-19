package cn.iocoder.yudao.module.icbc.controller.admin.tax.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 申报数据齐备性检查：还缺哪些数据。
 *
 * <p>「申报期临近时预警，并指出还缺哪些数据」——缺的不是金额，而是让清单能站住的东西：
 * 还有票没开出来、还有票缴税没成功、出售者缺身份证、红冲没收口、征收率没识别。
 */
@Schema(description = "管理后台 - 代办税费申报数据齐备性 Response VO")
@Data
public class TaxDeclarationPrecheckRespVO {

    @Schema(description = "申报月 yyyy-MM", example = "2026-08")
    private String periodMonth;

    @Schema(description = "是否可以申报 / 缴款")
    private Boolean ready;

    @Schema(description = "涉及出售者数")
    private Integer sellerCount;

    @Schema(description = "纳入清单的发票张数")
    private Integer invoiceCount;

    @Schema(description = "缺项数量")
    private Integer missingCount;

    @Schema(description = "缺什么、怎么补")
    private List<TaxMissingDataVO> missing;

    @Schema(description = "结论")
    private String message;

}

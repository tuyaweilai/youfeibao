package cn.iocoder.yudao.module.icbc.controller.admin.acquisition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 磅单识别 Response VO（#113）。
 *
 * <p><b>读不出来的字段一律为空</b>，绝不猜：填一个错的重量比空着危险得多。现场端只往**空着**的
 * 输入框里填（人工值优先，ADR 0013）。
 *
 * <p><b>原始文字行一并回</b>（{@code rawLines}）：解析规则靠关键词与坐标取，命中率受拍摄质量影响；
 * 把识别到的字回给收货员，当场能照着念的表填，比一句「识别失败」有用得多。它**只是回显**，
 * 不落库（ADR 0021 / 0037）。
 *
 * <p>重量**照抄磅单上的数字，不做单位换算**：平台对重量没有单位概念（结算重量 × 单价 = 金额 是裸乘），
 * 照抄才与收货员手输的口径一致。磅单上印的是 `Kg`。
 */
@Schema(description = "管理后台 - 磅单识别 Response VO")
@Data
public class AcquisitionWeightTicketRecognitionRespVO {

    @Schema(description = "磅单号；为空表示未识别", example = "2105")
    private String weightTicketNo;

    @Schema(description = "毛重（磅单上多印作「总重 GROSS」，照抄数字、不换算单位）", example = "32220")
    private BigDecimal grossWeight;

    @Schema(description = "皮重", example = "13090")
    private BigDecimal tareWeight;

    @Schema(description = "净重", example = "19130")
    private BigDecimal netWeight;

    @Schema(description = "磅单上的车号（可能只读到一部分，见 warnings）", example = "豫A05648")
    private String plateNo;

    @Schema(description = "扣杂值；磅单上的「扣率 DISCOUNT %」是百分数，这里已换成 0~1 的比例", example = "0.025")
    private BigDecimal deduction;

    @Schema(description = "扣杂录法：磅单上读的是扣率，所以是 RATIO-按比例（ADR 0019）", example = "RATIO")
    private String deductionMethod;

    @Schema(description = "提示类告警（可读文案）：重量不自洽 / 车号可能不完整……；不拦继续")
    private List<String> warnings;

    @Schema(description = "识别到的原始文字行（按识别顺序），供现场端展开核对；不落库")
    private List<String> rawLines;

}

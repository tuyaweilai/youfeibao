package cn.iocoder.yudao.module.icbc.controller.admin.workbench.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工作台待办的**来源明细**行（#56 T18）。
 *
 * <p>每项待办都能下钻到「到底是哪几笔」。各待办的单据字段不同，所以这里取的是它们的共同投影：
 * 单号、标题（谁）、副标题（卖什么 / 在哪个场站）、状态、时间与金额。前端按 {@code code} 决定
 * 点击后进哪个模块看全部，本行只负责把来源摆出来，不做二次加工。
 */
@Schema(description = "管理后台 - 工作台待办来源明细")
@Data
public class WorkbenchItemRespVO {

    @Schema(description = "来源单据编号", example = "1024")
    private Long id;

    @Schema(description = "来源单号", example = "ST202612010001")
    private String no;

    @Schema(description = "标题（出售者 / 自然人或车牌）", example = "张三")
    private String title;

    @Schema(description = "副标题（品类 / 场站 / 单号等）", example = "废钢 M2 · 城东场站")
    private String subtitle;

    @Schema(description = "状态名", example = "待到站")
    private String statusName;

    @Schema(description = "时间（到站时间 / 登记时间 / 生成时间 / 异常发生时间）")
    private LocalDateTime time;

    @Schema(description = "金额（元）；无金额口径的单据为空", example = "14740.00")
    private BigDecimal amount;

}

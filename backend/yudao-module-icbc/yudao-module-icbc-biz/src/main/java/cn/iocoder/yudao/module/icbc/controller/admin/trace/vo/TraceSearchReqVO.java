package cn.iocoder.yudao.module.icbc.controller.admin.trace.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 管理后台 - 关联单据查询 Request VO（#55 T17，只读）。
 *
 * <p>按单号 / 车牌 / 主体反查「这批货经历了什么」。至少要给出一个条件，否则报
 * {@code TRACE_QUERY_CONDITION_REQUIRED}——不做无条件全表翻页（那是经营报表的事，#57）。
 */
@Schema(description = "管理后台 - 关联单据查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TraceSearchReqVO extends PageParam {

    @Schema(description = "查号方式，枚举 TraceKeywordTypeEnum；为空按 AUTO 自动识别", example = "AUTO")
    private String keywordType;

    @Schema(description = "关键字：单号 / 车牌 / 出售者姓名或手机号", example = "ACQ17645472000001234")
    private String keyword;

    @Schema(description = "收购单号（与 keyword 二选一，或叠加使用）", example = "ACQ17645472000001234")
    private String acquisitionNo;

    @Schema(description = "车牌号（也匹配交接批次上的车牌）", example = "京A12345")
    private String plateNo;

    @Schema(description = "出售者姓名或手机号", example = "张三")
    private String sellerName;

    @Schema(description = "交易时间范围")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] tradeTime;

    @Schema(description = "只看有差异 / 缺失关联的；null-不限", example = "true")
    private Boolean onlyDifference;

}

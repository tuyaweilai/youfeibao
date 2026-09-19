package cn.iocoder.yudao.module.icbc.controller.admin.acquisition.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 单笔收购确认书（可打印）。
 *
 * <p>对应税总 5 号公告第十七条「收购合同或协议」：载明名称、数量、规格、回收期次、
 * 结算方式。这里是针对单笔收购的确认书，是应对「稽查要求每笔一笔合同」的保险。
 */
@Schema(description = "管理后台 - 单笔收购确认书 VO")
@Data
@ExcelIgnoreUnannotated
public class AcquisitionConfirmationRespVO {

    @Schema(description = "收购单号（合同编号）", example = "ACQ17645472000001234")
    @ExcelProperty("合同编号")
    private String acquisitionNo;

    @Schema(description = "出售者姓名", example = "张三")
    @ExcelProperty("出售者姓名")
    private String sellerName;

    @Schema(description = "出售者联系方式", example = "13800138000")
    @ExcelProperty("出售者联系方式")
    private String sellerMobile;

    @Schema(description = "报废产品名称", example = "废钢")
    @ExcelProperty("报废产品名称")
    private String productName;

    @Schema(description = "规格", example = "重型")
    @ExcelProperty("规格")
    private String specification;

    @Schema(description = "数量", example = "12.5")
    @ExcelProperty("数量")
    private BigDecimal quantity;

    @Schema(description = "计量单位", example = "吨")
    @ExcelProperty("计量单位")
    private String unit;

    @Schema(description = "含税单价", example = "2600.00")
    @ExcelProperty("含税单价(元)")
    private BigDecimal unitPrice;

    @Schema(description = "金额", example = "32500.00")
    @ExcelProperty("金额(元)")
    private BigDecimal amount;

    @Schema(description = "净重", example = "12500.00")
    @ExcelProperty("净重")
    private BigDecimal netWeight;

    @Schema(description = "扣杂", example = "200.00")
    @ExcelProperty("扣杂")
    private BigDecimal deduction;

    @Schema(description = "结算重量 = 毛重 − 皮重 − 扣杂（计价基准）", example = "12300.00")
    @ExcelProperty("结算重量")
    private BigDecimal settlementWeight;

    @Schema(description = "调整项（元）", example = "-100.00")
    @ExcelProperty("调整项(元)")
    private BigDecimal adjustmentAmount;

    @Schema(description = "磅单号", example = "WD20261201001")
    @ExcelProperty("磅单号")
    private String weightTicketNo;

    @Schema(description = "车牌号", example = "京A12345")
    @ExcelProperty("车牌号")
    private String vehiclePlateNo;

    @Schema(description = "交易时间", example = "2026-12-01 10:00:00")
    @ExcelProperty("交易时间")
    private String tradeTime;

    @Schema(description = "交易地点", example = "北京市朝阳区再生资源回收站")
    @ExcelProperty("交易地点")
    private String tradeAddress;

    @Schema(description = "结算方式", example = "银行转账，过磅后 3 日内结清")
    @ExcelProperty("结算方式")
    private String settlementMethod;

    @Schema(description = "额度余量（元）：连续 12 个月反向开票累计销售额的余量", example = "4876543.22")
    @ExcelProperty("额度余量(元)")
    private BigDecimal quotaRemainingAmount;

    @Schema(description = "额度结论（500 万上限 / 10 万元免征线）")
    @ExcelProperty("额度提示")
    private String quotaMessage;

}

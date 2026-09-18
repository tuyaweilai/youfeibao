package cn.iocoder.yudao.module.icbc.controller.admin.evidence.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收购台账的一行。字段取自税总 5 号公告第十七条对信息流的要求：
 * 时间、地点、出售者及联系方式、报废产品名称、数量、价格。
 */
@Schema(description = "管理后台 - 收购台账 Response VO")
@Data
@ExcelIgnoreUnannotated
public class AcquisitionLedgerRespVO {

    @Schema(description = "交易时间", example = "2024-12-01 10:00:00")
    @ExcelProperty("交易时间")
    private LocalDateTime tradeTime;

    @Schema(description = "交易地点", example = "北京市朝阳区回收站")
    @ExcelProperty("交易地点")
    private String tradeAddress;

    @Schema(description = "出售者姓名", example = "张三")
    @ExcelProperty("出售者姓名")
    private String sellerName;

    @Schema(description = "出售者联系方式", example = "13800138000")
    @ExcelProperty("出售者联系方式")
    private String sellerMobile;

    @Schema(description = "报废产品名称", example = "废铁")
    @ExcelProperty("报废产品名称")
    private String productName;

    @Schema(description = "规格型号", example = "重型")
    @ExcelProperty("规格型号")
    private String specification;

    @Schema(description = "数量", example = "100.0000")
    @ExcelProperty("数量")
    private BigDecimal quantity;

    @Schema(description = "计量单位", example = "吨")
    @ExcelProperty("计量单位")
    private String unit;

    @Schema(description = "含税单价", example = "10.00")
    @ExcelProperty("含税单价(元)")
    private BigDecimal unitPrice;

    @Schema(description = "金额", example = "1000.00")
    @ExcelProperty("金额(元)")
    private BigDecimal amount;

    @Schema(description = "发票号码", example = "12345678901234567890")
    @ExcelProperty("发票号码")
    private String invoiceNo;

    @Schema(description = "合作方订单号", example = "ORDER_20231201_001")
    @ExcelProperty("合作方订单号")
    private String partnerOrderId;

}

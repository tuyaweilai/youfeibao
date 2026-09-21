package cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * 工行反向开票预下单请求 VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - 工行反向开票预下单请求 VO")
@Data
public class InvoicePreOrderReqVO {

    @Schema(description = "合作方订单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2018040908")
    @NotEmpty(message = "合作方订单ID不能为空")
    @Size(max = 35, message = "合作方订单ID长度不能超过35个字符")
    private String outOrderId;

    @Schema(description = "付方编号（付方平台外部编号 / 子商户编号，即回收企业）", requiredMode = Schema.RequiredMode.REQUIRED, example = "010020200513111111")
    @NotEmpty(message = "付方编号不能为空")
    @Size(max = 40, message = "付方编号长度不能超过40个字符")
    private String outVendorId;

    @Schema(description = "收方编号（外部用户编号，即自然人出售者）", requiredMode = Schema.RequiredMode.REQUIRED, example = "10000000000000003")
    @NotEmpty(message = "收方编号不能为空")
    @Size(max = 20, message = "收方编号长度不能超过20个字符")
    private String outUserId;

    @Schema(description = "卖方主体类型（平台侧字段，不上送工行）：1-自然人出售者，2~6-非自然人。反向开票只对自然人开放（ADR 0029）", example = "1")
    private Integer sellerSubjectType;

    @Schema(description = "线上异步支付标识：0-其他1-线上企网异步支付", example = "0")
    @Size(max = 1, message = "线上异步支付标识长度不能超过1个字符")
    private String asynFlag;

    @Schema(description = "付款人手机号", example = "13800138000")
    @Size(max = 30, message = "付款人手机号长度不能超过30个字符")
    private String payPhoneno;

    @Schema(description = "收货方省份", example = "北京市")
    @Size(max = 60, message = "收货方省份长度不能超过60个字符")
    private String province;

    @Schema(description = "收货人城市", example = "北京市")
    @Size(max = 60, message = "收货人城市长度不能超过60个字符")
    private String city;

    @Schema(description = "收货人区县", example = "朝阳区")
    @Size(max = 60, message = "收货人区县长度不能超过60个字符")
    private String county;

    @Schema(description = "收货人邮箱", example = "test@example.com")
    @Size(max = 60, message = "收货人邮箱长度不能超过60个字符")
    @Email(message = "收货人邮箱格式不正确")
    private String email;

    @Schema(description = "收货人电话", example = "13800138000")
    @Size(max = 30, message = "收货人电话长度不能超过30个字符")
    private String phone;

    @Schema(description = "收货人详细地址", example = "朝阳区建国路1号")
    @Size(max = 100, message = "收货人详细地址长度不能超过100个字符")
    private String address;

    @Schema(description = "收货人邮编", example = "100000")
    @Size(max = 10, message = "收货人邮编长度不能超过10个字符")
    private String post;

    @Schema(description = "付款备注", example = "货款支付")
    @Size(max = 200, message = "付款备注长度不能超过200个字符")
    private String payRem;

    @Schema(description = "订单备注", example = "紧急订单")
    @Size(max = 140, message = "订单备注长度不能超过140个字符")
    private String orderRem;

    @Schema(description = "回单补充信息备注", example = "补充信息")
    @Size(max = 140, message = "回单补充信息备注长度不能超过140个字符")
    private String addRemark;

    @Schema(description = "交易币种：001-人民币", requiredMode = Schema.RequiredMode.REQUIRED, example = "001")
    @NotEmpty(message = "交易币种不能为空")
    @Size(max = 3, message = "交易币种长度不能超过3个字符")
    private String currency = "001";

    @Schema(description = "打印到回单的摘要栏", example = "货款支付")
    @Size(max = 20, message = "摘要栏长度不能超过20个字符")
    private String summary;

    @Schema(description = "打印到回单的用途栏", example = "采购货款")
    @Size(max = 20, message = "用途栏长度不能超过20个字符")
    private String purpose;

    @Schema(description = "支付返回页面地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://example.com/pay/return")
    @NotEmpty(message = "支付返回页面地址不能为空")
    @Size(max = 200, message = "支付返回页面地址长度不能超过200个字符")
    private String payJumpUrl;

    @Schema(description = "发票状态变更通知地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://example.com/invoice/notify")
    @NotEmpty(message = "发票状态变更通知地址不能为空")
    @Size(max = 200, message = "发票状态变更通知地址长度不能超过200个字符")
    private String invoiceNotifyUrl;

    @Schema(description = "自然人确认成功返回页面地址", example = "https://example.com/invoice/success")
    @Size(max = 200, message = "自然人确认成功返回页面地址长度不能超过200个字符")
    private String invoiceJumpUrl;

    @Schema(description = "发票类型：01-增值税专用发票，02-普通发票", requiredMode = Schema.RequiredMode.REQUIRED, example = "02")
    @NotEmpty(message = "发票类型不能为空")
    @Pattern(regexp = "^(01|02)$", message = "发票类型只能是01或02")
    private String invoiceType;

    @Schema(description = "价税合计", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    @NotNull(message = "价税合计不能为空")
    @DecimalMin(value = "0.01", message = "价税合计必须大于0")
    @Digits(integer = 14, fraction = 2, message = "价税合计整数部分不能超过14位，小数部分不能超过2位")
    private BigDecimal orderAmount;

    @Schema(description = "收方姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotEmpty(message = "收方姓名不能为空")
    @Size(max = 100, message = "收方姓名长度不能超过100个字符")
    private String naturalPersonName;

    @Schema(description = "收方身份证件类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "111")
    @NotEmpty(message = "收方身份证件类型不能为空")
    @Size(max = 3, message = "收方身份证件类型长度不能超过3个字符")
    private String cardType;

    @Schema(description = "收方身份证件号码", requiredMode = Schema.RequiredMode.REQUIRED, example = "110101199001011234")
    @NotEmpty(message = "收方身份证件号码不能为空")
    @Size(max = 20, message = "收方身份证件号码长度不能超过20个字符")
    private String cardNumber;

    @Schema(description = "收方地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "北京市朝阳区")
    @NotEmpty(message = "收方地址不能为空")
    @Size(max = 30, message = "收方地址长度不能超过30个字符")
    private String sellerAddress;

    @Schema(description = "收方电话", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    @NotEmpty(message = "收方电话不能为空")
    @Size(max = 20, message = "收方电话长度不能超过20个字符")
    private String sellerTelephone;

    @Schema(description = "付方纳税人识别号", requiredMode = Schema.RequiredMode.REQUIRED, example = "91110000123456789X")
    @NotEmpty(message = "付方纳税人识别号不能为空")
    @Size(max = 20, message = "付方纳税人识别号长度不能超过20个字符")
    private String taxpayerNo;

    @Schema(description = "付方纳税人名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "北京某某有限公司")
    @NotEmpty(message = "付方纳税人名称不能为空")
    @Size(max = 160, message = "付方纳税人名称长度不能超过160个字符")
    private String taxpayerName;

    @Schema(description = "开票人", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @NotEmpty(message = "开票人不能为空")
    @Size(max = 200, message = "开票人长度不能超过200个字符")
    private String drawerName;

    @Schema(description = "开票人证件类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "111")
    @NotEmpty(message = "开票人证件类型不能为空")
    @Size(max = 4, message = "开票人证件类型长度不能超过4个字符")
    private String drawerCardType;

    @Schema(description = "开票人证件号码", requiredMode = Schema.RequiredMode.REQUIRED, example = "110101199001011234")
    @NotEmpty(message = "开票人证件号码不能为空")
    @Size(max = 30, message = "开票人证件号码长度不能超过30个字符")
    private String drawerCardNumber;

    @Schema(description = "收款人姓名", example = "王五")
    @Size(max = 150, message = "收款人姓名长度不能超过150个字符")
    private String payeeName;

    @Schema(description = "复核人姓名", example = "赵六")
    @Size(max = 75, message = "复核人姓名长度不能超过75个字符")
    private String reviewerName;

    @Schema(description = "特定要素：16-农产品收购，24-报废产品收购", requiredMode = Schema.RequiredMode.REQUIRED, example = "24")
    @NotEmpty(message = "特定要素不能为空")
    @Pattern(regexp = "^(16|24)$", message = "特定要素只能是16或24")
    private String specificElements;

    @Schema(description = "区域代码", requiredMode = Schema.RequiredMode.REQUIRED, example = "110000")
    @NotEmpty(message = "区域代码不能为空")
    @Size(max = 20, message = "区域代码长度不能超过20个字符")
    private String areaCode;

    @Schema(description = "是否销售自然人使用过的报废产品：Y-是，N-否", example = "N")
    @Pattern(regexp = "^(Y|N)$", message = "是否销售自然人使用过的报废产品只能是Y或N")
    private String isSellerPersonProduct = "N";

    @Schema(description = "个人所得税项目：1-财产转让所得，2-经营所得", example = "1")
    @Pattern(regexp = "^(1|2)$", message = "个人所得税项目只能是1或2")
    private String iitProject;

    @Schema(description = "收购发票类型代码：01-农产品收购发票，04-报废产品收购发票", requiredMode = Schema.RequiredMode.REQUIRED, example = "04")
    @NotEmpty(message = "收购发票类型代码不能为空")
    @Pattern(regexp = "^(01|04)$", message = "收购发票类型代码只能是01或04")
    private String buyerInvTypeCode;

    @Schema(description = "应税行为发生地", example = "110000")
    @Size(max = 11, message = "应税行为发生地长度不能超过11个字符")
    private String shouldTaxLocation;

    @Schema(description = "备注（开票时备注）", example = "特殊备注信息")
    @Size(max = 450, message = "备注长度不能超过450个字符")
    private String notes;

    @Schema(description = "mac地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "00:11:22:33:44:55")
    @NotEmpty(message = "mac地址不能为空")
    @Size(max = 17, message = "mac地址长度不能超过17个字符")
    private String mac;

    @Schema(description = "CPU序列号", example = "BFEBFBFF000906E9")
    @Size(max = 20, message = "CPU序列号长度不能超过20个字符")
    private String cpuId;

    @Schema(description = "主板序列号", example = "MB123456789")
    @Size(max = 20, message = "主板序列号长度不能超过20个字符")
    private String mainboaedId;

    @Schema(description = "缴税付款账户", example = "1234567890123456789")
    @Size(max = 32, message = "缴税付款账户长度不能超过32个字符")
    private String taxPayerAccountNo;

    @Schema(description = "缴税付款开户行行号", example = "102100000001")
    @Size(max = 12, message = "缴税付款开户行行号长度不能超过12个字符")
    private String taxPayerBankCode;

    @Schema(description = "缴税单位名称", example = "北京某某有限公司")
    @Size(max = 200, message = "缴税单位名称长度不能超过200个字符")
    private String taxPayerOrgName;

    @Schema(description = "付款账户户名", example = "北京某某有限公司")
    @Size(max = 120, message = "付款账户户名长度不能超过120个字符")
    private String payerName;

    @Schema(description = "付款账户", example = "1234567890123456789")
    @Size(max = 34, message = "付款账户长度不能超过34个字符")
    private String payerAcctNum;

    @Schema(description = "是否展示销售方银行账号标签：Y-展示，N-不展示", example = "Y")
    @Pattern(regexp = "^(Y|N)$", message = "是否展示销售方银行账号标签只能是Y或N")
    private String isShowSellBankAcct = "Y";

    @Schema(description = "是否展示购买方银行账号标签：Y-展示，N-不展示", example = "Y")
    @Pattern(regexp = "^(Y|N)$", message = "是否展示购买方银行账号标签只能是Y或N")
    private String isShowBuyBankAcct = "Y";

    @Schema(description = "增值税税率", requiredMode = Schema.RequiredMode.REQUIRED, example = "0.13")
    @NotNull(message = "增值税税率不能为空")
    @DecimalMin(value = "0", message = "增值税税率不能小于0")
    @DecimalMax(value = "1", message = "增值税税率不能大于1")
    @Digits(integer = 2, fraction = 2, message = "增值税税率格式不正确")
    private BigDecimal taxRate;

    @Schema(description = "补缴税费标志：Y-补缴，N-不补缴", example = "Y")
    @Pattern(regexp = "^(Y|N)$", message = "补缴税费标志只能是Y或N")
    private String supplementaryTax = "Y";

    @Schema(description = "减按征税类型代码", example = "55")
    @Size(max = 2, message = "减按征税类型代码长度不能超过2个字符")
    private String unuseReduceTaxCode;

    @Schema(description = "是否重复下单：Y-是，N-否", example = "N")
    @Pattern(regexp = "^(Y|N)$", message = "是否重复下单只能是Y或N")
    private String isRedo = "N";

    @Schema(description = "支付渠道：01-e企付，02-场景结算，04-公对公结算，05-公对私结算", example = "05")
    @Pattern(regexp = "^(01|02|04|05)$", message = "支付渠道只能是01、02、04或05")
    private String payChannel = "05";

    @Schema(description = "机构编码", example = "20201128531215026")
    @Size(max = 30, message = "机构编码长度不能超过30个字符")
    private String verifiedCode;

    @Schema(description = "U盾ID", example = "20201128531215026")
    @Size(max = 24, message = "U盾ID长度不能超过24个字符")
    private String ukeyId;

    @Schema(description = "商品信息列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "商品信息列表不能为空")
    @Valid
    private List<GoodsInfoVO> goodsInfo;

    /**
     * 商品信息 VO
     */
    @Schema(description = "商品信息")
    @Data
    public static class GoodsInfoVO {

        @Schema(description = "商品信息子序号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        @NotEmpty(message = "商品信息子序号不能为空")
        @Size(max = 5, message = "商品信息子序号长度不能超过5个字符")
        private String goodsSeqno;

        @Schema(description = "项目名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "废铁回收")
        @NotEmpty(message = "项目名称不能为空")
        @Size(max = 300, message = "项目名称长度不能超过300个字符")
        private String projectName;

        @Schema(description = "商品总数", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
        @NotNull(message = "商品总数不能为空")
        @DecimalMin(value = "0.0001", message = "商品总数必须大于0")
        @Digits(integer = 14, fraction = 4, message = "商品总数整数部分不能超过14位，小数部分不能超过4位")
        private BigDecimal goodsNum;

        @Schema(description = "商品金额(元)", requiredMode = Schema.RequiredMode.REQUIRED, example = "888.88")
        @NotNull(message = "商品金额不能为空")
        @DecimalMin(value = "0.01", message = "商品金额必须大于0")
        @Digits(integer = 14, fraction = 2, message = "商品金额整数部分不能超过14位，小数部分不能超过2位")
        private BigDecimal goodsAmt;

        @Schema(description = "订单商品规格", example = "大型")
        @Size(max = 10, message = "订单商品规格长度不能超过10个字符")
        private String weight;

        @Schema(description = "数量口径说明（结算重量计价时解释发票数量与磅单净重的差异）", example = "结算重量计价，含扣杂")
        @Size(max = 200, message = "数量口径说明长度不能超过200个字符")
        private String quantityNote;

        @Schema(description = "含税单价(元)", requiredMode = Schema.RequiredMode.REQUIRED, example = "8.89")
        @NotNull(message = "含税单价不能为空")
        @DecimalMin(value = "0.01", message = "含税单价必须大于0")
        @Digits(integer = 14, fraction = 2, message = "含税单价整数部分不能超过14位，小数部分不能超过2位")
        private BigDecimal price;

        @Schema(description = "计量单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "个")
        @NotEmpty(message = "计量单位不能为空")
        @Size(max = 10, message = "计量单位长度不能超过10个字符")
        private String units;

        @Schema(description = "增值税税率", requiredMode = Schema.RequiredMode.REQUIRED, example = "0.13")
        @NotNull(message = "增值税税率不能为空")
        @DecimalMin(value = "0", message = "增值税税率不能小于0")
        @DecimalMax(value = "1", message = "增值税税率不能大于1")
        @Digits(integer = 2, fraction = 2, message = "增值税税率格式不正确")
        private BigDecimal taxRate;

        @Schema(description = "商品和服务税收分类合并编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "1090101010000000000")
        @NotEmpty(message = "商品和服务税收分类合并编码不能为空")
        @Size(max = 19, message = "商品和服务税收分类合并编码长度不能超过19个字符")
        private String mergedCode;

    }

} 
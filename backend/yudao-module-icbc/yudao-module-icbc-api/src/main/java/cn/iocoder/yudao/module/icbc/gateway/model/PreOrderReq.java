package cn.iocoder.yudao.module.icbc.gateway.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 开票信息预下单请求
 *
 * 对应工行 `/ui/jft/ui/invoice/pre/order/V1`。字段含义见
 * `docs/icbc/融e聚开票信息预下单接口V1-250605.pdf`。
 */
@Data
@Builder
public class PreOrderReq {

    /**
     * 合作方订单编号（幂等键）
     */
    private String outOrderId;
    /**
     * 付方编号（反向开票场景下与 appId 一致）
     */
    private String outUserId;
    /**
     * 收方编号（子商户 / 回收企业）
     */
    private String outVendorId;
    /**
     * 发票类型：01 增值税专用发票，02 普通发票
     */
    private String invoiceType;
    /**
     * 价税合计
     */
    private String orderAmount;
    /**
     * 特定要素：16 农产品收购，24 报废产品收购
     */
    private String specificElements;
    /**
     * 收购发票类型代码：01 农产品收购发票，04 报废产品收购发票
     */
    private String buyerInvTypeCode;
    /**
     * 付方纳税人识别号
     */
    private String taxpayerNo;
    /**
     * 付方纳税人名称
     */
    private String taxpayerName;
    /**
     * 开票人姓名
     */
    private String drawerName;
    /**
     * 开票人证件类型
     */
    private String drawerCardType;
    /**
     * 开票人证件号码
     */
    private String drawerCardNumber;
    /**
     * 收方（销售方）姓名
     */
    private String naturalPersonName;
    /**
     * 收方身份证件类型
     */
    private String cardType;
    /**
     * 收方身份证件号码
     */
    private String cardNumber;
    /**
     * 收方地址
     */
    private String sellerAddress;
    /**
     * 收方电话
     */
    private String sellerTelephone;
    /**
     * 省级税务机关代码
     */
    private String areaCode;
    /**
     * 支付渠道：05 公对私结算
     */
    private String payChannel;
    /**
     * 是否销售自然人使用过的报废产品：固定 N
     */
    private String isSellerPersonProduct;
    /**
     * 个人所得税项目：1 财产转让所得，2 经营所得
     */
    private String iitProject;
    /**
     * 备注
     */
    private String notes;
    /**
     * 增值税税率
     */
    private String taxRate;
    /**
     * 补缴税费标志，目前固定 Y
     */
    private String supplementaryTax;
    /**
     * 减按征税类型代码，税率传 3 时必输
     */
    private String unuseReduceTaxCode;
    /**
     * 缴税付款账户
     */
    private String taxPayerAccountNo;
    /**
     * 缴税付款开户行行号
     */
    private String taxPayerBankCode;
    /**
     * 缴税单位名称
     */
    private String taxPayerOrgName;
    /**
     * 发票状态变更通知地址
     */
    private String invoiceNotifyUrl;
    /**
     * 支付返回页面地址
     */
    private String payJumpUrl;
    /**
     * 自然人确认成功返回页面地址
     */
    private String invoiceJumpUrl;
    /**
     * 付款备注
     */
    private String payRem;
    /**
     * 订单备注
     */
    private String orderRem;
    /**
     * 商品明细
     */
    private List<PreOrderGoods> goods;

}

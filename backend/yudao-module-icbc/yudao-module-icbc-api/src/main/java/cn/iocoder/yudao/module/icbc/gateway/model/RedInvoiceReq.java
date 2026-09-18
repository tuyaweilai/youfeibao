package cn.iocoder.yudao.module.icbc.gateway.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 红字冲销开票请求
 *
 * 对应工行 `/ui/jft/ui/red/invoice/offset/V1`，返回红字确认单页面。
 * 开票有误（01）必须全额红冲，明细单价 / 金额 / 数量必须与蓝票一致。
 */
@Data
@Builder
public class RedInvoiceReq {

    /**
     * 红冲流水号（幂等键）
     */
    private String outRedOffsetId;
    /**
     * 原蓝字合作方订单编号
     */
    private String outOrderId;
    /**
     * 红冲原因：01 开票有误，02 销货退回，03 服务中止，04 销售折让
     */
    private String redOffsetReason;
    /**
     * 红冲金额
     */
    private String redOffsetAmount;
    /**
     * 是否重复下单
     */
    private String isRedo;
    /**
     * 渠道
     */
    private String channel;
    /**
     * 返回页面地址
     */
    private String jumpUrl;
    /**
     * 红冲明细
     */
    private List<RedInvoiceGoods> goods;

}

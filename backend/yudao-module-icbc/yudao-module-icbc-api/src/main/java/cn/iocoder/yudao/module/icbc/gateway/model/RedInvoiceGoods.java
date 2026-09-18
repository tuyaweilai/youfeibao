package cn.iocoder.yudao.module.icbc.gateway.model;

import lombok.Builder;
import lombok.Data;

/**
 * 红字冲销明细
 */
@Data
@Builder
public class RedInvoiceGoods {

    private String goodsSeqno;
    /**
     * 对应蓝票明细序号
     */
    private String blueGoodsSeqno;
    private String projectName;
    private String goodsNum;
    private String goodsAmt;
    private String weight;
    private String price;
    private String units;

}

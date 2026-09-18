package cn.iocoder.yudao.module.icbc.gateway.model;

import lombok.Builder;
import lombok.Data;

/**
 * 预下单商品明细
 */
@Data
@Builder
public class PreOrderGoods {

    /**
     * 商品信息子序号，订单内不能重复
     */
    private String goodsSeqno;
    /**
     * 项目名称
     */
    private String projectName;
    /**
     * 商品总数
     */
    private String goodsNum;
    /**
     * 商品金额（元）
     */
    private String goodsAmt;
    /**
     * 订单商品规格
     */
    private String weight;
    /**
     * 含税单价（元）
     */
    private String price;
    /**
     * 计量单位
     */
    private String units;
    /**
     * 增值税税率
     */
    private String taxRate;
    /**
     * 商品和服务税收分类合并编码
     */
    private String mergedCode;

}

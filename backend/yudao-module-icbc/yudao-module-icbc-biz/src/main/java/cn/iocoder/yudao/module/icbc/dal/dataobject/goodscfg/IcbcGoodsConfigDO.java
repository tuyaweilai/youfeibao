package cn.iocoder.yudao.module.icbc.dal.dataobject.goodscfg;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * 品类与税收分类编码配置 DO
 */
@TableName("icbc_goods_config")
@KeySequence("icbc_goods_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class IcbcGoodsConfigDO extends TenantBaseDO {

    @TableId
    private Long id;

    /** 品类名称 */
    private String name;
    /** 计量单位 */
    private String unit;
    /** 税率 */
    private BigDecimal taxRate;
    /** 计税方法：SIMPLE-简易计税，GENERAL-一般计税 */
    private String taxMethod;
    /** 商品和服务税收分类合并编码 */
    private String mergedCode;
    /** 状态：0-启用，1-停用 */
    private Integer status;
    /** 备注 */
    private String remark;

}

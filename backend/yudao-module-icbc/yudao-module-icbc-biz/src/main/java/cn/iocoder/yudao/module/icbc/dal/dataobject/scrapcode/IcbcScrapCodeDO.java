package cn.iocoder.yudao.module.icbc.dal.dataobject.scrapcode;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * 平台级报废产品税收分类编码 DO。
 *
 * <p>由平台运营统一维护，跨租户共享，因此<b>不带租户维度</b>（继承 {@link BaseDO}），
 * 对应表名需登记在 {@code yudao.tenant.ignore-tables} 中，否则会被多租户拦截器拼上 tenant_id。
 */
@TableName("icbc_scrap_code")
@KeySequence("icbc_scrap_code_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class IcbcScrapCodeDO extends BaseDO {

    @TableId
    private Long id;

    /** 报废产品名称 */
    private String name;
    /** 商品和服务税收分类合并编码 */
    private String mergedCode;
    /** 计量单位 */
    private String unit;
    /** 税率 */
    private BigDecimal taxRate;
    /** 状态：0-启用，1-停用 */
    private Integer status;
    /** 备注 */
    private String remark;

}

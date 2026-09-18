package cn.iocoder.yudao.module.enterprise.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 企业门店 DO
 *
 * @author 芋道源码
 */
@TableName("enterprise_store")
@KeySequence("enterprise_store_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EnterpriseStoreDO extends BaseDO {

    /**
     * 主键ID (门店ID)
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属企业ID
     */
    private Long enterpriseId;

    /**
     * 上级门店ID (用于分层结构, 总店/区域中心等, 关联自身)
     */
    private Long parentId;

    /**
     * 门店名称
     */
    private String name;

    /**
     * 门店编码
     */
    private String storeCode;

    /**
     * 门店地址-省编码
     */
    private String addressProvinceCode;

    /**
     * 门店地址-市编码
     */
    private String addressCityCode;

    /**
     * 门店地址-区编码
     */
    private String addressDistrictCode;

    /**
     * 门店地址-详细地址
     */
    private String addressDetail;

    /**
     * 门店联系人
     */
    private String contactName;

    /**
     * 门店联系电话
     */
    private String contactPhone;

    /**
     * 门店状态
     * 
     * 枚举 {@link cn.iocoder.yudao.module.enterprise.enums.EnterpriseStoreStatusEnum}
     */
    private Integer status;
} 
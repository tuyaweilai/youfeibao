package cn.iocoder.yudao.module.icbc.dal.dataobject.payer;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

/**
 * 付款方信息 DO
 *
 * @author 芋道源码
 */
@TableName("icbc_payer_info")
@KeySequence("icbc_payer_info_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PayerInfoDO extends BaseDO {

    /**
     * ID
     */
    @TableId
    private Long id;

    /**
     * 付款方编号
     */
    private String payerNo;

    /**
     * 合作方付款方ID
     */
    private String partnerPayerId;

    /**
     * 付款方名称
     */
    private String name;

    /**
     * 统一社会信用代码
     */
    private String creditCode;

    /**
     * 税号
     */
    private String taxNo;

    /**
     * 银行账号
     */
    private String bankAccount;

    /**
     * 开户行名称
     */
    private String bankName;

    /**
     * 地址
     */
    private String address;

    /**
     * 联系电话
     */
    private String telephone;

    /**
     * 联系人姓名
     */
    private String contactName;

    /**
     * 联系人手机号
     */
    private String contactMobile;

    /**
     * 纳税人类型：01-一般纳税人，02-小规模纳税人
     */
    private String taxpayerType;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 审核消息
     */
    private String auditMsg;

    /**
     * 工行付款方状态
     */
    private String icbcPayerStatus;

    /**
     * 租户编号
     */
    private Long tenantId;

    /**
     * 是否删除
     */
    private Boolean deleted;

    /**
     * 自定义请求参数 - 仅用于测试
     */
    @TableField(exist = false)
    private Map<String, Object> transMap = new HashMap<>();
} 
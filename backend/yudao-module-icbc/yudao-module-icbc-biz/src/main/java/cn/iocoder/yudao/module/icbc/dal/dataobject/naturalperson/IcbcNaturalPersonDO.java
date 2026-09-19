package cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.icbc.enums.PayeeRealNameStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 自然人主体 DO（平台级身份档案）。
 *
 * <p>出售者是跨回收企业的**同一个人**：他在本平台可以有多家回收企业的收方档案，但只有一个
 * 自然人主体、一个平台级外部用户编号（工行 {@code outUserId}）。见 ADR 0017。
 *
 * <p>本表<b>不带租户维度</b>（继承 {@link BaseDO}），表名必须登记在
 * {@code yudao.tenant.ignore-tables} 中，否则会被多租户拦截器拼上 {@code tenant_id}。
 *
 * <p>实人认证结果归本表（跨企业复用），收方入驻状态归收方档案（那是与子商户绑定的动作）。
 */
@TableName("icbc_natural_person")
@KeySequence("icbc_natural_person_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcNaturalPersonDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 平台级外部用户编号，即工行报文的 {@code outUserId}。生成后不可变：
     * 实人认证、收方入驻、预下单、付款都用同一个值。
     */
    private String outUserId;

    /** 身份登记姓名（最近一次登记的快照） */
    private String name;

    /**
     * 身份证件号码。**这是身份的唯一锚点**：同一号码在本表只能有一条记录，
     * 新增时不覆盖、不自动合并（见 ADR 0017）。
     */
    private String idCardNo;

    /** 身份登记手机号（最近一次登记的快照，不等于登录凭证） */
    private String mobile;

    // ==================== 实人认证（工行，跨企业复用） ====================

    /** 实人认证状态，枚举 {@link PayeeRealNameStatusEnum} */
    private Integer realNameStatus;

    /** 实人认证失败原因 */
    private String realNameMsg;

    /** 实人认证通过时间 */
    private LocalDateTime realNameTime;

    // ==================== 平台运营处置 ====================

    /** 状态：0-正常，1-已停用（冒用等情形由平台运营人工处置） */
    private Integer status;

    /** 备注（平台运营处置原因） */
    private String remark;

}

package cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 自然人主体与登录凭证的绑定 DO。
 *
 * <p>登录凭证（{@code member_user}）与身份锚点（{@link IcbcNaturalPersonDO}）是两件事，所以这里是
 * **多对多**：一个手机号可以被多个自然人主体复用（子女代老人操作），一个自然人也可能绑多个入口。
 * 选择用哪个身份操作时，必须由调用方**显式指定**，不做静默推断（见 ADR 0017）。
 *
 * <p>本表不带租户维度（继承 {@link BaseDO}），需登记进 {@code yudao.tenant.ignore-tables}。
 */
@TableName("icbc_natural_person_login")
@KeySequence("icbc_natural_person_login_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcNaturalPersonLoginDO extends BaseDO {

    @TableId
    private Long id;

    /** 自然人主体编号 */
    private Long naturalPersonId;

    /** 登录凭证：平台租户下的会员用户编号 */
    private Long memberUserId;

    /** 绑定时间 */
    private LocalDateTime boundAt;

    /** 绑定来源：REGISTER-本人注册，OPS_CLAIM-平台运营人工认领 */
    private String bindSource;

    /** 备注 */
    private String remark;

}

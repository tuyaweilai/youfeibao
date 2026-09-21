package cn.iocoder.yudao.module.icbc.dal.dataobject.esign;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 电子签章的平台级参数 DO（#92，ADR 0036）。
 *
 * <p>平台运维在后台维护、跨租户共享，因此<b>不带租户维度</b>（继承 {@link BaseDO}），
 * 对应表名需登记在 {@code yudao.tenant.ignore-tables} 中。
 *
 * <p><b>密钥只落后端、界面不回显明文</b>：{@code secretId} / {@code secretKey} / {@code callbackSignKey}
 * 只写不读——保存时留空表示「不改动」，查询响应里只回「已配置」与否，不回值。
 *
 * <p>「两套 endpoint」指第三方电子签章的两类地址：{@code apiEndpoint} 是服务端接口地址，
 * {@code consoleEndpoint} 是**控制台地址**——一次性开通链接就拼在它上面（企业认证与制章在第三方侧完成）。
 */
@TableName("icbc_esign_config")
@KeySequence("icbc_esign_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcEsignConfigDO extends BaseDO {

    @TableId
    private Long id;

    /** 环境：TEST-测试，PROD-生产 */
    private String environment;

    /** 服务端接口地址（两套 endpoint 之一） */
    private String apiEndpoint;

    /** 控制台地址（两套 endpoint 之二）：一次性开通链接拼在它上面 */
    private String consoleEndpoint;

    /** 应用标识（第三方分配给平台的 AppId） */
    private String appId;

    /** 应用密钥 ID（只写不读） */
    private String secretId;

    /** 应用密钥（只写不读） */
    private String secretKey;

    /** 签署状态回调地址（平台外网可达，供第三方推状态通知） */
    private String callbackUrl;

    /** 回调验签密钥（只写不读）：验签失败必须明确失败，不得静默吞掉 */
    private String callbackSignKey;

    /** 签署链接渠道：H5 / MINI_PROGRAM / PC（第三方对同一条链接在不同渠道的行为不同） */
    private String signLinkChannel;

    /** 平台模板：框架收购协议（租户只能填变量，不给编辑权） */
    private String agreementTemplateId;

    /** 平台模板：反向发票合规告知函 */
    private String noticeTemplateId;

    /** 备注 */
    private String remark;

}

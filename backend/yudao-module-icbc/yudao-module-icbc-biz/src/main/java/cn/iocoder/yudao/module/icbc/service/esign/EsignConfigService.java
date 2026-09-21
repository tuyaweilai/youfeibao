package cn.iocoder.yudao.module.icbc.service.esign;

import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.EsignConfigRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.EsignConfigSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.esign.IcbcEsignConfigDO;

/**
 * 电子签章平台级参数 Service（#92，ADR 0036）。
 *
 * <p>接入第三方电子签章是一次**配置动作而不是改代码**：环境、两套 endpoint、应用标识、密钥、
 * 回调地址与验签、签署链接渠道、平台模板都在这里维护，跨租户共享一份。
 *
 * <p><b>密钥只写不读</b>：保存时留空表示不改动，查询响应绝不回明文。
 */
public interface EsignConfigService {

    /**
     * 平台参数（回显用）：密钥只回「已配置」与否。
     */
    EsignConfigRespVO getConfig();

    /**
     * 保存 / 更新平台参数。密钥字段留空表示不改动既有值。
     *
     * @param reqVO 平台参数
     * @return 配置行编号
     */
    Long saveConfig(EsignConfigSaveReqVO reqVO);

    /**
     * 平台参数是否已齐备（齐备才允许租户侧拿到控制台链接 / 发起电子签署）。
     */
    boolean isPlatformConfigured();

    /**
     * 原始平台参数（内部用：拼控制台链接、出站时读 endpoint 与密钥）。没配置过时返回 {@code null}。
     */
    IcbcEsignConfigDO getRawConfig();

}

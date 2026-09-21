package cn.iocoder.yudao.module.icbc.service.esign.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.EsignConfigRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.EsignConfigSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.esign.IcbcEsignConfigDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.esign.IcbcEsignConfigMapper;
import cn.iocoder.yudao.module.icbc.service.esign.EsignConfigService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 电子签章平台级参数 Service 实现（#92，ADR 0036）。
 *
 * <p>唯一一份配置，只该有一行；保存是原地更新（没有就插入），不回明文密钥。
 */
@Service
@Validated
public class EsignConfigServiceImpl implements EsignConfigService {

    @Resource
    private IcbcEsignConfigMapper esignConfigMapper;

    @Override
    public EsignConfigRespVO getConfig() {
        IcbcEsignConfigDO config = esignConfigMapper.selectConfig();
        EsignConfigRespVO resp = config == null ? new EsignConfigRespVO()
                : BeanUtils.toBean(config, EsignConfigRespVO.class);
        // 密钥只写不读：回「已配置」与否，绝不回明文
        resp.setSecretIdConfigured(config != null && StrUtil.isNotBlank(config.getSecretId()));
        resp.setSecretKeyConfigured(config != null && StrUtil.isNotBlank(config.getSecretKey()));
        resp.setCallbackSignKeyConfigured(config != null && StrUtil.isNotBlank(config.getCallbackSignKey()));
        List<String> missing = missingRequiredFields(config);
        resp.setMissingFields(missing);
        resp.setConfigured(missing.isEmpty());
        return resp;
    }

    @Override
    public Long saveConfig(EsignConfigSaveReqVO reqVO) {
        IcbcEsignConfigDO existing = esignConfigMapper.selectConfig();
        IcbcEsignConfigDO save = BeanUtils.toBean(reqVO, IcbcEsignConfigDO.class);
        // 密钥留空 = 不改动：既支持「只改 endpoint」这种局部保存，也不会因为前端不回显而把密钥清掉
        if (StrUtil.isBlank(save.getSecretId())) {
            save.setSecretId(existing != null ? existing.getSecretId() : null);
        }
        if (StrUtil.isBlank(save.getSecretKey())) {
            save.setSecretKey(existing != null ? existing.getSecretKey() : null);
        }
        if (StrUtil.isBlank(save.getCallbackSignKey())) {
            save.setCallbackSignKey(existing != null ? existing.getCallbackSignKey() : null);
        }
        if (existing == null) {
            esignConfigMapper.insert(save);
            return save.getId();
        }
        save.setId(existing.getId());
        esignConfigMapper.updateById(save);
        return existing.getId();
    }

    @Override
    public boolean isPlatformConfigured() {
        return missingRequiredFields(esignConfigMapper.selectConfig()).isEmpty();
    }

    @Override
    public IcbcEsignConfigDO getRawConfig() {
        return esignConfigMapper.selectConfig();
    }

    /**
     * 齐备的判据只有一处：缺任何一项都答「未配置」，租户侧就安静降级，不把现场卡死。
     */
    private List<String> missingRequiredFields(IcbcEsignConfigDO config) {
        // 没配置过时当作一份全空配置，齐备判据只有一处，不把「没有行」与「有行但缺字段」分成两套
        IcbcEsignConfigDO effective = config != null ? config : new IcbcEsignConfigDO();
        List<String> missing = new ArrayList<>();
        if (StrUtil.isBlank(effective.getEnvironment())) {
            missing.add("环境");
        }
        if (StrUtil.isBlank(effective.getApiEndpoint())) {
            missing.add("服务端接口地址");
        }
        if (StrUtil.isBlank(effective.getConsoleEndpoint())) {
            missing.add("控制台地址");
        }
        if (StrUtil.isBlank(effective.getAppId())) {
            missing.add("应用标识");
        }
        if (StrUtil.isBlank(effective.getSecretId())) {
            missing.add("应用密钥 ID");
        }
        if (StrUtil.isBlank(effective.getSecretKey())) {
            missing.add("应用密钥");
        }
        if (StrUtil.isBlank(effective.getCallbackUrl())) {
            missing.add("回调地址");
        }
        if (StrUtil.isBlank(effective.getCallbackSignKey())) {
            missing.add("回调验签密钥");
        }
        return missing;
    }

}

package cn.iocoder.yudao.module.icbc.service.cardrecognition.config;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.cardrecognition.vo.CardRecognitionCheckReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.cardrecognition.vo.CardRecognitionCheckRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.cardrecognition.vo.CardRecognitionConfigRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.cardrecognition.vo.CardRecognitionConfigSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.cardrecognition.IcbcCardRecognitionConfigDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.cardrecognition.IcbcCardRecognitionConfigMapper;
import cn.iocoder.yudao.module.icbc.enums.CardRecognitionCheckResultEnum;
import cn.iocoder.yudao.module.icbc.enums.CardRecognitionProviderEnum;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent.TencentCardRecognitionProperties;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent.TencentOcrClient;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.tencent.TencentOcrSettings;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.CARD_RECOGNITION_CHECK_NOT_CONFIGURED;

/**
 * 卡证识别平台级参数 Service 实现（#103，ADR 0037）。
 *
 * <p>唯一一份配置，只该有一行；保存是原地更新（没有就插入），不回明文密钥。生效参数每次现算
 * （{@link #resolveEffectiveConfig()}），DB 优先、空则回落 yaml / env——后台改完立刻生效，无需重启。
 */
@Slf4j
@Service
@Validated
public class CardRecognitionConfigServiceImpl implements CardRecognitionConfigService {

    /**
     * 1x1 PNG：足以让腾讯云走完鉴权、在解码 / 识别阶段报业务错（照 {@code TencentCardRecognitionLiveTest}）。
     * 自检因此**不消耗识别额度**，也拿不到任何真证件信息。
     */
    private static final String TINY_PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

    @Resource
    private IcbcCardRecognitionConfigMapper cardRecognitionConfigMapper;
    @Resource
    private TencentCardRecognitionProperties properties;
    @Resource
    private TencentOcrClient tencentOcrClient;

    @Override
    public CardRecognitionConfigRespVO getConfig() {
        IcbcCardRecognitionConfigDO db = cardRecognitionConfigMapper.selectConfig();
        CardRecognitionEffectiveConfig effective = resolveEffectiveConfig();

        CardRecognitionConfigRespVO resp = new CardRecognitionConfigRespVO();
        resp.setProvider(effective.getProvider());
        resp.setProviderFromConfigFile(StrUtil.isBlank(db == null ? null : db.getProvider()));
        // 密钥只写不读：回「已配置」与否，绝不回明文
        resp.setSecretIdConfigured(StrUtil.isNotBlank(effective.getSecretId()));
        resp.setSecretKeyConfigured(StrUtil.isNotBlank(effective.getSecretKey()));
        resp.setRegion(effective.getRegion());
        resp.setEndpoint(effective.getEndpoint());
        resp.setTimeout(effective.getTimeout());
        resp.setConfigFileFields(configFileFields(db, effective));
        List<String> missing = missingRequiredFields(effective);
        resp.setMissingFields(missing);
        resp.setConfigured(effective.isEnabled());
        resp.setLastCheckResult(db == null ? null : db.getLastCheckResult());
        resp.setLastCheckResultName(checkResultName(db == null ? null : db.getLastCheckResult()));
        resp.setLastCheckTime(db == null ? null : db.getLastCheckTime());
        resp.setRemark(db == null ? null : db.getRemark());
        return resp;
    }

    @Override
    public Long saveConfig(CardRecognitionConfigSaveReqVO reqVO) {
        IcbcCardRecognitionConfigDO existing = cardRecognitionConfigMapper.selectConfig();
        IcbcCardRecognitionConfigDO save = BeanUtils.toBean(reqVO, IcbcCardRecognitionConfigDO.class);
        // 留空 = 不改动：既支持「只改 endpoint」这种局部保存，也不会因为前端不回显而把密钥 / 供应商清掉
        if (StrUtil.isBlank(save.getProvider())) {
            save.setProvider(existing != null ? existing.getProvider() : null);
        }
        if (StrUtil.isBlank(save.getSecretId())) {
            save.setSecretId(existing != null ? existing.getSecretId() : null);
        }
        if (StrUtil.isBlank(save.getSecretKey())) {
            save.setSecretKey(existing != null ? existing.getSecretKey() : null);
        }
        if (StrUtil.isBlank(save.getRegion())) {
            save.setRegion(existing != null ? existing.getRegion() : null);
        }
        if (StrUtil.isBlank(save.getEndpoint())) {
            save.setEndpoint(existing != null ? existing.getEndpoint() : null);
        }
        if (save.getTimeout() == null) {
            save.setTimeout(existing != null ? existing.getTimeout() : null);
        }
        if (existing == null) {
            cardRecognitionConfigMapper.insert(save);
            return save.getId();
        }
        save.setId(existing.getId());
        // 保存参数不抹掉上一次自检结果：「先验证、再保存」是页面主流程，自检写在请求里、
        // 保存发生在它之后，不能把刚拿到的结论清掉。改完密钥想重新确认，再点一次自检即可。
        save.setLastCheckResult(existing.getLastCheckResult());
        save.setLastCheckTime(existing.getLastCheckTime());
        cardRecognitionConfigMapper.updateById(save);
        return existing.getId();
    }

    @Override
    public CardRecognitionCheckRespVO checkConnectivity(CardRecognitionCheckReqVO reqVO) {
        CardRecognitionEffectiveConfig effective = resolveEffectiveConfig();
        TencentOcrSettings settings = TencentOcrSettings.builder()
                .secretId(firstNonBlank(reqVO.getSecretId(), effective.getSecretId()))
                .secretKey(firstNonBlank(reqVO.getSecretKey(), effective.getSecretKey()))
                .region(firstNonBlank(reqVO.getRegion(), effective.getRegion()))
                .endpoint(firstNonBlank(reqVO.getEndpoint(), effective.getEndpoint()))
                .timeout(reqVO.getTimeout() != null ? reqVO.getTimeout() : effective.getTimeout())
                .build();
        if (!settings.hasCredentials()) {
            // 手里一件密钥都没有：不该拿一次必然 401 的请求冒充「验证失败」，直接让运维先填密钥
            throw exception(CARD_RECOGNITION_CHECK_NOT_CONFIGURED);
        }
        CardRecognitionCheckResultEnum result = classify(settings);
        LocalDateTime checkTime = LocalDateTime.now();
        persistCheckResult(result, checkTime);

        CardRecognitionCheckRespVO resp = new CardRecognitionCheckRespVO();
        // 只在 AUTH_FAILED 与 NETWORK 上判失败：1x1 占位图必然在识别阶段报业务错（VENDOR_ERROR），
        // 那恰恰说明鉴权已通过（照 TencentCardRecognitionLiveTest 的判据）。
        resp.setOk(result == CardRecognitionCheckResultEnum.OK
                || result == CardRecognitionCheckResultEnum.VENDOR_ERROR);
        resp.setResult(result.name());
        resp.setResultName(result.getName());
        resp.setCheckTime(checkTime);
        return resp;
    }

    @Override
    public CardRecognitionEffectiveConfig resolveEffectiveConfig() {
        IcbcCardRecognitionConfigDO db = cardRecognitionConfigMapper.selectConfig();
        String provider = firstNonBlank(db == null ? null : db.getProvider(), properties.getProvider());
        // 非 tencent 一律当 stub：安静降级是 ADR 0037 的决策，一个拼错的供应商不该让现场报错
        if (!CardRecognitionProviderEnum.TENCENT.getCode().equals(provider)) {
            provider = CardRecognitionProviderEnum.STUB.getCode();
        }
        return CardRecognitionEffectiveConfig.builder()
                .provider(provider)
                .secretId(firstNonBlank(db == null ? null : db.getSecretId(), properties.getSecretId()))
                .secretKey(firstNonBlank(db == null ? null : db.getSecretKey(), properties.getSecretKey()))
                .region(firstNonBlank(db == null ? null : db.getRegion(), properties.getRegion()))
                .endpoint(firstNonBlank(db == null ? null : db.getEndpoint(), properties.getEndpoint()))
                .timeout(db != null && db.getTimeout() != null ? db.getTimeout() : properties.getTimeout())
                .build();
    }

    /**
     * 自检走同一枚 {@link TencentOcrClient} 的 1x1 占位图，**只在 {@code AuthFailure.*} 上判 AUTH_FAILED**：
     * 占位图必然在解码 / 识别阶段报业务错，那不是密钥的问题，归 VENDOR_ERROR。
     */
    private CardRecognitionCheckResultEnum classify(TencentOcrSettings settings) {
        JSONObject payload = JSON.parseObject(
                "{\"ImageBase64\":\"" + TINY_PNG_BASE64 + "\",\"CardSide\":\"FRONT\"}");
        JSONObject response = tencentOcrClient.callRaw(settings, "IDCardOCR", payload);
        if (response == null) {
            // 网络 / 超时 / DNS / 非 2xx / 响应不可解析：客户端已统一收成 null
            return CardRecognitionCheckResultEnum.NETWORK;
        }
        JSONObject error = response.getJSONObject("Error");
        if (error == null) {
            return CardRecognitionCheckResultEnum.OK;
        }
        String code = error.getString("Code");
        return code != null && code.startsWith("AuthFailure")
                ? CardRecognitionCheckResultEnum.AUTH_FAILED
                : CardRecognitionCheckResultEnum.VENDOR_ERROR;
    }

    /**
     * 落库**只有分类与时间**：不存密钥、不整段存厂商原始报文（它是给人看的状态，不是调试垃圾桶）。
     * 还没保存过配置时也要有一条行可写——自检允许发生在保存之前（「先验证、再保存」）。
     */
    private void persistCheckResult(CardRecognitionCheckResultEnum result, LocalDateTime checkTime) {
        IcbcCardRecognitionConfigDO existing = cardRecognitionConfigMapper.selectConfig();
        if (existing == null) {
            IcbcCardRecognitionConfigDO created = new IcbcCardRecognitionConfigDO();
            created.setLastCheckResult(result.name());
            created.setLastCheckTime(checkTime);
            cardRecognitionConfigMapper.insert(created);
            return;
        }
        existing.setLastCheckResult(result.name());
        existing.setLastCheckTime(checkTime);
        cardRecognitionConfigMapper.updateById(existing);
    }

    /**
     * 齐备的判据只有一处：供应商是 tencent 时，缺任何一项密钥都答「未配齐」，现场端就安静降级。
     * provider=stub 时没有必填项（它就是「未启用」），由页面的「未启用」提示承载，不混进 missingFields。
     */
    private List<String> missingRequiredFields(CardRecognitionEffectiveConfig effective) {
        List<String> missing = new ArrayList<>();
        if (!effective.isTencent()) {
            return missing;
        }
        if (StrUtil.isBlank(effective.getSecretId())) {
            missing.add("SecretId");
        }
        if (StrUtil.isBlank(effective.getSecretKey())) {
            missing.add("SecretKey");
        }
        return missing;
    }

    /**
     * 哪些生效值来自配置文件（DB 为空、但回落取到了值）：页面据此标注「来自配置文件」，
     * 避免「我明明配了却不生效」的困惑。DB 与配置文件都为空时不算「来自配置文件」，它只是缺失。
     */
    private List<String> configFileFields(IcbcCardRecognitionConfigDO db,
                                          CardRecognitionEffectiveConfig effective) {
        List<String> fields = new ArrayList<>();
        if (db == null || StrUtil.isBlank(db.getProvider())) {
            fields.add("供应商");
        }
        if (StrUtil.isBlank(db == null ? null : db.getSecretId()) && StrUtil.isNotBlank(effective.getSecretId())) {
            fields.add("SecretId");
        }
        if (StrUtil.isBlank(db == null ? null : db.getSecretKey()) && StrUtil.isNotBlank(effective.getSecretKey())) {
            fields.add("SecretKey");
        }
        if (StrUtil.isBlank(db == null ? null : db.getRegion()) && StrUtil.isNotBlank(effective.getRegion())) {
            fields.add("地域");
        }
        if (StrUtil.isBlank(db == null ? null : db.getEndpoint()) && StrUtil.isNotBlank(effective.getEndpoint())) {
            fields.add("Endpoint");
        }
        if ((db == null || db.getTimeout() == null) && effective.getTimeout() != null) {
            fields.add("超时");
        }
        return fields;
    }

    private String checkResultName(String result) {
        if (StrUtil.isBlank(result)) {
            return null;
        }
        for (CardRecognitionCheckResultEnum item : CardRecognitionCheckResultEnum.values()) {
            if (item.name().equals(result)) {
                return item.getName();
            }
        }
        return null;
    }

    private static String firstNonBlank(String first, String fallback) {
        return StrUtil.isNotBlank(first) ? first : fallback;
    }

}

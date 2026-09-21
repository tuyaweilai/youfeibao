package cn.iocoder.yudao.module.icbc.service.cardrecognition.config;

import cn.iocoder.yudao.module.icbc.controller.admin.cardrecognition.vo.CardRecognitionCheckReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.cardrecognition.vo.CardRecognitionCheckRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.cardrecognition.vo.CardRecognitionConfigRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.cardrecognition.vo.CardRecognitionConfigSaveReqVO;

/**
 * 卡证识别平台级参数 Service（#103，ADR 0037）。
 *
 * <p>接入腾讯云 OCR 是一次**配置动作而不是改代码**：供应商（运行期生效）/ 密钥 / 地域 / endpoint /
 * 超时都在这里维护，跨租户共享一份。与电子签章**各立一处**（ADR 0037）。
 *
 * <p><b>密钥只写不读</b>：保存时留空表示不改动，查询响应绝不回明文。
 */
public interface CardRecognitionConfigService {

    /**
     * 平台参数（回显用）：密钥只回「已配置」与否。
     */
    CardRecognitionConfigRespVO getConfig();

    /**
     * 保存 / 更新平台参数。密钥 / 供应商留空表示不改动既有值。
     *
     * @param reqVO 平台参数
     * @return 配置行编号
     */
    Long saveConfig(CardRecognitionConfigSaveReqVO reqVO);

    /**
     * 连通性自检：用 1x1 占位图走完鉴权，把结果分类落库。
     *
     * <p>请求允许带密钥（留空 = 用已存值），因此「先验证、再保存」成立。不消耗识别额度。
     */
    CardRecognitionCheckRespVO checkConnectivity(CardRecognitionCheckReqVO reqVO);

    /**
     * 运行期生效的整套参数（DB 优先、空则回落 yaml / env）。
     *
     * <p>识别端口每次调用时取一次——这是「保存后无需重启即生效」的唯一入口，也是本票
     * 对 #93 启动期二选一的替代。
     */
    CardRecognitionEffectiveConfig resolveEffectiveConfig();

}

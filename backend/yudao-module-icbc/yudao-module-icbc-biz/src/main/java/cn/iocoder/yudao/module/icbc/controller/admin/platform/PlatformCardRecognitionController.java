package cn.iocoder.yudao.module.icbc.controller.admin.platform;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.controller.admin.cardrecognition.vo.CardRecognitionCheckReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.cardrecognition.vo.CardRecognitionCheckRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.cardrecognition.vo.CardRecognitionConfigRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.cardrecognition.vo.CardRecognitionConfigSaveReqVO;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.cardrecognition.config.CardRecognitionConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 平台运营：卡证识别平台级参数与连通性自检（#103，ADR 0037）。
 *
 * <p>接入腾讯云 OCR 是一次**配置动作而不是改代码**：供应商（运行期生效）/ 密钥 / 地域 / endpoint /
 * 超时都在这里维护。密钥**只落后端、界面不回显明文**。
 *
 * <p><b>与电子签章各立一处</b>（ADR 0037）：OCR 的额度与密钥是平台共享的，回收企业不该自己配腾讯密钥；
 * 也不把这一页并进 {@code /icbc/platform/esign} 或那套页面里。
 *
 * <p>保存与自检的请求体允许携带密钥，因此**整段关掉请求体记录**（{@code requestEnable = false}）：
 * 密钥只该走「整段不记」，不该把安全寄托在「字段名碰巧命中默认脱敏名单」上（#98 的口径）。
 */
@Tag(name = "管理后台 - 平台运营：卡证识别配置")
@RestController
@RequestMapping("/icbc/platform/card-recognition")
@Validated
public class PlatformCardRecognitionController {

    @Resource
    private CardRecognitionConfigService cardRecognitionConfigService;

    @GetMapping("/config")
    @Operation(summary = "查看卡证识别平台参数", description = "密钥只回「已配置」与否，不回明文")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PLATFORM_CARD_RECOGNITION_QUERY + "')")
    public CommonResult<CardRecognitionConfigRespVO> getConfig() {
        return success(cardRecognitionConfigService.getConfig());
    }

    @PutMapping("/config")
    @Operation(summary = "保存卡证识别平台参数", description = "密钥留空表示不改动既有值；保存后无需重启即生效")
    @ApiAccessLog(requestEnable = false)
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PLATFORM_CARD_RECOGNITION_MANAGE + "')")
    public CommonResult<Long> saveConfig(@Valid @RequestBody CardRecognitionConfigSaveReqVO reqVO) {
        return success(cardRecognitionConfigService.saveConfig(reqVO));
    }

    @PostMapping("/config/check")
    @Operation(summary = "卡证识别连通性自检", description = "用 1x1 占位图走完鉴权，只在 AuthFailure 上判失败；不涉及真证件影像，但会计入腾讯云调用次数")
    @ApiAccessLog(requestEnable = false)
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PLATFORM_CARD_RECOGNITION_MANAGE + "')")
    public CommonResult<CardRecognitionCheckRespVO> checkConnectivity(@Valid @RequestBody CardRecognitionCheckReqVO reqVO) {
        return success(cardRecognitionConfigService.checkConnectivity(reqVO));
    }

}

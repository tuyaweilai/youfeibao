package cn.iocoder.yudao.module.icbc.service.acquisition.recognition.tencent;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.icbc.service.acquisition.recognition.AcquisitionRecognitionPort;
import com.alibaba.fastjson.JSONObject;

import java.util.Collections;
import java.util.List;

/**
 * 把腾讯云 OCR 的车牌识别响应映射成 {@link AcquisitionRecognitionPort.PlateRecognition}（#112）。
 *
 * <p>厂商差异全部收敛在这里：业务层只看到车牌号、置信度与可读告警（照 ADR 0037 的
 * {@code TencentOcrResultMapper} 先例）。映射是**纯函数**，测试喂官方示例形状的报文、不触网。
 *
 * <p><b>字段名以腾讯云官方 SDK 模型为准</b>（{@code LicensePlateOCR}，{@code 2018-11-19}，同一个
 * 文字识别产品的 action）：{@code Number} 车牌号、{@code Confidence} 置信度 0-100、
 * {@code Color} 车牌颜色、{@code LicensePlateCategory} 车牌类别（标准实体 / 非标准实体 / 临牌 / 喷漆）、
 * {@code LicensePlateInfos} 一张图里的全部车牌。
 *
 * <p><b>只取顶层 {@code Number}</b>（厂商给出的主结果）：车头照里出现第二张牌通常是背景里别的车，
 * 让收货员当场在几张牌里选只会更慢更容易错。{@code Color} 与 {@code LicensePlateCategory}
 * **不展示、不落库**——现场看不懂，也没有下游要用；临牌与喷漆放大号同样照常回填，
 * 号码准不准最终由现场确认。
 */
public final class TencentPlateResultMapper {

    /**
     * 置信度低于它就在现场端提示「请核对」，但**不回退成未识别、也不拦提交**：读数偏低仍比空着强。
     */
    public static final int LOW_CONFIDENCE_THRESHOLD = 80;

    /** 置信度偏低的提示文案（现场端直接展示，不在前端另写一套） */
    public static final String LOW_CONFIDENCE_WARNING = "识别置信度偏低，请核对车牌";

    private TencentPlateResultMapper() {
    }

    /**
     * @param response 厂商响应里的 {@code Response} 节点
     * @return 识别结果；没有读出车牌时返回 {@link AcquisitionRecognitionPort.PlateRecognition#empty()}
     */
    public static AcquisitionRecognitionPort.PlateRecognition toPlate(JSONObject response) {
        String plateNo = StrUtil.trimToNull(response.getString("Number"));
        if (plateNo == null) {
            // 读不出车牌 = 未识别：现场端据此去试车尾、再退回手工录入
            return AcquisitionRecognitionPort.PlateRecognition.empty();
        }
        Integer confidence = parseConfidence(response);
        AcquisitionRecognitionPort.PlateRecognition.PlateRecognitionBuilder builder =
                AcquisitionRecognitionPort.PlateRecognition.builder()
                        .plateNo(plateNo)
                        .confidence(confidence);
        if (confidence == null || confidence >= LOW_CONFIDENCE_THRESHOLD) {
            return builder.warnings(Collections.emptyList()).build();
        }
        return builder.warnings(lowConfidenceWarnings()).build();
    }

    /**
     * 厂商的 {@code Confidence} 是数值；换成不是数值的形状时按「没给置信度」处理（不回退成低置信度告警，
     * 免得把一次读得好好的车牌标成可疑）。
     */
    private static Integer parseConfidence(JSONObject response) {
        Object raw = response.get("Confidence");
        if (raw == null) {
            return null;
        }
        try {
            return response.getInteger("Confidence");
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static List<String> lowConfidenceWarnings() {
        return Collections.singletonList(LOW_CONFIDENCE_WARNING);
    }

}

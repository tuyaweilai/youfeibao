package cn.iocoder.yudao.module.icbc.service.acquisition.recognition;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 收购现场识别端口。
 *
 * <p>现场端拍下的磅单与车头车尾照片要由「系统识别回填毛重/皮重/净重与车牌」（见 issue #7）。
 * 识别能力来自第三方 OCR / 车牌识别服务，与工行的银税通道是两回事，因此单列一个端口，
 * 不混进 {@code IcbcGateway}（后者只管工行出站）。
 *
 * <p>默认实现 {@link StubAcquisitionRecognition} 不触网、返回空结果，识别失败或未配置时
 * 由收货员手工录入，不阻断登记。接入真实服务时替换这个 Bean 即可，业务层不感知。
 */
public interface AcquisitionRecognitionPort {

    /**
     * 识别磅单：毛重、皮重、净重、磅单号、车牌
     *
     * @param imageUrl 磅单照片地址
     * @return 识别结果；无法识别时返回 {@link WeightTicketRecognition#empty()}
     */
    WeightTicketRecognition recognizeWeightTicket(String imageUrl);

    /**
     * 识别车牌：车头 / 车尾照片
     *
     * @param imageUrl 车辆照片地址
     * @return 识别结果；无法识别时返回 {@link PlateRecognition#empty()}
     */
    PlateRecognition recognizePlate(String imageUrl);

    @Data
    @Builder
    class WeightTicketRecognition {

        private String weightTicketNo;
        private BigDecimal grossWeight;
        private BigDecimal tareWeight;
        private BigDecimal netWeight;
        private String plateNo;

        public static WeightTicketRecognition empty() {
            return WeightTicketRecognition.builder().build();
        }

    }

    @Data
    @Builder
    class PlateRecognition {

        private String plateNo;

        public static PlateRecognition empty() {
            return PlateRecognition.builder().build();
        }

    }

}

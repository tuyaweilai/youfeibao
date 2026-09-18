package cn.iocoder.yudao.module.icbc.service.acquisition.recognition;

import org.springframework.stereotype.Component;

/**
 * 默认的收购现场识别实现：不识别，返回空结果。
 *
 * <p>一期没有接第三方 OCR / 车牌识别服务，识别结果由收货员手工录入或现场端本地识别后上送，
 * 因此这里保持空实现，不阻断登记。接入真实服务时替换本 Bean 即可。
 */
@Component
public class StubAcquisitionRecognition implements AcquisitionRecognitionPort {

    @Override
    public WeightTicketRecognition recognizeWeightTicket(String imageUrl) {
        return WeightTicketRecognition.empty();
    }

    @Override
    public PlateRecognition recognizePlate(String imageUrl) {
        return PlateRecognition.empty();
    }

}

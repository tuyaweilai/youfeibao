package cn.iocoder.yudao.module.icbc.service.acquisition.recognition;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * 收购现场识别端口。
 *
 * <p>现场端拍下的磅单与车头车尾照片要由「系统识别回填毛重/皮重/净重与车牌」（见 issue #7）。
 * 识别能力来自第三方 OCR / 车牌识别服务，与工行的银税通道是两回事，因此单列一个端口，
 * 不混进 {@code IcbcGateway}（后者只管工行出站）。
 *
 * <p>默认实现 {@link StubAcquisitionRecognition} 不触网、返回空结果，识别失败或未配置时
 * 由收货员手工录入，不阻断登记。
 *
 * <p><b>契约要点</b>：
 * <ul>
 *   <li>入参是 <b>base64</b>（#112 起，两个方法都是）。原先是 {@code imageUrl}，改口的理由：
 *       识别只该依赖「手里有没有这张图」，不该依赖它存在哪儿。照片存在本机的文件服务里
 *       （`infra_file_config.domain` 开发环境是 {@code 127.0.0.1:48080}），把地址交给厂商必然下载不到；
 *       走后端按地址回取字节，等于多开一个「按客户端给的地址出站下载」的面（SSRF）。现场弱网下
 *       照片上传失败是常事，而识别恰恰最需要在那种时候还能用。见 ADR 0013 的修订注记。</li>
 *   <li>识别是**可失败**的：返回空结果时登记照常进行，由收货员手工补录，不阻断现场作业。</li>
 *   <li>「只在空缺处回填、人工值优先」是**业务层 / 现场端**的事，端口只负责「读出了什么」。</li>
 * </ul>
 */
public interface AcquisitionRecognitionPort {

    /**
     * 识别磅单：毛重、皮重、净重、磅单号、车牌、扣率
     *
     * <p>#113 起已接入：通用印刷体识别（{@code GeneralBasicOCR}）+ 平台侧的解析规则。
     * 磅单格式因磅房而异，字段靠关键词与坐标取（见
     * {@code TencentWeightTicketParser}）；解析不出来的字段留空，由收货员手工补录。
     *
     * @param imageBase64 磅单照片（base64）
     * @return 识别结果；无法识别时返回 {@link WeightTicketRecognition#empty()}
     */
    WeightTicketRecognition recognizeWeightTicket(String imageBase64);

    /**
     * 识别车牌：车头 / 车尾照片
     *
     * @param imageBase64 车辆照片（base64）
     * @return 识别结果；无法识别时返回 {@link PlateRecognition#empty()}
     */
    PlateRecognition recognizePlate(String imageBase64);

    /**
     * 磅单识别结果。
     *
     * <p>只有「读出什么」「原始文字行」与「提示什么」：读不出来的字段一律留空，**绝不猜**
     * （填一个错的重量比空着更危险）。原始文字行是给收货员当场核对、也给我们自己调解析规则用的，
     * **不落库**（ADR 0021 / 0037）。
     */
    @Data
    @Builder
    class WeightTicketRecognition {

        private String weightTicketNo;
        private BigDecimal grossWeight;
        private BigDecimal tareWeight;
        private BigDecimal netWeight;
        private String plateNo;

        /**
         * 扣杂值 / 录法（磅单上印着「扣率 DISCOUNT %」时回填）
         *
         * <p>磅单上是百分数，库里按 {@link cn.iocoder.yudao.module.icbc.enums.DeductionMethodEnum#RATIO}
         * 存的是 0~1 的比例（ADR 0019），所以已除过 100。
         */
        private BigDecimal deduction;
        private String deductionMethod;

        /**
         * 识别到的原始文字行（按识别顺序）
         */
        private List<String> rawLines;

        /**
         * 提示类告警（可读文案）：重量不自洽 / 车号可能不完整……
         *
         * <p>与车牌识别一样只有提示、**没有硬拦**：识别只是输入辅助，人工录入那条路必须始终能走。
         */
        private List<String> warnings;

        public static WeightTicketRecognition empty() {
            return WeightTicketRecognition.builder()
                    .rawLines(Collections.emptyList())
                    .warnings(Collections.emptyList())
                    .build();
        }

    }

    /**
     * 车牌识别结果。
     *
     * <p>只有「读出什么」与「提示什么」，没有硬拦：车牌读不出来只是要人手工录入，不该挡住登记。
     */
    @Data
    @Builder
    class PlateRecognition {

        private String plateNo;

        /**
         * 置信度（0-100）；为空表示未识别
         */
        private Integer confidence;

        /**
         * 提示类告警（可读文案）：置信度偏低……
         *
         * <p>展示给收货员当场判断，**不拦继续**（照 {@code CardRecognitionPort} 的先例）。
         */
        private List<String> warnings;

        public static PlateRecognition empty() {
            return PlateRecognition.builder().build();
        }

    }

}

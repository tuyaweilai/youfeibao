package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 人工登记的证据类型。每条证据归属于五流中的一流，归属关系在这里写死，
 * 避免「同一份材料被挂到两流」或「流的取值自由发挥」。
 *
 * <p>系统能从业务表自动取到的证据（资金流的支付单、发票流的发票原件、信息流的
 * 台账条目）不走本枚举；本枚举只服务需要人工补录的材料，主要是合同流与货物流。
 */
public enum IcbcEvidenceTypeEnum {

    FRAMEWORK_AGREEMENT("FRAMEWORK_AGREEMENT", "框架收购协议", EvidenceFlowEnum.CONTRACT),
    ACQUISITION_CONTRACT("ACQUISITION_CONTRACT", "该笔收购合同", EvidenceFlowEnum.CONTRACT),
    WEIGHBRIDGE_TICKET("WEIGHBRIDGE_TICKET", "过磅单", EvidenceFlowEnum.GOODS),
    TRANSPORT_VOUCHER("TRANSPORT_VOUCHER", "运输凭证", EvidenceFlowEnum.GOODS),
    TRANSFER_RECEIPT("TRANSFER_RECEIPT", "转账回单", EvidenceFlowEnum.CAPITAL),
    INVOICE_COPY("INVOICE_COPY", "发票复印件", EvidenceFlowEnum.INVOICE),
    LEDGER_ENTRY("LEDGER_ENTRY", "收购台账条目", EvidenceFlowEnum.INFO);

    private final String code;
    private final String name;
    private final EvidenceFlowEnum flow;

    IcbcEvidenceTypeEnum(String code, String name, EvidenceFlowEnum flow) {
        this.code = code;
        this.name = name;
        this.flow = flow;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public EvidenceFlowEnum getFlow() {
        return flow;
    }

    public static Optional<IcbcEvidenceTypeEnum> ofCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst();
    }

}

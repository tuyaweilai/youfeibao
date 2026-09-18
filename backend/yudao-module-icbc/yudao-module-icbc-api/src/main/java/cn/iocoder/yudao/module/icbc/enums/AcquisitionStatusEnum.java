package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 收购单状态。
 *
 * <p>一笔收购从现场登记开始，依次可能走到付款与开票。状态是收货员在收购现场就能看到、
 * 用来回答「这一笔卡在哪一步、钱什么时候到」的东西（见 issue #7）。
 */
public enum AcquisitionStatusEnum {

    /** 已登记：现场要件齐备，收购单已落库 */
    REGISTERED(0, "已登记"),
    /** 待付款：已发起了开票/付款链路，等待向出售者付款 */
    PENDING_PAYMENT(1, "待付款"),
    /** 已付款：货款已通过工行公对私结算到出售者本人银行卡 */
    PAID(2, "已付款"),
    /** 已开票：反向发票已开具 */
    INVOICED(3, "已开票"),
    /** 已取消：登记有误或交易未成立 */
    CANCELLED(9, "已取消");

    private final Integer status;
    private final String name;

    AcquisitionStatusEnum(Integer status, String name) {
        this.status = status;
        this.name = name;
    }

    public Integer getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public static Optional<AcquisitionStatusEnum> ofStatus(Integer status) {
        return Arrays.stream(values())
                .filter(item -> item.status.equals(status))
                .findFirst();
    }

}

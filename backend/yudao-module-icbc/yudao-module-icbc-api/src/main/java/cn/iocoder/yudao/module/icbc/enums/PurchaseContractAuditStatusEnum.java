package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;

/**
 * 采购合同版本快照的审核状态（#45 / T07）。
 *
 * <p>版本只追加、不覆盖；每次送审落一版快照，审核结论写在那一版上，因此「历史版本可回查」
 * 的同时也能看到每一版当时是过了还是被驳回。
 */
public enum PurchaseContractAuditStatusEnum {

    /** 待审核：已送审，尚无结论 */
    PENDING(0, "待审核"),
    /** 已通过：这一版审核通过，合同据此生效 */
    APPROVED(1, "已通过"),
    /** 已驳回：这一版被退回，合同回到草稿 */
    REJECTED(2, "已驳回");

    private final Integer status;
    private final String name;

    PurchaseContractAuditStatusEnum(Integer status, String name) {
        this.status = status;
        this.name = name;
    }

    public Integer getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public static String nameOf(Integer status) {
        return Arrays.stream(values())
                .filter(item -> item.status.equals(status))
                .map(PurchaseContractAuditStatusEnum::getName)
                .findFirst()
                .orElse(null);
    }

}

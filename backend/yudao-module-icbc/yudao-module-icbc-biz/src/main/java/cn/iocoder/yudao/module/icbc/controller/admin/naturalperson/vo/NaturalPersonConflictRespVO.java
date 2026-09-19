package cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 平台运营 - 身份冲突清单 Response VO。
 *
 * <p>「同一身份证在不同租户的姓名 / 手机号不一致」是 ADR 0017 明令**不自动合并、不覆盖**的情形：
 * 迁移按身份证件号码合并，但命中这条规则的必须出人工清单、由平台运营核实后处置。
 * 本 VO 就是那份清单，**只读**，不代表任何合并动作。
 */
@Schema(description = "平台运营 - 身份冲突清单 Response VO")
@Data
public class NaturalPersonConflictRespVO {

    @Schema(description = "身份证件号码（脱敏）", example = "1101**********1234")
    private String idCardNo;

    @Schema(description = "已有的自然人主体编号；尚未建档时为空", example = "1024")
    private Long naturalPersonId;

    @Schema(description = "平台级外部用户编号（工行 outUserId）；尚未建档时为空", example = "NP0f1e2d3c")
    private String outUserId;

    @Schema(description = "自然人主体状态：0-正常，1-已停用；尚未建档时为空", example = "0")
    private Integer status;

    @Schema(description = "冲突涉及的收方档案条数", example = "2")
    private Integer recordCount;

    @Schema(description = "各租户下的收方档案明细")
    private List<Record> records;

    /**
     * 一条收方档案快照：冲突就发生在这些档案之间。
     */
    @Schema(description = "身份冲突 - 收方档案明细")
    @Data
    public static class Record {

        @Schema(description = "收方档案编号", example = "1024")
        private Long payeeId;

        @Schema(description = "所属租户（回收企业）编号", example = "1")
        private Long tenantId;

        @Schema(description = "收方姓名", example = "张三")
        private String name;

        @Schema(description = "手机号（脱敏）", example = "138****8000")
        private String mobile;

        @Schema(description = "收方编号", example = "PAYEE001")
        private String payeeNo;

        @Schema(description = "合作方收方编号（收方档案编号；不再当 outUserId 用）", example = "PARTNER001")
        private String partnerPayeeId;

        @Schema(description = "建档时间")
        private LocalDateTime createTime;

    }

}

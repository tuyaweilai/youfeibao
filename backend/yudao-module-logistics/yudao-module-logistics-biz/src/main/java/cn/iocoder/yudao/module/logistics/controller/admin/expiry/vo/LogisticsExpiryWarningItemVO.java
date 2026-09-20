package cn.iocoder.yudao.module.logistics.controller.admin.expiry.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 到期提醒的一条（V3 #70）。
 *
 * <p>四类证件合成**一个扁平列表**：工作台要的是「今天该处理什么」，不是四棵互不相干的树；
 * 页面按 {@code category} 分组显示即可。分类不是状态、也不是错误码，所以用可读文案而不是枚举值。
 */
@Schema(description = "管理后台 - 证件到期提醒项")
@Data
public class LogisticsExpiryWarningItemVO {

    @Schema(description = "类别：车辆-行驶证 / 车辆-保险 / 司机-驾驶证 / 司机-从业资格证", example = "车辆-行驶证")
    private String category;

    @Schema(description = "对象类型：1-车辆，2-司机", example = "1")
    private Integer subjectType;

    @Schema(description = "对象编号", example = "1")
    private Long subjectId;

    @Schema(description = "对象名称（车牌号或司机姓名）", example = "浙A12345")
    private String subjectName;

    @Schema(description = "到期日")
    private LocalDate expiryDate;

    @Schema(description = "剩余天数（负数表示已过期）", example = "-3")
    private Integer daysLeft;

    @Schema(description = "是否已过期", example = "true")
    private Boolean expired;

}

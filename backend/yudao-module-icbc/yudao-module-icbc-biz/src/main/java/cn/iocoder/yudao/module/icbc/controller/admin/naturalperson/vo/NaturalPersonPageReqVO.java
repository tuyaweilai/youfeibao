package cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * 自然人主体分页查询 Request VO（平台运营跨租户查看）。
 */
@Schema(description = "平台运营 - 自然人主体分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class NaturalPersonPageReqVO extends PageParam {

    @Schema(description = "姓名", example = "张三")
    private String name;

    @Schema(description = "身份证件号码", example = "110101199001011234")
    private String idCardNo;

    @Schema(description = "手机号", example = "13800138000")
    private String mobile;

    @Schema(description = "平台级外部用户编号", example = "NP0f1e2d3c")
    private String outUserId;

    @Schema(description = "状态：0-正常，1-已停用", example = "0")
    private Integer status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}

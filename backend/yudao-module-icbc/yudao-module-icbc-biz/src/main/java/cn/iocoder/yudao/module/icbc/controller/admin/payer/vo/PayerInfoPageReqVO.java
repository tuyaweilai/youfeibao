package cn.iocoder.yudao.module.icbc.controller.admin.payer.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 工行付方信息分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PayerInfoPageReqVO extends PageParam {

    @Schema(description = "企业名称", example = "XX科技有限公司")
    private String name;

    @Schema(description = "统一社会信用代码", example = "91110105MA01R2278M")
    private String creditCode;

    @Schema(description = "纳税人识别号", example = "91110105MA01R2278M")
    private String taxNo;

    @Schema(description = "联系人手机号", example = "13800138000")
    private String contactMobile;

    @Schema(description = "状态：0-待审核，1-审核通过，2-审核拒绝", example = "0")
    private Integer status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

} 
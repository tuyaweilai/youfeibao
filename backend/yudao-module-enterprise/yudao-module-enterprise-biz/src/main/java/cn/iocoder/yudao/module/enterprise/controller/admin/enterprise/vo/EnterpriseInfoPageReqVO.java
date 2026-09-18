package cn.iocoder.yudao.module.enterprise.controller.admin.enterprise.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * 管理后台 - 企业信息分页 Request VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - 企业信息分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EnterpriseInfoPageReqVO extends PageParam {

    @Schema(description = "企业名称", example = "芋道源码")
    private String name;

    @Schema(description = "统一社会信用代码", example = "91110105MA01RUYX8Q")
    private String creditCode;

    @Schema(description = "企业类型", example = "1")
    private Integer enterpriseType;

    @Schema(description = "法定代表人姓名", example = "芋道")
    private String legalPersonName;

    @Schema(description = "企业联系人电话", example = "13800138000")
    private String contactPhone;

    @Schema(description = "企业状态", example = "2")
    private Integer status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime beginCreateTime;

    @Schema(description = "结束时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime endCreateTime;

} 
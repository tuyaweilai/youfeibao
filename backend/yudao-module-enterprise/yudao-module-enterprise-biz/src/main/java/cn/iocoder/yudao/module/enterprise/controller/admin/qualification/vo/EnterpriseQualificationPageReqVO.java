package cn.iocoder.yudao.module.enterprise.controller.admin.qualification.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;
import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * 管理后台 - 企业资质分页 Request VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - 企业资质分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EnterpriseQualificationPageReqVO extends PageParam {

    @Schema(description = "企业ID", example = "1024")
    private Long enterpriseId;

    @Schema(description = "资质类型", example = "1")
    private Integer qualificationType;

    @Schema(description = "资质名称", example = "危废经营许可证")
    private String qualificationName;

    @Schema(description = "资质编号", example = "HW12345678")
    private String qualificationCode;

    @Schema(description = "资质状态", example = "1")
    private Integer status;

    @Schema(description = "开始到期日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate beginExpiryDate;

    @Schema(description = "结束到期日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate endExpiryDate;

    @Schema(description = "开始创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime beginCreateTime;

    @Schema(description = "结束创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime endCreateTime;

} 
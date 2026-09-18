package cn.iocoder.yudao.module.enterprise.controller.admin.store.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * 管理后台 - 企业门店分页 Request VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - 企业门店分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EnterpriseStorePageReqVO extends PageParam {

    @Schema(description = "企业ID", example = "1024")
    private Long enterpriseId;

    @Schema(description = "上级门店ID", example = "0")
    private Long parentId;

    @Schema(description = "门店名称", example = "总店")
    private String name;

    @Schema(description = "门店编码", example = "S001")
    private String storeCode;

    @Schema(description = "门店联系人电话", example = "13800138000")
    private String contactPhone;

    @Schema(description = "门店状态", example = "0")
    private Integer status;

    @Schema(description = "开始创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime beginCreateTime;

    @Schema(description = "结束创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime endCreateTime;

} 
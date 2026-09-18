package cn.iocoder.yudao.module.icbc.controller.admin.log.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 工行接口调用日志分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ApiLogPageReqVO extends PageParam {

    @Schema(description = "消息通讯唯一编号", example = "MSG123456")
    private String msgId;

    @Schema(description = "接口名称", example = "收方新增接口")
    private String apiName;

    @Schema(description = "调用状态", example = "1")
    private Integer status;

    @Schema(description = "业务ID", example = "ORDER123456")
    private String businessId;

    @Schema(description = "业务类型", example = "PAYEE_ADD")
    private String businessType;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

} 
package cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 物流运输节点记录分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class TransportNodePageReqVO extends PageParam {

    @Schema(description = "运输任务ID", example = "1024")
    private Long taskId;

    @Schema(description = "节点类型", example = "1")
    private Integer nodeType;

    @Schema(description = "操作员ID", example = "1024")
    private Long operatorId;

    @Schema(description = "操作员姓名", example = "张三")
    private String operatorName;

    @Schema(description = "节点位置", example = "北京市朝阳区")
    private String nodeLocation;

    @Schema(description = "节点时间范围-开始", example = "2024-01-01 00:00:00")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime beginNodeTime;

    @Schema(description = "节点时间范围-结束", example = "2024-01-01 23:59:59")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime endNodeTime;

    @Schema(description = "创建时间范围-开始", example = "2024-01-01 00:00:00")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime beginCreateTime;

    @Schema(description = "创建时间范围-结束", example = "2024-01-01 23:59:59")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime endCreateTime;

} 
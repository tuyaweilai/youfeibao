package cn.iocoder.yudao.module.logistics.controller.admin.temporaryorder.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 物流临时订单分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class TemporaryOrderPageReqVO extends PageParam {

    @Schema(description = "订单编号", example = "TO202401010001")
    private String orderNo;

    @Schema(description = "运输任务ID", example = "1024")
    private Long taskId;

    @Schema(description = "司机ID", example = "1024")
    private Long driverId;

    @Schema(description = "司机姓名", example = "张三")
    private String driverName;

    @Schema(description = "废料类型", example = "HW01")
    private String wasteType;

    @Schema(description = "废料名称", example = "废机油")
    private String wasteName;

    @Schema(description = "取货地址", example = "北京市朝阳区")
    private String pickupLocation;

    @Schema(description = "产废方姓名", example = "李四")
    private String producerName;

    @Schema(description = "产废方电话", example = "13800138000")
    private String producerPhone;

    @Schema(description = "支付状态", example = "0")
    private Integer paymentStatus;

    @Schema(description = "是否已转为正式订单", example = "false")
    private Boolean convertedToFormal;

    @Schema(description = "取货时间范围-开始", example = "2024-01-01 00:00:00")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime beginPickupTime;

    @Schema(description = "取货时间范围-结束", example = "2024-01-01 23:59:59")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime endPickupTime;

    @Schema(description = "创建时间范围-开始", example = "2024-01-01 00:00:00")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime beginCreateTime;

    @Schema(description = "创建时间范围-结束", example = "2024-01-01 23:59:59")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime endCreateTime;

} 
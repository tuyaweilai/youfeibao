package cn.iocoder.yudao.module.waste.controller.admin.order.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 危废转移订单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TransferOrderPageReqVO extends PageParam {

    @Schema(description = "订单号", example = "WO202412010001")
    private String orderNo;

    @Schema(description = "关联预约单ID", example = "1024")
    private Long appointmentId;

    @Schema(description = "产废企业ID", example = "1024")
    private Long producingEnterpriseId;

    @Schema(description = "回收企业ID", example = "2048")
    private Long recyclingEnterpriseId;

    @Schema(description = "危险废物代码", example = "HW01")
    private String wasteCode;

    @Schema(description = "危险废物名称", example = "医疗废物")
    private String wasteName;

    @Schema(description = "业务状态 (0:待确认, 1:已确认, 2:待结算, 3:已结算, 4:已完成, 5:已取消)", example = "1")
    private Integer businessStatus;

    @Schema(description = "付款状态 (0:未付款, 1:已付款, 2:付款失败, 3:待凭证上传, 4:凭证已上传, 5:凭证已确认)", example = "1")
    private Integer paymentStatus;

    @Schema(description = "订单来源类型 (0:预约转订单, 1:扫街临时订单, 2:补单)", example = "0")
    private Integer sourceType;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

} 
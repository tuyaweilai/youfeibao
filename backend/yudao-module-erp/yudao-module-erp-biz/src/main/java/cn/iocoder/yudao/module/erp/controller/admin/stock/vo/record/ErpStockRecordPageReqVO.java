package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 品类库存明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpStockRecordPageReqVO extends PageParam {

    @Schema(description = "品类编号", example = "10625")
    private Long goodsConfigId;

    @Schema(description = "仓库编号", example = "32407")
    private Long warehouseId;

    @Schema(description = "库位编号", example = "3001")
    private Long locationId;

    @Schema(description = "批次编号", example = "4001")
    private Long batchId;

    @Schema(description = "业务类型", example = "10")
    private Integer bizType;

    @Schema(description = "业务单号", example = "Z110")
    private String bizNo;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
package cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 跨仓调拨单分页 Request VO（#54 T16）。
 */
@Schema(description = "管理后台 - 跨仓调拨单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StockMovePageReqVO extends PageParam {

    @Schema(description = "调拨单号", example = "SM2026")
    private String moveNo;

    @Schema(description = "状态：0-待过账，1-已过账，2-已作废", example = "1")
    private Integer status;

}

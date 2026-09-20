package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.location;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 库位分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpStockLocationPageReqVO extends PageParam {

    @Schema(description = "仓库编号", example = "1024")
    private Long warehouseId;

    @Schema(description = "库位名称", example = "A 区")
    private String name;

    @Schema(description = "开启状态", example = "0")
    @InEnum(CommonStatusEnum.class)
    private Integer status;

}

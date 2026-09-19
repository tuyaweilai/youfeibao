package cn.iocoder.yudao.module.icbc.controller.admin.tax.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 需补缴税费分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TaxSupplementPageReqVO extends PageParam {

    @Schema(description = "申报月 yyyy-MM", example = "2026-08")
    private String periodMonth;

    @Schema(description = "状态：0-待补缴，1-已补缴", example = "0")
    private Integer status;

    @Schema(description = "出售者档案编号", example = "2048")
    private Long payeeId;

    @Schema(description = "出售者姓名", example = "张三")
    private String sellerName;

}

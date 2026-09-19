package cn.iocoder.yudao.module.icbc.controller.admin.quota.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 出售者额度超限引导分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SellerQuotaGuidancePageReqVO extends PageParam {

    @Schema(description = "出售者档案编号", example = "2048")
    private Long payeeId;

    @Schema(description = "出售者姓名", example = "张三")
    private String sellerName;

    @Schema(description = "引导状态：0-待引导，1-已引导，2-已办结", example = "0")
    private Integer status;

    @Schema(description = "触发场景：INVOICE_APPLICATION / ACQUISITION")
    private String triggerScene;

}

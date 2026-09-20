package cn.iocoder.yudao.module.logistics.controller.admin.carrier.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 承运商分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LogisticsCarrierPageReqVO extends PageParam {

    @Schema(description = "承运商名称（模糊）", example = "某某物流")
    private String name;

    @Schema(description = "联系人（模糊）")
    private String contactName;

    @Schema(description = "联系电话（模糊）")
    private String contactMobile;

    @Schema(description = "状态：0-合作中，1-已停用", example = "0")
    private Integer status;

}

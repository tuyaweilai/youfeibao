package cn.iocoder.yudao.module.icbc.controller.admin.qualification.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 租户资质分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class IcbcQualificationPageReqVO extends PageParam {

    @Schema(description = "资质层")
    private String type;

    @Schema(description = "资质名称")
    private String name;

    @Schema(description = "状态")
    private Integer status;

}

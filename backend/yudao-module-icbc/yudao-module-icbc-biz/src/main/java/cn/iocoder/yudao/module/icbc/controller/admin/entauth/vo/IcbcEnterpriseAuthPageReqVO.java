package cn.iocoder.yudao.module.icbc.controller.admin.entauth.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 工行企业授权分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class IcbcEnterpriseAuthPageReqVO extends PageParam {

    @Schema(description = "付方编号")
    private String outVendorId;

    @Schema(description = "授权状态")
    private Integer authStatus;

}

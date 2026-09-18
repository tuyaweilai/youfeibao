package cn.iocoder.yudao.module.icbc.controller.admin.goodscfg.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 品类与税收分类编码配置分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class IcbcGoodsConfigPageReqVO extends PageParam {

    @Schema(description = "品类名称")
    private String name;

    @Schema(description = "状态")
    private Integer status;

}

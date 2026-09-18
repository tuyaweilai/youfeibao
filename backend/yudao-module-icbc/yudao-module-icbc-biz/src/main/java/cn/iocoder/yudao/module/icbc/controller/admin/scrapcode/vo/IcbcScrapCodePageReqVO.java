package cn.iocoder.yudao.module.icbc.controller.admin.scrapcode.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 平台级报废产品编码分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class IcbcScrapCodePageReqVO extends PageParam {

    @Schema(description = "报废产品名称")
    private String name;

    @Schema(description = "商品和服务税收分类合并编码")
    private String mergedCode;

    @Schema(description = "状态")
    private Integer status;

}

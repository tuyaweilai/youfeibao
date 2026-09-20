package cn.iocoder.yudao.module.logistics.controller.admin.driver.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 司机分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LogisticsDriverPageReqVO extends PageParam {

    @Schema(description = "司机姓名（模糊）", example = "张")
    private String name;

    @Schema(description = "手机号（模糊）", example = "138")
    private String mobile;

    @Schema(description = "司机来源：1-自有，2-承运商", example = "1")
    private Integer source;

    @Schema(description = "所属承运商编号", example = "1")
    private Long carrierId;

    @Schema(description = "司机状态：0-在职，1-离职，2-请假", example = "0")
    private Integer status;

}

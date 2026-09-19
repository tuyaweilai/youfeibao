package cn.iocoder.yudao.module.icbc.controller.admin.station.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 场站分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StationPageReqVO extends PageParam {

    @Schema(description = "场站名称")
    private String name;

    @Schema(description = "场站码")
    private String stationCode;

    @Schema(description = "是否在收货：1-在收货，0-暂停收货")
    private Integer openStatus;

}

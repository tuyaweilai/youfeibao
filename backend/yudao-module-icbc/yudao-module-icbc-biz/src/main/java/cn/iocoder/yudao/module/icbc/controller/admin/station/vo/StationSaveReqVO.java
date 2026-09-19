package cn.iocoder.yudao.module.icbc.controller.admin.station.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Schema(description = "管理后台 - 场站新增/修改 Request VO")
@Data
public class StationSaveReqVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "场站码（全局唯一；二维码只编码它）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "场站码不能为空")
    private String stationCode;

    @Schema(description = "场站名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "场站名称不能为空")
    private String name;

    @Schema(description = "场站地址")
    private String address;

    @Schema(description = "场站联系电话（公开）")
    private String contactMobile;

    @Schema(description = "是否在收货：1-在收货，0-暂停收货", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer openStatus;

    @Schema(description = "备注")
    private String remark;

}

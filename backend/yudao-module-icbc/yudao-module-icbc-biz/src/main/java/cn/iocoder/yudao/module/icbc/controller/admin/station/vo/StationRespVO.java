package cn.iocoder.yudao.module.icbc.controller.admin.station.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 场站 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StationRespVO extends StationSaveReqVO {

    @Schema(description = "二维码入口地址（用场站码拼出，印在磅房 / 墙上）")
    private String entryUrl;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}

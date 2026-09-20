package cn.iocoder.yudao.module.logistics.controller.admin.driver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 司机 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LogisticsDriverRespVO extends LogisticsDriverSaveReqVO {

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}

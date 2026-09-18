package cn.iocoder.yudao.module.icbc.controller.admin.scrapcode.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 平台级报废产品编码 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class IcbcScrapCodeRespVO extends IcbcScrapCodeSaveReqVO {

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}

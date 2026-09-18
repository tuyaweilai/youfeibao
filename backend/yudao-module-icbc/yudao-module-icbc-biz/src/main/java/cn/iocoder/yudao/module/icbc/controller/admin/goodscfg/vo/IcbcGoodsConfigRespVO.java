package cn.iocoder.yudao.module.icbc.controller.admin.goodscfg.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 品类与税收分类编码配置 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class IcbcGoodsConfigRespVO extends IcbcGoodsConfigSaveReqVO {

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}

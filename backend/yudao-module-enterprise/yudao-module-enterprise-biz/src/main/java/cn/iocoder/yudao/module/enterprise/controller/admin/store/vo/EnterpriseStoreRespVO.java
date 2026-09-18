package cn.iocoder.yudao.module.enterprise.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 管理后台 - 企业门店 Response VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - 企业门店 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EnterpriseStoreRespVO extends EnterpriseStoreBaseVO {

    @Schema(description = "门店ID", required = true, example = "1024")
    private Long id;

    @Schema(description = "创建时间", required = true)
    private LocalDateTime createTime;

} 
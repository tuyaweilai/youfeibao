package cn.iocoder.yudao.module.enterprise.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * 管理后台 - 企业门店树形结构 Response VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - 企业门店树形结构 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EnterpriseStoreTreeRespVO extends EnterpriseStoreRespVO {

    @Schema(description = "子门店列表")
    private List<EnterpriseStoreTreeRespVO> children;

} 
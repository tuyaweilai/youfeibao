package cn.iocoder.yudao.module.enterprise.controller.admin.relation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 管理后台 - 用户企业关系 Response VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - 用户企业关系 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EnterpriseUserRelationRespVO extends EnterpriseUserRelationBaseVO {

    @Schema(description = "关系ID", required = true, example = "1024")
    private Long id;

    @Schema(description = "创建时间", required = true)
    private LocalDateTime createTime;

    // 以下为扩展信息，可能会根据需要从外部关联查询得到

    @Schema(description = "用户名称", example = "张三")
    private String userName;

    @Schema(description = "企业名称", example = "XXX有限公司")
    private String enterpriseName;

    @Schema(description = "门店名称", example = "总店")
    private String storeName;

} 
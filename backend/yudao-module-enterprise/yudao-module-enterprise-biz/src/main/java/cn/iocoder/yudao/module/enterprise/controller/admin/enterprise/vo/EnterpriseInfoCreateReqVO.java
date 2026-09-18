package cn.iocoder.yudao.module.enterprise.controller.admin.enterprise.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 管理后台 - 企业信息创建 Request VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - 企业信息创建 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EnterpriseInfoCreateReqVO extends EnterpriseInfoBaseVO {

} 
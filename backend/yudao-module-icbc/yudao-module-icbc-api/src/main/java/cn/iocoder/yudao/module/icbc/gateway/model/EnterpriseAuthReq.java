package cn.iocoder.yudao.module.icbc.gateway.model;

import lombok.Builder;
import lombok.Data;

/**
 * 企业授权请求
 *
 * 对应工行 `/ui/jft/ui/invoice/authorization/V1`，返回税务可信二维码：
 * 由使用单位的法定代表人或财务负责人用税务 App 扫码完成实人认证并录入授权有效期。
 */
@Data
@Builder
public class EnterpriseAuthReq {

    /**
     * 子商户编号（回收企业）
     */
    private String outVendorId;
    /**
     * 外部用户编号（操作人）
     */
    private String outUserId;
    /**
     * 站点类型
     */
    private String siteType;
    /**
     * 用户类型
     */
    private String userType;

}

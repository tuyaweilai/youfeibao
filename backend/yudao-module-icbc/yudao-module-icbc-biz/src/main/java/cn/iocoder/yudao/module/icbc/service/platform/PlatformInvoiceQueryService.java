package cn.iocoder.yudao.module.icbc.service.platform;

import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;

import java.util.List;

/**
 * 平台运营查询 Service 接口。
 *
 * <p>平台运营看的是全平台，不限于某一个回收企业租户，所以这里的查询必须跨租户。
 * 跨租户只允许平台运营角色调用，鉴权在 Controller 层用
 * {@code @icbc.hasPermission('icbc:platform:invoice:query')} 完成。
 */
public interface PlatformInvoiceQueryService {

    /**
     * 获得全平台的反向开票订单（跨租户，不做租户过滤）。
     *
     * @return 全部租户的订单列表
     */
    List<InvoiceOrderDO> getPlatformInvoiceList();

}

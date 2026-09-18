package cn.iocoder.yudao.module.icbc.service.platform.impl;

import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.service.platform.PlatformInvoiceQueryService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 平台运营查询 Service 实现。
 *
 * <p>跨租户读取用一个显式的 {@link TenantUtils#executeIgnore} 表达：只在这一个
 * 方法的范围内关掉租户过滤，读完全平台就立刻恢复。不要把「忽略租户」散落到
 * 其它服务里，否则租户隔离就名存实亡。
 */
@Service
public class PlatformInvoiceQueryServiceImpl implements PlatformInvoiceQueryService {

    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;

    @Override
    public List<InvoiceOrderDO> getPlatformInvoiceList() {
        return TenantUtils.executeIgnore(() -> invoiceOrderMapper.selectList());
    }

}

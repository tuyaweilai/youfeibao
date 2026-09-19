package cn.iocoder.yudao.module.icbc.service.publicapi.impl;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.icbc.controller.admin.download.vo.InvoiceDownloadRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.download.vo.InvoiceFileRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicContactLeadReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicQuotaRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.quota.vo.SellerQuotaRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.lead.IcbcContactLeadDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.lead.IcbcContactLeadMapper;
import cn.iocoder.yudao.module.icbc.enums.PublicTokenPurposeEnum;
import cn.iocoder.yudao.module.icbc.service.download.InvoiceDownloadService;
import cn.iocoder.yudao.module.icbc.service.publicapi.PublicAccessService;
import cn.iocoder.yudao.module.icbc.service.quota.NaturalPersonQuotaService;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenPayload;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.function.Supplier;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 公开端点 Service 实现。
 *
 * <p>入口在安全与租户白名单里（{@code /icbc/public/**}），没有租户上下文；每个方法先用
 * 令牌校验并占用次数，再显式 {@link #inTenant} 切到令牌解析出的租户下执行，等于把多租户
 * 隔离补回来，而不是绕过它。
 */
@Service
@Validated
public class PublicAccessServiceImpl implements PublicAccessService {

    @Resource
    private PublicTokenService publicTokenService;
    @Resource
    private InvoiceDownloadService invoiceDownloadService;
    @Resource
    private IcbcContactLeadMapper contactLeadMapper;
    @Resource
    private NaturalPersonQuotaService naturalPersonQuotaService;

    @Override
    public void downloadInvoicePdf(String token, HttpServletResponse response) {
        PublicTokenPayload payload = publicTokenService.verify(token, PublicTokenPurposeEnum.INVOICE_DOWNLOAD);
        inTenant(payload.getTenantId(), () -> {
            InvoiceDownloadRespVO record = invoiceDownloadService.getDownloadRecord(payload.getBusinessKey());
            InvoiceFileRespVO pdf = (record.getFiles() == null
                    ? Collections.<InvoiceFileRespVO>emptyList() : record.getFiles()).stream()
                    .filter(file -> file.getFileType() != null && "PDF".equalsIgnoreCase(file.getFileType()))
                    .findFirst()
                    .orElseThrow(() -> exception(INVOICE_FILE_NOT_FOUND));
            // 确认原件确实可取，再扣次数：单用途令牌不该被一次失败的取件烧掉
            publicTokenService.consume(payload);
            invoiceDownloadService.downloadFile(record.getId(), pdf.getFileType(), response);
        });
    }

    @Override
    public void submitContactLead(PublicContactLeadReqVO reqVO) {
        PublicTokenPayload payload = publicTokenService.redeem(reqVO.getToken(), PublicTokenPurposeEnum.CONTACT_LEAD);
        inTenant(payload.getTenantId(), () -> contactLeadMapper.insert(IcbcContactLeadDO.builder()
                .payeeId(Long.valueOf(payload.getBusinessKey()))
                .name(reqVO.getName())
                .mobile(reqVO.getMobile())
                .remark(reqVO.getRemark())
                .build()));
    }

    @Override
    public PublicQuotaRespVO queryQuota(String token) {
        PublicTokenPayload payload = publicTokenService.redeem(token, PublicTokenPurposeEnum.QUOTA_QUERY);
        return inTenant(payload.getTenantId(), () -> {
            // 额度是自然人的：这里跨租户合并了他在本平台其它租户的开票额
            SellerQuotaRespVO quota = naturalPersonQuotaService.getQuota(Long.valueOf(payload.getBusinessKey()));
            return BeanUtils.toBean(quota, PublicQuotaRespVO.class);
        });
    }

    private void inTenant(Long tenantId, Runnable runnable) {
        inTenant(tenantId, () -> {
            runnable.run();
            return null;
        });
    }

    private <T> T inTenant(Long tenantId, Supplier<T> supplier) {
        Long oldTenantId = TenantContextHolder.getTenantId();
        Boolean oldIgnore = TenantContextHolder.isIgnore();
        TenantContextHolder.setTenantId(tenantId);
        TenantContextHolder.setIgnore(false);
        try {
            return supplier.get();
        } finally {
            TenantContextHolder.setTenantId(oldTenantId);
            TenantContextHolder.setIgnore(oldIgnore);
        }
    }

}

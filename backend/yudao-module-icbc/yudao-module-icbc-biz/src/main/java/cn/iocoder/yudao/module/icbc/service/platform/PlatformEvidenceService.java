package cn.iocoder.yudao.module.icbc.service.platform;

import cn.iocoder.yudao.module.icbc.controller.admin.evidence.vo.EvidenceCompletenessSummaryRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.platform.vo.PlatformExceptionInvoiceRespVO;

import java.util.List;

/**
 * 平台运营查询 Service：全平台五流齐备率与异常票（#15）。
 *
 * <p>平台运营看的是全平台，不限于某一个回收企业租户，所以这里的查询必须跨租户。
 * 跨租户只允许平台运营角色调用，鉴权在 Controller 层完成。
 */
public interface PlatformEvidenceService {

    /**
     * 全平台五流齐备率（跨租户）。
     */
    EvidenceCompletenessSummaryRespVO getPlatformCompleteness();

    /**
     * 全平台异常票清单（跨租户）：状态线异常或已开票但五流不齐的票。
     */
    List<PlatformExceptionInvoiceRespVO> getExceptionInvoiceList();

}

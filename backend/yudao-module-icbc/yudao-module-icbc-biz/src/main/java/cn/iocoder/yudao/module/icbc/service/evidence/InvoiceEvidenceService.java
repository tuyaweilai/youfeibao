package cn.iocoder.yudao.module.icbc.service.evidence;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.evidence.vo.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 一票一档证据链 Service 接口。
 *
 * <p>一张票 = 一笔收购。本服务把这张票背后的五流证据聚合成一个可展开、可度量、
 * 可导出的整体，供税务核查使用。
 */
public interface InvoiceEvidenceService {

    /**
     * 获得一张票的五流证据链。
     *
     * @param partnerOrderId 合作方订单号
     * @return 证据链
     */
    EvidenceChainRespVO getEvidenceChain(String partnerOrderId);

    /**
     * 分页获得一票一档（含齐备率）。
     *
     * @param pageReqVO 分页查询
     * @return 分页结果
     */
    PageResult<EvidenceChainRespVO> getEvidencePage(EvidencePageReqVO pageReqVO);

    /**
     * 批量齐备率：逐票齐备率 + 汇总口径。
     *
     * @param scopeReqVO 取数范围
     * @return 批量齐备率
     */
    EvidenceCompletenessSummaryRespVO getCompleteness(EvidenceScopeReqVO scopeReqVO);

    /**
     * 补录一条证据。
     *
     * @param reqVO 证据信息
     * @return 证据ID
     */
    Long attachEvidence(EvidenceAttachReqVO reqVO);

    /**
     * 删除一条证据。
     *
     * @param id 证据ID
     */
    void deleteEvidence(Long id);

    /**
     * 取收购台账行（信息流的明细），供页面展示与导出共用。
     *
     * @param reqVO 查询条件
     * @return 台账行
     */
    List<AcquisitionLedgerRespVO> getLedgerRows(AcquisitionLedgerReqVO reqVO);

    /**
     * 导出单张票的证据包（zip）。
     */
    void exportEvidencePackage(String partnerOrderId, HttpServletResponse response);

    /**
     * 批量导出证据包（zip，每张票一个目录）。
     */
    void exportEvidencePackageBatch(EvidenceScopeReqVO scopeReqVO, HttpServletResponse response);

    /**
     * 导出收购台账（Excel）。
     */
    void exportAcquisitionLedger(AcquisitionLedgerReqVO reqVO, HttpServletResponse response);

}

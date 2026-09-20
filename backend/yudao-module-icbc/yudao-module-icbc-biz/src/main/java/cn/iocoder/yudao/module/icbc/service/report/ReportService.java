package cn.iocoder.yudao.module.icbc.service.report;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportAcquisitionLedgerPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportAcquisitionLedgerRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportAnomalyPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportAnomalyRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportPurchasePerformancePageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportPurchasePerformanceRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportSettlementPaymentPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportSettlementPaymentRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportStockBalancePageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportStockBalanceRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportStockRecordPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportStockRecordRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.report.vo.ReportTableRespVO;

import javax.validation.Valid;
import java.util.List;

/**
 * 经营报表 Service 接口（#57 T19）。
 *
 * <p>四张表（采购履约 / 收购台账 / 库存 / 结算付款）+ 一张异常表，全部**只读聚合**。
 * 聚合逻辑收在本包（{@code service/report}），不 import #55 的追溯类；跨域数据通过别的模块自己的
 * Service（如 #47 的 {@code PurchaseOrderService#getProgress}）或 {@code erp-api} 的只读端口取，
 * **不重算别人已经算好的口径**。
 *
 * <p>每张表的口径定义来自 {@code ReportTableEnum}，异常类型的判定口径来自
 * {@code ReportAnomalyTypeEnum}，都随响应返回，前端只展示、不自己判。
 */
public interface ReportService {

    /**
     * 五张表的清单与口径说明（前端用来在每张表上标注口径）。
     */
    List<ReportTableRespVO> getTables();

    /**
     * 采购履约表：计划量 / 实际履约量 / 余额 / 到期日 / 进度 / 超量与过期异常。
     *
     * <p>五口径与完成比例直接取自 {@code PurchaseOrderService#getProgress}（#47 的唯一口径来源）。
     */
    PageResult<ReportPurchasePerformanceRespVO> getPurchasePerformancePage(
            @Valid ReportPurchasePerformancePageReqVO reqVO);

    /**
     * 收购台账：一行一张收购单，各重量口径分列。
     *
     * <p><b>实际收购量只取收购单上已发生的计量结果</b>，预约约量与采购计划量不混入。
     */
    PageResult<ReportAcquisitionLedgerRespVO> getAcquisitionLedgerPage(
            @Valid ReportAcquisitionLedgerPageReqVO reqVO);

    /**
     * 库存表 - 在库量：一行一个「品类 + 仓库 + 库位 + 批次」的库存数，含库龄。只给数量口径。
     */
    PageResult<ReportStockBalanceRespVO> getStockBalancePage(@Valid ReportStockBalancePageReqVO reqVO);

    /**
     * 库存表 - 入出流水：一行一条库存变更（正入负出）。只给数量口径。
     */
    PageResult<ReportStockRecordRespVO> getStockRecordPage(@Valid ReportStockRecordPageReqVO reqVO);

    /**
     * 结算付款表：一行一张结算单，汇总结算金额与付款办理进度、回单状态、失败原因与未办理时长。
     */
    PageResult<ReportSettlementPaymentRespVO> getSettlementPaymentPage(
            @Valid ReportSettlementPaymentPageReqVO reqVO);

    /**
     * 异常表（派生清单，不新建表）。
     *
     * <p>{@code type} 为空时合并六类异常按时间倒序返回；指定类型时只返回该类。
     */
    PageResult<ReportAnomalyRespVO> getAnomalyPage(@Valid ReportAnomalyPageReqVO reqVO);

}

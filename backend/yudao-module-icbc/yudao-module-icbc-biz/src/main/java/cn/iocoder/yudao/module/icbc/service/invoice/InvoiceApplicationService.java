package cn.iocoder.yudao.module.icbc.service.invoice;

import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceApplicationApplyReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceApplicationBatchReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoiceApplicationResultVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoicePreCheckRespVO;

import javax.validation.Valid;
import java.util.List;

/**
 * 开票申请 Service 接口。
 *
 * <p>对应 issue #8：开票员对已登记的收购发起开票申请。系统先校验这一笔能不能开，
 * 通过后生成自然人确认页面交给出售者确认。<strong>这一步不产生发票</strong>，
 * 真正的票要等付款之后（#9 / #10）。
 *
 * <p>一切入参都从已登记的收购单与档案推导，开票员只选收购单与票种。
 */
public interface InvoiceApplicationService {

    /**
     * 发起前校验：逐项列出五类校验结果与补齐方式，不通过不产生任何业务。
     *
     * @param acquisitionId 收购单编号
     * @param invoiceType   发票类型：01-专票，02-普票
     */
    InvoicePreCheckRespVO preCheck(Long acquisitionId, String invoiceType);

    /**
     * 单笔发起开票申请：校验通过后经适配层预下单，返回自然人确认页面。
     */
    InvoiceApplicationResultVO apply(@Valid InvoiceApplicationApplyReqVO reqVO);

    /**
     * 批量发起开票申请：逐笔独立校验与提交，某一笔失败不影响其他笔。
     */
    List<InvoiceApplicationResultVO> applyBatch(@Valid InvoiceApplicationBatchReqVO reqVO);
}

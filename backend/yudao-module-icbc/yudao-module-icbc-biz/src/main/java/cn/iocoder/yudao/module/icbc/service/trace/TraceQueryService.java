package cn.iocoder.yudao.module.icbc.service.trace;

import cn.iocoder.yudao.module.icbc.controller.admin.trace.vo.TraceRowRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.trace.vo.TraceSearchReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.trace.vo.TraceSearchRespVO;

import javax.servlet.http.HttpServletResponse;

/**
 * 关联单据查询 Service（#55 T17）。
 *
 * <p>只读聚合：回答「这批货经历了什么」——采购订单 → 现场收货 → 仓储入库 → 结算确认四栏，
 * 后面接付款与发票；支持按单号 / 车牌 / 主体反查。不新建业务表，所有数字都能落到一张来源单据。
 *
 * <p>两条边界：
 * <ul>
 *     <li>差异 / 缺失关联只说明、不抹平，也不从库存反推结算金额（ADR 0028）；</li>
 *     <li>敏感字段（税号 / 身份证 / 手机号 / 银行卡）按岗位权限脱敏，导出走同一套判断并留记录。</li>
 * </ul>
 */
public interface TraceQueryService {

    /**
     * 按单号 / 车牌 / 主体反查一批收购单的链路（分页），并给出汇总、筛选范围与未展示明细数。
     * 至少要给出一个条件，否则报 {@code TRACE_QUERY_CONDITION_REQUIRED}。
     */
    TraceSearchRespVO search(TraceSearchReqVO reqVO);

    /**
     * 单张收购单的完整链路（一对多环节展开全部明细）。
     *
     * @param unmask 是否按岗位权限放开敏感字段；由 Controller 依 {@code icbc:trace:sensitive:view} 决定
     */
    TraceRowRespVO getTrace(Long acquisitionId, boolean unmask);

    /**
     * 导出查询结果（Excel）。与在线查看同一套脱敏判断，导出动作另记一条操作日志。
     */
    void export(TraceSearchReqVO reqVO, HttpServletResponse response);

}

package cn.iocoder.yudao.module.icbc.service.workbench;

import cn.iocoder.yudao.module.icbc.controller.admin.workbench.vo.WorkbenchOverviewRespVO;

/**
 * 工作台待办与预警（#56 T18）。
 *
 * <p>一个只读聚合：把散在收购 / 预约 / 结算 / 付款 / 开票 / 额度 / 资质里的「今天该处理什么」
 * 收成一屏，并给出开票就绪徽标。它不写任何业务数据，也不自己造状态——每一项都能追到来源单据。
 */
public interface WorkbenchService {

    /**
     * 工作台一屏：八类待办 + 三条预警（额度 / 资质到期 / 开票就绪）+ 开票就绪徽标。
     */
    WorkbenchOverviewRespVO getOverview();

}

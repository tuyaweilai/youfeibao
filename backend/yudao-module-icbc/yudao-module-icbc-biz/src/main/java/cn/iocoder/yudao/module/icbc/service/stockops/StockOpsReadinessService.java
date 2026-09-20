package cn.iocoder.yudao.module.icbc.service.stockops;

import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOpsReadinessRespVO;

/**
 * 「当前库存」口径就绪 Service（#54 T16，规格 #38 user story 37）。
 *
 * <p>四项能力（期初 / 出库 / 调拨 / 盘点）齐备之前，页面只能把余额称作「累计入库」；
 * 齐备之后还要**已导入期初**，才敢称「当前库存」。判定只有这一处，页面不自己拼口径。
 */
public interface StockOpsReadinessService {

    /**
     * 读取就绪状态：四项能力、是否已导期初、页面该用的称呼与提示。
     */
    StockOpsReadinessRespVO getReadiness();

}

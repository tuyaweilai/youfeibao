package cn.iocoder.yudao.module.icbc.service.stockops;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOpeningCancelReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOpeningImportReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOpeningPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOpeningRespVO;

import javax.validation.Valid;

/**
 * 期初导入 Service（#54 T16）。
 *
 * <p>启用平台之前就在仓库里的货要先录进来，余额才可能被称作「当前库存」。
 * 一行 = 一个「品类 + 仓库 + 库位 + 批次」，导入即过账（经 {@code StockApi#in} 写
 * {@code OPENING_IN} 流水）；同一维度只允许一条生效期初，重复导入整批拒绝。
 */
public interface StockOpeningService {

    /**
     * 导入一批期初并立即过账。
     *
     * @return 导入批次号
     */
    String importOpening(@Valid StockOpeningImportReqVO reqVO);

    /**
     * 作废一条期初：经 {@code StockApi#out} 冲销入库的库存（业务类型 {@code OPENING_IN_CANCEL}）。
     */
    void cancelOpening(@Valid StockOpeningCancelReqVO reqVO);

    /**
     * 期初记录分页
     */
    PageResult<StockOpeningRespVO> getOpeningPage(@Valid StockOpeningPageReqVO reqVO);

    /**
     * 是否已有生效中的期初（「当前库存」口径的一条数据前提，供就绪查询与测试断言）。
     */
    boolean hasActiveOpening();

}

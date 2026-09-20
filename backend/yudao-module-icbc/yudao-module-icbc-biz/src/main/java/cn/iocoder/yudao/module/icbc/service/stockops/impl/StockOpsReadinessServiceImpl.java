package cn.iocoder.yudao.module.icbc.service.stockops.impl;

import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOpsReadinessRespVO;
import cn.iocoder.yudao.module.icbc.service.stockops.StockOpeningService;
import cn.iocoder.yudao.module.icbc.service.stockops.StockOpsReadinessService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 「当前库存」口径就绪 Service 实现（#54 T16）。
 *
 * <p>判定拆成两层，都摊在返回值里：
 * <ol>
 *     <li>**能力**：期初 / 出库 / 调拨 / 盘点四项，#54 落地后恒为 true；</li>
 *     <li>**数据**：是否已导入期初。没有期初时余额漏掉启用平台之前的存量，
 *         只能说「累计入库」，说了「当前库存」就是误导（规格 #38 user story 37）。</li>
 * </ol>
 */
@Service
@Validated
public class StockOpsReadinessServiceImpl implements StockOpsReadinessService {

    @Resource
    private StockOpeningService stockOpeningService;

    @Override
    public StockOpsReadinessRespVO getReadiness() {
        // #54（T16）一次补齐四项能力，所以能力位恒为 true；写成字段而不是常量，
        // 是为了让页面/测试读到的是「这一版实际具备什么」，将来缺哪项就置哪项为 false
        boolean capabilitiesReady = true;
        boolean openingImported = stockOpeningService.hasActiveOpening();
        boolean currentStockReady = capabilitiesReady && openingImported;

        StockOpsReadinessRespVO resp = new StockOpsReadinessRespVO();
        resp.setOpeningSupported(true);
        resp.setOutboundSupported(true);
        resp.setMoveSupported(true);
        resp.setCheckSupported(true);
        resp.setCapabilitiesReady(capabilitiesReady);
        resp.setOpeningImported(openingImported);
        resp.setCurrentStockReady(currentStockReady);
        resp.setLabel(currentStockReady ? "当前库存" : "累计入库");
        List<String> missing = new ArrayList<>();
        if (!openingImported) {
            missing.add("尚未导入期初");
        }
        if (!capabilitiesReady) {
            missing.add("期初 / 出库 / 调拨 / 盘点四项能力未齐备");
        }
        resp.setMissingItems(missing);
        resp.setNotice(currentStockReady
                ? "已具备期初、出库、调拨与盘点，页面可以按「当前库存」展示余额。"
                : "尚未导入期初：当前余额只包含通过平台入库的数量，不能当作全量在库量，只能称「累计入库」。"
                + "请在「期初导入」把启用平台之前的存量录入后，页面才按「当前库存」展示。");
        return resp;
    }

}

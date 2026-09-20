package cn.iocoder.yudao.module.icbc.service.stockops.impl;

import cn.hutool.core.util.RandomUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.api.stock.StockApi;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockChangeReqDTO;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOpeningCancelReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOpeningImportReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOpeningItemReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOpeningPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOpeningRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockops.IcbcStockOpeningDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.stockops.IcbcStockOpeningMapper;
import cn.iocoder.yudao.module.icbc.enums.StockOpsStatusEnum;
import cn.iocoder.yudao.module.icbc.service.stockops.StockOpeningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 期初导入 Service 实现（#54 T16）。
 *
 * <p>导入即过账：一行期初写一条 {@code OPENING_IN} 流水并加库存。同一维度只允许一条生效期初，
 * 整批导入在一次事务里，任一行越界（维度重复、数量非法）都整批失败，不留下半份期初。
 */
@Slf4j
@Service
@Validated
public class StockOpeningServiceImpl implements StockOpeningService {

    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private IcbcStockOpeningMapper stockOpeningMapper;
    @Resource
    private StockApi stockApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importOpening(@Valid StockOpeningImportReqVO reqVO) {
        String openingNo = generateOpeningNo();
        // 先整批校验、再整批写入：任一行不合法就不动库存，不留下半份期初
        Set<String> dimensions = new HashSet<>();
        for (StockOpeningItemReqVO item : reqVO.getItems()) {
            assertItemValid(item);
            Long locationId = normalizeZero(item.getLocationId());
            Long batchId = normalizeZero(item.getBatchId());
            String dimension = item.getGoodsConfigId() + ":" + item.getWarehouseId() + ":"
                    + locationId + ":" + batchId;
            // 批内重复（同一维度在本次导入里出现了两次）
            if (!dimensions.add(dimension)) {
                throw exception(STOCK_OPENING_DIMENSION_DUPLICATED);
            }
            // 与已有生效期初重复
            if (stockOpeningMapper.selectActiveByDimension(item.getGoodsConfigId(), item.getWarehouseId(),
                    locationId, batchId) != null) {
                throw exception(STOCK_OPENING_DIMENSION_DUPLICATED);
            }
        }
        for (StockOpeningItemReqVO item : reqVO.getItems()) {
            IcbcStockOpeningDO opening = IcbcStockOpeningDO.builder()
                    .openingNo(openingNo)
                    .goodsConfigId(item.getGoodsConfigId())
                    .warehouseId(item.getWarehouseId())
                    .locationId(normalizeZero(item.getLocationId()))
                    .batchId(normalizeZero(item.getBatchId()))
                    .quantity(item.getQuantity())
                    .status(StockOpsStatusEnum.POSTED.getStatus())
                    .postedTime(LocalDateTime.now())
                    .remark(item.getRemark() != null ? item.getRemark() : reqVO.getRemark())
                    .build();
            stockOpeningMapper.insert(opening);
            stockApi.in(buildChangeReq(opening, ErpStockRecordBizTypeEnum.OPENING_IN.getType()));
        }
        log.info("期初已导入 - openingNo: {}, 行数: {}", openingNo, reqVO.getItems().size());
        return openingNo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOpening(@Valid StockOpeningCancelReqVO reqVO) {
        // 加锁读作为事务第一条语句：作废与（并发）导入的校验从第一条语句起串行化
        IcbcStockOpeningDO opening = reqVO.getId() == null ? null
                : stockOpeningMapper.selectByIdForUpdate(reqVO.getId());
        if (opening == null) {
            throw exception(STOCK_OPENING_NOT_EXISTS);
        }
        if (Objects.equals(opening.getStatus(), StockOpsStatusEnum.CANCELLED.getStatus())) {
            throw exception(STOCK_OPENING_ALREADY_CANCELLED);
        }
        // 冲销入库的库存（业务类型 OPENING_IN_CANCEL）
        stockApi.out(buildChangeReq(opening, ErpStockRecordBizTypeEnum.OPENING_IN_CANCEL.getType()));
        IcbcStockOpeningDO update = new IcbcStockOpeningDO();
        update.setId(opening.getId());
        update.setStatus(StockOpsStatusEnum.CANCELLED.getStatus());
        update.setCancelReason(reqVO.getReason());
        update.setCancelledTime(LocalDateTime.now());
        stockOpeningMapper.updateById(update);
        log.info("期初已作废 - openingNo: {}, 数量: {}, 原因: {}",
                opening.getOpeningNo(), opening.getQuantity(), reqVO.getReason());
    }

    @Override
    public PageResult<StockOpeningRespVO> getOpeningPage(@Valid StockOpeningPageReqVO reqVO) {
        PageResult<IcbcStockOpeningDO> page = stockOpeningMapper.selectPage(reqVO);
        return new PageResult<>(page.getList().stream().map(this::toResp).collect(Collectors.toList()),
                page.getTotal());
    }

    @Override
    public boolean hasActiveOpening() {
        return stockOpeningMapper.selectActiveCount() > 0;
    }

    // ==================== 内部方法 ====================

    private void assertItemValid(StockOpeningItemReqVO item) {
        if (item.getGoodsConfigId() == null || item.getWarehouseId() == null) {
            throw exception(STOCK_OPENING_ITEM_INVALID, "每行期初都要填写品类与仓库");
        }
        if (item.getQuantity() == null || item.getQuantity().signum() <= 0) {
            throw exception(STOCK_OPENING_ITEM_INVALID, "期初数量必须大于 0");
        }
    }

    private StockChangeReqDTO buildChangeReq(IcbcStockOpeningDO opening, Integer bizType) {
        StockChangeReqDTO reqDTO = new StockChangeReqDTO();
        reqDTO.setGoodsConfigId(opening.getGoodsConfigId());
        reqDTO.setWarehouseId(opening.getWarehouseId());
        reqDTO.setLocationId(normalizeZero(opening.getLocationId()));
        reqDTO.setBatchId(normalizeZero(opening.getBatchId()));
        reqDTO.setCount(opening.getQuantity());
        reqDTO.setBizType(bizType);
        // 一行期初 = 一个业务项：业务编号与业务项编号都用本行编号
        reqDTO.setBizId(opening.getId());
        reqDTO.setBizItemId(opening.getId());
        reqDTO.setBizNo(opening.getOpeningNo());
        return reqDTO;
    }

    private StockOpeningRespVO toResp(IcbcStockOpeningDO opening) {
        StockOpeningRespVO resp = BeanUtils.toBean(opening, StockOpeningRespVO.class);
        resp.setStatusName(StockOpsStatusEnum.nameOf(opening.getStatus()));
        return resp;
    }

    private static Long normalizeZero(Long id) {
        return id == null ? 0L : id;
    }

    private String generateOpeningNo() {
        return "OP" + LocalDateTime.now().format(NO_FORMATTER) + RandomUtil.randomNumbers(4);
    }

}

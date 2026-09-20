package cn.iocoder.yudao.module.icbc.service.stockops.impl;

import cn.hutool.core.util.RandomUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.api.stock.StockApi;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockChangeReqDTO;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockMoveReqDTO;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockMoveCancelReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockMoveItemReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockMoveItemRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockMovePageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockMoveRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockMoveSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockops.IcbcStockMoveDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockops.IcbcStockMoveItemDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.stockops.IcbcStockMoveItemMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.stockops.IcbcStockMoveMapper;
import cn.iocoder.yudao.module.icbc.enums.StockOpsStatusEnum;
import cn.iocoder.yudao.module.icbc.service.stockops.StockMoveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 跨仓调拨 Service 实现（#54 T16）。
 *
 * <p>写入路径：过账走 {@code StockApi#move}（ERP 在同一事务里写 {@code MOVE_OUT} + {@code MOVE_IN}），
 * 作废已过账的单按相反方向调回（{@code MOVE_OUT_CANCEL} / {@code MOVE_IN_CANCEL}）。
 * 源库存不足时整单回滚，不做部分调拨。
 */
@Slf4j
@Service
@Validated
public class StockMoveServiceImpl implements StockMoveService {

    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private IcbcStockMoveMapper stockMoveMapper;
    @Resource
    private IcbcStockMoveItemMapper stockMoveItemMapper;
    @Resource
    private StockApi stockApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStockMove(@Valid StockMoveSaveReqVO reqVO) {
        return doCreateStockMove(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long confirmStockMove(@Valid StockMoveSaveReqVO reqVO) {
        return doPostStockMove(doCreateStockMove(reqVO));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void postStockMove(Long id) {
        doPostStockMove(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelStockMove(@Valid StockMoveCancelReqVO reqVO) {
        // 加锁读作为事务第一条语句：作废与过账的并发从第一条语句起串行化
        IcbcStockMoveDO stockMove = getStockMoveForUpdate(reqVO.getId());
        if (Objects.equals(stockMove.getStatus(), StockOpsStatusEnum.CANCELLED.getStatus())) {
            throw exception(STOCK_MOVE_STATUS_NOT_ALLOW, StockOpsStatusEnum.CANCELLED.getName());
        }
        if (Objects.equals(stockMove.getStatus(), StockOpsStatusEnum.POSTED.getStatus())) {
            // 已过账：按相反方向调回。先给源加回去（一定安全），再从目标减——
            // 目标已经把货用掉时整单回滚，不做出「只调回一半」的假账
            for (IcbcStockMoveItemDO item : stockMoveItemMapper.selectListByMoveId(stockMove.getId())) {
                stockApi.in(buildChangeReq(stockMove, item, item.getFromWarehouseId(),
                        item.getFromLocationId(), item.getFromBatchId(),
                        ErpStockRecordBizTypeEnum.MOVE_OUT_CANCEL.getType()));
                stockApi.out(buildChangeReq(stockMove, item, item.getToWarehouseId(),
                        item.getToLocationId(), item.getToBatchId(),
                        ErpStockRecordBizTypeEnum.MOVE_IN_CANCEL.getType()));
            }
        }
        IcbcStockMoveDO update = new IcbcStockMoveDO();
        update.setId(stockMove.getId());
        update.setStatus(StockOpsStatusEnum.CANCELLED.getStatus());
        update.setCancelReason(reqVO.getReason());
        update.setCancelledTime(LocalDateTime.now());
        stockMoveMapper.updateById(update);
        log.info("跨仓调拨单已作废 - moveNo: {}, 原状态: {}, 原因: {}",
                stockMove.getMoveNo(), stockMove.getStatus(), reqVO.getReason());
    }

    @Override
    public StockMoveRespVO getStockMove(Long id) {
        IcbcStockMoveDO stockMove = getStockMoveDO(id);
        StockMoveRespVO resp = toResp(stockMove);
        resp.setItems(stockMoveItemMapper.selectListByMoveId(id).stream()
                .map(item -> BeanUtils.toBean(item, StockMoveItemRespVO.class))
                .collect(Collectors.toList()));
        return resp;
    }

    @Override
    public PageResult<StockMoveRespVO> getStockMovePage(@Valid StockMovePageReqVO reqVO) {
        PageResult<IcbcStockMoveDO> page = stockMoveMapper.selectPage(reqVO);
        return new PageResult<>(page.getList().stream().map(this::toResp).collect(Collectors.toList()),
                page.getTotal());
    }

    // ==================== 内部方法 ====================

    private Long doCreateStockMove(StockMoveSaveReqVO reqVO) {
        BigDecimal total = BigDecimal.ZERO;
        for (StockMoveItemReqVO item : reqVO.getItems()) {
            if (item.getGoodsConfigId() == null) {
                throw exception(STOCK_MOVE_ITEM_INVALID, "每条明细都要选择品类");
            }
            if (item.getFromWarehouseId() == null || item.getToWarehouseId() == null) {
                throw exception(STOCK_MOVE_ITEM_INVALID, "每条明细都要选择源仓库与目标仓库");
            }
            if (item.getQuantity() == null || item.getQuantity().signum() <= 0) {
                throw exception(STOCK_MOVE_ITEM_INVALID, "调拨数量必须大于 0");
            }
            if (isSamePosition(item)) {
                throw exception(STOCK_MOVE_SAME_POSITION);
            }
            total = total.add(item.getQuantity());
        }
        IcbcStockMoveDO stockMove = IcbcStockMoveDO.builder()
                .moveNo(generateMoveNo())
                .totalQuantity(total)
                .status(StockOpsStatusEnum.PENDING.getStatus())
                .remark(reqVO.getRemark())
                .build();
        stockMoveMapper.insert(stockMove);
        for (StockMoveItemReqVO item : reqVO.getItems()) {
            stockMoveItemMapper.insert(IcbcStockMoveItemDO.builder()
                    .moveId(stockMove.getId())
                    .goodsConfigId(item.getGoodsConfigId())
                    .fromWarehouseId(item.getFromWarehouseId())
                    .fromLocationId(normalizeZero(item.getFromLocationId()))
                    .fromBatchId(normalizeZero(item.getFromBatchId()))
                    .toWarehouseId(item.getToWarehouseId())
                    .toLocationId(normalizeZero(item.getToLocationId()))
                    .toBatchId(normalizeZero(item.getToBatchId()))
                    .quantity(item.getQuantity())
                    .remark(item.getRemark())
                    .build());
        }
        return stockMove.getId();
    }

    private Long doPostStockMove(Long id) {
        // 加锁读作为第一条语句（原因见 cancelStockMove）
        IcbcStockMoveDO stockMove = getStockMoveForUpdate(id);
        if (Objects.equals(stockMove.getStatus(), StockOpsStatusEnum.POSTED.getStatus())) {
            return stockMove.getId(); // 幂等：重复过账不再移动
        }
        if (Objects.equals(stockMove.getStatus(), StockOpsStatusEnum.CANCELLED.getStatus())) {
            throw exception(STOCK_MOVE_STATUS_NOT_ALLOW, StockOpsStatusEnum.CANCELLED.getName());
        }
        for (IcbcStockMoveItemDO item : stockMoveItemMapper.selectListByMoveId(stockMove.getId())) {
            StockMoveReqDTO reqDTO = new StockMoveReqDTO();
            reqDTO.setGoodsConfigId(item.getGoodsConfigId());
            reqDTO.setFromWarehouseId(item.getFromWarehouseId());
            reqDTO.setFromLocationId(normalizeZero(item.getFromLocationId()));
            reqDTO.setFromBatchId(normalizeZero(item.getFromBatchId()));
            reqDTO.setToWarehouseId(item.getToWarehouseId());
            reqDTO.setToLocationId(normalizeZero(item.getToLocationId()));
            reqDTO.setToBatchId(normalizeZero(item.getToBatchId()));
            reqDTO.setCount(item.getQuantity());
            reqDTO.setBizId(stockMove.getId());
            reqDTO.setBizItemId(item.getId());
            reqDTO.setBizNo(stockMove.getMoveNo());
            stockApi.move(reqDTO);
        }
        IcbcStockMoveDO update = new IcbcStockMoveDO();
        update.setId(stockMove.getId());
        update.setStatus(StockOpsStatusEnum.POSTED.getStatus());
        update.setPostedTime(LocalDateTime.now());
        stockMoveMapper.updateById(update);
        log.info("跨仓调拨单已过账 - moveNo: {}, 数量: {}", stockMove.getMoveNo(), stockMove.getTotalQuantity());
        return stockMove.getId();
    }

    private IcbcStockMoveDO getStockMoveDO(Long id) {
        IcbcStockMoveDO stockMove = id == null ? null : stockMoveMapper.selectById(id);
        if (stockMove == null) {
            throw exception(STOCK_MOVE_NOT_EXISTS);
        }
        return stockMove;
    }

    private IcbcStockMoveDO getStockMoveForUpdate(Long id) {
        IcbcStockMoveDO stockMove = id == null ? null : stockMoveMapper.selectByIdForUpdate(id);
        if (stockMove == null) {
            throw exception(STOCK_MOVE_NOT_EXISTS);
        }
        return stockMove;
    }

    /** 源与目标的仓库 / 库位 / 批次全都相同 → 没有可移动的货。 */
    private static boolean isSamePosition(StockMoveItemReqVO item) {
        return Objects.equals(item.getFromWarehouseId(), item.getToWarehouseId())
                && Objects.equals(normalizeZero(item.getFromLocationId()), normalizeZero(item.getToLocationId()))
                && Objects.equals(normalizeZero(item.getFromBatchId()), normalizeZero(item.getToBatchId()));
    }

    /** 作废调拨时用的单侧变更请求（源用 MOVE_OUT_CANCEL 加回、目标用 MOVE_IN_CANCEL 减掉）。 */
    private StockChangeReqDTO buildChangeReq(IcbcStockMoveDO stockMove, IcbcStockMoveItemDO item,
                                             Long warehouseId, Long locationId, Long batchId, Integer bizType) {
        StockChangeReqDTO reqDTO = new StockChangeReqDTO();
        reqDTO.setGoodsConfigId(item.getGoodsConfigId());
        reqDTO.setWarehouseId(warehouseId);
        reqDTO.setLocationId(normalizeZero(locationId));
        reqDTO.setBatchId(normalizeZero(batchId));
        reqDTO.setCount(item.getQuantity());
        reqDTO.setBizType(bizType);
        reqDTO.setBizId(stockMove.getId());
        reqDTO.setBizItemId(item.getId());
        reqDTO.setBizNo(stockMove.getMoveNo());
        return reqDTO;
    }

    private StockMoveRespVO toResp(IcbcStockMoveDO stockMove) {
        StockMoveRespVO resp = BeanUtils.toBean(stockMove, StockMoveRespVO.class);
        resp.setStatusName(StockOpsStatusEnum.nameOf(stockMove.getStatus()));
        return resp;
    }

    private static Long normalizeZero(Long id) {
        return id == null ? 0L : id;
    }

    private String generateMoveNo() {
        return "SM" + LocalDateTime.now().format(NO_FORMATTER) + RandomUtil.randomNumbers(4);
    }

}

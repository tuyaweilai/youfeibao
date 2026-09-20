package cn.iocoder.yudao.module.icbc.service.stockops.impl;

import cn.hutool.core.util.RandomUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.api.stock.StockApi;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockAdjustReqDTO;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockChangeReqDTO;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockCheckCancelReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockCheckItemReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockCheckItemRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockCheckPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockCheckRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockCheckSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockops.IcbcStockCheckDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockops.IcbcStockCheckItemDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.stockops.IcbcStockCheckItemMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.stockops.IcbcStockCheckMapper;
import cn.iocoder.yudao.module.icbc.enums.StockOpsStatusEnum;
import cn.iocoder.yudao.module.icbc.service.stockops.StockCheckService;
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
 * 盘点调整 Service 实现（#54 T16）。
 *
 * <p>差额由 ERP 在 {@code StockApi#adjustTo} 里算并写成盘盈 / 盘亏流水，本类只把实盘数递过去、
 * 把返回的差额落明细。作废已过账的盘点单时按**记录的差额**冲销（不是重新算一遍），
 * 否则期间发生出入库后会冲错。
 */
@Slf4j
@Service
@Validated
public class StockCheckServiceImpl implements StockCheckService {

    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private IcbcStockCheckMapper stockCheckMapper;
    @Resource
    private IcbcStockCheckItemMapper stockCheckItemMapper;
    @Resource
    private StockApi stockApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStockCheck(@Valid StockCheckSaveReqVO reqVO) {
        return doCreateStockCheck(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long confirmStockCheck(@Valid StockCheckSaveReqVO reqVO) {
        return doPostStockCheck(doCreateStockCheck(reqVO));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void postStockCheck(Long id) {
        doPostStockCheck(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelStockCheck(@Valid StockCheckCancelReqVO reqVO) {
        // 加锁读作为事务第一条语句：作废与过账的并发从第一条语句起串行化
        IcbcStockCheckDO stockCheck = getStockCheckForUpdate(reqVO.getId());
        if (Objects.equals(stockCheck.getStatus(), StockOpsStatusEnum.CANCELLED.getStatus())) {
            throw exception(STOCK_CHECK_STATUS_NOT_ALLOW, StockOpsStatusEnum.CANCELLED.getName());
        }
        if (Objects.equals(stockCheck.getStatus(), StockOpsStatusEnum.POSTED.getStatus())) {
            // 已过账：按记录的差额反向冲销（盘盈的减掉、盘亏的加回）
            for (IcbcStockCheckItemDO item : stockCheckItemMapper.selectListByCheckId(stockCheck.getId())) {
                BigDecimal diff = item.getDifferenceQuantity();
                if (diff == null || diff.signum() == 0) {
                    continue;
                }
                StockChangeReqDTO reqDTO = buildChangeReq(stockCheck, item,
                        diff.signum() > 0 ? ErpStockRecordBizTypeEnum.CHECK_MORE_IN_CANCEL.getType()
                                : ErpStockRecordBizTypeEnum.CHECK_LESS_OUT_CANCEL.getType());
                reqDTO.setCount(diff.abs());
                if (diff.signum() > 0) {
                    stockApi.out(reqDTO);
                } else {
                    stockApi.in(reqDTO);
                }
            }
        }
        IcbcStockCheckDO update = new IcbcStockCheckDO();
        update.setId(stockCheck.getId());
        update.setStatus(StockOpsStatusEnum.CANCELLED.getStatus());
        update.setCancelReason(reqVO.getReason());
        update.setCancelledTime(LocalDateTime.now());
        stockCheckMapper.updateById(update);
        log.info("盘点单已作废 - checkNo: {}, 原状态: {}, 原因: {}",
                stockCheck.getCheckNo(), stockCheck.getStatus(), reqVO.getReason());
    }

    @Override
    public StockCheckRespVO getStockCheck(Long id) {
        IcbcStockCheckDO stockCheck = getStockCheckDO(id);
        StockCheckRespVO resp = toResp(stockCheck);
        resp.setItems(stockCheckItemMapper.selectListByCheckId(id).stream()
                .map(item -> BeanUtils.toBean(item, StockCheckItemRespVO.class))
                .collect(Collectors.toList()));
        return resp;
    }

    @Override
    public PageResult<StockCheckRespVO> getStockCheckPage(@Valid StockCheckPageReqVO reqVO) {
        PageResult<IcbcStockCheckDO> page = stockCheckMapper.selectPage(reqVO);
        return new PageResult<>(page.getList().stream().map(this::toResp).collect(Collectors.toList()),
                page.getTotal());
    }

    // ==================== 内部方法 ====================

    private Long doCreateStockCheck(StockCheckSaveReqVO reqVO) {
        for (StockCheckItemReqVO item : reqVO.getItems()) {
            if (item.getGoodsConfigId() == null || item.getWarehouseId() == null) {
                throw exception(STOCK_CHECK_ITEM_INVALID, "每条明细都要选择品类与仓库");
            }
            if (item.getActualQuantity() == null || item.getActualQuantity().signum() < 0) {
                throw exception(STOCK_CHECK_ACTUAL_NEGATIVE);
            }
        }
        IcbcStockCheckDO stockCheck = IcbcStockCheckDO.builder()
                .checkNo(generateCheckNo())
                .status(StockOpsStatusEnum.PENDING.getStatus())
                .remark(reqVO.getRemark())
                .build();
        stockCheckMapper.insert(stockCheck);
        for (StockCheckItemReqVO item : reqVO.getItems()) {
            stockCheckItemMapper.insert(IcbcStockCheckItemDO.builder()
                    .checkId(stockCheck.getId())
                    .goodsConfigId(item.getGoodsConfigId())
                    .warehouseId(item.getWarehouseId())
                    .locationId(normalizeZero(item.getLocationId()))
                    .batchId(normalizeZero(item.getBatchId()))
                    .actualQuantity(item.getActualQuantity())
                    .remark(item.getRemark())
                    .build());
        }
        return stockCheck.getId();
    }

    private Long doPostStockCheck(Long id) {
        // 加锁读作为第一条语句（原因见 cancelStockCheck）
        IcbcStockCheckDO stockCheck = getStockCheckForUpdate(id);
        if (Objects.equals(stockCheck.getStatus(), StockOpsStatusEnum.POSTED.getStatus())) {
            return stockCheck.getId(); // 幂等：重复过账不再调整
        }
        if (Objects.equals(stockCheck.getStatus(), StockOpsStatusEnum.CANCELLED.getStatus())) {
            throw exception(STOCK_CHECK_STATUS_NOT_ALLOW, StockOpsStatusEnum.CANCELLED.getName());
        }
        for (IcbcStockCheckItemDO item : stockCheckItemMapper.selectListByCheckId(stockCheck.getId())) {
            StockAdjustReqDTO reqDTO = new StockAdjustReqDTO();
            reqDTO.setGoodsConfigId(item.getGoodsConfigId());
            reqDTO.setWarehouseId(item.getWarehouseId());
            reqDTO.setLocationId(normalizeZero(item.getLocationId()));
            reqDTO.setBatchId(normalizeZero(item.getBatchId()));
            reqDTO.setTargetCount(item.getActualQuantity());
            reqDTO.setBizId(stockCheck.getId());
            reqDTO.setBizItemId(item.getId());
            reqDTO.setBizNo(stockCheck.getCheckNo());
            // 差额在 ERP 侧算（同一个事务、加行锁），返回后落明细：账面 = 实盘 − 差额
            BigDecimal diff = stockApi.adjustTo(reqDTO);
            IcbcStockCheckItemDO update = new IcbcStockCheckItemDO();
            update.setId(item.getId());
            update.setDifferenceQuantity(diff);
            update.setBookQuantity(item.getActualQuantity().subtract(diff));
            stockCheckItemMapper.updateById(update);
        }
        IcbcStockCheckDO update = new IcbcStockCheckDO();
        update.setId(stockCheck.getId());
        update.setStatus(StockOpsStatusEnum.POSTED.getStatus());
        update.setPostedTime(LocalDateTime.now());
        stockCheckMapper.updateById(update);
        log.info("盘点单已过账 - checkNo: {}, 明细数: {}", stockCheck.getCheckNo(),
                stockCheckItemMapper.selectListByCheckId(stockCheck.getId()).size());
        return stockCheck.getId();
    }

    private IcbcStockCheckDO getStockCheckDO(Long id) {
        IcbcStockCheckDO stockCheck = id == null ? null : stockCheckMapper.selectById(id);
        if (stockCheck == null) {
            throw exception(STOCK_CHECK_NOT_EXISTS);
        }
        return stockCheck;
    }

    private IcbcStockCheckDO getStockCheckForUpdate(Long id) {
        IcbcStockCheckDO stockCheck = id == null ? null : stockCheckMapper.selectByIdForUpdate(id);
        if (stockCheck == null) {
            throw exception(STOCK_CHECK_NOT_EXISTS);
        }
        return stockCheck;
    }

    private StockChangeReqDTO buildChangeReq(IcbcStockCheckDO stockCheck, IcbcStockCheckItemDO item,
                                             Integer bizType) {
        StockChangeReqDTO reqDTO = new StockChangeReqDTO();
        reqDTO.setGoodsConfigId(item.getGoodsConfigId());
        reqDTO.setWarehouseId(item.getWarehouseId());
        reqDTO.setLocationId(normalizeZero(item.getLocationId()));
        reqDTO.setBatchId(normalizeZero(item.getBatchId()));
        reqDTO.setBizType(bizType);
        reqDTO.setBizId(stockCheck.getId());
        reqDTO.setBizItemId(item.getId());
        reqDTO.setBizNo(stockCheck.getCheckNo());
        return reqDTO;
    }

    private StockCheckRespVO toResp(IcbcStockCheckDO stockCheck) {
        StockCheckRespVO resp = BeanUtils.toBean(stockCheck, StockCheckRespVO.class);
        resp.setStatusName(StockOpsStatusEnum.nameOf(stockCheck.getStatus()));
        return resp;
    }

    private static Long normalizeZero(Long id) {
        return id == null ? 0L : id;
    }

    private String generateCheckNo() {
        return "SC" + LocalDateTime.now().format(NO_FORMATTER) + RandomUtil.randomNumbers(4);
    }

}

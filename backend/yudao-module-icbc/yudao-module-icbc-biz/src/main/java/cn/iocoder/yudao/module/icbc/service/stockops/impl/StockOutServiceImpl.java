package cn.iocoder.yudao.module.icbc.service.stockops.impl;

import cn.hutool.core.util.RandomUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.api.stock.StockApi;
import cn.iocoder.yudao.module.erp.api.stock.dto.StockChangeReqDTO;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOutCancelReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOutItemReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOutItemRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOutPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOutRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOutSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockops.IcbcStockOutDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.stockops.IcbcStockOutItemDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.stockops.IcbcStockOutItemMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.stockops.IcbcStockOutMapper;
import cn.iocoder.yudao.module.icbc.enums.StockOpsStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.StockOutTypeEnum;
import cn.iocoder.yudao.module.icbc.service.stockops.StockOutService;
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
 * 非销售出库 Service 实现（#54 T16）。
 *
 * <p>写入路径：过账走 {@code StockApi#out}（业务类型按出库类型取报损 / 退货出库 / 内部领用），
 * 作废已过账的单按相反方向调回（对应 {@code *_CANCEL}）。icbc 不直接碰 {@code erp_stock*}。
 */
@Slf4j
@Service
@Validated
public class StockOutServiceImpl implements StockOutService {

    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private IcbcStockOutMapper stockOutMapper;
    @Resource
    private IcbcStockOutItemMapper stockOutItemMapper;
    @Resource
    private StockApi stockApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStockOut(@Valid StockOutSaveReqVO reqVO) {
        return doCreateStockOut(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long confirmStockOut(@Valid StockOutSaveReqVO reqVO) {
        return doPostStockOut(doCreateStockOut(reqVO));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void postStockOut(Long id) {
        doPostStockOut(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelStockOut(@Valid StockOutCancelReqVO reqVO) {
        // 加锁读作为事务第一条语句：作废与过账的并发从第一条语句起串行化
        IcbcStockOutDO stockOut = getStockOutForUpdate(reqVO.getId());
        if (Objects.equals(stockOut.getStatus(), StockOpsStatusEnum.CANCELLED.getStatus())) {
            throw exception(STOCK_OUT_STATUS_NOT_ALLOW, StockOpsStatusEnum.CANCELLED.getName());
        }
        if (Objects.equals(stockOut.getStatus(), StockOpsStatusEnum.POSTED.getStatus())) {
            // 已过账：把减掉的库存加回去（业务类型用 *_CANCEL）
            Integer cancelBizType = cancelBizType(stockOut.getOutType());
            for (IcbcStockOutItemDO item : stockOutItemMapper.selectListByStockOutId(stockOut.getId())) {
                stockApi.in(buildChangeReq(stockOut, item, cancelBizType));
            }
        }
        IcbcStockOutDO update = new IcbcStockOutDO();
        update.setId(stockOut.getId());
        update.setStatus(StockOpsStatusEnum.CANCELLED.getStatus());
        update.setCancelReason(reqVO.getReason());
        update.setCancelledTime(LocalDateTime.now());
        stockOutMapper.updateById(update);
        log.info("非销售出库单已作废 - stockOutNo: {}, 原状态: {}, 原因: {}",
                stockOut.getStockOutNo(), stockOut.getStatus(), reqVO.getReason());
    }

    @Override
    public StockOutRespVO getStockOut(Long id) {
        IcbcStockOutDO stockOut = getStockOutDO(id);
        StockOutRespVO resp = toResp(stockOut);
        resp.setItems(stockOutItemMapper.selectListByStockOutId(id).stream()
                .map(item -> BeanUtils.toBean(item, StockOutItemRespVO.class))
                .collect(Collectors.toList()));
        return resp;
    }

    @Override
    public PageResult<StockOutRespVO> getStockOutPage(@Valid StockOutPageReqVO reqVO) {
        PageResult<IcbcStockOutDO> page = stockOutMapper.selectPage(reqVO);
        return new PageResult<>(page.getList().stream().map(this::toResp).collect(Collectors.toList()),
                page.getTotal());
    }

    // ==================== 内部方法 ====================

    private Long doCreateStockOut(StockOutSaveReqVO reqVO) {
        if (StockOutTypeEnum.ofType(reqVO.getOutType()).isEmpty()) {
            throw exception(STOCK_OUT_TYPE_INVALID, reqVO.getOutType());
        }
        // 校验明细：品类 / 仓库必填，数量为正
        BigDecimal total = BigDecimal.ZERO;
        for (StockOutItemReqVO item : reqVO.getItems()) {
            if (item.getGoodsConfigId() == null || item.getWarehouseId() == null) {
                throw exception(STOCK_OUT_ITEM_INVALID, "每条明细都要选择品类与仓库");
            }
            if (item.getQuantity() == null || item.getQuantity().signum() <= 0) {
                throw exception(STOCK_OUT_ITEM_INVALID, "出库数量必须大于 0");
            }
            total = total.add(item.getQuantity());
        }
        IcbcStockOutDO stockOut = IcbcStockOutDO.builder()
                .stockOutNo(generateStockOutNo())
                .outType(reqVO.getOutType())
                .totalQuantity(total)
                .status(StockOpsStatusEnum.PENDING.getStatus())
                .remark(reqVO.getRemark())
                .build();
        stockOutMapper.insert(stockOut);
        for (StockOutItemReqVO item : reqVO.getItems()) {
            stockOutItemMapper.insert(IcbcStockOutItemDO.builder()
                    .stockOutId(stockOut.getId())
                    .goodsConfigId(item.getGoodsConfigId())
                    .warehouseId(item.getWarehouseId())
                    .locationId(normalizeZero(item.getLocationId()))
                    .batchId(normalizeZero(item.getBatchId()))
                    .quantity(item.getQuantity())
                    .remark(item.getRemark())
                    .build());
        }
        return stockOut.getId();
    }

    private Long doPostStockOut(Long id) {
        // 加锁读作为第一条语句（原因见 cancelStockOut）
        IcbcStockOutDO stockOut = getStockOutForUpdate(id);
        if (Objects.equals(stockOut.getStatus(), StockOpsStatusEnum.POSTED.getStatus())) {
            return stockOut.getId(); // 幂等：重复过账不再减库存
        }
        if (Objects.equals(stockOut.getStatus(), StockOpsStatusEnum.CANCELLED.getStatus())) {
            throw exception(STOCK_OUT_STATUS_NOT_ALLOW, StockOpsStatusEnum.CANCELLED.getName());
        }
        Integer outBizType = outBizType(stockOut.getOutType());
        for (IcbcStockOutItemDO item : stockOutItemMapper.selectListByStockOutId(stockOut.getId())) {
            // 库存不足时 StockApi 抛 STOCK_COUNT_NEGATIVE，整单回滚，不做部分出库
            stockApi.out(buildChangeReq(stockOut, item, outBizType));
        }
        IcbcStockOutDO update = new IcbcStockOutDO();
        update.setId(stockOut.getId());
        update.setStatus(StockOpsStatusEnum.POSTED.getStatus());
        update.setPostedTime(LocalDateTime.now());
        stockOutMapper.updateById(update);
        log.info("非销售出库单已过账 - stockOutNo: {}, 类型: {}, 数量: {}",
                stockOut.getStockOutNo(), StockOutTypeEnum.nameOf(stockOut.getOutType()),
                stockOut.getTotalQuantity());
        return stockOut.getId();
    }

    private IcbcStockOutDO getStockOutDO(Long id) {
        IcbcStockOutDO stockOut = id == null ? null : stockOutMapper.selectById(id);
        if (stockOut == null) {
            throw exception(STOCK_OUT_NOT_EXISTS);
        }
        return stockOut;
    }

    private IcbcStockOutDO getStockOutForUpdate(Long id) {
        IcbcStockOutDO stockOut = id == null ? null : stockOutMapper.selectByIdForUpdate(id);
        if (stockOut == null) {
            throw exception(STOCK_OUT_NOT_EXISTS);
        }
        return stockOut;
    }

    private static Integer outBizType(Integer outType) {
        StockOutTypeEnum type = StockOutTypeEnum.ofType(outType).orElse(StockOutTypeEnum.SCRAP);
        switch (type) {
            case RETURN:
                return ErpStockRecordBizTypeEnum.RETURN_OUT.getType();
            case INTERNAL_USE:
                return ErpStockRecordBizTypeEnum.INTERNAL_USE_OUT.getType();
            case SCRAP:
            default:
                return ErpStockRecordBizTypeEnum.SCRAP_OUT.getType();
        }
    }

    private static Integer cancelBizType(Integer outType) {
        StockOutTypeEnum type = StockOutTypeEnum.ofType(outType).orElse(StockOutTypeEnum.SCRAP);
        switch (type) {
            case RETURN:
                return ErpStockRecordBizTypeEnum.RETURN_OUT_CANCEL.getType();
            case INTERNAL_USE:
                return ErpStockRecordBizTypeEnum.INTERNAL_USE_OUT_CANCEL.getType();
            case SCRAP:
            default:
                return ErpStockRecordBizTypeEnum.SCRAP_OUT_CANCEL.getType();
        }
    }

    private StockChangeReqDTO buildChangeReq(IcbcStockOutDO stockOut, IcbcStockOutItemDO item, Integer bizType) {
        StockChangeReqDTO reqDTO = new StockChangeReqDTO();
        reqDTO.setGoodsConfigId(item.getGoodsConfigId());
        reqDTO.setWarehouseId(item.getWarehouseId());
        reqDTO.setLocationId(normalizeZero(item.getLocationId()));
        reqDTO.setBatchId(normalizeZero(item.getBatchId()));
        reqDTO.setCount(item.getQuantity());
        reqDTO.setBizType(bizType);
        // 业务编号用出库单、业务项编号用明细：同一明细只写一次流水
        reqDTO.setBizId(stockOut.getId());
        reqDTO.setBizItemId(item.getId());
        reqDTO.setBizNo(stockOut.getStockOutNo());
        return reqDTO;
    }

    private StockOutRespVO toResp(IcbcStockOutDO stockOut) {
        StockOutRespVO resp = BeanUtils.toBean(stockOut, StockOutRespVO.class);
        resp.setOutTypeName(StockOutTypeEnum.nameOf(stockOut.getOutType()));
        resp.setStatusName(StockOpsStatusEnum.nameOf(stockOut.getStatus()));
        return resp;
    }

    private static Long normalizeZero(Long id) {
        return id == null ? 0L : id;
    }

    private String generateStockOutNo() {
        return "SO" + LocalDateTime.now().format(NO_FORMATTER) + RandomUtil.randomNumbers(4);
    }

}

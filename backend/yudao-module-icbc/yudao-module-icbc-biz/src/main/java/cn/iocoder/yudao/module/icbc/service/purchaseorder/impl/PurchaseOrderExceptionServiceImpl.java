package cn.iocoder.yudao.module.icbc.service.purchaseorder.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderExceptionPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderExceptionRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderExceptionReviewReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderExceptionSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseExceptionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseOrderItemDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.station.IcbcStationDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.purchaseorder.IcbcPurchaseExceptionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.purchaseorder.IcbcPurchaseOrderItemMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.purchaseorder.IcbcPurchaseOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.station.IcbcStationMapper;
import cn.iocoder.yudao.module.icbc.enums.PurchaseExceptionStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PurchaseExceptionTypeEnum;
import cn.iocoder.yudao.module.icbc.service.purchaseorder.PurchaseOrderExceptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 履约异常授权单 Service 实现（#47 T09）。
 *
 * <p>提交与审核都在这里；交货门禁（{@code PurchaseOrderServiceImpl#checkDelivery}）只读这张表，
 * 两边共用同一个 Mapper，避免出现「提交一套、校验另一套」。
 */
@Service("icbcPurchaseOrderExceptionServiceImpl")
@Validated
@Slf4j
public class PurchaseOrderExceptionServiceImpl implements PurchaseOrderExceptionService {

    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private IcbcPurchaseExceptionMapper exceptionMapper;
    @Resource
    private IcbcPurchaseOrderMapper orderMapper;
    @Resource
    private IcbcPurchaseOrderItemMapper itemMapper;
    @Resource
    private IcbcStationMapper stationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long requestException(PurchaseOrderExceptionSaveReqVO reqVO) {
        IcbcPurchaseOrderDO order = orderMapper.selectById(reqVO.getOrderId());
        if (order == null) {
            throw exception(PURCHASE_ORDER_NOT_EXISTS);
        }
        PurchaseExceptionTypeEnum type = PurchaseExceptionTypeEnum.ofCode(reqVO.getExceptionType())
                .orElseThrow(() -> exception(PURCHASE_EXCEPTION_TYPE_INVALID, reqVO.getExceptionType()));
        if (StrUtil.isBlank(reqVO.getReason())) {
            throw exception(PURCHASE_EXCEPTION_REASON_REQUIRED);
        }
        if (reqVO.getRequestedQuantity() == null || reqVO.getRequestedQuantity().signum() <= 0) {
            throw exception(PURCHASE_EXCEPTION_QUANTITY_INVALID);
        }

        Long itemId = reqVO.getItemId();
        String categoryName = null;
        Long stationId = reqVO.getStationId();
        String stationName = null;
        if (type == PurchaseExceptionTypeEnum.OVER_QUANTITY) {
            // 超量必须指明明细：授权的是「这条明细可以多收多少」，不指明细就没法收紧
            if (itemId == null) {
                throw exception(PURCHASE_EXCEPTION_ITEM_REQUIRED);
            }
            IcbcPurchaseOrderItemDO item = itemMapper.selectById(itemId);
            if (item == null || !Objects.equals(item.getOrderId(), order.getId())) {
                throw exception(PURCHASE_ORDER_ITEM_NOT_EXISTS, itemId);
            }
            categoryName = item.getCategoryName();
        } else if (type == PurchaseExceptionTypeEnum.CROSS_STATION) {
            // 跨场站授权必须指明允许交货的场站，否则等于放开所有场站
            if (stationId == null) {
                throw exception(PURCHASE_EXCEPTION_STATION_REQUIRED);
            }
            IcbcStationDO station = stationMapper.selectById(stationId);
            if (station == null) {
                throw exception(PURCHASE_ORDER_STATION_NOT_EXISTS);
            }
            stationName = station.getName();
            itemId = null;
            categoryName = null;
        } else {
            // 过期是订单级异常：不挂在某条明细上，也不指向某个场站
            itemId = null;
            categoryName = null;
            stationId = null;
        }

        IcbcPurchaseExceptionDO pending = exceptionMapper.selectPending(order.getId(), type.getCode(), itemId);
        if (pending != null) {
            throw exception(PURCHASE_EXCEPTION_ALREADY_PENDING, pending.getExceptionNo());
        }

        IcbcPurchaseExceptionDO exceptionDO = IcbcPurchaseExceptionDO.builder()
                .exceptionNo(generateExceptionNo())
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .itemId(itemId)
                .categoryName(categoryName)
                .exceptionType(type.getCode())
                .stationId(stationId)
                .stationName(stationName)
                .requestedQuantity(reqVO.getRequestedQuantity())
                .validUntil(reqVO.getValidUntil())
                .reason(StrUtil.trim(reqVO.getReason()))
                .status(PurchaseExceptionStatusEnum.PENDING.getStatus())
                .requestedBy(SecurityFrameworkUtils.getLoginUserId())
                .requestedTime(LocalDateTime.now())
                .build();
        exceptionMapper.insert(exceptionDO);
        log.info("履约异常授权申请已提交 - orderNo: {}, type: {}, exceptionNo: {}",
                order.getOrderNo(), type.getCode(), exceptionDO.getExceptionNo());
        return exceptionDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewException(PurchaseOrderExceptionReviewReqVO reqVO) {
        IcbcPurchaseExceptionDO exceptionDO = getExceptionDO(reqVO.getId());
        if (!PurchaseExceptionStatusEnum.PENDING.getStatus().equals(exceptionDO.getStatus())) {
            throw exception(PURCHASE_EXCEPTION_STATUS_NOT_ALLOW,
                    PurchaseExceptionStatusEnum.nameOf(exceptionDO.getStatus()));
        }
        exceptionDO.setReviewedBy(SecurityFrameworkUtils.getLoginUserId());
        exceptionDO.setReviewedTime(LocalDateTime.now());
        exceptionDO.setReviewRemark(StrUtil.trim(reqVO.getReviewRemark()));
        if (reqVO.getValidUntil() != null) {
            exceptionDO.setValidUntil(reqVO.getValidUntil());
        }
        if (Boolean.TRUE.equals(reqVO.getApproved())) {
            exceptionDO.setStatus(PurchaseExceptionStatusEnum.APPROVED.getStatus());
            if (PurchaseExceptionTypeEnum.OVER_QUANTITY.getCode().equals(exceptionDO.getExceptionType())) {
                BigDecimal approved = reqVO.getApprovedQuantity() != null
                        ? reqVO.getApprovedQuantity() : exceptionDO.getRequestedQuantity();
                if (approved == null || approved.signum() < 0) {
                    throw exception(PURCHASE_EXCEPTION_APPROVED_QUANTITY_INVALID);
                }
                exceptionDO.setApprovedQuantity(approved);
            }
        } else {
            exceptionDO.setStatus(PurchaseExceptionStatusEnum.REJECTED.getStatus());
            exceptionDO.setApprovedQuantity(null);
        }
        exceptionMapper.updateById(exceptionDO);
        log.info("履约异常授权单已审核 - exceptionNo: {}, status: {}",
                exceptionDO.getExceptionNo(), PurchaseExceptionStatusEnum.nameOf(exceptionDO.getStatus()));
    }

    @Override
    public PageResult<PurchaseOrderExceptionRespVO> getExceptionPage(PurchaseOrderExceptionPageReqVO reqVO) {
        PageResult<IcbcPurchaseExceptionDO> page = exceptionMapper.selectPage(reqVO);
        return new PageResult<>(page.getList().stream().map(this::toResp).toList(), page.getTotal());
    }

    @Override
    public PurchaseOrderExceptionRespVO getException(Long id) {
        return toResp(getExceptionDO(id));
    }

    private IcbcPurchaseExceptionDO getExceptionDO(Long id) {
        IcbcPurchaseExceptionDO exceptionDO = id == null ? null : exceptionMapper.selectById(id);
        if (exceptionDO == null) {
            throw exception(PURCHASE_EXCEPTION_NOT_EXISTS);
        }
        return exceptionDO;
    }

    private PurchaseOrderExceptionRespVO toResp(IcbcPurchaseExceptionDO exceptionDO) {
        PurchaseOrderExceptionRespVO resp = BeanUtils.toBean(exceptionDO, PurchaseOrderExceptionRespVO.class);
        PurchaseExceptionTypeEnum type = PurchaseExceptionTypeEnum.ofCode(exceptionDO.getExceptionType())
                .orElse(null);
        resp.setExceptionTypeName(type == null ? null : type.getName());
        resp.setExceptionTypeDefinition(type == null ? null : type.getDefinition());
        resp.setStatusName(PurchaseExceptionStatusEnum.nameOf(exceptionDO.getStatus()));
        boolean effective = PurchaseExceptionStatusEnum.APPROVED.getStatus().equals(exceptionDO.getStatus())
                && (exceptionDO.getValidUntil() == null || !exceptionDO.getValidUntil().isBefore(LocalDate.now()));
        resp.setEffective(effective);
        resp.setScopeNote(buildScopeNote(exceptionDO, type, effective));
        return resp;
    }

    private String buildScopeNote(IcbcPurchaseExceptionDO exceptionDO, PurchaseExceptionTypeEnum type,
                                  boolean effective) {
        String prefix;
        if (effective) {
            prefix = "授权生效中：";
        } else if (PurchaseExceptionStatusEnum.APPROVED.getStatus().equals(exceptionDO.getStatus())) {
            prefix = "授权已过有效期：";
        } else {
            prefix = "授权未生效：";
        }
        String scope;
        if (type == PurchaseExceptionTypeEnum.OVER_QUANTITY) {
            if (exceptionDO.getApprovedQuantity() == null) {
                scope = "申请在计划量之外追加收货（追加量由审核人给出）";
            } else {
                scope = "允许「" + exceptionDO.getCategoryName() + "」这条明细在计划量之外多收 "
                        + plain(exceptionDO.getApprovedQuantity())
                        + "（超出这个追加量的部分仍被拦）";
            }
        } else if (type == PurchaseExceptionTypeEnum.CROSS_STATION) {
            scope = "允许在「" + exceptionDO.getStationName() + "」交货（其他场站仍按订单执行场站校验）";
        } else {
            scope = "允许在订单结束日期之后继续交货"
                    + (exceptionDO.getValidUntil() == null ? "（不设有效期）" : "（至 " + exceptionDO.getValidUntil() + "）");
        }
        return prefix + scope + "。授权只放宽这一件事，不改订单状态、不改已发生的业务。";
    }

    private String generateExceptionNo() {
        return "PE" + LocalDateTime.now().format(NO_FORMATTER) + RandomUtil.randomNumbers(4);
    }

    /**
     * 面向人的数字：去掉没意义的尾零（10.0000 → 10）。
     */
    private String plain(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

}

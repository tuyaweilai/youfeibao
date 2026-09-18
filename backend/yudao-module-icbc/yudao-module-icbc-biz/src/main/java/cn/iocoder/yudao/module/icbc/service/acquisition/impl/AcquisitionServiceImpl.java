package cn.iocoder.yudao.module.icbc.service.acquisition.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.acquisition.vo.*;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.goodscfg.IcbcGoodsConfigDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.goodscfg.IcbcGoodsConfigMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.AcquisitionStatusEnum;
import cn.iocoder.yudao.module.icbc.service.acquisition.AcquisitionService;
import cn.iocoder.yudao.module.icbc.service.acquisition.recognition.AcquisitionRecognitionPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 收购登记 Service 实现。
 *
 * <p>登记的产出是一张 {@code icbc_acquisition}：它同时是合同流（本单即该笔确认书）、
 * 货物流（磅单 + 车辆照片）与信息流（时间 / 地点 / 出售者 / 产品 / 数量 / 价格）的骨架。
 * 资金流与发票流由后续付款 / 开票环节补齐。
 */
@Slf4j
@Service
public class AcquisitionServiceImpl implements AcquisitionService {

    private static final DateTimeFormatter ACQ_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SOURCE_ONLINE = "ONLINE";

    @Resource
    private IcbcAcquisitionMapper acquisitionMapper;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private IcbcGoodsConfigMapper goodsConfigMapper;
    @Resource
    private AcquisitionRecognitionPort recognitionPort;

    // ==================== 登记 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAcquisition(@Valid AcquisitionCreateReqVO reqVO) {
        // 1. 幂等：同一 clientRequestId 重复登记只落一条（离线补传的核心保证）
        if (StrUtil.isNotBlank(reqVO.getClientRequestId())) {
            IcbcAcquisitionDO existing = acquisitionMapper.selectByClientRequestId(reqVO.getClientRequestId());
            if (existing != null) {
                return existing.getId();
            }
        }

        // 2. 必须要件校验：缺哪样说哪样，不做一个笼统的「参数错误」
        BigDecimal amount = resolveAmount(reqVO);
        assertRequiredElementsPresent(reqVO, amount);

        // 3. 出售者与品类必须存在
        PayeeInfoDO payee = payeeInfoMapper.selectById(reqVO.getPayeeId());
        if (payee == null) {
            throw exception(ACQUISITION_SELLER_NOT_EXISTS);
        }
        IcbcGoodsConfigDO config = goodsConfigMapper.selectById(reqVO.getGoodsConfigId());
        if (config == null) {
            throw exception(ACQUISITION_GOODS_CONFIG_NOT_EXISTS);
        }

        // 4. 现场识别回填（人工已填的值优先，识别只在空缺处补）
        fillByRecognition(reqVO);

        // 5. 组装并落库
        IcbcAcquisitionDO acquisition = buildAcquisition(reqVO, payee, config, amount);
        try {
            acquisitionMapper.insert(acquisition);
        } catch (DuplicateKeyException e) {
            // 并发下同一 clientRequestId 可能在预检之后才落库，靠唯一约束兜底；
            // 命中则视为同一笔，返回既有单据。
            IcbcAcquisitionDO existing = StrUtil.isNotBlank(reqVO.getClientRequestId())
                    ? acquisitionMapper.selectByClientRequestId(reqVO.getClientRequestId()) : null;
            if (existing != null) {
                return existing.getId();
            }
            throw e;
        }
        log.info("收购登记成功 - acquisitionNo: {}, payeeId: {}, plate: {}",
                acquisition.getAcquisitionNo(), payee.getId(), acquisition.getVehiclePlateNo());
        return acquisition.getId();
    }

    private BigDecimal resolveAmount(AcquisitionCreateReqVO reqVO) {
        if (reqVO.getAmount() != null) {
            return reqVO.getAmount();
        }
        if (reqVO.getUnitPrice() != null && reqVO.getQuantity() != null) {
            return reqVO.getUnitPrice().multiply(reqVO.getQuantity()).setScale(2, RoundingMode.HALF_UP);
        }
        return null;
    }

    /**
     * 缺关键要件时列出缺了什么。要件清单来自 issue #7：磅单、品类、数量、金额、出售者。
     */
    private void assertRequiredElementsPresent(AcquisitionCreateReqVO reqVO, BigDecimal amount) {
        List<String> missing = new ArrayList<>();
        if (reqVO.getPayeeId() == null) {
            missing.add("出售者");
        }
        if (reqVO.getGoodsConfigId() == null) {
            missing.add("品类");
        }
        if (reqVO.getQuantity() == null || reqVO.getQuantity().signum() <= 0) {
            missing.add("数量");
        }
        if (amount == null || amount.signum() <= 0) {
            missing.add("金额");
        }
        if (StrUtil.isBlank(reqVO.getWeightTicketNo()) && StrUtil.isBlank(reqVO.getWeightTicketImageUrl())) {
            missing.add("磅单");
        }
        if (!missing.isEmpty()) {
            throw exception(ACQUISITION_REQUIRED_ELEMENT_MISSING, String.join("、", missing));
        }
    }

    /**
     * 用识别服务补全空缺的磅单字段与车牌。识别失败（空结果）不阻断，仍可人工录入。
     */
    private void fillByRecognition(AcquisitionCreateReqVO reqVO) {
        if (StrUtil.isNotBlank(reqVO.getWeightTicketImageUrl())) {
            AcquisitionRecognitionPort.WeightTicketRecognition recognition =
                    recognitionPort.recognizeWeightTicket(reqVO.getWeightTicketImageUrl());
            if (recognition != null) {
                if (StrUtil.isBlank(reqVO.getWeightTicketNo())) {
                    reqVO.setWeightTicketNo(recognition.getWeightTicketNo());
                }
                if (reqVO.getGrossWeight() == null) {
                    reqVO.setGrossWeight(recognition.getGrossWeight());
                }
                if (reqVO.getTareWeight() == null) {
                    reqVO.setTareWeight(recognition.getTareWeight());
                }
                if (reqVO.getNetWeight() == null) {
                    reqVO.setNetWeight(recognition.getNetWeight());
                }
                if (StrUtil.isBlank(reqVO.getWeightTicketPlateNo())) {
                    reqVO.setWeightTicketPlateNo(recognition.getPlateNo());
                }
            }
        }
        String vehicleImageUrl = StrUtil.isNotBlank(reqVO.getVehicleFrontImageUrl())
                ? reqVO.getVehicleFrontImageUrl() : reqVO.getVehicleRearImageUrl();
        if (StrUtil.isNotBlank(vehicleImageUrl) && StrUtil.isBlank(reqVO.getVehiclePlateNo())) {
            AcquisitionRecognitionPort.PlateRecognition recognition =
                    recognitionPort.recognizePlate(vehicleImageUrl);
            if (recognition != null) {
                reqVO.setVehiclePlateNo(recognition.getPlateNo());
            }
        }
    }

    private IcbcAcquisitionDO buildAcquisition(AcquisitionCreateReqVO reqVO, PayeeInfoDO payee,
                                               IcbcGoodsConfigDO config, BigDecimal amount) {
        IcbcAcquisitionDO acquisition = BeanUtils.toBean(reqVO, IcbcAcquisitionDO.class);
        acquisition.setId(null);
        acquisition.setAcquisitionNo(generateAcquisitionNo());
        acquisition.setAmount(amount);
        acquisition.setPartnerPayeeId(payee.getPartnerPayeeId());
        acquisition.setSellerName(payee.getName());
        acquisition.setSellerMobile(payee.getMobile());
        acquisition.setCategoryName(config.getName());
        acquisition.setUnit(config.getUnit());
        acquisition.setTaxRate(config.getTaxRate());
        acquisition.setTaxMethod(config.getTaxMethod());
        acquisition.setMergedCode(config.getMergedCode());
        acquisition.setNetWeight(resolveNetWeight(acquisition));
        acquisition.setStatus(AcquisitionStatusEnum.REGISTERED.getStatus());
        acquisition.setPlateMatched(comparePlate(acquisition.getWeightTicketPlateNo(), acquisition.getVehiclePlateNo()));
        acquisition.setSource(StrUtil.blankToDefault(reqVO.getSource(), SOURCE_ONLINE));
        if (acquisition.getTradeTime() == null) {
            acquisition.setTradeTime(LocalDateTime.now());
        }
        return acquisition;
    }

    private BigDecimal resolveNetWeight(IcbcAcquisitionDO acquisition) {
        BigDecimal gross = acquisition.getGrossWeight();
        BigDecimal tare = acquisition.getTareWeight();
        if (gross != null && tare != null) {
            if (gross.compareTo(tare) < 0) {
                throw exception(ACQUISITION_WEIGHT_INVALID);
            }
            if (acquisition.getNetWeight() == null) {
                return gross.subtract(tare);
            }
        }
        return acquisition.getNetWeight();
    }

    private String generateAcquisitionNo() {
        return "ACQ" + LocalDateTime.now().format(ACQ_NO_FORMATTER) + RandomUtil.randomNumbers(4);
    }

    /**
     * 车牌比对：去空格、转大写后逐字比较。任一侧为空时返回 null，表示「无法比对」，
     * 与「比对不一致」是两件事。
     */
    static Boolean comparePlate(String weightTicketPlateNo, String vehiclePlateNo) {
        if (StrUtil.isBlank(weightTicketPlateNo) || StrUtil.isBlank(vehiclePlateNo)) {
            return null;
        }
        return normalizePlate(weightTicketPlateNo).equals(normalizePlate(vehiclePlateNo));
    }

    private static String normalizePlate(String plateNo) {
        return plateNo.replaceAll("\\s", "").toUpperCase();
    }

    // ==================== 离线补传 ====================

    @Override
    public List<AcquisitionSyncResultVO> syncOffline(@Valid AcquisitionOfflineSyncReqVO reqVO) {
        List<AcquisitionSyncResultVO> results = new ArrayList<>();
        for (AcquisitionCreateReqVO item : reqVO.getItems()) {
            results.add(syncOne(item));
        }
        return results;
    }

    private AcquisitionSyncResultVO syncOne(AcquisitionCreateReqVO item) {
        AcquisitionSyncResultVO result = new AcquisitionSyncResultVO();
        result.setClientRequestId(item.getClientRequestId());
        try {
            boolean duplicated = StrUtil.isNotBlank(item.getClientRequestId())
                    && acquisitionMapper.selectByClientRequestId(item.getClientRequestId()) != null;
            Long id = createAcquisition(item);
            IcbcAcquisitionDO acquisition = acquisitionMapper.selectById(id);
            result.setId(id);
            result.setAcquisitionNo(acquisition != null ? acquisition.getAcquisitionNo() : null);
            result.setDuplicated(duplicated);
            result.setSuccess(true);
        } catch (ServiceException e) {
            result.setSuccess(false);
            result.setErrorMsg(e.getMessage());
        }
        return result;
    }

    // ==================== 识别结果人工修正 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void correctRecognition(@Valid AcquisitionCorrectionReqVO reqVO) {
        IcbcAcquisitionDO acquisition = getAcquisition(reqVO.getId());
        if (Objects.equals(acquisition.getStatus(), AcquisitionStatusEnum.CANCELLED.getStatus())) {
            throw exception(ACQUISITION_STATUS_NOT_ALLOW_UPDATE);
        }
        if (reqVO.getGrossWeight() != null) {
            acquisition.setGrossWeight(reqVO.getGrossWeight());
        }
        if (reqVO.getTareWeight() != null) {
            acquisition.setTareWeight(reqVO.getTareWeight());
        }
        if (reqVO.getNetWeight() != null) {
            acquisition.setNetWeight(reqVO.getNetWeight());
        } else if (reqVO.getGrossWeight() != null || reqVO.getTareWeight() != null) {
            // 毛重或皮重被改了：净重若没显式给，按新值重算
            acquisition.setNetWeight(null);
            acquisition.setNetWeight(resolveNetWeight(acquisition));
        }
        if (StrUtil.isNotBlank(reqVO.getWeightTicketNo())) {
            acquisition.setWeightTicketNo(reqVO.getWeightTicketNo());
        }
        if (StrUtil.isNotBlank(reqVO.getWeightTicketPlateNo())) {
            acquisition.setWeightTicketPlateNo(reqVO.getWeightTicketPlateNo());
        }
        if (StrUtil.isNotBlank(reqVO.getVehiclePlateNo())) {
            acquisition.setVehiclePlateNo(reqVO.getVehiclePlateNo());
        }
        if (StrUtil.isNotBlank(reqVO.getRemark())) {
            acquisition.setRemark(reqVO.getRemark());
        }
        acquisition.setPlateMatched(comparePlate(
                acquisition.getWeightTicketPlateNo(), acquisition.getVehiclePlateNo()));
        acquisitionMapper.updateById(acquisition);
    }

    // ==================== 查询 ====================

    @Override
    public IcbcAcquisitionDO getAcquisition(Long id) {
        IcbcAcquisitionDO acquisition = acquisitionMapper.selectById(id);
        if (acquisition == null) {
            throw exception(ACQUISITION_NOT_EXISTS);
        }
        return acquisition;
    }

    @Override
    public PageResult<IcbcAcquisitionDO> getAcquisitionPage(AcquisitionPageReqVO reqVO) {
        return acquisitionMapper.selectPage(reqVO);
    }

    @Override
    public List<IcbcAcquisitionDO> getAcquisitionsByPayeeId(Long payeeId) {
        return acquisitionMapper.selectListByPayeeId(payeeId);
    }

    // ==================== 与开票 / 付款链路的关联 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void linkInvoice(Long acquisitionId, String partnerOrderId) {
        IcbcAcquisitionDO acquisition = getAcquisition(acquisitionId);
        acquisition.setInvoicePartnerOrderId(partnerOrderId);
        acquisition.setStatus(AcquisitionStatusEnum.PENDING_PAYMENT.getStatus());
        acquisitionMapper.updateById(acquisition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markPaidByInvoicePartnerOrderId(String partnerOrderId) {
        updateStatusByInvoice(partnerOrderId, AcquisitionStatusEnum.PAID);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markInvoicedByInvoicePartnerOrderId(String partnerOrderId) {
        updateStatusByInvoice(partnerOrderId, AcquisitionStatusEnum.INVOICED);
    }

    private void updateStatusByInvoice(String partnerOrderId, AcquisitionStatusEnum status) {
        List<IcbcAcquisitionDO> acquisitions = acquisitionMapper.selectListByInvoicePartnerOrderIds(
                Collections.singletonList(partnerOrderId));
        if (acquisitions.isEmpty()) {
            log.warn("按合作方订单号回写收购单状态时未找到收购单 - partnerOrderId: {}", partnerOrderId);
            return;
        }
        for (IcbcAcquisitionDO acquisition : acquisitions) {
            IcbcAcquisitionDO update = new IcbcAcquisitionDO();
            update.setId(acquisition.getId());
            update.setStatus(status.getStatus());
            acquisitionMapper.updateById(update);
        }
    }

    // ==================== 确认书导出 ====================

    @Override
    public void exportConfirmation(Long id, HttpServletResponse response) {
        IcbcAcquisitionDO acquisition = getAcquisition(id);
        AcquisitionConfirmationRespVO confirmation = BeanUtils.toBean(acquisition,
                AcquisitionConfirmationRespVO.class);
        confirmation.setProductName(acquisition.getCategoryName());
        confirmation.setTradeTime(acquisition.getTradeTime() != null
                ? acquisition.getTradeTime().format(TIME_FORMATTER) : null);
        try {
            ExcelUtils.write(response, "收购确认书_" + acquisition.getAcquisitionNo() + ".xls",
                    "收购确认书", AcquisitionConfirmationRespVO.class,
                    Collections.singletonList(confirmation));
        } catch (IOException e) {
            log.error("导出收购确认书失败 - id: {}", id, e);
            throw exception(ACQUISITION_CONFIRMATION_EXPORT_FAILED);
        }
    }

}

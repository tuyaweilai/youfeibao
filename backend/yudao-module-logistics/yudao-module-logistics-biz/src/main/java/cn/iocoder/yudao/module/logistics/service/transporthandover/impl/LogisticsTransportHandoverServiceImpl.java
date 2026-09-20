package cn.iocoder.yudao.module.logistics.service.transporthandover.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.logistics.controller.admin.transporthandover.vo.LogisticsTransportHandoverCreateReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporthandover.vo.LogisticsTransportHandoverPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporthandover.vo.LogisticsTransportHandoverRespVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportStopDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporthandover.LogisticsTransportHandoverDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.transporthandover.LogisticsTransportHandoverMapper;
import cn.iocoder.yudao.module.logistics.enums.LogisticsHandoverDocumentStatusEnum;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportTaskStatusEnum;
import cn.iocoder.yudao.module.logistics.service.transportnode.TransportNodeConverter;
import cn.iocoder.yudao.module.logistics.service.transportstop.LogisticsTransportStopService;
import cn.iocoder.yudao.module.logistics.service.transporttask.LogisticsTransportTaskService;
import cn.iocoder.yudao.module.logistics.service.transporthandover.LogisticsTransportHandoverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.logistics.enums.ErrorCodeConstants.*;

/**
 * 交接登记 Service 实现（V6 #73）。
 *
 * <p>**现场不产生金额**：本类里不会有金额计算，也不会调用任何收购 / 结算服务——收购单在回场复磅后
 * 由 icbc 侧生成（ADR 0031 的两端分工）。参考量 / 参考单价只是现场约定值。
 *
 * <p>物流不引用 icbc 的类（ADR 0032）：出售者与品类都是 icbc 侧编号 + 快照，本类不知道它们是什么。
 */
@Service
@Validated
@Slf4j
public class LogisticsTransportHandoverServiceImpl implements LogisticsTransportHandoverService {

    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private LogisticsTransportHandoverMapper logisticsTransportHandoverMapper;
    @Resource
    private LogisticsTransportTaskService logisticsTransportTaskService;
    @Resource
    private LogisticsTransportStopService logisticsTransportStopService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHandover(LogisticsTransportHandoverCreateReqVO reqVO) {
        // 1. 幂等：弱网下同一条重复提交只落一条（与节点上报同一手法）
        if (StrUtil.isNotBlank(reqVO.getClientRequestId())) {
            LogisticsTransportHandoverDO existing =
                    logisticsTransportHandoverMapper.selectByClientRequestId(reqVO.getClientRequestId());
            if (existing != null) {
                return existing.getId();
            }
        }

        // 2. 任务必须真的跑过：待分配（没车没人）与已取消（这趟没跑）都不是登记对象
        LogisticsTransportTaskDO task = logisticsTransportTaskService.getTask(reqVO.getTaskId());
        LogisticsTransportTaskStatusEnum taskStatus = LogisticsTransportTaskStatusEnum.ofStatus(task.getStatus())
                .orElse(LogisticsTransportTaskStatusEnum.PENDING);
        if (taskStatus == LogisticsTransportTaskStatusEnum.PENDING
                || taskStatus == LogisticsTransportTaskStatusEnum.CANCELLED) {
            throw exception(TRANSPORT_HANDOVER_TASK_NOT_REGISTRABLE);
        }

        // 3. 停靠点：任务是集货时**必须**说清是哪一家，否则几家会被混成一次交接
        LogisticsTransportStopDO stop = resolveStop(task, reqVO);

        // 4. 出售者与品类：两者都是 icbc 侧编号（ADR 0032），这里只校验「有没有」
        Long payeeId = reqVO.getPayeeId() != null ? reqVO.getPayeeId()
                : (stop != null ? stop.getPayeeId() : null);
        if (payeeId == null) {
            throw exception(TRANSPORT_HANDOVER_PAYEE_REQUIRED);
        }
        assertHandoverValid(reqVO);

        // 5. 同一个停靠点只登记一次：重复登记是操作错误，不是新事实
        if (stop != null && logisticsTransportHandoverMapper.selectCountByStopId(stop.getId()) > 0) {
            throw exception(TRANSPORT_HANDOVER_STOP_ALREADY_REGISTERED);
        }

        LogisticsTransportHandoverDO handover = LogisticsTransportHandoverDO.builder()
                .handoverNo(generateHandoverNo())
                .taskId(task.getId())
                .taskNo(task.getTaskNo())
                .stopId(stop == null ? null : stop.getId())
                .address(StrUtil.blankToDefault(StrUtil.trim(reqVO.getAddress()),
                        stop == null ? null : stop.getAddress()))
                .payeeId(payeeId)
                .payeeName(StrUtil.blankToDefault(reqVO.getPayeeName(),
                        stop == null ? null : stop.getPayeeName()))
                .payeeMobile(StrUtil.blankToDefault(reqVO.getPayeeMobile(),
                        stop == null ? null : stop.getPayeeMobile()))
                .goodsConfigId(reqVO.getGoodsConfigId())
                .categoryName(StrUtil.trim(reqVO.getCategoryName()))
                .unit(StrUtil.trim(reqVO.getUnit()))
                .referenceQuantity(reqVO.getReferenceQuantity())
                .referenceUnitPrice(reqVO.getReferenceUnitPrice())
                .photos(toPhotosJson(reqVO))
                // 司机与车辆取任务上的引用 + 快照：档案改名或删档都不影响历史交接与一票一档
                .driverId(task.getDriverId())
                .driverName(task.getDriverName())
                .driverMobile(task.getDriverMobile())
                .vehicleId(task.getVehicleId())
                .plateNo(task.getPlateNo())
                .occurTime(reqVO.getOccurTime() == null ? LocalDateTime.now() : reqVO.getOccurTime())
                .documentStatus(resolveDocumentStatus(reqVO).getStatus())
                .documentGap(StrUtil.trim(reqVO.getDocumentGap()))
                .clientRequestId(StrUtil.trim(reqVO.getClientRequestId()))
                .remark(reqVO.getRemark())
                .build();
        try {
            logisticsTransportHandoverMapper.insert(handover);
        } catch (DuplicateKeyException e) {
            // 并发下同一 clientRequestId 可能在预检之后才落库，靠唯一键兜底；命中即视为同一条
            LogisticsTransportHandoverDO existing = StrUtil.isNotBlank(reqVO.getClientRequestId())
                    ? logisticsTransportHandoverMapper.selectByClientRequestId(reqVO.getClientRequestId()) : null;
            if (existing != null) {
                return existing.getId();
            }
            throw e;
        }
        log.info("交接登记成功 - handoverNo: {}, taskNo: {}, payeeId: {}, plate: {}, documentStatus: {}",
                handover.getHandoverNo(), handover.getTaskNo(), handover.getPayeeId(),
                handover.getPlateNo(), handover.getDocumentStatus());
        return handover.getId();
    }

    @Override
    public LogisticsTransportHandoverDO getHandover(Long id) {
        LogisticsTransportHandoverDO handover = id == null ? null : logisticsTransportHandoverMapper.selectById(id);
        if (handover == null) {
            throw exception(TRANSPORT_HANDOVER_NOT_EXISTS);
        }
        return handover;
    }

    @Override
    public LogisticsTransportHandoverDO getHandoverByStopId(Long stopId) {
        return stopId == null ? null : logisticsTransportHandoverMapper.selectByStopId(stopId);
    }

    @Override
    public List<LogisticsTransportHandoverDO> getHandoverListByTaskId(Long taskId) {
        return taskId == null ? List.of() : logisticsTransportHandoverMapper.selectListByTaskId(taskId);
    }

    @Override
    public PageResult<LogisticsTransportHandoverDO> getPage(LogisticsTransportHandoverPageReqVO reqVO) {
        return logisticsTransportHandoverMapper.selectPage(reqVO);
    }

    @Override
    public LogisticsTransportHandoverRespVO toResp(LogisticsTransportHandoverDO handover) {
        LogisticsTransportHandoverRespVO resp = BeanUtils.toBean(handover, LogisticsTransportHandoverRespVO.class);
        resp.setPhotos(TransportNodeConverter.fromPhotosJson(handover.getPhotos()));
        resp.setDocumentStatusName(LogisticsHandoverDocumentStatusEnum.nameOf(handover.getDocumentStatus()));
        return resp;
    }

    // ==================== 内部方法 ====================

    private LogisticsTransportStopDO resolveStop(LogisticsTransportTaskDO task,
                                                 LogisticsTransportHandoverCreateReqVO reqVO) {
        if (reqVO.getStopId() != null) {
            LogisticsTransportStopDO stop = logisticsTransportStopService.getStop(reqVO.getStopId());
            if (!Objects.equals(stop.getTaskId(), task.getId())) {
                throw exception(TRANSPORT_HANDOVER_STOP_NOT_BELONG_TO_TASK);
            }
            return stop;
        }
        // 没有停靠点的历史任务（单点口径）允许不传；有停靠点就必须指明是哪一家
        if (!logisticsTransportStopService.getStopListByTaskId(task.getId()).isEmpty()) {
            throw exception(TRANSPORT_HANDOVER_STOP_REQUIRED);
        }
        return null;
    }

    /**
     * 现场事实的合法性：参考量要大于 0、参考单价不能为负、要有凭证照片、待补档要说明缺什么。
     */
    private void assertHandoverValid(LogisticsTransportHandoverCreateReqVO reqVO) {
        if (reqVO.getReferenceQuantity() == null || reqVO.getReferenceQuantity().signum() <= 0) {
            throw exception(TRANSPORT_HANDOVER_QUANTITY_INVALID);
        }
        if (reqVO.getReferenceUnitPrice() != null
                && reqVO.getReferenceUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw exception(TRANSPORT_HANDOVER_QUANTITY_INVALID);
        }
        if (CollUtil.isEmpty(reqVO.getPhotos())) {
            throw exception(TRANSPORT_HANDOVER_PHOTO_REQUIRED);
        }
        LogisticsHandoverDocumentStatusEnum status = resolveDocumentStatus(reqVO);
        if (status == LogisticsHandoverDocumentStatusEnum.PENDING && StrUtil.isBlank(reqVO.getDocumentGap())) {
            throw exception(TRANSPORT_HANDOVER_DOCUMENT_GAP_REQUIRED);
        }
    }

    /**
     * 要件状态：不填即「已齐」；写了就必须是登记过的状态，避免拼错变成第三种状态。
     */
    private LogisticsHandoverDocumentStatusEnum resolveDocumentStatus(
            LogisticsTransportHandoverCreateReqVO reqVO) {
        if (StrUtil.isBlank(reqVO.getDocumentStatus())) {
            return LogisticsHandoverDocumentStatusEnum.COMPLETE;
        }
        return LogisticsHandoverDocumentStatusEnum.ofStatus(reqVO.getDocumentStatus())
                .orElseThrow(() -> exception(TRANSPORT_HANDOVER_DOCUMENT_STATUS_UNKNOWN));
    }

    private String toPhotosJson(LogisticsTransportHandoverCreateReqVO reqVO) {
        return CollUtil.isEmpty(reqVO.getPhotos()) ? null : JsonUtils.toJsonString(reqVO.getPhotos());
    }

    private String generateHandoverNo() {
        return "HO" + LocalDateTime.now().format(NO_FORMATTER) + RandomUtil.randomNumbers(4);
    }

}

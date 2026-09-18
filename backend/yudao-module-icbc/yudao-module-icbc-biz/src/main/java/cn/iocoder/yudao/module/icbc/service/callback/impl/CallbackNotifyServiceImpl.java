package cn.iocoder.yudao.module.icbc.service.callback.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.icbc.controller.admin.callback.vo.CallbackNotifyPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.callback.CallbackNotifyDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.callback.CallbackNotifyMapper;
import cn.iocoder.yudao.module.icbc.enums.CallbackNotifyTypeEnum;
import cn.iocoder.yudao.module.icbc.enums.CallbackProcessStatusEnum;
import cn.iocoder.yudao.module.icbc.service.callback.CallbackNotifyService;
import cn.iocoder.yudao.module.icbc.service.callback.IcbcNotifyContext;
import cn.iocoder.yudao.module.icbc.service.callback.IcbcNotifyHandler;
import cn.iocoder.yudao.module.icbc.service.callback.IcbcNotifyMessage;
import cn.iocoder.yudao.module.icbc.service.callback.IcbcNotifyParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.CALLBACK_NOTIFY_NOT_EXISTS;

/**
 * 工行回调通知 Service 实现类
 *
 * 处理顺序固定为：解析 → 落表（PENDING）→ 分发处理 → 回写状态。
 * 落表与处理分离，保证「通知先于平台数据到达」也能成立：
 * 处理器可以查不到业务数据而失败，通知记录仍在，待数据落库后可重放。
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class CallbackNotifyServiceImpl implements CallbackNotifyService {

    @Resource
    private CallbackNotifyMapper callbackNotifyMapper;

    @Resource
    private IcbcNotifyParser notifyParser;

    /**
     * 九类通知的处理器，按类型索引
     */
    @Autowired(required = false)
    private List<IcbcNotifyHandler> notifyHandlers;

    private final Map<CallbackNotifyTypeEnum, IcbcNotifyHandler> handlerRegistry =
            new EnumMap<>(CallbackNotifyTypeEnum.class);

    @PostConstruct
    public void initHandlerRegistry() {
        if (notifyHandlers == null) {
            return;
        }
        for (IcbcNotifyHandler handler : notifyHandlers) {
            IcbcNotifyHandler previous = handlerRegistry.put(handler.supportType(), handler);
            if (previous != null) {
                throw new IllegalStateException("重复的通知处理器：" + handler.supportType());
            }
        }
    }

    @Override
    public Long createCallbackNotify(CallbackNotifyCreateReqVO createReqVO) {
        CallbackNotifyDO callbackNotify = CallbackNotifyDO.builder()
                .notifyId(createReqVO.getNotifyId())
                .notifyType(createReqVO.getNotifyType())
                .businessId(createReqVO.getBusinessId())
                .notifyData(createReqVO.getNotifyData())
                .sign(createReqVO.getSign())
                .processStatus(createReqVO.getProcessStatus())
                .processMsg(createReqVO.getProcessMsg())
                .retryCount(createReqVO.getRetryCount() != null ? createReqVO.getRetryCount() : 0)
                .build();
        callbackNotifyMapper.insert(callbackNotify);
        return callbackNotify.getId();
    }

    @Override
    public PageResult<CallbackNotifyDO> getCallbackNotifyPage(CallbackNotifyPageReqVO pageReqVO) {
        return callbackNotifyMapper.selectPage(pageReqVO);
    }

    @Override
    public CallbackNotifyDO getCallbackNotify(Long id) {
        return callbackNotifyMapper.selectById(id);
    }

    @Override
    public CallbackNotifyDO getCallbackNotifyByNotifyId(String notifyId) {
        return callbackNotifyMapper.selectByNotifyId(notifyId);
    }

    @Override
    @TenantIgnore
    public String receive(String body) {
        IcbcNotifyMessage message = notifyParser.parse(body);
        Long id = ingest(message);
        process(id);
        CallbackNotifyDO record = callbackNotifyMapper.selectById(id);
        return record != null && CallbackProcessStatusEnum.SUCCESS.getStatus().equals(record.getProcessStatus())
                ? "SUCCESS" : "FAILURE";
    }

    /**
     * 先落表：同一 notifyId 只保留一条，天然去重
     */
    @TenantIgnore
    public Long ingest(IcbcNotifyMessage message) {
        CallbackNotifyDO existing = callbackNotifyMapper.selectByNotifyId(message.getNotifyId());
        if (existing != null) {
            return existing.getId();
        }
        CallbackNotifyDO record = CallbackNotifyDO.builder()
                .notifyId(message.getNotifyId())
                .notifyType(message.getNotifyType().getType())
                .businessId(message.getBusinessId())
                .notifyData(message.getNotifyData())
                .sign(message.getSign())
                .processStatus(CallbackProcessStatusEnum.PENDING.getStatus())
                .retryCount(0)
                .build();
        try {
            callbackNotifyMapper.insert(record);
            return record.getId();
        } catch (DuplicateKeyException e) {
            // 并发重复到达：唯一索引兜底
            CallbackNotifyDO duplicated = callbackNotifyMapper.selectByNotifyId(message.getNotifyId());
            if (duplicated == null) {
                throw e;
            }
            return duplicated.getId();
        }
    }

    /**
     * 再处理：已成功的通知不重复处理
     */
    @TenantIgnore
    public void process(Long id) {
        CallbackNotifyDO record = callbackNotifyMapper.selectById(id);
        if (record == null) {
            throw exception(CALLBACK_NOTIFY_NOT_EXISTS);
        }
        if (CallbackProcessStatusEnum.SUCCESS.getStatus().equals(record.getProcessStatus())) {
            log.info("[process][通知已处理成功，跳过] notifyId={}", record.getNotifyId());
            return;
        }
        CallbackNotifyTypeEnum notifyType = CallbackNotifyTypeEnum.of(record.getNotifyType());
        IcbcNotifyHandler handler = notifyType != null ? handlerRegistry.get(notifyType) : null;
        if (handler == null) {
            markFailure(record, "通知类型处理器未实现：" + record.getNotifyType());
            return;
        }
        try {
            handler.handle(IcbcNotifyContext.builder()
                    .recordId(record.getId())
                    .notifyId(record.getNotifyId())
                    .notifyType(notifyType)
                    .businessId(record.getBusinessId())
                    .notifyData(record.getNotifyData())
                    .build());
            markSuccess(record);
        } catch (Exception e) {
            log.error("[process][通知处理失败] notifyId={}", record.getNotifyId(), e);
            markFailure(record, e.getMessage());
        }
    }

    @Override
    @TenantIgnore
    public void replay(Long id) {
        CallbackNotifyDO record = callbackNotifyMapper.selectById(id);
        if (record == null) {
            throw exception(CALLBACK_NOTIFY_NOT_EXISTS);
        }
        if (CallbackProcessStatusEnum.SUCCESS.getStatus().equals(record.getProcessStatus())) {
            // 已成功的通知不重放，避免重复业务（issue #3 验收）
            log.info("[replay][通知已处理成功，忽略重放] notifyId={}", record.getNotifyId());
            return;
        }
        CallbackNotifyDO update = CallbackNotifyDO.builder()
                .id(id)
                .processStatus(CallbackProcessStatusEnum.PENDING.getStatus())
                .retryCount((record.getRetryCount() == null ? 0 : record.getRetryCount()) + 1)
                .build();
        callbackNotifyMapper.updateById(update);
        process(id);
    }

    @Override
    public List<CallbackNotifyDO> getPendingCallbacks() {
        return callbackNotifyMapper.selectListByProcessStatus(CallbackProcessStatusEnum.PENDING.getStatus());
    }

    private void markSuccess(CallbackNotifyDO record) {
        callbackNotifyMapper.updateById(CallbackNotifyDO.builder()
                .id(record.getId())
                .processStatus(CallbackProcessStatusEnum.SUCCESS.getStatus())
                .processMsg("处理成功")
                .processTime(LocalDateTime.now())
                .build());
    }

    private void markFailure(CallbackNotifyDO record, String message) {
        callbackNotifyMapper.updateById(CallbackNotifyDO.builder()
                .id(record.getId())
                .processStatus(CallbackProcessStatusEnum.FAILURE.getStatus())
                .processMsg(message)
                .processTime(LocalDateTime.now())
                .build());
    }

}

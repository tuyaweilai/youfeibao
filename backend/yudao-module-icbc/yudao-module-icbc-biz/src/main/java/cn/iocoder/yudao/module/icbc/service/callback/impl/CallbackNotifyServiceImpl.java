package cn.iocoder.yudao.module.icbc.service.callback.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.icbc.controller.admin.callback.vo.CallbackNotifyPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.callback.CallbackNotifyDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.callback.CallbackNotifyMapper;
import cn.iocoder.yudao.module.icbc.enums.CallbackNotifyTypeEnum;
import cn.iocoder.yudao.module.icbc.enums.CallbackProcessStatusEnum;
import cn.iocoder.yudao.module.icbc.service.callback.CallbackNotifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工行回调通知 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class CallbackNotifyServiceImpl implements CallbackNotifyService {

    @Resource
    private CallbackNotifyMapper callbackNotifyMapper;

    @Override
    public Long createCallbackNotify(CallbackNotifyCreateReqVO createReqVO) {
        // 插入
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
        // 返回
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
    public String processCallback(String notifyId, String notifyType, String businessId, String notifyData, String sign) {
        try {
            // 检查是否已经处理过
            CallbackNotifyDO existingNotify = getCallbackNotifyByNotifyId(notifyId);
            if (existingNotify != null && CallbackProcessStatusEnum.SUCCESS.getStatus().equals(existingNotify.getProcessStatus())) {
                log.info("回调通知已处理过，notifyId: {}", notifyId);
                return "SUCCESS";
            }

            // 创建或更新回调通知记录
            CallbackNotifyDO callbackNotify;
            if (existingNotify == null) {
                callbackNotify = CallbackNotifyDO.builder()
                        .notifyId(notifyId)
                        .notifyType(notifyType)
                        .businessId(businessId)
                        .notifyData(notifyData)
                        .sign(sign)
                        .processStatus(CallbackProcessStatusEnum.PENDING.getStatus())
                        .retryCount(0)
                        .build();
                callbackNotifyMapper.insert(callbackNotify);
            } else {
                callbackNotify = existingNotify;
            }

            // 根据通知类型处理业务逻辑
            boolean processResult = processBusinessLogic(notifyType, businessId, notifyData);

            // 更新处理结果
            CallbackNotifyDO updateObj = CallbackNotifyDO.builder()
                    .id(callbackNotify.getId())
                    .processStatus(processResult ? CallbackProcessStatusEnum.SUCCESS.getStatus() : CallbackProcessStatusEnum.FAILURE.getStatus())
                    .processMsg(processResult ? "处理成功" : "处理失败")
                    .processTime(LocalDateTime.now())
                    .build();
            callbackNotifyMapper.updateById(updateObj);

            return processResult ? "SUCCESS" : "FAILURE";

        } catch (Exception e) {
            log.error("处理回调通知异常，notifyId: {}", notifyId, e);
            return "FAILURE";
        }
    }

    @Override
    public void retryCallback(Long id) {
        CallbackNotifyDO callbackNotify = getCallbackNotify(id);
        if (callbackNotify == null) {
            log.warn("回调通知不存在，id: {}", id);
            return;
        }

        // 重新处理
        String result = processCallback(callbackNotify.getNotifyId(), callbackNotify.getNotifyType(),
                callbackNotify.getBusinessId(), callbackNotify.getNotifyData(), callbackNotify.getSign());

        // 更新重试次数
        CallbackNotifyDO updateObj = CallbackNotifyDO.builder()
                .id(id)
                .retryCount(callbackNotify.getRetryCount() + 1)
                .build();
        callbackNotifyMapper.updateById(updateObj);

        log.info("重试回调通知完成，id: {}, result: {}", id, result);
    }

    @Override
    public List<CallbackNotifyDO> getPendingCallbacks() {
        return callbackNotifyMapper.selectListByProcessStatus(CallbackProcessStatusEnum.PENDING.getStatus());
    }

    /**
     * 处理业务逻辑
     *
     * @param notifyType 通知类型
     * @param businessId 业务ID
     * @param notifyData 通知数据
     * @return 处理结果
     */
    private boolean processBusinessLogic(String notifyType, String businessId, String notifyData) {
        try {
            CallbackNotifyTypeEnum typeEnum = getNotifyTypeEnum(notifyType);
            if (typeEnum == null) {
                log.warn("未知的通知类型: {}", notifyType);
                return false;
            }

            switch (typeEnum) {
                case PAYEE_AUDIT:
                    return processPayeeAudit(businessId, notifyData);
                case PAYER_AUDIT:
                    return processPayerAudit(businessId, notifyData);
                case INVOICE_STATUS:
                    return processInvoiceStatus(businessId, notifyData);
                case PAYMENT_STATUS:
                    return processPaymentStatus(businessId, notifyData);
                default:
                    log.warn("未处理的通知类型: {}", notifyType);
                    return false;
            }
        } catch (Exception e) {
            log.error("处理业务逻辑异常，notifyType: {}, businessId: {}", notifyType, businessId, e);
            return false;
        }
    }

    private CallbackNotifyTypeEnum getNotifyTypeEnum(String notifyType) {
        for (CallbackNotifyTypeEnum typeEnum : CallbackNotifyTypeEnum.values()) {
            if (typeEnum.getType().equals(notifyType)) {
                return typeEnum;
            }
        }
        return null;
    }

    private boolean processPayeeAudit(String businessId, String notifyData) {
        // TODO: 实现收方审核逻辑
        log.info("处理收方审核通知，businessId: {}, notifyData: {}", businessId, notifyData);
        return true;
    }

    private boolean processPayerAudit(String businessId, String notifyData) {
        // TODO: 实现付方审核逻辑
        log.info("处理付方审核通知，businessId: {}, notifyData: {}", businessId, notifyData);
        return true;
    }

    private boolean processInvoiceStatus(String businessId, String notifyData) {
        // TODO: 实现发票状态更新逻辑
        log.info("处理发票状态通知，businessId: {}, notifyData: {}", businessId, notifyData);
        return true;
    }

    private boolean processPaymentStatus(String businessId, String notifyData) {
        // TODO: 实现支付状态更新逻辑
        log.info("处理支付状态通知，businessId: {}, notifyData: {}", businessId, notifyData);
        return true;
    }

} 
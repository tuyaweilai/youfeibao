package cn.iocoder.yudao.module.icbc.service.callback;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.callback.vo.CallbackNotifyPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.callback.CallbackNotifyDO;

import java.util.List;

/**
 * 工行回调通知 Service 接口
 *
 * @author 芋道源码
 */
public interface CallbackNotifyService {

    /**
     * 创建回调通知
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createCallbackNotify(CallbackNotifyCreateReqVO createReqVO);

    /**
     * 获得回调通知分页
     *
     * @param pageReqVO 分页查询
     * @return 回调通知分页
     */
    PageResult<CallbackNotifyDO> getCallbackNotifyPage(CallbackNotifyPageReqVO pageReqVO);

    /**
     * 获得回调通知
     *
     * @param id 编号
     * @return 回调通知
     */
    CallbackNotifyDO getCallbackNotify(Long id);

    /**
     * 根据通知ID获得回调通知
     *
     * @param notifyId 通知ID
     * @return 回调通知
     */
    CallbackNotifyDO getCallbackNotifyByNotifyId(String notifyId);

    /**
     * 处理回调通知
     *
     * @param notifyId 通知ID
     * @param notifyType 通知类型
     * @param businessId 业务ID
     * @param notifyData 通知数据
     * @param sign 签名
     * @return 处理结果
     */
    String processCallback(String notifyId, String notifyType, String businessId, String notifyData, String sign);

    /**
     * 重试失败的回调通知
     *
     * @param id 回调通知ID
     */
    void retryCallback(Long id);

    /**
     * 获取待处理的回调通知
     *
     * @return 待处理的回调通知列表
     */
    List<CallbackNotifyDO> getPendingCallbacks();

    /**
     * 回调通知创建请求VO
     */
    class CallbackNotifyCreateReqVO {
        private String notifyId;
        private String notifyType;
        private String businessId;
        private String notifyData;
        private String sign;
        private Integer processStatus;
        private String processMsg;
        private Integer retryCount;

        // getters and setters
        public String getNotifyId() { return notifyId; }
        public void setNotifyId(String notifyId) { this.notifyId = notifyId; }
        public String getNotifyType() { return notifyType; }
        public void setNotifyType(String notifyType) { this.notifyType = notifyType; }
        public String getBusinessId() { return businessId; }
        public void setBusinessId(String businessId) { this.businessId = businessId; }
        public String getNotifyData() { return notifyData; }
        public void setNotifyData(String notifyData) { this.notifyData = notifyData; }
        public String getSign() { return sign; }
        public void setSign(String sign) { this.sign = sign; }
        public Integer getProcessStatus() { return processStatus; }
        public void setProcessStatus(Integer processStatus) { this.processStatus = processStatus; }
        public String getProcessMsg() { return processMsg; }
        public void setProcessMsg(String processMsg) { this.processMsg = processMsg; }
        public Integer getRetryCount() { return retryCount; }
        public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    }

} 
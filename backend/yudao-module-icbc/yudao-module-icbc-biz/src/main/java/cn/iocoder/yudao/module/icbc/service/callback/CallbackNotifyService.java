package cn.iocoder.yudao.module.icbc.service.callback;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.callback.vo.CallbackNotifyPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.callback.CallbackNotifyDO;

import java.util.List;

/**
 * 工行回调通知 Service 接口
 *
 * 九类异步通知的<strong>唯一</strong>入口。所有通知一律「先落表、再处理」，
 * 支持重放，重放不产生重复业务。
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
     */
    PageResult<CallbackNotifyDO> getCallbackNotifyPage(CallbackNotifyPageReqVO pageReqVO);

    /**
     * 获得回调通知
     */
    CallbackNotifyDO getCallbackNotify(Long id);

    /**
     * 根据通知ID获得回调通知
     */
    CallbackNotifyDO getCallbackNotifyByNotifyId(String notifyId);

    /**
     * 接收工行通知报文：解析 → 先落表 → 再处理
     *
     * @param body 工行通知报文（{@code {notifyData, signData}} 或明文 JSON）
     * @return "SUCCESS" / "FAILURE"
     */
    String receive(String body);

    /**
     * 重放一条通知：重新分发处理（已成功的通知不会被重复处理）
     */
    void replay(Long id);

    /**
     * 获取待处理的回调通知
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

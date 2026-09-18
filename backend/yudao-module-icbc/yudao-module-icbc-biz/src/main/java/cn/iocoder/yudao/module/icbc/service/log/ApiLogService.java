package cn.iocoder.yudao.module.icbc.service.log;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.log.vo.ApiLogPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.log.ApiLogDO;

/**
 * 工行接口调用日志 Service 接口
 *
 * @author 芋道源码
 */
public interface ApiLogService {

    /**
     * 创建接口调用日志
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createApiLog(ApiLogCreateReqVO createReqVO);

    /**
     * 获得接口调用日志分页
     *
     * @param pageReqVO 分页查询
     * @return 接口调用日志分页
     */
    PageResult<ApiLogDO> getApiLogPage(ApiLogPageReqVO pageReqVO);

    /**
     * 获得接口调用日志
     *
     * @param id 编号
     * @return 接口调用日志
     */
    ApiLogDO getApiLog(Long id);

    /**
     * 根据消息ID获得接口调用日志
     *
     * @param msgId 消息ID
     * @return 接口调用日志
     */
    ApiLogDO getApiLogByMsgId(String msgId);

    /**
     * 记录API调用开始
     *
     * @param msgId 消息ID
     * @param apiName 接口名称
     * @param apiUrl 接口URL
     * @param method 请求方法
     * @param requestParams 请求参数
     * @param businessId 业务ID
     * @param businessType 业务类型
     * @return 日志ID
     */
    Long logApiStart(String msgId, String apiName, String apiUrl, String method, 
                     String requestParams, String businessId, String businessType);

    /**
     * 记录API调用成功
     *
     * @param logId 日志ID
     * @param responseData 响应数据
     * @param returnCode 返回码
     * @param returnMsg 返回消息
     * @param costTime 耗时
     */
    void logApiSuccess(Long logId, String responseData, String returnCode, String returnMsg, Integer costTime);

    /**
     * 记录API调用失败
     *
     * @param logId 日志ID
     * @param errorMsg 错误信息
     * @param costTime 耗时
     */
    void logApiFailure(Long logId, String errorMsg, Integer costTime);

    /**
     * API日志创建请求VO
     */
    class ApiLogCreateReqVO {
        private String msgId;
        private String apiName;
        private String apiUrl;
        private String method;
        private String requestParams;
        private String responseData;
        private String returnCode;
        private String returnMsg;
        private Integer status;
        private Integer costTime;
        private String businessId;
        private String businessType;
        private String errorMsg;

        // getters and setters
        public String getMsgId() { return msgId; }
        public void setMsgId(String msgId) { this.msgId = msgId; }
        public String getApiName() { return apiName; }
        public void setApiName(String apiName) { this.apiName = apiName; }
        public String getApiUrl() { return apiUrl; }
        public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public String getRequestParams() { return requestParams; }
        public void setRequestParams(String requestParams) { this.requestParams = requestParams; }
        public String getResponseData() { return responseData; }
        public void setResponseData(String responseData) { this.responseData = responseData; }
        public String getReturnCode() { return returnCode; }
        public void setReturnCode(String returnCode) { this.returnCode = returnCode; }
        public String getReturnMsg() { return returnMsg; }
        public void setReturnMsg(String returnMsg) { this.returnMsg = returnMsg; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public Integer getCostTime() { return costTime; }
        public void setCostTime(Integer costTime) { this.costTime = costTime; }
        public String getBusinessId() { return businessId; }
        public void setBusinessId(String businessId) { this.businessId = businessId; }
        public String getBusinessType() { return businessType; }
        public void setBusinessType(String businessType) { this.businessType = businessType; }
        public String getErrorMsg() { return errorMsg; }
        public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    }

} 
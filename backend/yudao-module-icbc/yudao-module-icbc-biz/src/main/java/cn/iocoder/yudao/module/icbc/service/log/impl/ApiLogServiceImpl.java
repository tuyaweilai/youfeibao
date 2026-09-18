package cn.iocoder.yudao.module.icbc.service.log.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.icbc.controller.admin.log.vo.ApiLogPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.log.ApiLogDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.log.ApiLogMapper;
import cn.iocoder.yudao.module.icbc.enums.ApiLogStatusEnum;
import cn.iocoder.yudao.module.icbc.service.log.ApiLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

/**
 * 工行接口调用日志 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class ApiLogServiceImpl implements ApiLogService {

    @Resource
    private ApiLogMapper apiLogMapper;

    @Override
    public Long createApiLog(ApiLogCreateReqVO createReqVO) {
        // 插入
        ApiLogDO apiLog = ApiLogDO.builder()
                .msgId(createReqVO.getMsgId())
                .apiName(createReqVO.getApiName())
                .apiUrl(createReqVO.getApiUrl())
                .method(createReqVO.getMethod())
                .requestParams(createReqVO.getRequestParams())
                .responseData(createReqVO.getResponseData())
                .returnCode(createReqVO.getReturnCode())
                .returnMsg(createReqVO.getReturnMsg())
                .status(createReqVO.getStatus())
                .costTime(createReqVO.getCostTime())
                .businessId(createReqVO.getBusinessId())
                .businessType(createReqVO.getBusinessType())
                .errorMsg(createReqVO.getErrorMsg())
                .build();
        apiLogMapper.insert(apiLog);
        // 返回
        return apiLog.getId();
    }

    @Override
    public PageResult<ApiLogDO> getApiLogPage(ApiLogPageReqVO pageReqVO) {
        return apiLogMapper.selectPage(pageReqVO);
    }

    @Override
    public ApiLogDO getApiLog(Long id) {
        return apiLogMapper.selectById(id);
    }

    @Override
    public ApiLogDO getApiLogByMsgId(String msgId) {
        return apiLogMapper.selectByMsgId(msgId);
    }

    @Override
    @TenantIgnore
    public Long logApiStart(String msgId, String apiName, String apiUrl, String method, 
                           String requestParams, String businessId, String businessType) {
        ApiLogDO apiLog = ApiLogDO.builder()
                .msgId(msgId)
                .apiName(apiName)
                .apiUrl(apiUrl)
                .method(method)
                .requestParams(requestParams)
                .businessId(businessId)
                .businessType(businessType)
                .status(ApiLogStatusEnum.SUCCESS.getStatus()) // 先设为成功，后续可能更新为失败
                .build();
        apiLogMapper.insert(apiLog);
        return apiLog.getId();
    }

    @Override
    @TenantIgnore
    public void logApiSuccess(Long logId, String responseData, String returnCode, String returnMsg, Integer costTime) {
        ApiLogDO updateObj = ApiLogDO.builder()
                .id(logId)
                .responseData(responseData)
                .returnCode(returnCode)
                .returnMsg(returnMsg)
                .status(ApiLogStatusEnum.SUCCESS.getStatus())
                .costTime(costTime)
                .build();
        apiLogMapper.updateById(updateObj);
    }

    @Override
    @TenantIgnore
    public void logApiFailure(Long logId, String errorMsg, Integer costTime) {
        ApiLogDO updateObj = ApiLogDO.builder()
                .id(logId)
                .status(ApiLogStatusEnum.FAILURE.getStatus())
                .errorMsg(errorMsg)
                .costTime(costTime)
                .build();
        apiLogMapper.updateById(updateObj);
    }

} 
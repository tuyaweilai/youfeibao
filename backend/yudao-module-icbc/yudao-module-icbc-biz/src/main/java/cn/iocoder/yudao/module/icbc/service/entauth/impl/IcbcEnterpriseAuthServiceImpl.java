package cn.iocoder.yudao.module.icbc.service.entauth.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.entauth.vo.IcbcEnterpriseAuthInitReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.entauth.vo.IcbcEnterpriseAuthPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.entauth.vo.IcbcEnterpriseAuthUpdateReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.entauth.IcbcEnterpriseAuthDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.entauth.IcbcEnterpriseAuthMapper;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGateway;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGatewayResult;
import cn.iocoder.yudao.module.icbc.gateway.model.EnterpriseAuthReq;
import cn.iocoder.yudao.module.icbc.gateway.model.IcbcPage;
import cn.iocoder.yudao.module.icbc.service.entauth.IcbcEnterpriseAuthService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.ENTERPRISE_AUTH_NOT_EXISTS;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.ICBC_API_CALL_FAILED;

/**
 * 工行企业授权 Service 实现
 */
@Service
@Validated
public class IcbcEnterpriseAuthServiceImpl implements IcbcEnterpriseAuthService {

    @Resource
    private IcbcGateway icbcGateway;

    @Resource
    private IcbcEnterpriseAuthMapper enterpriseAuthMapper;

    @Override
    public String initEnterpriseAuth(IcbcEnterpriseAuthInitReqVO reqVO) {
        // 1. 经适配层生成授权页面表单
        IcbcGatewayResult<IcbcPage> result = icbcGateway.submitEnterpriseAuthorization(
                EnterpriseAuthReq.builder()
                        .outVendorId(reqVO.getOutVendorId())
                        .siteType(reqVO.getSiteType())
                        .userType(reqVO.getUserType())
                        .build());
        if (!result.isSuccess()) {
            throw exception(ICBC_API_CALL_FAILED);
        }
        // 2. 落一条授权记录（状态待人工确认，工行无查询接口）
        IcbcEnterpriseAuthDO record = enterpriseAuthMapper.selectByOutVendorId(reqVO.getOutVendorId());
        if (record == null) {
            record = new IcbcEnterpriseAuthDO();
            record.setOutVendorId(reqVO.getOutVendorId());
            record.setSiteType(reqVO.getSiteType());
            record.setUserType(reqVO.getUserType());
            record.setAuthStatus(0);
            enterpriseAuthMapper.insert(record);
        }
        // 3. 返回页面表单
        return result.getData() != null ? result.getData().getFormHtml() : null;
    }

    @Override
    public PageResult<IcbcEnterpriseAuthDO> getEnterpriseAuthPage(IcbcEnterpriseAuthPageReqVO pageReqVO) {
        return enterpriseAuthMapper.selectPage(pageReqVO);
    }

    @Override
    public void updateAuthResult(IcbcEnterpriseAuthUpdateReqVO reqVO) {
        if (reqVO.getId() == null || enterpriseAuthMapper.selectById(reqVO.getId()) == null) {
            throw exception(ENTERPRISE_AUTH_NOT_EXISTS);
        }
        Integer authStatus = reqVO.getAuthStatus();
        LocalDateTime authTime = reqVO.getAuthTime();
        LocalDateTime expireTime = reqVO.getExpireTime();
        // 已授权但未填授权时间时，默认取当前时间
        if (Integer.valueOf(1).equals(authStatus) && authTime == null) {
            authTime = LocalDateTime.now();
        }
        // 有效期已过则状态归为已失效，避免出现「已授权但已过期」的自相矛盾数据
        if (expireTime != null && !expireTime.isAfter(LocalDateTime.now())) {
            authStatus = 2;
        }
        IcbcEnterpriseAuthDO updateObj = new IcbcEnterpriseAuthDO();
        updateObj.setId(reqVO.getId());
        updateObj.setAuthStatus(authStatus);
        updateObj.setAuthTime(authTime);
        updateObj.setExpireTime(expireTime);
        updateObj.setRemark(reqVO.getRemark());
        enterpriseAuthMapper.updateById(updateObj);
    }

}

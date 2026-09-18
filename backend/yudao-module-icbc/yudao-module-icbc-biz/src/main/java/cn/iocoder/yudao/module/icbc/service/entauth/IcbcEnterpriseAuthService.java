package cn.iocoder.yudao.module.icbc.service.entauth;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.entauth.vo.IcbcEnterpriseAuthInitReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.entauth.vo.IcbcEnterpriseAuthPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.entauth.IcbcEnterpriseAuthDO;

/**
 * 工行企业授权 Service 接口
 */
public interface IcbcEnterpriseAuthService {

    /**
     * 发起企业授权：经适配层生成工行授权页面表单，并落一条授权记录
     *
     * @return 授权页面表单 HTML（交给前端在新窗口自动提交）
     */
    String initEnterpriseAuth(IcbcEnterpriseAuthInitReqVO reqVO);

    PageResult<IcbcEnterpriseAuthDO> getEnterpriseAuthPage(IcbcEnterpriseAuthPageReqVO pageReqVO);

    /**
     * 人工回填授权结果（工行无查询接口，暂由管理员确认）
     */
    void updateAuthStatus(Long id, Integer authStatus);

}

package cn.iocoder.yudao.module.enterprise.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.enterprise.controller.admin.enterprise.vo.EnterpriseInfoCreateReqVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.enterprise.vo.EnterpriseInfoPageReqVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.enterprise.vo.EnterpriseInfoUpdateReqVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.enterprise.vo.EnterpriseInfoAuditReqVO;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseInfoDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * 企业信息 Service 接口
 *
 * @author 芋道源码
 */
public interface EnterpriseInfoService {

    /**
     * 创建企业信息
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createEnterpriseInfo(@Valid EnterpriseInfoCreateReqVO createReqVO);

    /**
     * 更新企业信息
     *
     * @param updateReqVO 更新信息
     */
    void updateEnterpriseInfo(@Valid EnterpriseInfoUpdateReqVO updateReqVO);

    /**
     * 删除企业信息
     *
     * @param id 编号
     */
    void deleteEnterpriseInfo(Long id);

    /**
     * 获得企业信息
     *
     * @param id 编号
     * @return 企业信息
     */
    EnterpriseInfoDO getEnterpriseInfo(Long id);

    /**
     * 获得企业信息列表
     *
     * @param ids 编号列表
     * @return 企业信息列表
     */
    List<EnterpriseInfoDO> getEnterpriseInfoList(Collection<Long> ids);

    /**
     * 获得企业信息分页
     *
     * @param pageReqVO 分页查询
     * @return 企业信息分页
     */
    PageResult<EnterpriseInfoDO> pageEnterpriseInfo(EnterpriseInfoPageReqVO pageReqVO);

    /**
     * 根据统一社会信用代码获取企业信息
     *
     * @param creditCode 统一社会信用代码
     * @return 企业信息
     */
    EnterpriseInfoDO getEnterpriseInfoByCreditCode(String creditCode);
    
    /**
     * 获取企业信息简单列表
     *
     * @param name 企业名称
     * @param status 企业状态
     * @param enterpriseType 企业类型
     * @return 企业信息列表
     */
    List<EnterpriseInfoDO> getEnterpriseInfoSimpleList(String name, Integer status, Integer enterpriseType);

    /**
     * 企业入驻审核
     *
     * @param auditReqVO 审核请求
     */
    void auditEnterpriseInfo(@Valid EnterpriseInfoAuditReqVO auditReqVO);

} 
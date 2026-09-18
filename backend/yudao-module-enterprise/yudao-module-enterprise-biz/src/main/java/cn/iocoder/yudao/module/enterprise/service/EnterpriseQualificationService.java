package cn.iocoder.yudao.module.enterprise.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.enterprise.controller.admin.qualification.vo.EnterpriseQualificationAuditReqVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.qualification.vo.EnterpriseQualificationCreateReqVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.qualification.vo.EnterpriseQualificationPageReqVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.qualification.vo.EnterpriseQualificationUpdateReqVO;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseQualificationDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 企业资质 Service 接口
 *
 * @author 芋道源码
 */
public interface EnterpriseQualificationService {

    /**
     * 创建企业资质
     *
     * @param createReqVO 创建信息
     * @return 资质ID
     */
    Long createEnterpriseQualification(@Valid EnterpriseQualificationCreateReqVO createReqVO);

    /**
     * 更新企业资质
     *
     * @param updateReqVO 更新信息
     */
    void updateEnterpriseQualification(@Valid EnterpriseQualificationUpdateReqVO updateReqVO);

    /**
     * 删除企业资质
     *
     * @param id 资质ID
     */
    void deleteEnterpriseQualification(Long id);

    /**
     * 获得企业资质
     *
     * @param id 资质ID
     * @return 企业资质
     */
    EnterpriseQualificationDO getEnterpriseQualification(Long id);

    /**
     * 获得企业资质列表
     *
     * @param enterpriseId 企业ID
     * @return 企业资质列表
     */
    List<EnterpriseQualificationDO> getEnterpriseQualificationListByEnterpriseId(Long enterpriseId);

    /**
     * 获得企业资质分页
     *
     * @param pageReqVO 分页查询条件
     * @return 企业资质分页
     */
    PageResult<EnterpriseQualificationDO> pageEnterpriseQualification(EnterpriseQualificationPageReqVO pageReqVO);

    /**
     * 审核企业资质
     *
     * @param auditReqVO 审核信息
     */
    void auditEnterpriseQualification(@Valid EnterpriseQualificationAuditReqVO auditReqVO);

} 
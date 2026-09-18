package cn.iocoder.yudao.module.icbc.service.qualification;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.qualification.vo.IcbcQualificationPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.qualification.vo.IcbcQualificationSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.qualification.IcbcQualificationDO;

import java.util.List;

/**
 * 租户三层资质 Service 接口
 */
public interface IcbcQualificationService {

    Long createQualification(IcbcQualificationSaveReqVO createReqVO);

    void updateQualification(IcbcQualificationSaveReqVO updateReqVO);

    void deleteQualification(Long id);

    IcbcQualificationDO getQualification(Long id);

    PageResult<IcbcQualificationDO> getQualificationPage(IcbcQualificationPageReqVO pageReqVO);

    List<IcbcQualificationDO> getQualificationList();

    /**
     * 临近到期的资质（有效且 validTo 落在今天起的 days 天内）
     */
    List<IcbcQualificationDO> getExpiringList(int days);

    /**
     * 本租户开票就绪：三层的每一层都有一份「有效且未过期」的资质
     */
    boolean isTenantReady();

}

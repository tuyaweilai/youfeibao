package cn.iocoder.yudao.module.enterprise.convert;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.enterprise.controller.admin.enterprise.vo.EnterpriseInfoCreateReqVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.enterprise.vo.EnterpriseInfoRespVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.enterprise.vo.EnterpriseInfoSimpleRespVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.enterprise.vo.EnterpriseInfoUpdateReqVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.relation.vo.EnterpriseUserRelationCreateReqVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.relation.vo.EnterpriseUserRelationUpdateReqVO;
import cn.iocoder.yudao.module.enterprise.controller.app.enterprise.vo.AppEnterpriseInfoRespVO;
import cn.iocoder.yudao.module.enterprise.controller.app.enterprise.vo.AppEnterpriseInfoSimpleRespVO;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseInfoDO;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseUserRelationDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 企业信息 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface EnterpriseInfoConvert {

    EnterpriseInfoConvert INSTANCE = Mappers.getMapper(EnterpriseInfoConvert.class);

    @Mapping(source = "businessLicenseFile", target = "businessLicenseFile")
    EnterpriseInfoDO convert(EnterpriseInfoCreateReqVO bean);

    EnterpriseInfoDO convert(EnterpriseInfoUpdateReqVO bean);

    EnterpriseInfoRespVO convert(EnterpriseInfoDO bean);

    List<EnterpriseInfoRespVO> convertList(List<EnterpriseInfoDO> list);

    PageResult<EnterpriseInfoRespVO> convertPage(PageResult<EnterpriseInfoDO> page);
    
    // 新增企业信息简易VO的转换方法
    EnterpriseInfoSimpleRespVO convertSimple(EnterpriseInfoDO bean);
    List<EnterpriseInfoSimpleRespVO> convertSimpleList(List<EnterpriseInfoDO> list);
    
    // APP端企业信息简易VO的转换方法
    AppEnterpriseInfoSimpleRespVO convertAppSimple(EnterpriseInfoDO bean);
    List<AppEnterpriseInfoSimpleRespVO> convertAppSimpleList(List<EnterpriseInfoDO> list);
    
    // APP端完整企业信息VO的转换方法
    AppEnterpriseInfoRespVO convertApp(EnterpriseInfoDO bean);
    List<AppEnterpriseInfoRespVO> convertAppList(List<EnterpriseInfoDO> list);
    
    // 将 EnterpriseUserRelationDO 转换为 EnterpriseUserRelationCreateReqVO 和 UpdateReqVO
    EnterpriseUserRelationCreateReqVO convertToRelationCreateReq(EnterpriseUserRelationDO bean);
    EnterpriseUserRelationUpdateReqVO convertToRelationUpdateReq(EnterpriseUserRelationDO bean);
} 
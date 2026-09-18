package cn.iocoder.yudao.module.enterprise.convert;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.enterprise.controller.admin.relation.vo.EnterpriseUserRelationCreateReqVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.relation.vo.EnterpriseUserRelationRespVO;
import cn.iocoder.yudao.module.enterprise.controller.admin.relation.vo.EnterpriseUserRelationUpdateReqVO;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseInfoDO;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseStoreDO;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseUserRelationDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 用户企业关系 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface EnterpriseUserRelationConvert {

    EnterpriseUserRelationConvert INSTANCE = Mappers.getMapper(EnterpriseUserRelationConvert.class);

    EnterpriseUserRelationDO convert(EnterpriseUserRelationCreateReqVO bean);

    EnterpriseUserRelationDO convert(EnterpriseUserRelationUpdateReqVO bean);

    EnterpriseUserRelationRespVO convert(EnterpriseUserRelationDO bean);

    List<EnterpriseUserRelationRespVO> convertList(List<EnterpriseUserRelationDO> list);

    PageResult<EnterpriseUserRelationRespVO> convertPage(PageResult<EnterpriseUserRelationDO> page);

    /**
     * 设置关联的企业信息
     * 
     * @param respVO 用户企业关系响应VO
     * @param enterprise 企业信息
     * @return 更新后的用户企业关系响应VO
     */
    @Mapping(source = "enterprise.name", target = "enterpriseName")
    @Mapping(source = "respVO.id", target = "id")
    @Mapping(source = "respVO.userId", target = "userId")
    @Mapping(source = "respVO.enterpriseId", target = "enterpriseId")
    @Mapping(source = "respVO.storeId", target = "storeId")
    @Mapping(source = "respVO.relationType", target = "relationType")
    @Mapping(source = "respVO.isPrimaryContact", target = "isPrimaryContact")
    @Mapping(source = "respVO.isDefaultEnterprise", target = "isDefaultEnterprise")
    @Mapping(source = "respVO.createTime", target = "createTime")
    @Mapping(source = "respVO.userName", target = "userName")
    @Mapping(target = "storeName", expression = "java(respVO.getStoreName())")
    EnterpriseUserRelationRespVO setEnterpriseInfo(EnterpriseUserRelationRespVO respVO, EnterpriseInfoDO enterprise);

    /**
     * 设置关联的门店信息
     * 
     * @param respVO 用户企业关系响应VO
     * @param store 门店信息
     * @return 更新后的用户企业关系响应VO
     */
    @Mapping(source = "store.name", target = "storeName")
    @Mapping(source = "respVO.id", target = "id")
    @Mapping(source = "respVO.userId", target = "userId")
    @Mapping(source = "respVO.enterpriseId", target = "enterpriseId")
    @Mapping(source = "respVO.storeId", target = "storeId")
    @Mapping(source = "respVO.relationType", target = "relationType")
    @Mapping(source = "respVO.isPrimaryContact", target = "isPrimaryContact")
    @Mapping(source = "respVO.isDefaultEnterprise", target = "isDefaultEnterprise")
    @Mapping(source = "respVO.createTime", target = "createTime")
    @Mapping(source = "respVO.userName", target = "userName")
    @Mapping(target = "enterpriseName", expression = "java(respVO.getEnterpriseName())")
    EnterpriseUserRelationRespVO setStoreInfo(EnterpriseUserRelationRespVO respVO, EnterpriseStoreDO store);

} 
package cn.iocoder.yudao.module.icbc.dal.mysql.entauth;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.entauth.vo.IcbcEnterpriseAuthPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.entauth.IcbcEnterpriseAuthDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工行企业授权 Mapper
 */
@Mapper
public interface IcbcEnterpriseAuthMapper extends BaseMapperX<IcbcEnterpriseAuthDO> {

    default PageResult<IcbcEnterpriseAuthDO> selectPage(IcbcEnterpriseAuthPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<IcbcEnterpriseAuthDO>()
                .eqIfPresent(IcbcEnterpriseAuthDO::getOutVendorId, reqVO.getOutVendorId())
                .eqIfPresent(IcbcEnterpriseAuthDO::getAuthStatus, reqVO.getAuthStatus())
                .orderByDesc(IcbcEnterpriseAuthDO::getId));
    }

    default IcbcEnterpriseAuthDO selectByOutVendorId(String outVendorId) {
        return selectOne(IcbcEnterpriseAuthDO::getOutVendorId, outVendorId);
    }

    // ==================== 工作台开票就绪（#56 T18） ====================

    /** 本租户处于给定授权状态的记录数（开票就绪看「已授权」）。 */
    default long selectCountByAuthStatus(Integer authStatus) {
        return selectCount(new LambdaQueryWrapperX<IcbcEnterpriseAuthDO>()
                .eq(IcbcEnterpriseAuthDO::getAuthStatus, authStatus));
    }

}

package cn.iocoder.yudao.module.icbc.dal.mysql.lead;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.lead.IcbcContactLeadDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 收方入驻失败留联系方式 Mapper
 */
@Mapper
public interface IcbcContactLeadMapper extends BaseMapperX<IcbcContactLeadDO> {

    default List<IcbcContactLeadDO> selectListByPayeeId(Long payeeId) {
        return selectList(IcbcContactLeadDO::getPayeeId, payeeId);
    }

}

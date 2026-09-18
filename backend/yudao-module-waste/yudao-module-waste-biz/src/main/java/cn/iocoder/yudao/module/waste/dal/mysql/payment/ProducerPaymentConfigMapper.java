package cn.iocoder.yudao.module.waste.dal.mysql.payment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.ProducerPaymentConfigPageReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.payment.ProducerPaymentConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 产废企业付款配置 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ProducerPaymentConfigMapper extends BaseMapperX<ProducerPaymentConfigDO> {

    default PageResult<ProducerPaymentConfigDO> selectPage(ProducerPaymentConfigPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ProducerPaymentConfigDO>()
                .eqIfPresent(ProducerPaymentConfigDO::getProducingEnterpriseId, reqVO.getEnterpriseId())
                .eqIfPresent(ProducerPaymentConfigDO::getProducingStoreId, reqVO.getStoreId())
                .eqIfPresent(ProducerPaymentConfigDO::getPaymentMethod, reqVO.getPaymentMethod())
                .eqIfPresent(ProducerPaymentConfigDO::getIsDefaultConfig, reqVO.getIsDefault())
                .eqIfPresent(ProducerPaymentConfigDO::getConfigStatus, reqVO.getStatus())
                .betweenIfPresent(ProducerPaymentConfigDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ProducerPaymentConfigDO::getId));
    }

    default List<ProducerPaymentConfigDO> selectList(ProducerPaymentConfigPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<ProducerPaymentConfigDO>()
                .eqIfPresent(ProducerPaymentConfigDO::getProducingEnterpriseId, reqVO.getEnterpriseId())
                .eqIfPresent(ProducerPaymentConfigDO::getProducingStoreId, reqVO.getStoreId())
                .eqIfPresent(ProducerPaymentConfigDO::getPaymentMethod, reqVO.getPaymentMethod())
                .eqIfPresent(ProducerPaymentConfigDO::getIsDefaultConfig, reqVO.getIsDefault())
                .eqIfPresent(ProducerPaymentConfigDO::getConfigStatus, reqVO.getStatus())
                .betweenIfPresent(ProducerPaymentConfigDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ProducerPaymentConfigDO::getId));
    }

    default List<ProducerPaymentConfigDO> selectByProducingEnterpriseId(Long producingEnterpriseId) {
        return selectList(ProducerPaymentConfigDO::getProducingEnterpriseId, producingEnterpriseId);
    }

    default List<ProducerPaymentConfigDO> selectByProducingStoreId(Long producingStoreId) {
        return selectList(ProducerPaymentConfigDO::getProducingStoreId, producingStoreId);
    }

    default ProducerPaymentConfigDO selectDefaultByEnterpriseId(Long producingEnterpriseId) {
        return selectOne(new LambdaQueryWrapperX<ProducerPaymentConfigDO>()
                .eq(ProducerPaymentConfigDO::getProducingEnterpriseId, producingEnterpriseId)
                .eq(ProducerPaymentConfigDO::getIsDefaultConfig, true)
                .eq(ProducerPaymentConfigDO::getConfigStatus, 1) // 有效状态
                .orderByDesc(ProducerPaymentConfigDO::getCreateTime)
                .last("LIMIT 1"));
    }

    default ProducerPaymentConfigDO selectDefaultByStoreId(Long producingStoreId) {
        return selectOne(new LambdaQueryWrapperX<ProducerPaymentConfigDO>()
                .eq(ProducerPaymentConfigDO::getProducingStoreId, producingStoreId)
                .eq(ProducerPaymentConfigDO::getIsDefaultConfig, true)
                .eq(ProducerPaymentConfigDO::getConfigStatus, 1) // 有效状态
                .orderByDesc(ProducerPaymentConfigDO::getCreateTime)
                .last("LIMIT 1"));
    }

    default List<ProducerPaymentConfigDO> selectByPaymentMethod(Integer paymentMethod) {
        return selectList(ProducerPaymentConfigDO::getPaymentMethod, paymentMethod);
    }

    default List<ProducerPaymentConfigDO> selectByConfigStatus(Integer configStatus) {
        return selectList(ProducerPaymentConfigDO::getConfigStatus, configStatus);
    }

    default List<ProducerPaymentConfigDO> selectValidConfigs() {
        return selectList(new LambdaQueryWrapperX<ProducerPaymentConfigDO>()
                .eq(ProducerPaymentConfigDO::getConfigStatus, 1) // 有效状态
                .orderByDesc(ProducerPaymentConfigDO::getCreateTime));
    }

    default List<ProducerPaymentConfigDO> selectAutoPaymentConfigs() {
        return selectList(new LambdaQueryWrapperX<ProducerPaymentConfigDO>()
                .eq(ProducerPaymentConfigDO::getAutoPaymentEnabled, true)
                .eq(ProducerPaymentConfigDO::getConfigStatus, 1) // 有效状态
                .orderByDesc(ProducerPaymentConfigDO::getCreateTime));
    }

    // Service实现类需要的方法
    default ProducerPaymentConfigDO selectByEnterpriseId(Long enterpriseId) {
        return selectDefaultByEnterpriseId(enterpriseId);
    }

    default List<ProducerPaymentConfigDO> selectListByStoreId(Long storeId) {
        return selectByProducingStoreId(storeId);
    }

    default List<ProducerPaymentConfigDO> selectListByPaymentMethod(Integer paymentMethod) {
        return selectByPaymentMethod(paymentMethod);
    }

    default List<ProducerPaymentConfigDO> selectDefaultConfigs() {
        return selectList(new LambdaQueryWrapperX<ProducerPaymentConfigDO>()
                .eq(ProducerPaymentConfigDO::getIsDefaultConfig, true)
                .eq(ProducerPaymentConfigDO::getConfigStatus, 1) // 有效状态
                .orderByDesc(ProducerPaymentConfigDO::getCreateTime));
    }

    default List<ProducerPaymentConfigDO> selectCorporateSettlementConfigs() {
        return selectList(new LambdaQueryWrapperX<ProducerPaymentConfigDO>()
                .eq(ProducerPaymentConfigDO::getPaymentMethod, 1) // 对公转账
                .eq(ProducerPaymentConfigDO::getConfigStatus, 1) // 有效状态
                .orderByDesc(ProducerPaymentConfigDO::getCreateTime));
    }

    default List<ProducerPaymentConfigDO> selectPersonalSettlementConfigs() {
        return selectList(new LambdaQueryWrapperX<ProducerPaymentConfigDO>()
                .eq(ProducerPaymentConfigDO::getPaymentMethod, 2) // 个人转账
                .eq(ProducerPaymentConfigDO::getConfigStatus, 1) // 有效状态
                .orderByDesc(ProducerPaymentConfigDO::getCreateTime));
    }

    default void unsetDefaultByEnterpriseId(Long enterpriseId) {
        // 使用updateBatch方法批量更新
        List<ProducerPaymentConfigDO> configs = selectByProducingEnterpriseId(enterpriseId);
        for (ProducerPaymentConfigDO config : configs) {
            if (config.getIsDefaultConfig()) {
                config.setIsDefaultConfig(false);
                updateById(config);
            }
        }
    }

    default List<ProducerPaymentConfigDO> selectListByEnterpriseId(Long enterpriseId) {
        return selectByProducingEnterpriseId(enterpriseId);
    }

    default List<ProducerPaymentConfigDO> selectListByEnterpriseIdAndPaymentMethod(Long enterpriseId, Integer paymentMethod) {
        return selectList(new LambdaQueryWrapperX<ProducerPaymentConfigDO>()
                .eq(ProducerPaymentConfigDO::getProducingEnterpriseId, enterpriseId)
                .eq(ProducerPaymentConfigDO::getPaymentMethod, paymentMethod)
                .eq(ProducerPaymentConfigDO::getConfigStatus, 1) // 有效状态
                .orderByDesc(ProducerPaymentConfigDO::getCreateTime));
    }

} 
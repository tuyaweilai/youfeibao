package cn.iocoder.yudao.module.contract.dal.mysql.attachment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.contract.controller.admin.attachment.vo.ContractAttachmentPageReqVO;
import cn.iocoder.yudao.module.contract.dal.dataobject.attachment.ContractAttachmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 合同附件 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ContractAttachmentMapper extends BaseMapperX<ContractAttachmentDO> {

    /**
     * 根据版本ID查询附件列表
     *
     * @param versionId 版本ID
     * @return 附件列表
     */
    default List<ContractAttachmentDO> selectListByVersionId(Long versionId) {
        return selectList(ContractAttachmentDO::getVersionId, versionId);
    }

    /**
     * 根据版本ID和附件类型查询附件列表
     *
     * @param versionId 版本ID
     * @param attachmentType 附件类型
     * @return 附件列表
     */
    default List<ContractAttachmentDO> selectListByVersionIdAndType(Long versionId, Integer attachmentType) {
        return selectList(new LambdaQueryWrapperX<ContractAttachmentDO>()
                .eq(ContractAttachmentDO::getVersionId, versionId)
                .eq(ContractAttachmentDO::getAttachmentType, attachmentType));
    }

    /**
     * 分页查询合同附件
     *
     * @param reqVO 分页查询条件
     * @return 合同附件分页结果
     */
    default PageResult<ContractAttachmentDO> selectPage(ContractAttachmentPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ContractAttachmentDO>()
                .eqIfPresent(ContractAttachmentDO::getVersionId, reqVO.getVersionId())
                .likeIfPresent(ContractAttachmentDO::getFileName, reqVO.getFileName())
                .eqIfPresent(ContractAttachmentDO::getAttachmentType, reqVO.getAttachmentType())
                .eqIfPresent(ContractAttachmentDO::getIsPublic, reqVO.getIsPublic())
                .betweenIfPresent(ContractAttachmentDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ContractAttachmentDO::getId));
    }
} 
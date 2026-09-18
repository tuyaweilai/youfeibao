package cn.iocoder.yudao.module.contract.service.attachment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.contract.controller.admin.attachment.vo.ContractAttachmentCreateReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.attachment.vo.ContractAttachmentPageReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.attachment.vo.ContractAttachmentUpdateReqVO;
import cn.iocoder.yudao.module.contract.convert.attachment.ContractAttachmentConvert;
import cn.iocoder.yudao.module.contract.dal.dataobject.attachment.ContractAttachmentDO;
import cn.iocoder.yudao.module.contract.dal.mysql.attachment.ContractAttachmentMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.contract.enums.ErrorCodeConstants.*;

/**
 * 合同附件 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ContractAttachmentServiceImpl implements ContractAttachmentService {

    @Resource
    private ContractAttachmentMapper contractAttachmentMapper;

    @Override
    public Long createContractAttachment(@Valid ContractAttachmentCreateReqVO createReqVO) {
        // 插入
        ContractAttachmentDO contractAttachment = ContractAttachmentConvert.INSTANCE.convert(createReqVO);
        contractAttachmentMapper.insert(contractAttachment);
        // 返回
        return contractAttachment.getId();
    }

    @Override
    public void updateContractAttachment(@Valid ContractAttachmentUpdateReqVO updateReqVO) {
        // 校验存在
        validateContractAttachmentExists(updateReqVO.getId());
        // 更新
        ContractAttachmentDO updateObj = ContractAttachmentConvert.INSTANCE.convert(updateReqVO);
        contractAttachmentMapper.updateById(updateObj);
    }

    @Override
    public void deleteContractAttachment(Long id) {
        // 校验存在
        validateContractAttachmentExists(id);
        // 删除
        contractAttachmentMapper.deleteById(id);
    }

    @Override
    public ContractAttachmentDO validateContractAttachmentExists(Long id) {
        ContractAttachmentDO attachment = contractAttachmentMapper.selectById(id);
        if (attachment == null) {
            throw exception(CONTRACT_ATTACHMENT_NOT_EXISTS);
        }
        return attachment;
    }

    @Override
    public int increaseDownloadCount(Long id) {
        ContractAttachmentDO attachment = getContractAttachment(id);
        if (attachment == null) {
            throw exception(CONTRACT_ATTACHMENT_NOT_EXISTS);
        }
        
        int newCount = (attachment.getDownloadCount() == null ? 0 : attachment.getDownloadCount()) + 1;
        contractAttachmentMapper.updateById(
            ContractAttachmentDO.builder()
                .id(id)
                .downloadCount(newCount)
                .build()
        );
        
        return newCount;
    }

    @Override
    public ContractAttachmentDO getContractAttachment(Long id) {
        return contractAttachmentMapper.selectById(id);
    }

    @Override
    public PageResult<ContractAttachmentDO> getContractAttachmentPage(ContractAttachmentPageReqVO pageReqVO) {
        return contractAttachmentMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ContractAttachmentDO> getContractAttachmentListByVersionId(Long versionId) {
        return contractAttachmentMapper.selectListByVersionId(versionId);
    }

    @Override
    public List<ContractAttachmentDO> getContractAttachmentListByVersionIdAndType(Long versionId, Integer attachmentType) {
        return contractAttachmentMapper.selectListByVersionIdAndType(versionId, attachmentType);
    }

    @Override
    public byte[] downloadContractAttachment(Long id) {
        // 校验附件存在
        ContractAttachmentDO attachment = getContractAttachment(id);
        if (attachment == null) {
            throw exception(CONTRACT_ATTACHMENT_NOT_EXISTS);
        }
        
        // 增加下载次数
        contractAttachmentMapper.updateById(
            ContractAttachmentDO.builder()
                .id(id)
                .downloadCount((attachment.getDownloadCount() == null ? 0 : attachment.getDownloadCount()) + 1)
                .build()
        );
        
        // TODO: 实际的文件下载逻辑，这里返回空字节数组作为示例
        // 实际实现中应该根据 attachment.getFilePath() 或 attachment.getFileUrl() 读取文件内容
        return new byte[0];
    }
} 
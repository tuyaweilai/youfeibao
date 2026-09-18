package cn.iocoder.yudao.module.contract.service.attachment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.contract.controller.admin.attachment.vo.ContractAttachmentCreateReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.attachment.vo.ContractAttachmentPageReqVO;
import cn.iocoder.yudao.module.contract.controller.admin.attachment.vo.ContractAttachmentUpdateReqVO;
import cn.iocoder.yudao.module.contract.dal.dataobject.attachment.ContractAttachmentDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 合同附件 Service 接口
 *
 * @author 芋道源码
 */
public interface ContractAttachmentService {

    /**
     * 创建合同附件
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createContractAttachment(@Valid ContractAttachmentCreateReqVO createReqVO);

    /**
     * 更新合同附件
     *
     * @param updateReqVO 更新信息
     */
    void updateContractAttachment(@Valid ContractAttachmentUpdateReqVO updateReqVO);

    /**
     * 删除合同附件
     *
     * @param id 编号
     */
    void deleteContractAttachment(Long id);

    /**
     * 获得合同附件
     *
     * @param id 编号
     * @return 合同附件
     */
    ContractAttachmentDO getContractAttachment(Long id);

    /**
     * 获得合同附件分页
     *
     * @param pageReqVO 分页查询
     * @return 合同附件分页
     */
    PageResult<ContractAttachmentDO> getContractAttachmentPage(ContractAttachmentPageReqVO pageReqVO);

    /**
     * 获得合同附件列表
     *
     * @param versionId 合同版本ID
     * @return 合同附件列表
     */
    List<ContractAttachmentDO> getContractAttachmentListByVersionId(Long versionId);

    /**
     * 获取合同附件列表按类型
     *
     * @param versionId 合同版本ID
     * @param attachmentType 附件类型
     * @return 合同附件列表
     */
    List<ContractAttachmentDO> getContractAttachmentListByVersionIdAndType(Long versionId, Integer attachmentType);

    /**
     * 校验合同附件是否存在
     *
     * @param id 编号
     * @return 合同附件
     */
    ContractAttachmentDO validateContractAttachmentExists(Long id);

    /**
     * 增加下载次数
     *
     * @param id 附件ID
     * @return 增加后的下载次数
     */
    int increaseDownloadCount(Long id);

    /**
     * 下载合同附件
     *
     * @param id 附件ID
     * @return 文件内容字节数组
     */
    byte[] downloadContractAttachment(Long id);
} 
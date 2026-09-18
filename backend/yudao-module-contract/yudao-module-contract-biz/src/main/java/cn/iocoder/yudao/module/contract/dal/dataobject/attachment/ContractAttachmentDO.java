package cn.iocoder.yudao.module.contract.dal.dataobject.attachment;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 合同附件 DO
 *
 * @author 芋道源码
 */
@TableName("contract_attachments")
@KeySequence("contract_attachment_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractAttachmentDO extends BaseDO {

    /**
     * 附件ID
     */
    @TableId
    private Long id;

    /**
     * 合同版本ID
     */
    private Long versionId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 原始文件名
     */
    private String originalFileName;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件URL
     */
    private String fileUrl;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 文件类型(MIME)
     */
    private String fileType;

    /**
     * 附件类型(0:合同正文,1:扫描件,2:补充文件,3:签署凭证)
     */
    private Integer attachmentType;

    /**
     * 是否公开
     */
    private Boolean isPublic;

    /**
     * 下载次数
     */
    private Integer downloadCount;

    /**
     * 租户ID
     */
    private Long tenantId;
} 
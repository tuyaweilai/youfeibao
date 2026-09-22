package cn.iocoder.yudao.module.icbc.dal.mysql.download;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.download.InvoiceFileDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 工行发票文件 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface InvoiceFileMapper extends BaseMapperX<InvoiceFileDO> {

    /**
     * 根据下载记录ID查询文件列表
     *
     * @param downloadId 下载记录ID
     * @return 文件列表
     */
    default List<InvoiceFileDO> selectListByDownloadId(Long downloadId) {
        return selectList(InvoiceFileDO::getDownloadId, downloadId);
    }

    /**
     * 根据下载记录ID批量查询文件列表（列表页一次取完，不在循环里逐条查）
     *
     * @param downloadIds 下载记录ID集合
     * @return 文件列表
     */
    default List<InvoiceFileDO> selectListByDownloadIds(Collection<Long> downloadIds) {
        if (downloadIds == null || downloadIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<InvoiceFileDO>()
                .in(InvoiceFileDO::getDownloadId, downloadIds)
                .orderByAsc(InvoiceFileDO::getId));
    }

    /**
     * 根据发票号码查询文件列表
     *
     * @param invoiceNumber 发票号码
     * @return 文件列表
     */
    default List<InvoiceFileDO> selectListByInvoiceNumber(String invoiceNumber) {
        return selectList(InvoiceFileDO::getInvoiceNumber, invoiceNumber);
    }

    /**
     * 根据下载记录ID和文件类型查询文件
     *
     * @param downloadId 下载记录ID
     * @param fileType 文件类型
     * @return 文件记录
     */
    default InvoiceFileDO selectByDownloadIdAndFileType(Long downloadId, String fileType) {
        return selectOne(InvoiceFileDO::getDownloadId, downloadId, InvoiceFileDO::getFileType, fileType);
    }

    /**
     * 根据下载记录ID删除文件
     *
     * @param downloadId 下载记录ID
     * @return 删除数量
     */
    default int deleteByDownloadId(Long downloadId) {
        return delete(InvoiceFileDO::getDownloadId, downloadId);
    }

} 
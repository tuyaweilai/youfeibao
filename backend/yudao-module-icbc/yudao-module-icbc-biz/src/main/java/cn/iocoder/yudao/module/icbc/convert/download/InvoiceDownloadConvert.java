package cn.iocoder.yudao.module.icbc.convert.download;

import cn.iocoder.yudao.module.icbc.controller.admin.download.vo.InvoiceDownloadRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.download.vo.InvoiceFileRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.download.InvoiceDownloadDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.download.InvoiceFileDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 工行发票下载对象转换器
 *
 * @author 芋道源码
 */
@Mapper
public interface InvoiceDownloadConvert {

    InvoiceDownloadConvert INSTANCE = Mappers.getMapper(InvoiceDownloadConvert.class);

    /**
     * 转换下载记录
     *
     * @param downloadDO 下载记录DO
     * @return 下载记录VO
     */
    InvoiceDownloadRespVO convert(InvoiceDownloadDO downloadDO);

    /**
     * 转换文件记录
     *
     * @param fileDO 文件记录DO
     * @return 文件记录VO
     */
    InvoiceFileRespVO convertFile(InvoiceFileDO fileDO);

    /**
     * 转换文件记录列表
     *
     * @param files 文件记录DO列表
     * @return 文件记录VO列表
     */
    List<InvoiceFileRespVO> convertFileList(List<InvoiceFileDO> files);

} 
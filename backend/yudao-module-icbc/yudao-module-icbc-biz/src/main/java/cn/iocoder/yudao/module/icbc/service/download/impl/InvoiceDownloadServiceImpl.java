package cn.iocoder.yudao.module.icbc.service.download.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import java.time.format.DateTimeFormatter;
import cn.iocoder.yudao.module.icbc.controller.admin.download.vo.InvoiceDownloadPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.download.vo.InvoiceDownloadReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.download.vo.InvoiceDownloadRespVO;
import cn.iocoder.yudao.module.icbc.convert.download.InvoiceDownloadConvert;
import cn.iocoder.yudao.module.icbc.dal.dataobject.download.InvoiceDownloadDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.download.InvoiceFileDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.download.InvoiceDownloadMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.download.InvoiceFileMapper;
import cn.iocoder.yudao.module.icbc.enums.DownloadStatusEnum;
import cn.iocoder.yudao.module.icbc.service.download.InvoiceDownloadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 工行发票下载服务实现类
 *
 * @author 芋道源码
 */
@Service
@Slf4j
public class InvoiceDownloadServiceImpl implements InvoiceDownloadService {

    @Resource
    private InvoiceDownloadMapper invoiceDownloadMapper;
    
    @Resource
    private InvoiceFileMapper invoiceFileMapper;

    @Value("${yudao.file.base-path:/data/files}")
    private String fileBasePath;

    private static final String INVOICE_DIR = "invoices";
    private static final int MAX_RETRY_COUNT = 3;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDownloadTask(Long invoiceOrderId, String partnerOrderId, String orderNumber, 
                                  String invoiceNumber, String downloadUrl) {
        // 检查是否已存在下载记录
        InvoiceDownloadDO existingRecord = invoiceDownloadMapper.selectByPartnerOrderId(partnerOrderId);
        if (existingRecord != null) {
            log.info("发票下载任务已存在，partnerOrderId: {}", partnerOrderId);
            return existingRecord.getId();
        }

        // 创建下载记录
        InvoiceDownloadDO downloadDO = InvoiceDownloadDO.builder()
                .invoiceOrderId(invoiceOrderId)
                .partnerOrderId(partnerOrderId)
                .orderNumber(orderNumber)
                .invoiceNumber(invoiceNumber)
                .downloadUrl(downloadUrl)
                .downloadStatus(DownloadStatusEnum.PENDING.getStatus())
                .retryCount(0)
                .build();

        invoiceDownloadMapper.insert(downloadDO);
        log.info("创建发票下载任务成功，downloadId: {}, partnerOrderId: {}", downloadDO.getId(), partnerOrderId);
        return downloadDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InvoiceDownloadRespVO downloadInvoice(InvoiceDownloadReqVO reqVO) {
        // 查询下载记录
        InvoiceDownloadDO downloadDO = invoiceDownloadMapper.selectByPartnerOrderId(reqVO.getPartnerOrderId());
        if (downloadDO == null) {
            throw new ServiceException(INVOICE_DOWNLOAD_NOT_FOUND);
        }

        // 检查下载状态
        if (DownloadStatusEnum.SUCCESS.getStatus().equals(downloadDO.getDownloadStatus())) {
            log.info("发票已下载成功，无需重复下载，partnerOrderId: {}", reqVO.getPartnerOrderId());
            return buildDownloadRespVO(downloadDO);
        }

        // 执行下载
        return executeDownload(downloadDO);
    }

    @Override
    public InvoiceDownloadRespVO getDownloadRecord(String partnerOrderId) {
        InvoiceDownloadDO downloadDO = invoiceDownloadMapper.selectByPartnerOrderId(partnerOrderId);
        if (downloadDO == null) {
            throw new ServiceException(INVOICE_DOWNLOAD_NOT_FOUND);
        }
        return buildDownloadRespVO(downloadDO);
    }

    @Override
    public InvoiceDownloadRespVO getDownloadRecordByInvoiceNumber(String invoiceNumber) {
        InvoiceDownloadDO downloadDO = invoiceDownloadMapper.selectByInvoiceNumber(invoiceNumber);
        if (downloadDO == null) {
            throw new ServiceException(INVOICE_DOWNLOAD_NOT_FOUND);
        }
        return buildDownloadRespVO(downloadDO);
    }

    @Override
    public PageResult<InvoiceDownloadRespVO> getDownloadPage(InvoiceDownloadPageReqVO reqVO) {
        PageResult<InvoiceDownloadDO> page = invoiceDownloadMapper.selectPage(reqVO);
        if (page.getList().isEmpty()) {
            return PageResult.empty(page.getTotal());
        }
        // 文件一次批量取回：行内的「下载 PDF」要知道文件类型，详情里的文件表也要用
        List<InvoiceFileDO> files = invoiceFileMapper.selectListByDownloadIds(
                page.getList().stream().map(InvoiceDownloadDO::getId).collect(Collectors.toList()));
        Map<Long, List<InvoiceFileDO>> filesByDownloadId = files.stream()
                .collect(Collectors.groupingBy(InvoiceFileDO::getDownloadId));
        List<InvoiceDownloadRespVO> list = page.getList().stream()
                .map(downloadDO -> buildDownloadRespVO(downloadDO,
                        filesByDownloadId.getOrDefault(downloadDO.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public List<InvoiceDownloadDO> getPendingDownloadTasks() {
        return invoiceDownloadMapper.selectListByDownloadStatus(DownloadStatusEnum.PENDING.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InvoiceDownloadRespVO retryDownload(Long downloadId) {
        InvoiceDownloadDO downloadDO = invoiceDownloadMapper.selectById(downloadId);
        if (downloadDO == null) {
            throw new ServiceException(INVOICE_DOWNLOAD_NOT_FOUND);
        }

        // 检查重试次数
        if (downloadDO.getRetryCount() >= MAX_RETRY_COUNT) {
            throw new ServiceException(INVOICE_DOWNLOAD_MAX_RETRY);
        }

        return executeDownload(downloadDO);
    }

    @Override
    public void downloadFile(Long downloadId, String fileType, HttpServletResponse response) {
        // 查询文件记录
        InvoiceFileDO fileDO = invoiceFileMapper.selectByDownloadIdAndFileType(downloadId, fileType);
        if (fileDO == null) {
            throw new ServiceException(INVOICE_FILE_NOT_FOUND);
        }

        File file = new File(fileDO.getFilePath());
        if (!file.exists()) {
            throw new ServiceException(INVOICE_FILE_NOT_EXISTS);
        }

        // 更新访问记录
        updateFileAccessRecord(fileDO.getId());

        // 下载文件
        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileDO.getFileName());
            response.setContentLengthLong(fileDO.getFileSize());

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
            
        } catch (IOException e) {
            log.error("下载发票文件失败，fileId: {}, filePath: {}", fileDO.getId(), fileDO.getFilePath(), e);
            throw new ServiceException(INVOICE_FILE_DOWNLOAD_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDownloadStatus(Long downloadId, Integer status, String errorMsg) {
        InvoiceDownloadDO updateDO = new InvoiceDownloadDO();
        updateDO.setId(downloadId);
        updateDO.setDownloadStatus(status);
        updateDO.setErrorMsg(errorMsg);
        
        if (DownloadStatusEnum.SUCCESS.getStatus().equals(status)) {
            updateDO.setDownloadTime(LocalDateTime.now());
        }
        
        invoiceDownloadMapper.updateById(updateDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveInvoiceFile(Long downloadId, String invoiceNumber, String fileType, 
                               String filePath, String fileName, Long fileSize, String fileMd5) {
        InvoiceFileDO fileDO = InvoiceFileDO.builder()
                .downloadId(downloadId)
                .invoiceNumber(invoiceNumber)
                .fileType(fileType)
                .filePath(filePath)
                .fileName(fileName)
                .fileSize(fileSize)
                .fileMd5(fileMd5)
                .uploadTime(LocalDateTime.now())
                .accessCount(0)
                .build();

        invoiceFileMapper.insert(fileDO);
        return fileDO.getId();
    }

    /**
     * 执行下载
     */
    private InvoiceDownloadRespVO executeDownload(InvoiceDownloadDO downloadDO) {
        try {
            // 更新状态为下载中
            updateDownloadStatus(downloadDO.getId(), DownloadStatusEnum.DOWNLOADING.getStatus(), null);

            // 构建文件路径
            String fileName = buildFileName(downloadDO.getInvoiceNumber(), "PDF");
            String filePath = buildFilePath(fileName);

            // 下载文件
            long fileSize = HttpUtil.downloadFile(downloadDO.getDownloadUrl(), FileUtil.file(filePath));
            
            // 计算文件MD5
            String fileMd5 = DigestUtil.md5Hex(FileUtil.file(filePath));

            // 保存文件记录
            saveInvoiceFile(downloadDO.getId(), downloadDO.getInvoiceNumber(), "PDF", 
                          filePath, fileName, fileSize, fileMd5);

            // 更新下载记录
            InvoiceDownloadDO updateDO = new InvoiceDownloadDO();
            updateDO.setId(downloadDO.getId());
            updateDO.setFilePath(filePath);
            updateDO.setFileName(fileName);
            updateDO.setFileSize(fileSize);
            updateDO.setDownloadStatus(DownloadStatusEnum.SUCCESS.getStatus());
            updateDO.setDownloadTime(LocalDateTime.now());
            updateDO.setErrorMsg(null);
            invoiceDownloadMapper.updateById(updateDO);

            log.info("发票下载成功，downloadId: {}, filePath: {}", downloadDO.getId(), filePath);
            
            // 重新查询并返回
            InvoiceDownloadDO updatedDO = invoiceDownloadMapper.selectById(downloadDO.getId());
            return buildDownloadRespVO(updatedDO);

        } catch (Exception e) {
            log.error("发票下载失败，downloadId: {}, downloadUrl: {}", downloadDO.getId(), downloadDO.getDownloadUrl(), e);
            
            // 更新重试次数和错误信息
            InvoiceDownloadDO updateDO = new InvoiceDownloadDO();
            updateDO.setId(downloadDO.getId());
            updateDO.setDownloadStatus(DownloadStatusEnum.FAILED.getStatus());
            updateDO.setRetryCount(downloadDO.getRetryCount() + 1);
            updateDO.setErrorMsg(e.getMessage());
            invoiceDownloadMapper.updateById(updateDO);

            throw new ServiceException(INVOICE_DOWNLOAD_FAILED.getCode(), e.getMessage());
        }
    }

    /**
     * 构建文件名
     */
    private String buildFileName(String invoiceNumber, String fileType) {
        return String.format("invoice_%s_%s.%s", 
                invoiceNumber, 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")),
                fileType.toLowerCase());
    }

    /**
     * 构建文件路径
     */
    private String buildFilePath(String fileName) {
        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String dir = String.format("%s/%s/%s", fileBasePath, INVOICE_DIR, dateDir);
        
        // 确保目录存在
        FileUtil.mkdir(dir);
        
        return String.format("%s/%s", dir, fileName);
    }

    /**
     * 构建下载响应VO
     */
    private InvoiceDownloadRespVO buildDownloadRespVO(InvoiceDownloadDO downloadDO) {
        return buildDownloadRespVO(downloadDO,
                invoiceFileMapper.selectListByDownloadId(downloadDO.getId()));
    }

    /**
     * 构建下载响应VO（文件列表由调用方给：单条查询自己查，列表查询批量查）
     */
    private InvoiceDownloadRespVO buildDownloadRespVO(InvoiceDownloadDO downloadDO, List<InvoiceFileDO> files) {
        InvoiceDownloadRespVO respVO = InvoiceDownloadConvert.INSTANCE.convert(downloadDO);

        // 设置状态名称
        DownloadStatusEnum statusEnum = getDownloadStatusEnum(downloadDO.getDownloadStatus());
        if (statusEnum != null) {
            respVO.setDownloadStatusName(statusEnum.getName());
        }

        respVO.setFiles(InvoiceDownloadConvert.INSTANCE.convertFileList(files));
        return respVO;
    }

    /**
     * 获取下载状态枚举
     */
    private DownloadStatusEnum getDownloadStatusEnum(Integer status) {
        for (DownloadStatusEnum statusEnum : DownloadStatusEnum.values()) {
            if (statusEnum.getStatus().equals(status)) {
                return statusEnum;
            }
        }
        return null;
    }

    /**
     * 更新文件访问记录
     */
    private void updateFileAccessRecord(Long fileId) {
        InvoiceFileDO updateDO = new InvoiceFileDO();
        updateDO.setId(fileId);
        updateDO.setLastAccessTime(LocalDateTime.now());
        // 这里应该使用原子操作更新访问次数，简化处理直接+1
        InvoiceFileDO fileDO = invoiceFileMapper.selectById(fileId);
        if (fileDO != null) {
            updateDO.setAccessCount(fileDO.getAccessCount() + 1);
        }
        invoiceFileMapper.updateById(updateDO);
    }

} 
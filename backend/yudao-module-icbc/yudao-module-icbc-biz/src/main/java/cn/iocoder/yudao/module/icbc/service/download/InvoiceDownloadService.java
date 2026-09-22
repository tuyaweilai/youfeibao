package cn.iocoder.yudao.module.icbc.service.download;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.download.vo.InvoiceDownloadPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.download.vo.InvoiceDownloadReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.download.vo.InvoiceDownloadRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.download.InvoiceDownloadDO;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 工行发票下载服务接口
 *
 * @author 芋道源码
 */
public interface InvoiceDownloadService {

    /**
     * 创建发票下载任务
     *
     * @param invoiceOrderId 发票订单ID
     * @param partnerOrderId 合作方订单号
     * @param orderNumber 工行订单号
     * @param invoiceNumber 发票号码
     * @param downloadUrl 下载URL
     * @return 下载记录ID
     */
    Long createDownloadTask(Long invoiceOrderId, String partnerOrderId, String orderNumber, 
                           String invoiceNumber, String downloadUrl);

    /**
     * 执行发票下载
     *
     * @param reqVO 下载请求
     * @return 下载结果
     */
    InvoiceDownloadRespVO downloadInvoice(InvoiceDownloadReqVO reqVO);

    /**
     * 查询发票下载记录
     *
     * @param partnerOrderId 合作方订单号
     * @return 下载记录
     */
    InvoiceDownloadRespVO getDownloadRecord(String partnerOrderId);

    /**
     * 根据发票号码查询下载记录
     *
     * @param invoiceNumber 发票号码
     * @return 下载记录
     */
    InvoiceDownloadRespVO getDownloadRecordByInvoiceNumber(String invoiceNumber);

    /**
     * 分页查询发票下载记录（列表页一次把文件带上，行内可直接下载 PDF）
     *
     * @param reqVO 分页查询条件
     * @return 下载记录分页
     */
    PageResult<InvoiceDownloadRespVO> getDownloadPage(InvoiceDownloadPageReqVO reqVO);

    /**
     * 获取待下载的任务列表
     *
     * @return 待下载任务列表
     */
    List<InvoiceDownloadDO> getPendingDownloadTasks();

    /**
     * 重试下载失败的任务
     *
     * @param downloadId 下载记录ID
     * @return 重试结果
     */
    InvoiceDownloadRespVO retryDownload(Long downloadId);

    /**
     * 下载发票文件到本地
     *
     * @param downloadId 下载记录ID
     * @param fileType 文件类型
     * @param response HTTP响应
     */
    void downloadFile(Long downloadId, String fileType, HttpServletResponse response);

    /**
     * 更新下载状态
     *
     * @param downloadId 下载记录ID
     * @param status 下载状态
     * @param errorMsg 错误信息
     */
    void updateDownloadStatus(Long downloadId, Integer status, String errorMsg);

    /**
     * 保存发票文件信息
     *
     * @param downloadId 下载记录ID
     * @param invoiceNumber 发票号码
     * @param fileType 文件类型
     * @param filePath 文件路径
     * @param fileName 文件名称
     * @param fileSize 文件大小
     * @param fileMd5 文件MD5
     * @return 文件记录ID
     */
    Long saveInvoiceFile(Long downloadId, String invoiceNumber, String fileType, 
                        String filePath, String fileName, Long fileSize, String fileMd5);

} 
package cn.iocoder.yudao.module.icbc.service.download;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.controller.admin.download.vo.InvoiceDownloadReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.download.vo.InvoiceDownloadRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.download.InvoiceDownloadDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.download.InvoiceDownloadMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.download.InvoiceFileMapper;
import cn.iocoder.yudao.module.icbc.enums.DownloadStatusEnum;
import cn.iocoder.yudao.module.icbc.service.download.impl.InvoiceDownloadServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * {@link InvoiceDownloadServiceImpl} 的单元测试类
 *
 * @author 芋道源码
 */
@Import(InvoiceDownloadServiceImpl.class)
@TestPropertySource(properties = "yudao.file.base-path=/tmp/test")
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@Transactional
public class InvoiceDownloadServiceImplTest extends BaseDbUnitTest {

    @Resource
    private InvoiceDownloadService invoiceDownloadService;

    @Resource
    private InvoiceDownloadMapper invoiceDownloadMapper;
    
    @Resource
    private InvoiceFileMapper invoiceFileMapper;

    @Test
    public void testCreateDownloadTask_success() {
        // 准备参数
        Long invoiceOrderId = 1L;
        String partnerOrderId = "ORDER_20231201_001";
        String orderNumber = "ICBC_20231201_001";
        String invoiceNumber = "12345678901234567890";
        String downloadUrl = "https://example.com/invoice.pdf";

        // 调用
        Long downloadId = invoiceDownloadService.createDownloadTask(invoiceOrderId, partnerOrderId, 
                orderNumber, invoiceNumber, downloadUrl);

        // 断言
        assertNotNull(downloadId);
        
        // 验证数据库记录
        InvoiceDownloadDO downloadDO = invoiceDownloadMapper.selectById(downloadId);
        assertNotNull(downloadDO);
        assertEquals(invoiceOrderId, downloadDO.getInvoiceOrderId());
        assertEquals(partnerOrderId, downloadDO.getPartnerOrderId());
        assertEquals(orderNumber, downloadDO.getOrderNumber());
        assertEquals(invoiceNumber, downloadDO.getInvoiceNumber());
        assertEquals(downloadUrl, downloadDO.getDownloadUrl());
        assertEquals(DownloadStatusEnum.PENDING.getStatus(), downloadDO.getDownloadStatus());
        assertEquals(0, downloadDO.getRetryCount());
    }

    @Test
    public void testCreateDownloadTask_exists() {
        // 准备参数
        String partnerOrderId = "ORDER_20231201_001";
        InvoiceDownloadDO existingDO = randomPojo(InvoiceDownloadDO.class, o -> {
            o.setPartnerOrderId(partnerOrderId);
            o.setDownloadStatus(DownloadStatusEnum.SUCCESS.getStatus());
        });
        invoiceDownloadMapper.insert(existingDO);

        // 调用
        Long downloadId = invoiceDownloadService.createDownloadTask(1L, partnerOrderId, 
                "ICBC_20231201_001", "12345678901234567890", "https://example.com/invoice.pdf");

        // 断言 - 返回已存在的记录ID
        assertEquals(existingDO.getId(), downloadId);
    }

    @Test
    public void testGetDownloadRecord_success() {
        // 准备参数
        String partnerOrderId = "ORDER_20231201_001";
        InvoiceDownloadDO downloadDO = randomPojo(InvoiceDownloadDO.class, o -> {
            o.setPartnerOrderId(partnerOrderId);
            o.setDownloadStatus(DownloadStatusEnum.SUCCESS.getStatus());
        });
        invoiceDownloadMapper.insert(downloadDO);

        // 调用
        InvoiceDownloadRespVO result = invoiceDownloadService.getDownloadRecord(partnerOrderId);

        // 断言
        assertNotNull(result);
        assertEquals(downloadDO.getId(), result.getId());
        assertEquals(partnerOrderId, result.getPartnerOrderId());
        assertEquals("下载成功", result.getDownloadStatusName());
    }

    @Test
    public void testUpdateDownloadStatus_success() {
        // 准备参数
        InvoiceDownloadDO downloadDO = randomPojo(InvoiceDownloadDO.class, o -> {
            o.setDownloadStatus(DownloadStatusEnum.PENDING.getStatus());
        });
        invoiceDownloadMapper.insert(downloadDO);

        // 调用
        invoiceDownloadService.updateDownloadStatus(downloadDO.getId(), 
                DownloadStatusEnum.SUCCESS.getStatus(), null);

        // 断言
        InvoiceDownloadDO updatedDO = invoiceDownloadMapper.selectById(downloadDO.getId());
        assertEquals(DownloadStatusEnum.SUCCESS.getStatus(), updatedDO.getDownloadStatus());
        assertNotNull(updatedDO.getDownloadTime());
    }

    @Test
    public void testSaveInvoiceFile_success() {
        // 准备参数
        InvoiceDownloadDO downloadDO = randomPojo(InvoiceDownloadDO.class);
        invoiceDownloadMapper.insert(downloadDO);

        // 调用
        Long fileId = invoiceDownloadService.saveInvoiceFile(downloadDO.getId(), 
                "12345678901234567890", "PDF", "/tmp/test/invoice.pdf", 
                "invoice.pdf", 1024000L, "d41d8cd98f00b204e9800998ecf8427e");

        // 断言
        assertNotNull(fileId);
    }

} 
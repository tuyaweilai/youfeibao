package cn.iocoder.yudao.module.icbc.service.evidence.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.evidence.vo.AcquisitionLedgerRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.evidence.vo.EvidenceChainRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.evidence.vo.EvidenceFlowRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.evidence.vo.EvidenceSourceRespVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.EVIDENCE_EXPORT_FAILED;

/**
 * 一票一档证据包的落盘/落流工具：把一个已装配好的证据包写成 zip，或把台账写成 Excel。
 *
 * <p>只做「数据结构 → 输出格式」的转换，不查库、不认识业务流程；取数由
 * {@code InvoiceEvidenceServiceImpl} 负责。这样导出格式变化（比如加 XML、换压缩方式）
 * 不会牵动证据链的装配逻辑。
 */
@Slf4j
@Component
public class EvidencePackageWriter {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 写一个证据包 zip：每张票一个目录。
     *
     * @param response HTTP 响应
     * @param baseName zip 文件名（不含扩展名）
     * @param packages 各票的证据包内容
     */
    public void writeZip(HttpServletResponse response, String baseName, List<PackageContent> packages) {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition",
                "attachment;filename=" + HttpUtils.encodeUtf8(baseName + ".zip"));
        Set<String> usedFolders = new HashSet<>();
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (PackageContent content : packages) {
                String folder = uniqueFolder(content.getFolderName(), usedFolders);
                putTextEntry(zos, folder + "/证据链.json", JsonUtils.toJsonPrettyString(content.getChain()));
                putTextEntry(zos, folder + "/证据清单.csv", buildEvidenceCsv(content.getChain()));
                putTextEntry(zos, folder + "/收购台账.csv", buildLedgerCsv(content.getLedgerRows()));
                copyLocalFiles(zos, folder, content.getFiles());
            }
        } catch (IOException e) {
            log.error("导出证据包失败", e);
            throw exception(EVIDENCE_EXPORT_FAILED);
        }
    }

    /**
     * 写收购台账 Excel。
     */
    public void writeLedgerExcel(HttpServletResponse response, List<AcquisitionLedgerRespVO> rows) {
        try {
            ExcelUtils.write(response, "收购台账.xls", "收购台账", AcquisitionLedgerRespVO.class, rows);
        } catch (IOException e) {
            log.error("导出收购台账失败", e);
            throw exception(EVIDENCE_EXPORT_FAILED);
        }
    }

    private void copyLocalFiles(ZipOutputStream zos, String folder, List<EvidenceFileRef> files) throws IOException {
        for (EvidenceFileRef ref : files) {
            File local = new File(ref.getFilePath());
            if (!local.exists() || !local.isFile()) {
                continue; // 外部 URL 形态的原件不入包，证据清单里已列地址
            }
            zos.putNextEntry(new ZipEntry(folder + "/发票/" + safeName(ref.getFileName())));
            Files.copy(local.toPath(), zos);
            zos.closeEntry();
        }
    }

    private String buildEvidenceCsv(EvidenceChainRespVO chain) {
        StringBuilder sb = new StringBuilder("\ufeff");
        sb.append("流,来源类型,标题,引用,文件地址,发生时间\n");
        for (EvidenceFlowRespVO flow : chain.getFlows()) {
            for (EvidenceSourceRespVO source : flow.getSources()) {
                sb.append(csvRow(flow.getFlowName(), source.getSourceType(), source.getTitle(),
                        source.getRef(), source.getUrl(), formatTime(source.getOccurredTime())));
            }
        }
        return sb.toString();
    }

    private String buildLedgerCsv(List<AcquisitionLedgerRespVO> rows) {
        StringBuilder sb = new StringBuilder("\ufeff");
        sb.append("交易时间,交易地点,出售者姓名,出售者联系方式,报废产品名称,规格型号,数量,计量单位,含税单价,金额,发票号码,合作方订单号\n");
        for (AcquisitionLedgerRespVO row : rows) {
            sb.append(csvRow(formatTime(row.getTradeTime()), row.getTradeAddress(),
                    row.getSellerName(), row.getSellerMobile(),
                    row.getProductName(), row.getSpecification(),
                    plain(row.getQuantity()), row.getUnit(),
                    plain(row.getUnitPrice()), plain(row.getAmount()),
                    row.getInvoiceNo(), row.getPartnerOrderId()));
        }
        return sb.toString();
    }

    private String formatTime(LocalDateTime time) {
        return time != null ? time.format(TIME_FORMATTER) : "";
    }

    private String plain(Object number) {
        return number != null ? number.toString() : "";
    }

    private String csvRow(String... cells) {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                row.append(',');
            }
            String cell = cells[i] == null ? "" : cells[i];
            row.append('"').append(cell.replace("\"", "\"\"")).append('"');
        }
        return row.append('\n').toString();
    }

    private void putTextEntry(ZipOutputStream zos, String name, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        // 不能关闭 writer：它会连带关闭底层的 ZipOutputStream，后续 entry 就无法写入
        OutputStreamWriter writer = new OutputStreamWriter(zos, StandardCharsets.UTF_8);
        writer.write(content);
        writer.flush();
        zos.closeEntry();
    }

    private String uniqueFolder(String folderName, Set<String> used) {
        String base = safeName(StrUtil.blankToDefault(folderName, "未命名"));
        String folder = base;
        int index = 1;
        while (!used.add(folder)) {
            folder = base + "_" + index++;
        }
        return folder;
    }

    private String safeName(String name) {
        return name == null ? "未命名" : name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     * 一张票的证据包内容。装配由调用方完成，本类只负责写。
     */
    public static class PackageContent {

        private final String folderName;
        private final EvidenceChainRespVO chain;
        private final List<AcquisitionLedgerRespVO> ledgerRows;
        private final List<EvidenceFileRef> files;

        public PackageContent(String folderName, EvidenceChainRespVO chain,
                              List<AcquisitionLedgerRespVO> ledgerRows, List<EvidenceFileRef> files) {
            this.folderName = folderName;
            this.chain = chain;
            this.ledgerRows = ledgerRows;
            this.files = files;
        }

        public String getFolderName() {
            return folderName;
        }

        public EvidenceChainRespVO getChain() {
            return chain;
        }

        public List<AcquisitionLedgerRespVO> getLedgerRows() {
            return ledgerRows;
        }

        public List<EvidenceFileRef> getFiles() {
            return files;
        }
    }

    /**
     * 一个待入包的本地文件。
     */
    public static class EvidenceFileRef {

        private final String fileName;
        private final String filePath;

        public EvidenceFileRef(String fileName, String filePath) {
            this.fileName = fileName;
            this.filePath = filePath;
        }

        public String getFileName() {
            return fileName;
        }

        public String getFilePath() {
            return filePath;
        }
    }

}

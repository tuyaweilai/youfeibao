package cn.iocoder.yudao.module.icbc.service.esign;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.dal.dataobject.agreement.IcbcFrameworkAgreementDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.esign.IcbcEsignConfigDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.agreement.IcbcFrameworkAgreementMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.FrameworkAgreementSignMethodEnum;
import cn.iocoder.yudao.module.icbc.enums.FrameworkAgreementStatusEnum;
import cn.iocoder.yudao.module.icbc.service.esign.impl.FrameworkAgreementEsignServiceImpl;
import cn.iocoder.yudao.test.icbc.IcbcTenantTestConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link FrameworkAgreementEsignServiceImpl} 的单元测试（#95，ADR 0036）。
 *
 * <p>出站端口 {@link EsignPort} 用 {@code @MockBean} 替换、不触网；断言的是业务可观察行为：
 * 一个合同组两份文书、发起后协议停在待签署、回调整体签完才生效且可重放、旧生效协议作废、
 * 签署链接现生成现用。
 */
@Import({FrameworkAgreementEsignServiceImpl.class, UnitTestConfiguration.class, IcbcTenantTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class FrameworkAgreementEsignServiceImplTest extends BaseDbUnitTest {

    private static final Long TENANT_ID = 1L;

    @Resource
    private FrameworkAgreementEsignService frameworkAgreementEsignService;
    @Resource
    private IcbcFrameworkAgreementMapper frameworkAgreementMapper;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;

    @MockBean
    private EsignPort esignPort;
    @MockBean
    private EsignTenantService esignTenantService;
    @MockBean
    private EsignConfigService esignConfigService;

    @BeforeEach
    public void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    // ==================== 发起：两份文书一个合同组 ====================

    @Test
    public void testInitiate_sendsTwoDocumentsInOneGroupAndStoresTaskId() {
        PayeeInfoDO payee = insertPayee("张三", "13800000001");
        IcbcFrameworkAgreementDO agreement = insertAgreement(payee.getId(), "废钢",
                FrameworkAgreementSignMethodEnum.ELECTRONIC.getCode(), FrameworkAgreementStatusEnum.PENDING.getStatus());
        when(esignConfigService.getRawConfig()).thenReturn(IcbcEsignConfigDO.builder()
                .agreementTemplateId("TPL_AGREEMENT").noticeTemplateId("TPL_NOTICE").build());
        when(esignPort.initiate(eq(TENANT_ID), any())).thenReturn(EsignPort.EsignTask.builder()
                .signTaskId("TASK-1").build());

        String signTaskId = frameworkAgreementEsignService.initiate(TENANT_ID, agreement.getId());

        assertEquals("TASK-1", signTaskId);
        // 任务号写回协议、状态仍是待签署、没盖签署时间
        IcbcFrameworkAgreementDO stored = frameworkAgreementMapper.selectById(agreement.getId());
        assertEquals("TASK-1", stored.getSignTaskId());
        assertEquals(FrameworkAgreementStatusEnum.PENDING.getStatus(), stored.getStatus());
        assertNull(stored.getSignedAt());
        // 发起成功消耗一份合同额度
        verify(esignTenantService).consumeContract();

        // 一次发起、两份文书、同一合同组（发起方是回收企业 = tenantId 入参）
        ArgumentCaptor<EsignPort.EsignRequest> captor = ArgumentCaptor.forClass(EsignPort.EsignRequest.class);
        verify(esignPort).initiate(eq(TENANT_ID), captor.capture());
        EsignPort.EsignRequest request = captor.getValue();
        assertEquals(payee.getId(), request.getPayeeId());
        assertEquals(2, request.getDocuments().size(), "框架收购协议 + 反向发票合规告知函，两份一个合同组");
        assertEquals(Arrays.asList("框架收购协议", "反向发票合规告知函"),
                Arrays.asList(request.getDocuments().get(0).getName(), request.getDocuments().get(1).getName()));
        assertEquals("TPL_AGREEMENT", request.getDocuments().get(0).getTemplateId(), "模板由平台维护，发起时只带模板号");
        assertEquals("TPL_NOTICE", request.getDocuments().get(1).getTemplateId());
        assertEquals("废钢", request.getDocuments().get(0).getVariables().get("productName"));
        assertEquals("张三", request.getDocuments().get(0).getVariables().get("sellerName"));
        // 唯一签署方是自然人本人；回收企业是发起方、用租户级企业印章，不在签署方列表里（ADR 0036 决策 5）
        assertEquals("张三", request.getSigner().getName());
        assertEquals("13800000001", request.getSigner().getMobile());
        assertEquals(payee.getIdCardNo(), request.getSigner().getIdCardNo());
    }

    @Test
    public void testInitiate_whenPortReturnsNoTask_failsAndLeavesNoTaskId() {
        PayeeInfoDO payee = insertPayee("李四", "13800000002");
        IcbcFrameworkAgreementDO agreement = insertAgreement(payee.getId(), "废纸",
                FrameworkAgreementSignMethodEnum.ELECTRONIC.getCode(), FrameworkAgreementStatusEnum.PENDING.getStatus());
        when(esignConfigService.getRawConfig()).thenReturn(null);
        when(esignPort.initiate(anyLong(), any())).thenReturn(EsignPort.EsignTask.empty());

        assertServiceException(() -> frameworkAgreementEsignService.initiate(TENANT_ID, agreement.getId()),
                ESIGN_INITIATE_FAILED);
        assertNull(frameworkAgreementMapper.selectById(agreement.getId()).getSignTaskId(),
                "拿不到任务号就不该留下一个生成不出签署链接的半成品");
    }

    // ==================== 去签署：现生成现用 ====================

    @Test
    public void testCreateSignUrl_generatedOnDemandAndNeverReused() {
        PayeeInfoDO payee = insertPayee("王五", "13800000003");
        IcbcFrameworkAgreementDO agreement = insertAgreement(payee.getId(), "废铝",
                FrameworkAgreementSignMethodEnum.ELECTRONIC.getCode(), FrameworkAgreementStatusEnum.PENDING.getStatus());
        agreement.setSignTaskId("TASK-2");
        frameworkAgreementMapper.updateById(agreement);
        when(esignPort.createSignUrl(eq(TENANT_ID), eq("TASK-2"), any()))
                .thenReturn("https://esign/sign?t=1", "https://esign/sign?t=2");

        String first = frameworkAgreementEsignService.createSignUrl(payee.getId());
        String second = frameworkAgreementEsignService.createSignUrl(payee.getId());

        assertEquals("https://esign/sign?t=1", first);
        assertEquals("https://esign/sign?t=2", second);
        // 两次点击各生成一次新链接，不缓存、不复用
        verify(esignPort, times(2)).createSignUrl(eq(TENANT_ID), eq("TASK-2"), any());
    }

    @Test
    public void testCreateSignUrl_whenNotPending_readableError() {
        PayeeInfoDO payee = insertPayee("赵六", "13800000004");
        // 只有一份已生效的协议，没有待签署的
        insertAgreement(payee.getId(), "废铜",
                FrameworkAgreementSignMethodEnum.PAPER.getCode(), FrameworkAgreementStatusEnum.EFFECTIVE.getStatus());

        assertServiceException(() -> frameworkAgreementEsignService.createSignUrl(payee.getId()),
                ESIGN_AGREEMENT_NOT_PENDING, "生效");
    }

    @Test
    public void testCreateSignUrl_whenTaskIdMissing_readableError() {
        PayeeInfoDO payee = insertPayee("钱七", "13800000005");
        insertAgreement(payee.getId(), "废铁",
                FrameworkAgreementSignMethodEnum.ELECTRONIC.getCode(), FrameworkAgreementStatusEnum.PENDING.getStatus());

        assertServiceException(() -> frameworkAgreementEsignService.createSignUrl(payee.getId()),
                ESIGN_SIGN_TASK_ID_MISSING);
    }

    // ==================== 回调：生效 + 作废 + 幂等 ====================

    @Test
    public void testApplyFinishedCallback_promotesPendingAndVoidsOldEffective() {
        PayeeInfoDO payee = insertPayee("孙八", "13800000006");
        IcbcFrameworkAgreementDO oldEffective = insertAgreement(payee.getId(), "旧协议",
                FrameworkAgreementSignMethodEnum.PAPER.getCode(), FrameworkAgreementStatusEnum.EFFECTIVE.getStatus());
        IcbcFrameworkAgreementDO pending = insertAgreement(payee.getId(), "新协议",
                FrameworkAgreementSignMethodEnum.ELECTRONIC.getCode(), FrameworkAgreementStatusEnum.PENDING.getStatus());
        pending.setSignTaskId("TASK-3");
        frameworkAgreementMapper.updateById(pending);
        LocalDateTime signedAt = LocalDateTime.of(2026, 9, 21, 10, 0);
        when(esignPort.listSignedDocuments(TENANT_ID, "TASK-3")).thenReturn(Arrays.asList(
                EsignPort.SignedDocument.builder().name("反向发票合规告知函")
                        .fileUrl("https://esign/doc-notice").signedAt(signedAt).build(),
                EsignPort.SignedDocument.builder().name("框架收购协议")
                        .fileUrl("https://esign/doc-agreement").signedAt(signedAt).build()));

        frameworkAgreementEsignService.applyFinishedCallback(callback("TASK-3", true, signedAt));

        IcbcFrameworkAgreementDO promoted = frameworkAgreementMapper.selectById(pending.getId());
        assertEquals(FrameworkAgreementStatusEnum.EFFECTIVE.getStatus(), promoted.getStatus());
        assertEquals(signedAt, promoted.getSignedAt(), "回调把签署时间盖到协议上");
        assertEquals("https://esign/doc-agreement", promoted.getFileUrl(),
                "协议文件取框架收购协议那一份（主文书）");
        assertEquals("https://esign/doc-notice", promoted.getNoticeFileUrl(),
                "告知函单独落址：两份文书在证据链上分别成条，不拼成一个 PDF");
        // 新协议生效时旧生效协议作废、历史可查
        assertEquals(FrameworkAgreementStatusEnum.VOIDED.getStatus(),
                frameworkAgreementMapper.selectById(oldEffective.getId()).getStatus());
        assertEquals(2, frameworkAgreementMapper.selectListByPayeeId(payee.getId()).size(), "两条都在，只是旧的那条作废");
    }

    @Test
    public void testApplyFinishedCallback_replayHasNoSecondSideEffect() {
        PayeeInfoDO payee = insertPayee("周九", "13800000007");
        IcbcFrameworkAgreementDO oldEffective = insertAgreement(payee.getId(), "旧协议",
                FrameworkAgreementSignMethodEnum.PAPER.getCode(), FrameworkAgreementStatusEnum.EFFECTIVE.getStatus());
        IcbcFrameworkAgreementDO pending = insertAgreement(payee.getId(), "新协议",
                FrameworkAgreementSignMethodEnum.ELECTRONIC.getCode(), FrameworkAgreementStatusEnum.PENDING.getStatus());
        pending.setSignTaskId("TASK-4");
        frameworkAgreementMapper.updateById(pending);
        LocalDateTime signedAt = LocalDateTime.of(2026, 9, 21, 11, 0);
        when(esignPort.listSignedDocuments(TENANT_ID, "TASK-4")).thenReturn(Arrays.asList(
                EsignPort.SignedDocument.builder().name("框架收购协议")
                        .fileUrl("https://esign/doc").signedAt(signedAt).build(),
                EsignPort.SignedDocument.builder().name("反向发票合规告知函")
                        .fileUrl("https://esign/doc-notice").signedAt(signedAt).build()));

        frameworkAgreementEsignService.applyFinishedCallback(callback("TASK-4", true, signedAt));
        // 同一条通知再来一遍
        frameworkAgreementEsignService.applyFinishedCallback(callback("TASK-4", true, signedAt));

        // 只有一次副作用：只查了一次文件、只盖了一次时间
        verify(esignPort, times(1)).listSignedDocuments(TENANT_ID, "TASK-4");
        IcbcFrameworkAgreementDO promoted = frameworkAgreementMapper.selectById(pending.getId());
        assertEquals(FrameworkAgreementStatusEnum.EFFECTIVE.getStatus(), promoted.getStatus());
        assertEquals(signedAt, promoted.getSignedAt());
        assertEquals(FrameworkAgreementStatusEnum.VOIDED.getStatus(),
                frameworkAgreementMapper.selectById(oldEffective.getId()).getStatus());
    }

    @Test
    public void testApplyFinishedCallback_unfinishedDoesNotChangeStatus() {
        PayeeInfoDO payee = insertPayee("吴十", "13800000008");
        IcbcFrameworkAgreementDO pending = insertAgreement(payee.getId(), "新协议",
                FrameworkAgreementSignMethodEnum.ELECTRONIC.getCode(), FrameworkAgreementStatusEnum.PENDING.getStatus());
        pending.setSignTaskId("TASK-5");
        frameworkAgreementMapper.updateById(pending);

        frameworkAgreementEsignService.applyFinishedCallback(callback("TASK-5", false, null));

        IcbcFrameworkAgreementDO stored = frameworkAgreementMapper.selectById(pending.getId());
        assertEquals(FrameworkAgreementStatusEnum.PENDING.getStatus(), stored.getStatus(), "没签完就还是待签署");
        assertNull(stored.getSignedAt());
        verify(esignPort, never()).listSignedDocuments(anyLong(), anyString());
    }

    @Test
    public void testApplyFinishedCallback_unknownTaskIsExplicitFailure() {
        assertServiceException(() -> frameworkAgreementEsignService.applyFinishedCallback(
                        callback("TASK-UNKNOWN", true, LocalDateTime.now())),
                ESIGN_AGREEMENT_NOT_FOUND, "TASK-UNKNOWN");
    }

    @Test
    public void testApplyFinishedCallback_replayFillsFileUrlWhenThirdPartyFilesWereNotReadyYet() {
        // 回调早于第三方文件可查：本次拿不到文件地址，但协议已生效。
        // 重放时必须补取，否则幂等短路会让证据链永久缺 URL（#95 评审 SP-5）。
        PayeeInfoDO payee = insertPayee("冯十二", "13800000012");
        IcbcFrameworkAgreementDO pending = insertAgreement(payee.getId(), "废钢",
                FrameworkAgreementSignMethodEnum.ELECTRONIC.getCode(), FrameworkAgreementStatusEnum.PENDING.getStatus());
        pending.setSignTaskId("TASK-7");
        frameworkAgreementMapper.updateById(pending);
        LocalDateTime signedAt = LocalDateTime.of(2026, 9, 21, 12, 0);
        // 第一次通知：第三方还没生成文件
        when(esignPort.listSignedDocuments(TENANT_ID, "TASK-7"))
                .thenReturn(Collections.emptyList(), Arrays.asList(
                        EsignPort.SignedDocument.builder().name("框架收购协议")
                                .fileUrl("https://esign/doc-late").signedAt(signedAt).build(),
                        EsignPort.SignedDocument.builder().name("反向发票合规告知函")
                                .fileUrl("https://esign/doc-notice-late").signedAt(signedAt).build()));

        frameworkAgreementEsignService.applyFinishedCallback(callback("TASK-7", true, signedAt));
        IcbcFrameworkAgreementDO afterFirst = frameworkAgreementMapper.selectById(pending.getId());
        assertEquals(FrameworkAgreementStatusEnum.EFFECTIVE.getStatus(), afterFirst.getStatus());
        assertNull(afterFirst.getFileUrl(), "文件还没可查，先如实留空");

        // 重放：已生效，但文件地址缺，补取一次（不改状态 / 不重盖时间）
        frameworkAgreementEsignService.applyFinishedCallback(callback("TASK-7", true, signedAt));
        IcbcFrameworkAgreementDO afterReplay = frameworkAgreementMapper.selectById(pending.getId());
        assertEquals("https://esign/doc-late", afterReplay.getFileUrl(), "重放要把缺的主文书地址补回来");
        assertEquals("https://esign/doc-notice-late", afterReplay.getNoticeFileUrl());
        assertEquals(signedAt, afterReplay.getSignedAt(), "补地址不得改签署时间");
        verify(esignPort, times(2)).listSignedDocuments(TENANT_ID, "TASK-7");
    }

    // ==================== 已签文书查询 ====================

    @Test
    public void testListSignedDocuments_onlyForEffectiveElectronicAgreement() {
        PayeeInfoDO payee = insertPayee("郑十一", "13800000009");
        IcbcFrameworkAgreementDO effective = insertAgreement(payee.getId(), "废钢",
                FrameworkAgreementSignMethodEnum.ELECTRONIC.getCode(), FrameworkAgreementStatusEnum.EFFECTIVE.getStatus());
        effective.setSignTaskId("TASK-6");
        frameworkAgreementMapper.updateById(effective);
        when(esignPort.listSignedDocuments(TENANT_ID, "TASK-6")).thenReturn(List.of(
                EsignPort.SignedDocument.builder().name("框架收购协议")
                        .fileUrl("https://esign/doc").signedAt(LocalDateTime.now()).build()));

        List<EsignPort.SignedDocument> documents = frameworkAgreementEsignService.listSignedDocuments(payee.getId());

        assertEquals(1, documents.size());
        assertEquals("https://esign/doc", documents.get(0).getFileUrl());
    }

    @Test
    public void testListSignedDocuments_emptyForPaperOrMissingTaskIdOrNoEffective() {
        // S-2：名字里的 `only` 不能只验正例——纸协议 / 空任务号 / 没有生效协议都必须回空且不触第三方
        PayeeInfoDO paperPayee = insertPayee("陈十三", "13800000013");
        insertAgreement(paperPayee.getId(), "废纸",
                FrameworkAgreementSignMethodEnum.PAPER.getCode(), FrameworkAgreementStatusEnum.EFFECTIVE.getStatus());
        assertTrue(frameworkAgreementEsignService.listSignedDocuments(paperPayee.getId()).isEmpty(),
                "纸协议已签完，不归电子签署文书接口管");

        PayeeInfoDO noTaskPayee = insertPayee("褚十四", "13800000014");
        insertAgreement(noTaskPayee.getId(), "废铁",
                FrameworkAgreementSignMethodEnum.ELECTRONIC.getCode(), FrameworkAgreementStatusEnum.EFFECTIVE.getStatus());
        assertTrue(frameworkAgreementEsignService.listSignedDocuments(noTaskPayee.getId()).isEmpty(),
                "没有任务号就查不到文书，别拿空任务号去问第三方");

        PayeeInfoDO noAgreementPayee = insertPayee("卫十五", "13800000015");
        assertTrue(frameworkAgreementEsignService.listSignedDocuments(noAgreementPayee.getId()).isEmpty(),
                "没有生效协议就是空列表");

        // 三条短路都不应该触网
        verify(esignPort, never()).listSignedDocuments(anyLong(), anyString());
    }

    // ==================== 助手 ====================

    private EsignPort.EsignCallback callback(String signTaskId, boolean finished, LocalDateTime signedAt) {
        return EsignPort.EsignCallback.builder()
                .tenantId(TENANT_ID)
                .signTaskId(signTaskId)
                .finished(finished)
                .unfinishedReason(finished ? null : "已过期")
                .signedAt(signedAt)
                .build();
    }

    private PayeeInfoDO insertPayee(String name, String mobile) {
        PayeeInfoDO payee = PayeeInfoDO.builder()
                .name(name).mobile(mobile).address("北京市朝阳区")
                .idCardNo("11010119900101" + mobile.substring(7))
                .partnerPayeeId("PARTNER_" + mobile)
                .build();
        payeeInfoMapper.insert(payee);
        return payee;
    }

    private IcbcFrameworkAgreementDO insertAgreement(Long payeeId, String productName, String signMethod,
                                                     Integer status) {
        IcbcFrameworkAgreementDO agreement = IcbcFrameworkAgreementDO.builder()
                .payeeId(payeeId)
                .agreementNo("FW" + System.nanoTime())
                .productName(productName)
                .quantity("5 吨")
                .specification("重型")
                .recyclePeriod("2026 年 9 月第 1 期")
                .settlementMethod("银行转账")
                .signMethod(signMethod)
                .status(status)
                .build();
        frameworkAgreementMapper.insert(agreement);
        return agreement;
    }

}

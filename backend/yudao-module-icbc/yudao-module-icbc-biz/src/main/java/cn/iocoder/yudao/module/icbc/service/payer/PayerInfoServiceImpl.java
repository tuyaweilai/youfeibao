package cn.iocoder.yudao.module.icbc.service.payer;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.payer.vo.*;
import cn.iocoder.yudao.module.icbc.convert.payer.PayerInfoConvert;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payer.PayerInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.payer.PayerInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 工行付方信息 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class PayerInfoServiceImpl implements PayerInfoService {

    @Resource
    private PayerInfoMapper payerInfoMapper;

    @Override
    public Long createPayerInfo(PayerInfoSaveReqVO createReqVO) {
        // 1. 归一信用代码 / 税号：它们是全局唯一键，比对与落库必须说同一个值
        createReqVO.setCreditCode(normalizePayerKey(createReqVO.getCreditCode()));
        createReqVO.setTaxNo(normalizePayerKey(createReqVO.getTaxNo()));

        // 2. 校验唯一性：先本租户（读得到就直说），再跨租户（不点名别家），最后软删行
        validatePayerInfoUnique(null, createReqVO.getCreditCode(), createReqVO.getTaxNo());
        validatePayerNotRegisteredByOtherTenant(null, createReqVO.getCreditCode(), createReqVO.getTaxNo());
        validatePayerKeyNotHeldByDeletedRow(null, createReqVO.getCreditCode(), createReqVO.getTaxNo());
        // 2.1 合作方付方编号是**客户端可传**的合法输入（与工行约定的子商户编号），也是个全局唯一键：
        //     填重了（含跨租户、含软删行）要给可读错误，不能等到 insert 撞键才落成 500
        validatePartnerPayerIdNotHeld(null, createReqVO.getPartnerPayerId());

        // 3. 生成合作方付方编号
        if (createReqVO.getPartnerPayerId() == null) {
            createReqVO.setPartnerPayerId(generatePartnerPayerId());
        }

        // 4. 插入（唯一键兜底并发：预检与插入之间有窗口）
        PayerInfoDO payerInfo = PayerInfoConvert.INSTANCE.convert(createReqVO);
        insertPayerInfo(payerInfo);
        return payerInfo.getId();
    }

    @Override
    public void updatePayerInfo(PayerInfoSaveReqVO updateReqVO) {
        // 1. 校验存在
        validatePayerInfoExists(updateReqVO.getId());
        // 2. 归一信用代码 / 税号
        updateReqVO.setCreditCode(normalizePayerKey(updateReqVO.getCreditCode()));
        updateReqVO.setTaxNo(normalizePayerKey(updateReqVO.getTaxNo()));
        // 3. 校验唯一性：本租户 + 跨租户 + 软删行
        validatePayerInfoUnique(updateReqVO.getId(), updateReqVO.getCreditCode(), updateReqVO.getTaxNo());
        validatePayerNotRegisteredByOtherTenant(updateReqVO.getId(), updateReqVO.getCreditCode(), updateReqVO.getTaxNo());
        validatePayerKeyNotHeldByDeletedRow(updateReqVO.getId(), updateReqVO.getCreditCode(), updateReqVO.getTaxNo());
        validatePartnerPayerIdNotHeld(updateReqVO.getId(), updateReqVO.getPartnerPayerId());

        // 4. 更新（唯一键兜底并发）
        PayerInfoDO updateObj = PayerInfoConvert.INSTANCE.convert(updateReqVO);
        updatePayerInfoById(updateObj);
    }

    @Override
    public void deletePayerInfo(Long id) {
        // 校验存在
        validatePayerInfoExists(id);
        // 删除
        payerInfoMapper.deleteById(id);
    }

    @Override
    public void deletePayerInfos(Collection<Long> ids) {
        // 批量删除
        payerInfoMapper.deleteBatchIds(ids);
    }

    private void validatePayerInfoExists(Long id) {
        if (payerInfoMapper.selectById(id) == null) {
            throw exception(PAYER_INFO_NOT_EXISTS);
        }
    }

    private void validatePayerInfoUnique(Long id, String creditCode, String taxNo) {
        // 1. 校验统一社会信用代码唯一性
        PayerInfoDO payerInfo = payerInfoMapper.selectByCreditCode(creditCode);
        if (payerInfo != null && !payerInfo.getId().equals(id)) {
            throw exception(PAYER_INFO_CREDIT_CODE_EXISTS);
        }

        // 2. 校验纳税人识别号唯一性
        payerInfo = payerInfoMapper.selectByTaxNo(taxNo);
        if (payerInfo != null && !payerInfo.getId().equals(id)) {
            throw exception(PAYER_INFO_TAX_NO_EXISTS);
        }
    }

    /**
     * 跨租户唯一性预检：信用代码 / 税号是**全局唯一键**（ADR 0005 补充「一家公司只能是一个租户」，
     * 所以一家公司不可能在两个租户下各有一份付方档案）。
     *
     * <p>租户插件默认只让当前租户看到自己的行，跨租户要显式开阀（{@code TenantUtils.executeIgnore}）。
     * 命中的是**别家企业**的付方档案——同一租户内的重复由 {@link #validatePayerInfoUnique} 先挡掉，
     * 所以这里读不到本租户的行就意味着冲突来自别家企业。
     *
     * <p>文案只说「已被另一家企业登记为付方」，**不点名**是哪一家企业（不回租户名 / 租户编号）：
     * 跨租户只暴露「这个信用代码 / 税号被占用了」这一条业务事实，不泄露别家企业的存在与身份，
     * 与 #91 把跨租户作废链接做成「当作不存在」是同一条透明度口径。要处理只能找平台运营。
     */
    private void validatePayerNotRegisteredByOtherTenant(Long id, String creditCode, String taxNo) {
        PayerInfoDO byCreditCode = TenantUtils.executeIgnore(() -> payerInfoMapper.selectByCreditCode(creditCode));
        if (byCreditCode != null && !byCreditCode.getId().equals(id)) {
            throw exception(PAYER_CREDIT_CODE_REGISTERED_ELSEWHERE);
        }
        PayerInfoDO byTaxNo = TenantUtils.executeIgnore(() -> payerInfoMapper.selectByTaxNo(taxNo));
        if (byTaxNo != null && !byTaxNo.getId().equals(id)) {
            throw exception(PAYER_TAX_NO_REGISTERED_ELSEWHERE);
        }
    }

    /**
     * 软删行的预检：唯一键 {@code uk_credit_code} / {@code uk_tax_no} **不含 {@code deleted}**
     * （生产建表脚本 `icbc_payer_info.sql`），软删行会**永久占住**那个值；而 {@code @TableLogic}
     * 让普通查询无条件追加 {@code AND deleted = 0}，前两道预检都看不到软删行
     * （{@code TenantUtils.executeIgnore} 只关租户过滤、不关逻辑删除过滤）。
     *
     * <p>不纳入这一步的话，「建 → 软删 → 再建同一个值」会在预检处被判「可用」，然后直接撞唯一键；
     * 兜底回读也仍然说「可用」，最终落成裸 {@code DuplicateKeyException}（500）——预检与唯一键
     * 对「这一行算不算存在」必须说同一句话。
     *
     * <p>文案与 {@link #validatePayerNotRegisteredByOtherTenant} 区分开：那条是「另一家企业占着」，
     * 这条是「曾经登记过、已删除」，可执行动作是找平台运营恢复。两条都不点名是哪一家企业、
     * 不回租户名 / 租户编号。
     */
    private void validatePayerKeyNotHeldByDeletedRow(Long id, String creditCode, String taxNo) {
        PayerInfoDO byCreditCode = TenantUtils.executeIgnore(
                () -> payerInfoMapper.selectByCreditCodeIncludeDeleted(creditCode));
        if (byCreditCode != null && !byCreditCode.getId().equals(id)
                && Boolean.TRUE.equals(byCreditCode.getDeleted())) {
            throw exception(PAYER_CREDIT_CODE_ALREADY_REGISTERED_AND_DELETED);
        }
        PayerInfoDO byTaxNo = TenantUtils.executeIgnore(
                () -> payerInfoMapper.selectByTaxNoIncludeDeleted(taxNo));
        if (byTaxNo != null && !byTaxNo.getId().equals(id)
                && Boolean.TRUE.equals(byTaxNo.getDeleted())) {
            throw exception(PAYER_TAX_NO_ALREADY_REGISTERED_AND_DELETED);
        }
    }

    /**
     * 合作方付方编号的预检：它是**全局唯一键** {@code uk_partner_payer_id}，且是客户端可传的输入。
     *
     * <p>与信用代码 / 税号不同，它不是「找运营恢复」那种值——它是与工行约定的**子商户编号**，
     * 客户端完全有能力换一个，所以命中就给「已被占用，请换一个」（不区分是本租户还是别家、
     * 也不区分是否软删：可执行动作都是换一个，区分反而多泄露一层存在性）。文案同样不点名企业。
     *
     * <p>用 {@link PayerInfoMapper#selectByPartnerPayerIdIncludeDeleted} 查**唯一键实际覆盖的集合**
     * （含软删行，{@code @TableLogic} 看不到它们），并在 {@code TenantUtils.executeIgnore} 里跑
     * （唯一键是全局的，跨租户也要撞）。传 null 时直接跳过——{@code create} 会为 null 生成一个新值，
     * 构造上不会撞。
     */
    private void validatePartnerPayerIdNotHeld(Long id, String partnerPayerId) {
        if (partnerPayerId == null) {
            return;
        }
        PayerInfoDO holder = TenantUtils.executeIgnore(
                () -> payerInfoMapper.selectByPartnerPayerIdIncludeDeleted(partnerPayerId));
        if (holder != null && !holder.getId().equals(id)) {
            throw exception(PAYER_PARTNER_PAYER_ID_EXISTS);
        }
    }

    /**
     * 归一信用代码 / 税号：去首尾空白 + 转大写。
     *
     * <p>生产库的排序规则是 {@code utf8mb4_unicode_ci}（大小写不敏感），同一个代码的大小写差异本来
     * 就是同一个值；测试库（H2）默认大小写敏感。不归一的话，「同一个税号的大小写差异」会在测试里
     * 绕过预检、又在生产里被唯一键拦住——两边行为不一致。归一到规范形状后，预检与唯一键说的是同一个值。
     */
    private static String normalizePayerKey(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 插入付方档案，并把并发下的唯一键冲突翻译成可读错误。
     *
     * <p>预检与插入之间有窗口：并发下另一家企业可能刚提交同一个信用代码 / 税号 / 合作方付方编号。
     * 唯一键是兜底，撞上时不抛裸 {@code DuplicateKeyException}（500），而是回读一次确认后给同一条
     * 可读错误。这与 {@code NaturalPersonServiceImpl#register} 的「服务层预检 + 唯一键兜底」同一条模式。
     */
    private void insertPayerInfo(PayerInfoDO payerInfo) {
        try {
            payerInfoMapper.insert(payerInfo);
        } catch (DuplicateKeyException e) {
            throwReadablePayerDuplicateOrRethrow(e, null, payerInfo.getCreditCode(), payerInfo.getTaxNo(),
                    payerInfo.getPartnerPayerId());
        }
    }

    /** 更新付方档案，唯一键兜底同 {@link #insertPayerInfo}。 */
    private void updatePayerInfoById(PayerInfoDO updateObj) {
        try {
            payerInfoMapper.updateById(updateObj);
        } catch (DuplicateKeyException e) {
            throwReadablePayerDuplicateOrRethrow(e, updateObj.getId(), updateObj.getCreditCode(),
                    updateObj.getTaxNo(), updateObj.getPartnerPayerId());
        }
    }

    /**
     * 唯一键冲突 → 可读错误 的兜底翻译；确认不了是什么冲突（同租户并发之外的键、或并发行又回滚了）
     * 就**原样抛出**，不吞、不猜。
     *
     * <p>{@code partnerPayerId} 只在**客户端真传了值**时才可能撞键：内部生成的
     * （{@link #generatePartnerPayerId} / {@code addPayerToIcbc}）构造上全局唯一；传 null 时这里直接跳过。
     */
    private void throwReadablePayerDuplicateOrRethrow(DuplicateKeyException cause, Long id,
                                                      String creditCode, String taxNo, String partnerPayerId) {
        validatePayerInfoUnique(id, creditCode, taxNo);
        validatePayerNotRegisteredByOtherTenant(id, creditCode, taxNo);
        validatePayerKeyNotHeldByDeletedRow(id, creditCode, taxNo);
        validatePartnerPayerIdNotHeld(id, partnerPayerId);
        throw cause;
    }

    private String generatePartnerPayerId() {
        return "PAYER_" + IdUtil.fastSimpleUUID();
    }

    @Override
    public PayerInfoDO getPayerInfo(Long id) {
        return payerInfoMapper.selectById(id);
    }

    @Override
    public PayerInfoDO getPayerInfoByCreditCode(String creditCode) {
        return payerInfoMapper.selectByCreditCode(normalizePayerKey(creditCode));
    }

    @Override
    public PayerInfoDO getPayerInfoByTaxNo(String taxNo) {
        return payerInfoMapper.selectByTaxNo(normalizePayerKey(taxNo));
    }

    @Override
    public List<PayerInfoDO> getPayerInfoList(Collection<Long> ids) {
        return payerInfoMapper.selectBatchIds(ids);
    }

    @Override
    public List<PayerInfoDO> getPayerInfoList(PayerInfoExportReqVO exportReqVO) {
        return payerInfoMapper.selectList(exportReqVO);
    }

    @Override
    public PageResult<PayerInfoDO> getPayerInfoPage(PayerInfoPageReqVO pageReqVO) {
        return payerInfoMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addPayerToIcbc(PayerAddReqVO reqVO) {
        // 1. 归一 + 数据校验（本租户 + 跨租户）
        reqVO.setCreditCode(normalizePayerKey(reqVO.getCreditCode()));
        reqVO.setTaxNo(normalizePayerKey(reqVO.getTaxNo()));
        validateCreditCodeAndTaxNo(reqVO.getCreditCode(), reqVO.getTaxNo());

        // 2. 生成合作方付方编号
        String partnerPayerId = generatePartnerPayerId();

        // 3. 构建付方信息对象
        PayerInfoDO payerInfo = PayerInfoConvert.INSTANCE.convert(reqVO);
        payerInfo.setPartnerPayerId(partnerPayerId);
        payerInfo.setStatus(0); // 待审核
        payerInfo.setPayerNo(generatePayerNo()); // 生成工行付方编号

        // 4. 调用工行付方新增接口（模拟）
        // TODO: 实际实现中，这里应该调用工行的API接口
        // 示例: String payerNo = icbcApiService.addPayer(payerInfo);
        // 日志只记生成的合作方付方编号：PayerInfoDO 里有银行账户、联系人手机号、地址，
        // 整对象 toString 会把它们写进日志（PII）。
        log.info("[addPayerToIcbc] 调用工行付方新增接口：partnerPayerId={}", payerInfo.getPartnerPayerId());
        
        // 5. 保存到数据库（唯一键兜底并发）
        insertPayerInfo(payerInfo);

        // 6. 返回ID
        return payerInfo.getId();
    }

    @Override
    public PayerInfoDO queryPayerFromIcbc(PayerQueryReqVO reqVO) {
        // 1. 先从本地数据库查询（信用代码按归一后的值查，和落库时同一个形状）
        PayerInfoDO payerInfo = payerInfoMapper.selectByCreditCode(normalizePayerKey(reqVO.getCreditCode()));
        if (payerInfo != null) {
            return payerInfo;
        }

        // 2. 调用工行付方查询接口（模拟）
        // TODO: 实际实现中，这里应该调用工行的API接口
        // 示例: PayerInfoDO result = icbcApiService.queryPayer(reqVO.getCreditCode(), reqVO.getTaxNo());
        log.info("[queryPayerFromIcbc] 调用工行付方查询接口：creditCode={}, taxNo={}", reqVO.getCreditCode(), reqVO.getTaxNo());

        // 3. 如果不存在，抛出异常
        throw exception(PAYER_INFO_NOT_EXISTS);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePayerAuditCallback(String payerNo, Integer status, String auditMsg, String payerStatus) {
        // 1. 查询付方信息
        PayerInfoDO payerInfo = payerInfoMapper.selectOne(PayerInfoDO::getPayerNo, payerNo);
        if (payerInfo == null) {
            log.error("[handlePayerAuditCallback] 付方信息不存在：payerNo={}", payerNo);
            throw exception(PAYER_INFO_NOT_EXISTS);
        }

        // 2. 更新审核状态
        PayerInfoDO updateObj = new PayerInfoDO();
        updateObj.setId(payerInfo.getId());
        updateObj.setStatus(status);
        updateObj.setAuditMsg(auditMsg);
        updateObj.setIcbcPayerStatus(payerStatus);

        // 3. 更新数据库
        payerInfoMapper.updateById(updateObj);
        log.info("[handlePayerAuditCallback] 更新付方审核状态成功：payerNo={}, status={}", payerNo, status);
    }

    /**
     * 校验统一社会信用代码和纳税人识别号：本租户唯一 + 跨租户唯一。
     *
     * @param creditCode 统一社会信用代码（已归一）
     * @param taxNo      纳税人识别号（已归一）
     */
    private void validateCreditCodeAndTaxNo(String creditCode, String taxNo) {
        validatePayerInfoUnique(null, creditCode, taxNo);
        validatePayerNotRegisteredByOtherTenant(null, creditCode, taxNo);
        validatePayerKeyNotHeldByDeletedRow(null, creditCode, taxNo);
    }

    /**
     * 生成工行付方编号
     *
     * @return 工行付方编号
     */
    private String generatePayerNo() {
        return "P" + System.currentTimeMillis() + RandomUtil.randomNumbers(6);
    }
} 
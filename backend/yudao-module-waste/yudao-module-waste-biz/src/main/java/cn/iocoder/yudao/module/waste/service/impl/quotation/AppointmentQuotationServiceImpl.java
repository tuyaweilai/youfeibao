package cn.iocoder.yudao.module.waste.service.impl.quotation;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.waste.controller.admin.quotation.vo.AppointmentQuotationCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.quotation.vo.AppointmentQuotationPageReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.quotation.vo.AppointmentQuotationRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.quotation.vo.AppointmentQuotationUpdateReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.quotation.AppointmentQuotationDO;
import cn.iocoder.yudao.module.waste.dal.mysql.quotation.AppointmentQuotationMapper;
import cn.iocoder.yudao.module.waste.service.quotation.AppointmentQuotationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.waste.enums.ErrorCodeConstants.APPOINTMENT_QUOTATION_NOT_EXISTS;

/**
 * 预约报价记录 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class AppointmentQuotationServiceImpl implements AppointmentQuotationService {

    @Resource
    private AppointmentQuotationMapper appointmentQuotationMapper;

    @Override
    public Long createAppointmentQuotation(@Valid AppointmentQuotationCreateReqVO createReqVO) {
        // 插入
        AppointmentQuotationDO appointmentQuotation = BeanUtils.toBean(createReqVO, AppointmentQuotationDO.class);
        appointmentQuotationMapper.insert(appointmentQuotation);
        // 返回
        return appointmentQuotation.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAppointmentQuotation(@Valid AppointmentQuotationUpdateReqVO updateReqVO) {
        // 校验存在
        validateAppointmentQuotationExists(updateReqVO.getId());
        // 更新
        AppointmentQuotationDO updateObj = BeanUtils.toBean(updateReqVO, AppointmentQuotationDO.class);
        appointmentQuotationMapper.updateById(updateObj);
    }

    @Override
    public void deleteAppointmentQuotation(Long id) {
        // 校验存在
        validateAppointmentQuotationExists(id);
        // 删除
        appointmentQuotationMapper.deleteById(id);
    }

    @Override
    public AppointmentQuotationDO getAppointmentQuotation(Long id) {
        return appointmentQuotationMapper.selectById(id);
    }

    @Override
    public AppointmentQuotationRespVO getAppointmentQuotationDetail(Long id) {
        AppointmentQuotationDO appointmentQuotation = validateAppointmentQuotationExists(id);
        return BeanUtils.toBean(appointmentQuotation, AppointmentQuotationRespVO.class);
    }

    @Override
    public PageResult<AppointmentQuotationRespVO> getAppointmentQuotationPage(AppointmentQuotationPageReqVO pageReqVO) {
        PageResult<AppointmentQuotationDO> pageResult = appointmentQuotationMapper.selectPage(pageReqVO);
        return BeanUtils.toBean(pageResult, AppointmentQuotationRespVO.class);
    }

    @Override
    public List<AppointmentQuotationDO> getAppointmentQuotationList(AppointmentQuotationPageReqVO exportReqVO) {
        return appointmentQuotationMapper.selectList(exportReqVO);
    }

    @Override
    public List<AppointmentQuotationDO> getAppointmentQuotationListByAppointmentId(Long appointmentId) {
        return appointmentQuotationMapper.selectListByAppointmentId(appointmentId);
    }

    @Override
    public List<AppointmentQuotationDO> getAppointmentQuotationListByRecyclingEnterpriseId(Long recyclingEnterpriseId) {
        return appointmentQuotationMapper.selectListByRecyclingEnterpriseId(recyclingEnterpriseId);
    }

    @Override
    public List<AppointmentQuotationDO> getAppointmentQuotationListByStatus(Integer status) {
        return appointmentQuotationMapper.selectListByStatus(status);
    }

    @Override
    public List<AppointmentQuotationDO> getValidAppointmentQuotations() {
        return appointmentQuotationMapper.selectValidQuotations();
    }

    @Override
    public List<AppointmentQuotationDO> getExpiredAppointmentQuotations() {
        return appointmentQuotationMapper.selectExpiredQuotations();
    }

    @Override
    public List<AppointmentQuotationDO> getAcceptedAppointmentQuotations() {
        return appointmentQuotationMapper.selectAcceptedQuotations();
    }

    @Override
    public List<AppointmentQuotationDO> getAppointmentQuotationListByQuotationTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return appointmentQuotationMapper.selectListByQuotationTimeRange(startTime, endTime);
    }

    @Override
    public AppointmentQuotationDO getLatestAppointmentQuotationByAppointmentId(Long appointmentId) {
        return appointmentQuotationMapper.selectLatestByAppointmentId(appointmentId);
    }

    @Override
    public AppointmentQuotationDO getLowestPriceQuotationByAppointmentId(Long appointmentId) {
        return appointmentQuotationMapper.selectLowestPriceByAppointmentId(appointmentId);
    }

    @Override
    public AppointmentQuotationDO getHighestPriceQuotationByAppointmentId(Long appointmentId) {
        return appointmentQuotationMapper.selectHighestPriceByAppointmentId(appointmentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitQuotation(Long appointmentId, Long recyclerEnterpriseId, BigDecimal quotedPrice, String quotationNotes) {
        AppointmentQuotationDO quotation = new AppointmentQuotationDO();
        quotation.setAppointmentId(appointmentId);
        quotation.setRecyclingEnterpriseId(recyclerEnterpriseId);
        quotation.setQuotedPrice(quotedPrice);
        quotation.setQuotationRemark(quotationNotes);
        quotation.setValidUntil(LocalDateTime.now().plusDays(7)); // 默认7天有效期
        quotation.setStatus(1); // 待处理
        
        appointmentQuotationMapper.insert(quotation);
        log.info("[submitQuotation][提交报价] appointmentId={}, recyclerEnterpriseId={}, quotedPrice={}", 
                appointmentId, recyclerEnterpriseId, quotedPrice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitQuotation(Long appointmentId, Long recyclingEnterpriseId, BigDecimal quotedPrice, 
                               String quotationRemark, LocalDateTime validUntil) {
        AppointmentQuotationDO quotation = new AppointmentQuotationDO();
        quotation.setAppointmentId(appointmentId);
        quotation.setRecyclingEnterpriseId(recyclingEnterpriseId);
        quotation.setQuotedPrice(quotedPrice);
        quotation.setQuotationRemark(quotationRemark);
        quotation.setValidUntil(validUntil != null ? validUntil : LocalDateTime.now().plusDays(7));
        quotation.setStatus(1); // 待处理
        
        appointmentQuotationMapper.insert(quotation);
        log.info("[submitQuotation][提交报价] appointmentId={}, recyclingEnterpriseId={}, quotedPrice={}, validUntil={}", 
                appointmentId, recyclingEnterpriseId, quotedPrice, validUntil);
        return quotation.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptQuotation(Long id) {
        AppointmentQuotationDO quotation = validateAppointmentQuotationExists(id);
        quotation.setStatus(2); // 已接受
        quotation.setAcceptedTime(LocalDateTime.now());
        appointmentQuotationMapper.updateById(quotation);
        
        log.info("[acceptQuotation][接受报价] id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptQuotation(Long id, String acceptedBy, String acceptReason) {
        AppointmentQuotationDO quotation = validateAppointmentQuotationExists(id);
        quotation.setStatus(2); // 已接受
        quotation.setAcceptedTime(LocalDateTime.now());
        appointmentQuotationMapper.updateById(quotation);
        
        log.info("[acceptQuotation][接受报价] id={}, acceptedBy={}, acceptReason={}", id, acceptedBy, acceptReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectQuotation(Long id, String rejectionReason) {
        AppointmentQuotationDO quotation = validateAppointmentQuotationExists(id);
        quotation.setStatus(3); // 已拒绝
        quotation.setRejectionReason(rejectionReason);
        appointmentQuotationMapper.updateById(quotation);
        
        log.info("[rejectQuotation][拒绝报价] id={}, rejectionReason={}", id, rejectionReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectQuotation(Long id, String rejectedBy, String rejectReason) {
        AppointmentQuotationDO quotation = validateAppointmentQuotationExists(id);
        quotation.setStatus(3); // 已拒绝
        quotation.setRejectionReason(rejectReason);
        appointmentQuotationMapper.updateById(quotation);
        
        log.info("[rejectQuotation][拒绝报价] id={}, rejectedBy={}, rejectReason={}", id, rejectedBy, rejectReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawQuotation(Long id, String withdrawnBy, String withdrawReason) {
        AppointmentQuotationDO quotation = validateAppointmentQuotationExists(id);
        quotation.setStatus(4); // 已撤回
        // 注意：AppointmentQuotationDO中没有withdrawTime和withdrawReason字段，这里只设置状态
        appointmentQuotationMapper.updateById(quotation);
        
        log.info("[withdrawQuotation][撤回报价] id={}, withdrawnBy={}, withdrawReason={}", id, withdrawnBy, withdrawReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawQuotation(Long id, String withdrawalReason) {
        AppointmentQuotationDO quotation = validateAppointmentQuotationExists(id);
        quotation.setStatus(4); // 已撤回
        // 注意：AppointmentQuotationDO中没有withdrawTime和withdrawReason字段，这里只设置状态
        appointmentQuotationMapper.updateById(quotation);
        
        log.info("[withdrawQuotation][撤回报价] id={}, withdrawalReason={}", id, withdrawalReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuotationStatus(Long id, Integer status) {
        AppointmentQuotationDO quotation = validateAppointmentQuotationExists(id);
        quotation.setStatus(status);
        appointmentQuotationMapper.updateById(quotation);
        
        log.info("[updateQuotationStatus][更新报价状态] id={}, status={}", id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchExpireQuotations(List<Long> ids) {
        for (Long id : ids) {
            updateQuotationStatus(id, 5); // 已过期
        }
        log.info("[batchExpireQuotations][批量过期报价] ids={}", ids);
    }

    @Override
    public AppointmentQuotationDO validateAppointmentQuotationExists(Long id) {
        AppointmentQuotationDO appointmentQuotation = appointmentQuotationMapper.selectById(id);
        if (appointmentQuotation == null) {
            throw exception(APPOINTMENT_QUOTATION_NOT_EXISTS);
        }
        return appointmentQuotation;
    }

    @Override
    public List<AppointmentQuotationDO> getQuotationsByAppointmentId(Long appointmentId) {
        return appointmentQuotationMapper.selectListByAppointmentId(appointmentId);
    }

    @Override
    public List<AppointmentQuotationDO> getQuotationsByRecyclerEnterpriseId(Long recyclerEnterpriseId) {
        return appointmentQuotationMapper.selectListByRecyclingEnterpriseId(recyclerEnterpriseId);
    }

    @Override
    public List<AppointmentQuotationDO> getValidQuotations(Long appointmentId) {
        return appointmentQuotationMapper.selectValidQuotationsByAppointmentId(appointmentId);
    }

    @Override
    public List<AppointmentQuotationDO> getExpiredQuotations(Long appointmentId) {
        return appointmentQuotationMapper.selectExpiredQuotationsByAppointmentId(appointmentId);
    }

    @Override
    public List<AppointmentQuotationDO> getAcceptedQuotations(Long appointmentId) {
        return appointmentQuotationMapper.selectAcceptedQuotationsByAppointmentId(appointmentId);
    }

} 
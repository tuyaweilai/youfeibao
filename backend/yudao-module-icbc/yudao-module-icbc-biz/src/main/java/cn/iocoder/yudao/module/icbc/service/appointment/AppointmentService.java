package cn.iocoder.yudao.module.icbc.service.appointment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.appointment.vo.AppointmentArriveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.appointment.vo.AppointmentNoShowReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.appointment.vo.AppointmentPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.appointment.vo.AppointmentRespVO;
import cn.iocoder.yudao.module.icbc.controller.app.appointment.vo.AppointmentCancelReqVO;
import cn.iocoder.yudao.module.icbc.controller.app.appointment.vo.AppointmentCreateReqVO;
import cn.iocoder.yudao.module.icbc.controller.app.appointment.vo.AppointmentGoodsRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.appointment.IcbcAppointmentDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 到站预约 Service（#35，ADR 0020）。
 *
 * <p>约定（务必守住）：
 * <ul>
 *   <li>预约**不是订单**：没有接单 / 拒单，只有到场 / 未到场；</li>
 *   <li>不占额度、不产生开票、不进五流；它的全部价值是到站登记时**带出**已有信息；</li>
 *   <li>因此任何统计与额度口径都**不得**引用本 Service 的数据。</li>
 * </ul>
 */
public interface AppointmentService {

    // ==================== 自然人侧 ====================

    /**
     * 自然人发起预约。校验身份绑在当前登录名下，场站码必须能解析出企业与场站。
     */
    Long create(@Valid AppointmentCreateReqVO reqVO);

    /**
     * 自然人取消预约。**只有「待到站」可取消**；到场 / 未到场 / 已取消都是终态。
     */
    void cancel(@Valid AppointmentCancelReqVO reqVO);

    /**
     * 某一自然人主体名下的预约（跨企业，仅本人可见）。
     */
    List<AppointmentRespVO> getListForSeller(Long naturalPersonId);

    /**
     * 可预约的品类（当前租户启用中的品类；自然人端只用名称与单位）。
     */
    List<AppointmentGoodsRespVO> listEnabledGoods();

    // ==================== 现场 / 企业侧 ====================

    /**
     * 某出售者在本租户「待到站」的预约，按预计到站时间升序。
     *
     * <p>这是本功能的**全部价值**所在：收货员登记收购时据此带出品类、约多少、车牌，少填一遍。
     * 匹配不到返回空列表，不造假。
     */
    List<AppointmentRespVO> listPendingForPayee(Long payeeId);

    AppointmentRespVO getAppointment(Long id);

    PageResult<AppointmentRespVO> getPage(AppointmentPageReqVO reqVO);

    /**
     * 标记到场（通常随后建收购单）。重复标记幂等。
     */
    void markArrived(@Valid AppointmentArriveReqVO reqVO);

    /**
     * 标记未到场。
     */
    void markNoShow(@Valid AppointmentNoShowReqVO reqVO);

    /**
     * 按编号取预约（企业内部使用，带租户条件）。
     */
    IcbcAppointmentDO getAppointmentDO(Long id);

}

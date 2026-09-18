package cn.iocoder.yudao.module.icbc.service.payer;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.payer.vo.*;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payer.PayerInfoDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * 工行付方信息 Service 接口
 *
 * @author 芋道源码
 */
public interface PayerInfoService {

    /**
     * 创建付方信息
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createPayerInfo(@Valid PayerInfoSaveReqVO createReqVO);

    /**
     * 更新付方信息
     *
     * @param updateReqVO 更新信息
     */
    void updatePayerInfo(@Valid PayerInfoSaveReqVO updateReqVO);

    /**
     * 删除付方信息
     *
     * @param id 编号
     */
    void deletePayerInfo(Long id);

    /**
     * 批量删除付方信息
     *
     * @param ids 编号集合
     */
    void deletePayerInfos(Collection<Long> ids);

    /**
     * 获得付方信息
     *
     * @param id 编号
     * @return 付方信息
     */
    PayerInfoDO getPayerInfo(Long id);

    /**
     * 根据统一社会信用代码获得付方信息
     *
     * @param creditCode 统一社会信用代码
     * @return 付方信息
     */
    PayerInfoDO getPayerInfoByCreditCode(String creditCode);

    /**
     * 根据纳税人识别号获得付方信息
     *
     * @param taxNo 纳税人识别号
     * @return 付方信息
     */
    PayerInfoDO getPayerInfoByTaxNo(String taxNo);

    /**
     * 获得付方信息列表
     *
     * @param ids 编号
     * @return 付方信息列表
     */
    List<PayerInfoDO> getPayerInfoList(Collection<Long> ids);

    /**
     * 获得付方信息列表，用于导出
     *
     * @param exportReqVO 导出查询
     * @return 付方信息列表
     */
    List<PayerInfoDO> getPayerInfoList(PayerInfoExportReqVO exportReqVO);

    /**
     * 获得付方信息分页
     *
     * @param pageReqVO 分页查询
     * @return 付方信息分页
     */
    PageResult<PayerInfoDO> getPayerInfoPage(PayerInfoPageReqVO pageReqVO);

    /**
     * 工行付方新增接口
     *
     * @param reqVO 付方新增请求
     * @return 付方ID
     */
    Long addPayerToIcbc(@Valid PayerAddReqVO reqVO);

    /**
     * 工行付方查询接口
     *
     * @param reqVO 付方查询请求
     * @return 付方信息
     */
    PayerInfoDO queryPayerFromIcbc(@Valid PayerQueryReqVO reqVO);

    /**
     * 处理付方审核结果回调
     *
     * @param payerNo   付方编号
     * @param status    状态
     * @param auditMsg  审核消息
     * @param payerStatus 付方状态
     */
    void handlePayerAuditCallback(String payerNo, Integer status, String auditMsg, String payerStatus);

} 
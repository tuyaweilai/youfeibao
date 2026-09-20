package cn.iocoder.yudao.module.logistics.service.transportnode;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.*;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transportnode.TransportNodeDO;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 物流运输节点记录 Service 接口
 *
 * @author 芋道源码
 */
public interface TransportNodeService {

    /**
     * 获得物流运输节点记录
     *
     * @param id 编号
     * @return 物流运输节点记录
     */
    TransportNodeDO getTransportNode(Long id);

    /**
     * 获得物流运输节点记录详情
     *
     * @param id 编号
     * @return 物流运输节点记录详情
     */
    TransportNodeRespVO getTransportNodeDetail(Long id);

    /**
     * 获得物流运输节点记录分页
     *
     * @param pageReqVO 分页查询
     * @return 物流运输节点记录分页
     */
    PageResult<TransportNodeRespVO> getTransportNodePage(TransportNodePageReqVO pageReqVO);

    /**
     * 获得物流运输节点记录列表, 用于 Excel 导出
     *
     * @param exportReqVO 查询条件
     * @return 物流运输节点记录列表
     */
    List<TransportNodeDO> getTransportNodeList(TransportNodePageReqVO exportReqVO);

    /**
     * 根据任务ID获得物流运输节点记录列表
     *
     * @param taskId 任务ID
     * @return 物流运输节点记录列表
     */
    List<TransportNodeDO> getTransportNodeListByTaskId(Long taskId);

    /**
     * 根据任务ID获得物流运输节点记录列表（按时间排序）
     *
     * @param taskId 任务ID
     * @return 物流运输节点记录列表
     */
    List<TransportNodeRespVO> getTransportNodeListByTaskIdOrderByTime(Long taskId);

    /**
     * 根据节点类型获得物流运输节点记录列表
     *
     * @param nodeType 节点类型
     * @return 物流运输节点记录列表
     */
    List<TransportNodeDO> getTransportNodeListByNodeType(Integer nodeType);

    /**
     * 根据操作员ID获得物流运输节点记录列表
     *
     * @param operatorId 操作员ID
     * @return 物流运输节点记录列表
     */
    List<TransportNodeDO> getTransportNodeListByOperatorId(Long operatorId);

    /**
     * 获得任务的最新节点记录
     *
     * @param taskId 任务ID
     * @return 最新节点记录
     */
    TransportNodeDO getLatestTransportNodeByTaskId(Long taskId);

    /**
     * 统计任务的节点记录数量
     *
     * @param taskId 任务ID
     * @return 节点记录数量
     */
    Long getTransportNodeCountByTaskId(Long taskId);

    // ========== 内部方法，供其他模块调用 ==========

    /**
     * 创建运输节点记录
     *
     * @param taskId 任务ID
     * @param nodeType 节点类型
     * @param nodeTime 节点时间
     * @param nodeLocation 节点位置
     * @param latitude 纬度
     * @param longitude 经度
     * @param operatorId 操作员ID
     * @param operatorName 操作员姓名
     * @param photos 照片URLs
     * @param additionalData 附加数据
     * @param remark 备注
     * @return 节点记录ID
     */
    Long createTransportNode(Long taskId, Integer nodeType, LocalDateTime nodeTime,
                           String nodeLocation, BigDecimal latitude, BigDecimal longitude,
                           Long operatorId, String operatorName, String photos,
                           String additionalData, String remark);

    /**
     * 创建运输节点记录（简化版）
     *
     * @param taskId 任务ID
     * @param nodeType 节点类型
     * @param operatorId 操作员ID
     * @param operatorName 操作员姓名
     * @param remark 备注
     * @return 节点记录ID
     */
    Long createTransportNode(Long taskId, Integer nodeType, Long operatorId, String operatorName, String remark);

    /**
     * 创建运输节点记录（带位置信息）
     *
     * @param taskId 任务ID
     * @param nodeType 节点类型
     * @param nodeLocation 节点位置
     * @param latitude 纬度
     * @param longitude 经度
     * @param operatorId 操作员ID
     * @param operatorName 操作员姓名
     * @param remark 备注
     * @return 节点记录ID
     */
    Long createTransportNodeWithLocation(Long taskId, Integer nodeType, String nodeLocation,
                                       BigDecimal latitude, BigDecimal longitude,
                                       Long operatorId, String operatorName, String remark);

    /**
     * 创建运输节点记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createTransportNode(@Valid TransportNodeCreateReqVO createReqVO);

    /**
     * 更新运输节点记录
     *
     * @param updateReqVO 更新信息
     */
    void updateTransportNode(@Valid TransportNodeUpdateReqVO updateReqVO);

    /**
     * 删除运输节点记录
     *
     * @param id 编号
     */
    void deleteTransportNode(Long id);

} 
package cn.iocoder.yudao.module.logistics.service.transportnode;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.*;
import cn.iocoder.yudao.module.logistics.convert.transportnode.TransportNodeConvert;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transportnode.TransportNodeDO;
import cn.iocoder.yudao.module.logistics.dal.mysql.transportnode.TransportNodeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 物流运输节点记录 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class TransportNodeServiceImpl implements TransportNodeService {

    @Resource
    private TransportNodeMapper transportNodeMapper;

    @Override
    public TransportNodeDO getTransportNode(Long id) {
        return transportNodeMapper.selectById(id);
    }

    @Override
    public TransportNodeRespVO getTransportNodeDetail(Long id) {
        TransportNodeDO transportNode = getTransportNode(id);
        return TransportNodeConvert.INSTANCE.convert(transportNode);
    }

    @Override
    public PageResult<TransportNodeRespVO> getTransportNodePage(TransportNodePageReqVO pageReqVO) {
        PageResult<TransportNodeDO> pageResult = transportNodeMapper.selectPage(pageReqVO);
        return TransportNodeConvert.INSTANCE.convertPage(pageResult);
    }

    @Override
    public List<TransportNodeDO> getTransportNodeList(TransportNodePageReqVO exportReqVO) {
        return transportNodeMapper.selectList(exportReqVO);
    }

    @Override
    public List<TransportNodeDO> getTransportNodeListByTaskId(Long taskId) {
        return transportNodeMapper.selectByTaskId(taskId);
    }

    @Override
    public List<TransportNodeRespVO> getTransportNodeListByTaskIdOrderByTime(Long taskId) {
        List<TransportNodeDO> list = transportNodeMapper.selectByTaskIdOrderByNodeTime(taskId);
        return TransportNodeConvert.INSTANCE.convertList(list);
    }

    @Override
    public List<TransportNodeDO> getTransportNodeListByNodeType(Integer nodeType) {
        return transportNodeMapper.selectByNodeType(nodeType);
    }

    @Override
    public List<TransportNodeDO> getTransportNodeListByOperatorId(Long operatorId) {
        return transportNodeMapper.selectByOperatorId(operatorId);
    }

    @Override
    public TransportNodeDO getLatestTransportNodeByTaskId(Long taskId) {
        return transportNodeMapper.selectLatestByTaskId(taskId);
    }

    @Override
    public Long getTransportNodeCountByTaskId(Long taskId) {
        return transportNodeMapper.selectCountByTaskId(taskId);
    }

    @Override
    public Long createTransportNode(Long taskId, Integer nodeType, LocalDateTime nodeTime,
                                  String nodeLocation, BigDecimal latitude, BigDecimal longitude,
                                  Long operatorId, String operatorName, String photos,
                                  String additionalData, String remark) {
        // 构建节点记录
        TransportNodeDO transportNode = TransportNodeDO.builder()
                .taskId(taskId)
                .nodeType(nodeType)
                .nodeTime(nodeTime != null ? nodeTime : LocalDateTime.now())
                .nodeLocation(nodeLocation)
                .latitude(latitude)
                .longitude(longitude)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .photos(photos)
                .additionalData(additionalData)
                .remark(remark)
                .build();

        // 插入数据库
        transportNodeMapper.insert(transportNode);
        
        log.info("[createTransportNode][任务({})创建节点记录，类型：{}，操作员：{}]", 
                taskId, nodeType, operatorName);
        
        return transportNode.getId();
    }

    @Override
    public Long createTransportNode(Long taskId, Integer nodeType, Long operatorId, String operatorName, String remark) {
        return createTransportNode(taskId, nodeType, LocalDateTime.now(), 
                null, null, null, operatorId, operatorName, null, null, remark);
    }

    @Override
    public Long createTransportNodeWithLocation(Long taskId, Integer nodeType, String nodeLocation,
                                              BigDecimal latitude, BigDecimal longitude,
                                              Long operatorId, String operatorName, String remark) {
        return createTransportNode(taskId, nodeType, LocalDateTime.now(), 
                nodeLocation, latitude, longitude, operatorId, operatorName, null, null, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTransportNode(@Valid TransportNodeCreateReqVO createReqVO) {
        // 手动构建DO对象
        TransportNodeDO transportNode = new TransportNodeDO();
        transportNode.setTaskId(createReqVO.getTaskId());
        transportNode.setNodeType(createReqVO.getNodeType());
        transportNode.setNodeTime(createReqVO.getNodeTime());
        transportNode.setNodeLocation(createReqVO.getLocation());
        transportNode.setLatitude(createReqVO.getLatitude());
        transportNode.setLongitude(createReqVO.getLongitude());
        transportNode.setOperatorId(createReqVO.getOperatorId());
        transportNode.setOperatorName(createReqVO.getOperatorName());
        transportNode.setPhotos(createReqVO.getPhotos());
        transportNode.setAdditionalData(createReqVO.getAdditionalData());
        transportNode.setRemark(createReqVO.getRemark());
        
        transportNodeMapper.insert(transportNode);
        
        log.info("[createTransportNode][创建运输节点记录成功，任务ID：{}，节点类型：{}，操作员：{}]", 
                createReqVO.getTaskId(), createReqVO.getNodeType(), createReqVO.getOperatorName());
        
        return transportNode.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTransportNode(@Valid TransportNodeUpdateReqVO updateReqVO) {
        // 校验存在
        validateTransportNodeExists(updateReqVO.getId());
        
        // 手动构建更新对象
        TransportNodeDO updateObj = new TransportNodeDO();
        updateObj.setId(updateReqVO.getId());
        updateObj.setTaskId(updateReqVO.getTaskId());
        updateObj.setNodeType(updateReqVO.getNodeType());
        updateObj.setNodeTime(updateReqVO.getNodeTime());
        updateObj.setNodeLocation(updateReqVO.getLocation());
        updateObj.setLatitude(updateReqVO.getLatitude());
        updateObj.setLongitude(updateReqVO.getLongitude());
        updateObj.setOperatorId(updateReqVO.getOperatorId());
        updateObj.setOperatorName(updateReqVO.getOperatorName());
        updateObj.setPhotos(updateReqVO.getPhotos());
        updateObj.setAdditionalData(updateReqVO.getAdditionalData());
        updateObj.setRemark(updateReqVO.getRemark());
        
        transportNodeMapper.updateById(updateObj);
        
        log.info("[updateTransportNode][更新运输节点记录成功，ID：{}，操作员：{}]", 
                updateReqVO.getId(), updateReqVO.getOperatorName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTransportNode(Long id) {
        // 校验存在
        validateTransportNodeExists(id);
        
        // 删除
        transportNodeMapper.deleteById(id);
        
        log.info("[deleteTransportNode][删除运输节点记录成功，ID：{}]", id);
    }

    private TransportNodeDO validateTransportNodeExists(Long id) {
        TransportNodeDO transportNode = transportNodeMapper.selectById(id);
        if (transportNode == null) {
            throw new RuntimeException("运输节点记录不存在");
        }
        return transportNode;
    }

} 
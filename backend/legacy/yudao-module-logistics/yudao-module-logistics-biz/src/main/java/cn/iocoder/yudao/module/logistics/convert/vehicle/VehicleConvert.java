package cn.iocoder.yudao.module.logistics.convert.vehicle;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.*;
import cn.iocoder.yudao.module.logistics.dal.dataobject.vehicle.VehicleDO;
import cn.iocoder.yudao.module.logistics.enums.VehicleStatusEnum;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车辆信息 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface VehicleConvert {

    VehicleConvert INSTANCE = Mappers.getMapper(VehicleConvert.class);

    @Mapping(target = "vehiclePhotos", source = "vehiclePhotos", qualifiedByName = "listToJson")
    @Mapping(target = "licensePhotos", source = "licensePhotos", qualifiedByName = "listToJson")
    VehicleDO convert(VehicleCreateReqVO bean);

    @Mapping(target = "vehiclePhotos", source = "vehiclePhotos", qualifiedByName = "listToJson")
    @Mapping(target = "licensePhotos", source = "licensePhotos", qualifiedByName = "listToJson")
    VehicleDO convert(VehicleUpdateReqVO bean);

    @Mapping(target = "vehiclePhotos", source = "vehiclePhotos", qualifiedByName = "jsonToList")
    @Mapping(target = "licensePhotos", source = "licensePhotos", qualifiedByName = "jsonToList")
    @Mapping(target = "statusName", source = "status", qualifiedByName = "statusToName")
    VehicleRespVO convert(VehicleDO bean);

    @Mapping(target = "statusName", source = "status", qualifiedByName = "statusToName")
    VehicleExcelVO convertExcel(VehicleDO bean);

    List<VehicleRespVO> convertList(List<VehicleDO> list);

    List<VehicleExcelVO> convertExcelList(List<VehicleDO> list);

    PageResult<VehicleRespVO> convertPage(PageResult<VehicleDO> page);

    @Named("listToJson")
    default String listToJson(List<String> list) {
        return CollUtil.isEmpty(list) ? null : JsonUtils.toJsonString(list);
    }

    @Named("jsonToList")
    default List<String> jsonToList(String json) {
        return JsonUtils.parseArray(json, String.class);
    }

    @Named("statusToName")
    default String statusToName(Integer status) {
        if (status == null) {
            return null;
        }
        for (VehicleStatusEnum statusEnum : VehicleStatusEnum.values()) {
            if (statusEnum.getStatus().equals(status)) {
                return statusEnum.getName();
            }
        }
        return "未知状态";
    }

} 
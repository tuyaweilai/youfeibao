package cn.iocoder.yudao.module.logistics.convert.driver;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.*;
import cn.iocoder.yudao.module.logistics.dal.dataobject.driver.DriverQualificationDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 司机资质信息 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface DriverQualificationConvert {

    DriverQualificationConvert INSTANCE = Mappers.getMapper(DriverQualificationConvert.class);

    @Mapping(target = "driverLicensePhotos", expression = "java(convertListToJson(bean.getDriverLicensePhotos()))")
    @Mapping(target = "qualificationCertPhotos", expression = "java(convertListToJson(bean.getQualificationCertPhotos()))")
    @Mapping(target = "hazardousCertPhotos", expression = "java(convertListToJson(bean.getHazardousCertPhotos()))")
    DriverQualificationDO convert(DriverQualificationCreateReqVO bean);

    @Mapping(target = "driverLicensePhotos", expression = "java(convertListToJson(bean.getDriverLicensePhotos()))")
    @Mapping(target = "qualificationCertPhotos", expression = "java(convertListToJson(bean.getQualificationCertPhotos()))")
    @Mapping(target = "hazardousCertPhotos", expression = "java(convertListToJson(bean.getHazardousCertPhotos()))")
    DriverQualificationDO convert(DriverQualificationUpdateReqVO bean);

    @Mapping(target = "driverLicensePhotos", expression = "java(convertJsonToList(bean.getDriverLicensePhotos()))")
    @Mapping(target = "qualificationCertPhotos", expression = "java(convertJsonToList(bean.getQualificationCertPhotos()))")
    @Mapping(target = "hazardousCertPhotos", expression = "java(convertJsonToList(bean.getHazardousCertPhotos()))")
    DriverQualificationRespVO convert(DriverQualificationDO bean);

    List<DriverQualificationRespVO> convertList(List<DriverQualificationDO> list);

    PageResult<DriverQualificationRespVO> convertPage(PageResult<DriverQualificationDO> page);

    List<DriverQualificationExcelVO> convertExcelList(List<DriverQualificationDO> list);

    /**
     * 将 List<String> 转换为 JSON 字符串
     */
    default String convertListToJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return JsonUtils.toJsonString(list);
    }

    /**
     * 将 JSON 字符串转换为 List<String>
     */
    default List<String> convertJsonToList(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        return JsonUtils.parseArray(json, String.class);
    }

} 
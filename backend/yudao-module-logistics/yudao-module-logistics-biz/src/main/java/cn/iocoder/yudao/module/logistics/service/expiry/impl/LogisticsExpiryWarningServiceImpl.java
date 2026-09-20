package cn.iocoder.yudao.module.logistics.service.expiry.impl;

import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.LogisticsDriverPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.expiry.vo.LogisticsExpiryWarningItemVO;
import cn.iocoder.yudao.module.logistics.controller.admin.vehicle.vo.LogisticsVehiclePageReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.driver.LogisticsDriverDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.vehicle.LogisticsVehicleDO;
import cn.iocoder.yudao.module.logistics.service.driver.LogisticsDriverService;
import cn.iocoder.yudao.module.logistics.service.expiry.LogisticsExpiryWarningService;
import cn.iocoder.yudao.module.logistics.service.vehicle.LogisticsVehicleService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 证件到期提醒 Service 实现（V3 #70）。
 *
 * <p>**「过期」的判定只有一处**：{@code LogisticsVehicleService#isDocumentExpired} /
 * {@code LogisticsDriverService#isDocumentExpired}。这里只负责把「快到期」也算进来并排好序——
 * 两处各写一套判定，就一定会出现「提醒说没事、派车被拦」这种最难查的裂缝。
 */
@Service
@Validated
public class LogisticsExpiryWarningServiceImpl implements LogisticsExpiryWarningService {

    /** 对象类型：车辆 */
    private static final int SUBJECT_VEHICLE = 1;
    /** 对象类型：司机 */
    private static final int SUBJECT_DRIVER = 2;

    @Resource
    private LogisticsVehicleService logisticsVehicleService;
    @Resource
    private LogisticsDriverService logisticsDriverService;

    @Override
    public List<LogisticsExpiryWarningItemVO> getWarningList(int days) {
        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(days);
        List<LogisticsExpiryWarningItemVO> items = new ArrayList<>();

        for (LogisticsVehicleDO vehicle : logisticsVehicleService.getVehicleList(new LogisticsVehiclePageReqVO())) {
            addIfDue(items, "车辆-行驶证", SUBJECT_VEHICLE, vehicle.getId(), vehicle.getPlateNo(),
                    vehicle.getDrivingLicenseExpiryDate(), limit, today);
            addIfDue(items, "车辆-保险", SUBJECT_VEHICLE, vehicle.getId(), vehicle.getPlateNo(),
                    vehicle.getInsuranceExpiryDate(), limit, today);
        }
        for (LogisticsDriverDO driver : logisticsDriverService.getDriverList(new LogisticsDriverPageReqVO())) {
            addIfDue(items, "司机-驾驶证", SUBJECT_DRIVER, driver.getId(), driver.getName(),
                    driver.getDrivingLicenseExpiryDate(), limit, today);
            addIfDue(items, "司机-从业资格证", SUBJECT_DRIVER, driver.getId(), driver.getName(),
                    driver.getQualificationCertExpiryDate(), limit, today);
        }

        // 最快的排前面：工作台第一眼要看到最急的
        items.sort(Comparator.comparing(LogisticsExpiryWarningItemVO::getDaysLeft));
        return items;
    }

    private void addIfDue(List<LogisticsExpiryWarningItemVO> items, String category, int subjectType,
                          Long subjectId, String subjectName, LocalDate expiryDate,
                          LocalDate limit, LocalDate today) {
        if (expiryDate == null || expiryDate.isAfter(limit)) {
            return; // 没登记到期日的不提醒（一期允许先建档后补证）
        }
        LogisticsExpiryWarningItemVO item = new LogisticsExpiryWarningItemVO();
        item.setCategory(category);
        item.setSubjectType(subjectType);
        item.setSubjectId(subjectId);
        item.setSubjectName(subjectName);
        item.setExpiryDate(expiryDate);
        item.setDaysLeft((int) ChronoUnit.DAYS.between(today, expiryDate));
        item.setExpired(expiryDate.isBefore(today));
        items.add(item);
    }

}

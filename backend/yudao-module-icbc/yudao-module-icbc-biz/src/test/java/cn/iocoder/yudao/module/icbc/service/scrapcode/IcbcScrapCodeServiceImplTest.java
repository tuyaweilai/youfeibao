package cn.iocoder.yudao.module.icbc.service.scrapcode;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.scrapcode.vo.IcbcScrapCodePageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.scrapcode.vo.IcbcScrapCodeSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.scrapcode.IcbcScrapCodeDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.scrapcode.IcbcScrapCodeMapper;
import cn.iocoder.yudao.module.icbc.service.scrapcode.impl.IcbcScrapCodeServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.SCRAP_CODE_MERGED_CODE_EXISTS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link IcbcScrapCodeServiceImpl} 的单元测试类。
 *
 * <p>这张表是平台级、跨租户共享的编码表，由平台运营维护、租户只读。
 */
@Import({IcbcScrapCodeServiceImpl.class, UnitTestConfiguration.class})
@Transactional
@Rollback
public class IcbcScrapCodeServiceImplTest extends BaseDbUnitTest {

    @Resource
    private IcbcScrapCodeServiceImpl scrapCodeService;

    @Resource
    private IcbcScrapCodeMapper scrapCodeMapper;

    @Test
    public void testCreateScrapCode_success() {
        Long id = scrapCodeService.createScrapCode(buildReqVO("废钢铁", "1090101010000000000", 0));

        assertNotNull(id);
        IcbcScrapCodeDO scrapCode = scrapCodeMapper.selectById(id);
        assertEquals("废钢铁", scrapCode.getName());
        assertEquals("1090101010000000000", scrapCode.getMergedCode());
    }

    @Test
    public void testCreateScrapCode_mergedCodeExists() {
        scrapCodeService.createScrapCode(buildReqVO("废钢铁", "1090101010000000000", 0));

        assertServiceException(
                () -> scrapCodeService.createScrapCode(buildReqVO("钢铁废料", "1090101010000000000", 0)),
                SCRAP_CODE_MERGED_CODE_EXISTS);
    }

    @Test
    public void testGetEnabledList_onlyEnabled() {
        scrapCodeService.createScrapCode(buildReqVO("废钢铁", "1090101010000000000", 0));
        scrapCodeService.createScrapCode(buildReqVO("废铜", "1090101020000000000", 1));

        List<IcbcScrapCodeDO> enabled = scrapCodeService.getEnabledList();

        assertEquals(1, enabled.size());
        assertEquals("废钢铁", enabled.get(0).getName());
    }

    @Test
    public void testGetScrapCodePage_filterByMergedCode() {
        scrapCodeService.createScrapCode(buildReqVO("废钢铁", "1090101010000000000", 0));
        scrapCodeService.createScrapCode(buildReqVO("废铜", "1090101020000000000", 0));

        IcbcScrapCodePageReqVO pageReqVO = new IcbcScrapCodePageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(10);
        pageReqVO.setMergedCode("109010101");
        PageResult<IcbcScrapCodeDO> page = scrapCodeService.getScrapCodePage(pageReqVO);

        assertEquals(1, page.getTotal());
        assertEquals("废钢铁", page.getList().get(0).getName());
    }

    private IcbcScrapCodeSaveReqVO buildReqVO(String name, String mergedCode, int status) {
        IcbcScrapCodeSaveReqVO reqVO = new IcbcScrapCodeSaveReqVO();
        reqVO.setName(name);
        reqVO.setMergedCode(mergedCode);
        reqVO.setUnit("吨");
        reqVO.setTaxRate(new BigDecimal("0.13"));
        reqVO.setStatus(status);
        return reqVO;
    }

}

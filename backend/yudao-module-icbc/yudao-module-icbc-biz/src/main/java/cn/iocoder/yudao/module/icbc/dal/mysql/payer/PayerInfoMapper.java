package cn.iocoder.yudao.module.icbc.dal.mysql.payer;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.payer.vo.PayerInfoExportReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payer.vo.PayerInfoPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payer.PayerInfoDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 工行付方信息 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface PayerInfoMapper extends BaseMapperX<PayerInfoDO> {

    /**
     * 根据统一社会信用代码查询付方信息
     *
     * @param creditCode 统一社会信用代码
     * @return 付方信息
     */
    default PayerInfoDO selectByCreditCode(String creditCode) {
        return selectOne(PayerInfoDO::getCreditCode, creditCode);
    }

    /**
     * 根据纳税人识别号查询付方信息
     *
     * @param taxNo 纳税人识别号
     * @return 付方信息
     */
    default PayerInfoDO selectByTaxNo(String taxNo) {
        return selectOne(PayerInfoDO::getTaxNo, taxNo);
    }

    /**
     * 根据统一社会信用代码查询付方信息，**包含软删行**。
     *
     * <p>唯一键 {@code uk_credit_code} 不含 {@code deleted}（生产建表脚本 `icbc_payer_info.sql`），
     * 软删行仍占着这个值。{@code BaseDO.deleted} 上的 {@code @TableLogic} 会让普通查询无条件追加
     * {@code AND deleted = 0}，看不到软删行——{@code TenantUtils.executeIgnore} 只关租户过滤、不关逻辑删除。
     * 这里用裸 SQL 绕过 {@code @TableLogic}，查的正是唯一键实际覆盖的集合；租户过滤仍由租户插件
     * 加在 SQL 上，需要跨租户时由调用方在 {@code executeIgnore} 里调。
     *
     * @param creditCode 统一社会信用代码（已归一）
     * @return 付方信息（可能已软删）
     */
    @Select("SELECT * FROM icbc_payer_info WHERE credit_code = #{creditCode} LIMIT 1")
    PayerInfoDO selectByCreditCodeIncludeDeleted(@Param("creditCode") String creditCode);

    /**
     * 根据纳税人识别号查询付方信息，**包含软删行**。同 {@link #selectByCreditCodeIncludeDeleted}。
     *
     * @param taxNo 纳税人识别号（已归一）
     * @return 付方信息（可能已软删）
     */
    @Select("SELECT * FROM icbc_payer_info WHERE tax_no = #{taxNo} LIMIT 1")
    PayerInfoDO selectByTaxNoIncludeDeleted(@Param("taxNo") String taxNo);

    /**
     * 根据合作方付方编号查询付方信息
     *
     * @param partnerPayerId 合作方付方编号
     * @return 付方信息
     */
    default PayerInfoDO selectByPartnerPayerId(String partnerPayerId) {
        return selectOne(PayerInfoDO::getPartnerPayerId, partnerPayerId);
    }

    /**
     * 根据合作方付方编号查询付方信息，**包含软删行**。同 {@link #selectByCreditCodeIncludeDeleted}：
     * {@code uk_partner_payer_id} 不含 {@code deleted}，软删行仍占着这个值，普通查询看不到它。
     *
     * @param partnerPayerId 合作方付方编号
     * @return 付方信息（可能已软删）
     */
    @Select("SELECT * FROM icbc_payer_info WHERE partner_payer_id = #{partnerPayerId} LIMIT 1")
    PayerInfoDO selectByPartnerPayerIdIncludeDeleted(@Param("partnerPayerId") String partnerPayerId);

    /**
     * 根据分页条件查询付方信息
     *
     * @param reqVO 分页请求
     * @return 付方信息分页结果
     */
    default PageResult<PayerInfoDO> selectPage(PayerInfoPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PayerInfoDO>()
                .likeIfPresent(PayerInfoDO::getName, reqVO.getName())
                .eqIfPresent(PayerInfoDO::getCreditCode, reqVO.getCreditCode())
                .eqIfPresent(PayerInfoDO::getTaxNo, reqVO.getTaxNo())
                .eqIfPresent(PayerInfoDO::getContactMobile, reqVO.getContactMobile())
                .eqIfPresent(PayerInfoDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(PayerInfoDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(PayerInfoDO::getId));
    }

    /**
     * 根据条件查询付方信息列表
     *
     * @param reqVO 查询条件
     * @return 付方信息列表
     */
    default List<PayerInfoDO> selectList(PayerInfoPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<PayerInfoDO>()
                .likeIfPresent(PayerInfoDO::getName, reqVO.getName())
                .eqIfPresent(PayerInfoDO::getCreditCode, reqVO.getCreditCode())
                .eqIfPresent(PayerInfoDO::getTaxNo, reqVO.getTaxNo())
                .eqIfPresent(PayerInfoDO::getContactMobile, reqVO.getContactMobile())
                .eqIfPresent(PayerInfoDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(PayerInfoDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(PayerInfoDO::getId));
    }

    /**
     * 根据导出条件查询付方信息列表
     *
     * @param reqVO 导出查询条件
     * @return 付方信息列表
     */
    default List<PayerInfoDO> selectList(PayerInfoExportReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<PayerInfoDO>()
                .likeIfPresent(PayerInfoDO::getName, reqVO.getName())
                .eqIfPresent(PayerInfoDO::getCreditCode, reqVO.getCreditCode())
                .eqIfPresent(PayerInfoDO::getTaxNo, reqVO.getTaxNo())
                .eqIfPresent(PayerInfoDO::getContactMobile, reqVO.getContactMobile())
                .eqIfPresent(PayerInfoDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(PayerInfoDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(PayerInfoDO::getId));
    }

} 
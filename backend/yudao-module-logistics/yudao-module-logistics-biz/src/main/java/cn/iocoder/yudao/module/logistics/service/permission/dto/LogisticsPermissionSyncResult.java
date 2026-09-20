package cn.iocoder.yudao.module.logistics.service.permission.dto;

/**
 * 物流域权限同步的结果计数。
 *
 * <p>幂等的判断依据：重复执行时四个计数都会收敛到 0。
 */
public class LogisticsPermissionSyncResult {

    private final int createdMenuCount;
    private final int createdRoleCount;
    private final int assignedRoleMenuCount;
    private final int addedPackageMenuCount;

    public LogisticsPermissionSyncResult(int createdMenuCount, int createdRoleCount,
                                         int assignedRoleMenuCount, int addedPackageMenuCount) {
        this.createdMenuCount = createdMenuCount;
        this.createdRoleCount = createdRoleCount;
        this.assignedRoleMenuCount = assignedRoleMenuCount;
        this.addedPackageMenuCount = addedPackageMenuCount;
    }

    public int getCreatedMenuCount() {
        return createdMenuCount;
    }

    public int getCreatedRoleCount() {
        return createdRoleCount;
    }

    public int getAssignedRoleMenuCount() {
        return assignedRoleMenuCount;
    }

    public int getAddedPackageMenuCount() {
        return addedPackageMenuCount;
    }

    /**
     * 本应同步的东西全都已就位（幂等重跑的结果）。
     */
    public boolean isEmpty() {
        return createdMenuCount == 0 && createdRoleCount == 0
                && assignedRoleMenuCount == 0 && addedPackageMenuCount == 0;
    }

}

package com.xhzb.system.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.xhzb.system.domain.SysUserRole;

/**
 * 用户与角色关联表 数据层
 * 
 * @author ruoyi
 */
@Mapper
public interface SysUserRoleMapper
{
    /**
     * 通过用户ID删除用户和角色关联
     * 
     * @param userId 用户ID
     * @return 结果
     */
    public int deleteUserRoleByUserId(Long userId);

    /**
     * 批量删除用户和角色关联
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteUserRole(Long[] ids);

    /**
     * 通过角色ID查询角色使用数量
     * 
     * @param roleId 角色ID
     * @return 结果
     */
    public int countUserRoleByRoleId(Long roleId);

    /**
     * 批量新增用户角色信息
     * 
     * @param userRoleList 用户角色列表
     * @return 结果
     */
    public int batchUserRole(List<SysUserRole> userRoleList);

    /**
     * 删除用户和角色关联信息
     * 
     * @param userRole 用户和角色关联信息
     * @return 结果
     */
    public int deleteUserRoleInfo(SysUserRole userRole);

    /**
     * 批量取消授权用户角色
     * 
     * @param roleId 角色ID
     * @param userIds 需要删除的用户数据ID
     * @return 结果
     */
    public int deleteUserRoleInfos(@Param("roleId") Long roleId, @Param("userIds") Long[] userIds);

    /**
     * 通过角色标识符(role_key)查询该角色下所有用户ID
     *
     * @param roleKey 角色标识符，如 nursing_elder / admin / administrator
     * @return 用户ID列表
     */
    @Select("SELECT sur.user_id FROM sys_user_role sur " +
            "LEFT JOIN sys_role sr ON sur.role_id = sr.role_id " +
            "WHERE sr.role_key = #{roleKey}")
    List<Long> selectUserIdByRoleKey(String roleKey);
}

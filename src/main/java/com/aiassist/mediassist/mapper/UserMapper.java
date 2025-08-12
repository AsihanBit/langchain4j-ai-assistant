package com.aiassist.mediassist.mapper;

import com.aiassist.mediassist.dto.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    
    /**
     * 根据ID查询用户
     */
    User selectById(@Param("id") Integer id);
    
    /**
     * 根据IP地址查询用户
     */
    User selectByIpAddress(@Param("ipAddress") String ipAddress);
    
    /**
     * 根据用户名查询用户
     */
    User selectByUserName(@Param("userName") String userName);
    
    /**
     * 根据用户名和IP查询用户
     */
    User selectByUserNameAndIp(@Param("userName") String userName, @Param("ipAddress") String ipAddress);
    
    /**
     * 查询所有用户
     */
    List<User> selectAll();
    
    /**
     * 根据访问次数排序查询用户
     */
    List<User> selectAllOrderByVisitCount();
    
    /**
     * 插入新用户
     */
    int insertUser(User user);
    
    /**
     * 更新用户信息
     */
    int updateUser(User user);
    
    /**
     * 更新用户访问信息
     */
    int updateUserVisit(@Param("id") Integer id);
    
    /**
     * 增加用户token使用量
     */
    int addTokenUsage(@Param("id") Integer id, @Param("tokens") Long tokens);
    
    /**
     * 根据ID删除用户
     */
    int deleteById(@Param("id") Integer id);
    
    /**
     * 统计用户总数
     */
    int countUsers();
    
    /**
     * 检查IP是否已存在
     */
    boolean existsByIpAddress(@Param("ipAddress") String ipAddress);
    
    /**
     * 检查用户名是否已存在
     */
    boolean existsByUserName(@Param("userName") String userName);
}
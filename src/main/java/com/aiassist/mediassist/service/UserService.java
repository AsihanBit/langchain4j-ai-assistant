package com.aiassist.mediassist.service;

import com.aiassist.mediassist.dto.entity.User;

import java.util.List;

public interface UserService {
    
    /**
     * 根据ID查询用户
     */
    User getUserById(Integer id);
    
    /**
     * 根据IP地址查询用户
     */
    User getUserByIpAddress(String ipAddress);
    
    /**
     * 根据用户名查询用户
     */
    User getUserByUserName(String userName);
    
    /**
     * 根据用户名和IP查询用户
     */
    User getUserByUserNameAndIp(String userName, String ipAddress);
    
    /**
     * 查询所有用户
     */
    List<User> getAllUsers();
    
    /**
     * 根据访问次数排序查询用户
     */
    List<User> getUsersByVisitCount();
    
    /**
     * 新增用户
     */
    User createUser(String userName, String ipAddress);
    
    /**
     * 更新用户信息
     */
    boolean updateUser(User user);
    
    /**
     * 记录用户访问（如果用户不存在则创建，存在则更新访问信息）
     */
    User recordUserVisit(String userName, String ipAddress);
    
    /**
     * 更新用户访问信息
     */
    boolean updateUserVisit(Integer id);
    
    /**
     * 增加用户token使用量
     */
    boolean addTokenUsage(Integer id, Long tokens);
    
    /**
     * 删除用户
     */
    boolean deleteUser(Integer id);
    
    /**
     * 统计用户总数
     */
    int countUsers();
    
    /**
     * 检查IP是否已存在
     */
    boolean existsByIpAddress(String ipAddress);
    
    /**
     * 检查用户名是否已存在
     */
    boolean existsByUserName(String userName);
}
package com.aiassist.mediassist.service.impl;

import com.aiassist.mediassist.dto.entity.User;
import com.aiassist.mediassist.mapper.UserMapper;
import com.aiassist.mediassist.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Override
    public User getUserById(Integer id) {
        if (id == null) {
            return null;
        }
        return userMapper.selectById(id);
    }
    
    @Override
    public User getUserByIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return null;
        }
        return userMapper.selectByIpAddress(ipAddress.trim());
    }
    
    @Override
    public User getUserByUserName(String userName) {
        if (userName == null || userName.trim().isEmpty()) {
            return null;
        }
        return userMapper.selectByUserName(userName.trim());
    }
    
    @Override
    public User getUserByUserNameAndIp(String userName, String ipAddress) {
        if (userName == null || userName.trim().isEmpty() || 
            ipAddress == null || ipAddress.trim().isEmpty()) {
            return null;
        }
        return userMapper.selectByUserNameAndIp(userName.trim(), ipAddress.trim());
    }
    
    @Override
    public List<User> getAllUsers() {
        return userMapper.selectAll();
    }
    
    @Override
    public List<User> getUsersByVisitCount() {
        return userMapper.selectAllOrderByVisitCount();
    }
    
    @Override
    public User createUser(String userName, String ipAddress) {
        if (userName == null || userName.trim().isEmpty() || 
            ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名和IP地址不能为空");
        }
        
        // 检查是否已存在
        User existingUser = getUserByUserNameAndIp(userName.trim(), ipAddress.trim());
        if (existingUser != null) {
            log.info("用户已存在: {} - {}", userName, ipAddress);
            return existingUser;
        }
        
        // 创建新用户
        User newUser = new User(userName.trim(), ipAddress.trim());
        int result = userMapper.insertUser(newUser);
        
        if (result > 0) {
            log.info("创建用户成功: {} - {}, ID: {}", userName, ipAddress, newUser.getId());
            return newUser;
        } else {
            log.error("创建用户失败: {} - {}", userName, ipAddress);
            throw new RuntimeException("创建用户失败");
        }
    }
    
    @Override
    public boolean updateUser(User user) {
        if (user == null || user.getId() == null) {
            return false;
        }
        
        int result = userMapper.updateUser(user);
        boolean success = result > 0;
        
        if (success) {
            log.info("更新用户成功: ID {}", user.getId());
        } else {
            log.warn("更新用户失败: ID {}", user.getId());
        }
        
        return success;
    }
    
    @Override
    public User recordUserVisit(String userName, String ipAddress) {
        if (userName == null || userName.trim().isEmpty() || 
            ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名和IP地址不能为空");
        }
        
        userName = userName.trim();
        ipAddress = ipAddress.trim();
        
        // 首先查找是否存在该IP的用户
        User existingUser = getUserByIpAddress(ipAddress);
        
        if (existingUser != null) {
            // 如果是同一个用户，更新访问信息
            if (existingUser.getUserName().equals(userName)) {
                updateUserVisit(existingUser.getId());
                // 重新查询返回最新数据
                return getUserById(existingUser.getId());
            } else {
                // 如果是不同用户，更新用户名和访问信息
                existingUser.setUserName(userName);
                existingUser.updateVisit();
                updateUser(existingUser);
                return existingUser;
            }
        } else {
            // 不存在该IP的用户，创建新用户
            return createUser(userName, ipAddress);
        }
    }
    
    @Override
    public boolean updateUserVisit(Integer id) {
        if (id == null) {
            return false;
        }
        
        int result = userMapper.updateUserVisit(id);
        boolean success = result > 0;
        
        if (success) {
            log.info("更新用户访问信息成功: ID {}", id);
        } else {
            log.warn("更新用户访问信息失败: ID {}", id);
        }
        
        return success;
    }
    
    @Override
    public boolean addTokenUsage(Integer id, Long tokens) {
        if (id == null || tokens == null || tokens <= 0) {
            return false;
        }
        
        int result = userMapper.addTokenUsage(id, tokens);
        boolean success = result > 0;
        
        if (success) {
            log.info("增加用户token使用量成功: ID {}, tokens {}", id, tokens);
        } else {
            log.warn("增加用户token使用量失败: ID {}, tokens {}", id, tokens);
        }
        
        return success;
    }
    
    @Override
    public boolean deleteUser(Integer id) {
        if (id == null) {
            return false;
        }
        
        int result = userMapper.deleteById(id);
        boolean success = result > 0;
        
        if (success) {
            log.info("删除用户成功: ID {}", id);
        } else {
            log.warn("删除用户失败: ID {}", id);
        }
        
        return success;
    }
    
    @Override
    public int countUsers() {
        return userMapper.countUsers();
    }
    
    @Override
    public boolean existsByIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return false;
        }
        return userMapper.existsByIpAddress(ipAddress.trim());
    }
    
    @Override
    public boolean existsByUserName(String userName) {
        if (userName == null || userName.trim().isEmpty()) {
            return false;
        }
        return userMapper.existsByUserName(userName.trim());
    }
}
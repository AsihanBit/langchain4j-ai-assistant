package com.aiassist.chat.core.utils;

import com.aiassist.chat.core.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class MongoDBUtils {
    @Autowired
    private MongoTemplate mongoTemplate;

    public User insertUser(User user) {
        return mongoTemplate.insert(user);
    }
}

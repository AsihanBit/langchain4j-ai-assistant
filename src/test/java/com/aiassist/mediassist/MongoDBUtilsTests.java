package com.aiassist.mediassist;

import com.aiassist.mediassist.dto.entity.User;
import com.aiassist.mediassist.util.MongoDBUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class MongoDBUtilsTests {

    @Autowired
    private MongoDBUtils mongoDBUtils;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    void insertUser_shouldPersistAndBeRetrievable() {
        User user = new User("Alice", "30");

        User saved = mongoDBUtils.insertUser(user);

        Assertions.assertNotNull(saved, "保存结果不应为 null");
        Assertions.assertNotNull(saved.getId(), "保存后应生成 _id");

        User found = mongoTemplate.findById(saved.getId(), User.class);
        Assertions.assertNotNull(found, "应能根据 _id 查询到");
        Assertions.assertEquals("Alice", found.getUserName());
        Assertions.assertEquals(30, found.getUserName());

        // 清理测试数据
//        mongoTemplate.remove(found);
    }
}



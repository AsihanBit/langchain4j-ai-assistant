# MongoDB聊天记忆存储测试说明

## 🧪 测试文件

### 1. `MongoChatMemoryStoreTest` - 核心存储功能测试
测试 `MongoChatMemoryStore` 的核心功能：
- ✅ 基本消息存储和检索
- ✅ 会话自动创建
- ✅ 手动会话创建
- ✅ 消息添加功能
- ✅ 消息删除功能
- ✅ 多轮对话顺序保持
- ✅ 空消息处理
- ✅ 数据持久化验证

### 2. `MongoChatMemoryConfigurationTest` - 配置集成测试
测试 `ChatMemoryProvider` 的配置和集成：
- ✅ Bean正确注入
- ✅ MessageWindowChatMemory创建
- ✅ 完整消息存储流程
- ✅ 消息窗口限制功能
- ✅ 多memoryId隔离
- ✅ MongoDB持久化
- ✅ 清除功能
- ✅ ID验证

## 🚀 运行测试

### 运行所有MongoDB相关测试
```bash
mvn test -Dtest="*Mongo*"
```

### 运行单个测试类
```bash
# 测试存储功能
mvn test -Dtest=MongoChatMemoryStoreTest

# 测试配置集成
mvn test -Dtest=MongoChatMemoryConfigurationTest
```

### 运行特定测试方法
```bash
# 测试基本存储功能
mvn test -Dtest=MongoChatMemoryStoreTest#testBasicMessageStorageAndRetrieval

# 测试消息窗口限制
mvn test -Dtest=MongoChatMemoryConfigurationTest#testMessageWindowLimit
```

## 🔧 测试环境要求

### 1. MongoDB连接
确保 `application-dev.yml` 中配置了正确的MongoDB连接：
```yaml
dev:
  mongo:
    host: 103.117.122.68
    port: 27017
    database: mediassist_test  # 建议使用测试专用数据库
```

### 2. 测试配置
在 `application-test.yml` 中可以覆盖配置：
```yaml
chat:
  memory:
    enable-mongodb: true
    max-messages: 10
```

## 📊 测试覆盖范围

### 核心功能测试
- [x] 消息的CRUD操作
- [x] 会话生命周期管理
- [x] LangChain4j消息格式转换
- [x] MongoDB数据结构验证
- [x] 并发安全性（隐式）

### 集成测试
- [x] Spring容器集成
- [x] Bean依赖注入
- [x] 配置属性读取
- [x] ChatMemoryProvider工作流程
- [x] MessageWindowChatMemory功能

### 边界条件测试
- [x] 空消息处理
- [x] 不存在的memoryId
- [x] 消息窗口溢出
- [x] 多会话隔离

## 🎯 测试数据清理

测试使用以下策略确保数据清理：
- 测试用的memoryId以 `test_` 或 `config_test_` 开头
- 每个测试前后自动清理相关数据
- 使用正则表达式批量清理测试数据

## 🔍 调试技巧

### 查看测试日志
```bash
mvn test -Dtest=MongoChatMemoryStoreTest -X
```

### 验证MongoDB数据
测试后可以直接查看MongoDB中的数据：
```javascript
// 查看测试会话
db.conversations.find({"memory_id": /^test_/})

// 查看测试消息
db.messages.find({"memory_id": /^test_/})
```

### 常见问题
1. **MongoDB连接失败**: 检查网络和配置
2. **测试数据残留**: 手动清理或检查清理逻辑
3. **Bean注入失败**: 检查Spring配置和profile

## ✅ 预期结果

所有测试通过后，说明：
- MongoDB聊天记忆存储功能正常
- LangChain4j集成正确
- 数据持久化可靠
- 消息窗口控制有效
- 多用户隔离正常

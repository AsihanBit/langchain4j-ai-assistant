# MediAssist

一个基于 Spring Boot + LangChain4j 的智能医疗辅助示例，集成 Weaviate 作为向量检索，支持可运行时修改的系统提示词（prompt）。

## 特性
- OpenAI 兼容接口（通过 LangChain4j）
- Weaviate 语义检索（RAG）
- 可运行时修改的系统提示词（prompts/system-prompt.txt）
- DTO/工具类示例、单元测试示例

## 快速开始

1) 克隆项目并进入目录
```bash
git clone <your-repo-url>
cd MediAssist
```

2) 准备开发配置
- 复制示例文件为实际 dev 配置（不会提交到仓库）
```bash
copy application-dev.example.yml src\main\resources\application-dev.yml   # Windows
# 或
cp application-dev.example.yml src/main/resources/application-dev.yml             # macOS/Linux
```
- 按需修改 src/main/resources/application-dev.yml 中的 dev.* 值（数据库、OpenAI Key 等）

3) 启动项目
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## 配置说明

- src/main/resources/application.yml
  - 仅包含占位引用，如 ${dev.db.url}，不直接存放敏感信息
- src/main/resources/application-dev.yml
  - 提供 dev.* 变量的实际取值（不会推送到仓库）
- application-dev.example.yml
  - 示例模板，便于他人参考

## Prompt（系统提示词）
- 运行目录下的 ./prompts/system-prompt.txt 优先生效
- 若不存在，则使用 classpath:prompts/system-prompt.txt
- 修改后重启应用生效

## 重要命令

- 运行指定测试
```bash
mvn test -Dtest=WeaviateApiFixTest#testWeaviateAgent
```

## 贡献
欢迎提交 Issue 或 PR。

## 许可证
MIT


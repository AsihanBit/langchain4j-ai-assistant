# 系统提示词配置（运行时可修改）

## 📝 说明

这个目录包含了AI助手的系统提示词配置文件，可以在应用运行时动态修改。

## 📂 文件说明

- `system-prompt.txt` - AI助手的系统提示词
- `README.md` - 本说明文件

## 🔄 如何修改系统提示词（部署后）

### 方法1：直接修改文件（推荐）

1. 编辑 `prompts/system-prompt.txt` 文件
2. 重启应用以使更改生效

### 方法2：自定义外部文件路径

1. 将提示词文件放在任意位置
2. 设置环境变量：`SYSTEM_PROMPT_FILE=/path/to/your/prompt.txt`
3. 重启应用

## 📋 系统提示词指南

系统提示词应该包含以下部分：

1. **AI助手身份定义**
2. **核心功能描述**
3. **工作流程说明**
4. **可用工具列表**
5. **行为准则**

## ⚠️ 注意事项

- 修改后需要重启应用才能生效
- 建议在修改前备份原文件
- 确保提示词内容符合使用规范
- 工具函数名称必须与代码中定义的一致

## 🔧 技术实现（无需代码改动）

系统提示词通过 LangChain4j 的 `@SystemMessage(fromResource = "prompts/system-prompt.txt")` 注解加载，支持：

- 优先加载应用运行目录下的 `./prompts/system-prompt.txt`
- 如果找不到，则加载JAR包内置的 `classpath:prompts/system-prompt.txt`
- 支持通过环境变量指定外部文件路径

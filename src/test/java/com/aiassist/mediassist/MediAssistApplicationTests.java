package com.aiassist.mediassist;

import com.aiassist.mediassist.ai.CustomAiClient;
//import com.aiassist.mediassist.ai.OpenAiAgent;
import com.aiassist.mediassist.ai.OpenAiAgent;
import com.aiassist.mediassist.ai.OpenAiClient;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
//import dev.langchain4j.model.openai.internal.chat.UserMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Flux;

@SpringBootTest
class MediAssistApplicationTests {
    @Autowired
    OpenAiChatModel chatModel;
//    OpenAiChatModel chatModel;

//    @Autowired
//    ChatLanguageModel chatLanguageModel;

    @Autowired
    OpenAiClient openAiClient;

    @Autowired
    OpenAiAgent openAiAgent;

    @Test
    void contextLoads() {
    }

    // 自定义的
    @Test
    void testText() {
        CustomAiClient cusClient = new CustomAiClient();
        String res = cusClient.sendText("你好呀 你可以叫我三毛");
        System.out.println(res);
        String res2 = cusClient.sendText("你好呀 你记得我是谁吗");
        System.out.println(res2);
    }

    // openai starter的
    @Test
    void testText2() {
        String res = chatModel.chat("你好呀 你是什么模型");
        System.out.println(res);
    }

    // 增强 openai starter的
    @Test
    void testText3() {
        String res = openAiClient.chatStr("你好呀 我的agent");
        System.out.println(res);
    }

    @Test
    void testText4() {
        ChatResponse resp = chatModel.chat(UserMessage.from("你好呀")); // 注意：不是 chat(String)
        String text = resp.aiMessage().text();       // 或 resp.content().text()，视版本API而定
        TokenUsage usage = resp.tokenUsage();        // 可能为 null

        System.out.println(text);
        if (usage != null) {
            System.out.printf("prompt=%d, completion=%d, total=%d%n",
                    usage.inputTokenCount(), usage.outputTokenCount(), usage.totalTokenCount());
        }
    }
    @Test
    void testText6() {
        System.out.println("=== 第一次对话（新会话）===");
        String encryptMsg = "请加密一下下面的密码：123653";
        String ipMsg = "你知道我的ip地址吗";
        openAiAgent.chatStream("user-123",ipMsg )
                  .doOnNext(System.out::print)
                  .doOnComplete(() -> System.out.println("\n=== 第一次完成 ==="))
                  .blockLast();

//        System.out.println("\n=== 第三次对话（新会话，无记忆）===");
//        openAiAgent.chatStream("user-456", "你好呀叫我大强")
//                .doOnNext(System.out::print)
//                .doOnComplete(() -> System.out.println("\n=== 第三次完成 ==="))
//                .blockLast();
//
//        System.out.println("\n=== 第二次对话（同一会话，有记忆）===");
//        openAiAgent.chatStream("user-123", "你好呀 我叫什么")
//                  .doOnNext(System.out::print)
//                  .doOnComplete(() -> System.out.println("\n=== 第二次完成 ==="))
//                  .blockLast();


    }

    // agent测试


}

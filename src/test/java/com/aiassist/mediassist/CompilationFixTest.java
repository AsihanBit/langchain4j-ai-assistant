package com.aiassist.mediassist;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class CompilationFixTest {

    @Test
    void testBasicCompilation() {
        System.out.println("=== 测试基础编译修复 ===");
        System.out.println("如果这个测试能运行，说明基础编译问题已经修复！");
    }
}

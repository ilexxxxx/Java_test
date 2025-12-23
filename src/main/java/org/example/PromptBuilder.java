package org.example;

public class PromptBuilder {

    public static String buildPrompt(String userDescription) {
        return """
                请根据以下描述生成一段 JavaFX UI 代码：

                界面需求：
                %s

                要求：
                1. 使用 JavaFX 标准控件
                2. 代码结构清晰
                3. 只返回代码，不要解释
                """.formatted(userDescription);
    }
}

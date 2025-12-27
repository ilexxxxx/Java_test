package org.example;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class UIController {

    private TextArea inputArea;
    private TextArea outputArea;
    private Button generateButton;
    private Label statusLabel;

    public Parent createView() {
        inputArea = new TextArea();
        inputArea.setPromptText("请输入 UI 描述，例如：包含用户名、密码和登录按钮的界面");
        inputArea.setPrefHeight(150);

        generateButton = new Button("生成 JavaFX UI 代码");
        generateButton.setOnAction(e -> generateCode());

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPromptText("AI 生成的 JavaFX 代码将显示在这里");
        outputArea.setPrefHeight(300);

        // 使用等宽字体，更适合显示代码
        outputArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 13px;");

        // 添加状态标签
        statusLabel = new Label("等待输入...");
        statusLabel.setStyle("-fx-text-fill: gray;");

        VBox topBox = new VBox(10,
                new Label("UI 描述输入："),
                inputArea,
                generateButton,
                statusLabel
        );
        topBox.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(topBox);
        root.setCenter(new VBox(5, new Label("生成的代码："), outputArea));
        root.setPadding(new Insets(10));

        return root;
    }

    private void generateCode() {
        String userInput = inputArea.getText();
        if (userInput == null || userInput.isBlank()) {
            outputArea.setText("请输入有效的 UI 描述。");
            statusLabel.setText("请输入内容");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        // 禁用按钮防止重复点击
        generateButton.setDisable(true);
        statusLabel.setText("正在生成代码...");
        statusLabel.setStyle("-fx-text-fill: orange;");
        outputArea.setText("正在调用 AI 接口生成代码，请稍候...\n\n提示：此过程可能需要10-30秒");

        new Thread(() -> {
            try {
                System.out.println("\n=== 开始生成代码 ===");
                System.out.println("用户输入: " + userInput);

                String prompt = PromptBuilder.buildPrompt(userInput);
                System.out.println("Prompt: " + prompt);

                // 使用修改后的SparkClient
                String result = SparkClient.generateCode(prompt);

                javafx.application.Platform.runLater(() -> {
                    if (result.contains("失败") || result.contains("错误") || result.contains("超时")) {
                        outputArea.setText("生成失败:\n" + result +
                                "\n\n请检查：\n1. API配置是否正确\n2. 网络连接是否正常\n3. 是否已获取有效的API密钥");
                        statusLabel.setText("生成失败");
                        statusLabel.setStyle("-fx-text-fill: red;");
                    } else {
                        // 格式化代码
                        String formattedCode = formatJavaCode(result);
                        outputArea.setText("// ===== AI 生成的 JavaFX 代码 =====\n" +
                                "// 描述: " + userInput + "\n" +
                                "// 生成时间: " + java.time.LocalDateTime.now() + "\n" +
                                "// ====================================\n\n" +
                                formattedCode);
                        statusLabel.setText("代码生成完成 ✓");
                        statusLabel.setStyle("-fx-text-fill: green;");
                    }
                    generateButton.setDisable(false);
                });

            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    outputArea.setText("生成代码时出现异常：\n" + e.getMessage() +
                            "\n\n请查看控制台获取详细信息。");
                    statusLabel.setText("发生异常");
                    statusLabel.setStyle("-fx-text-fill: red;");
                    generateButton.setDisable(false);
                });
            }
        }).start();
    }

    // 格式化Java代码
    public String formatJavaCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return "// 没有生成有效的代码";
        }

        //  基础清理
        code = basicCleanup(code);

        // 专门处理"???"乱码
        code = fixQuestionMarkGarbage(code);

        //  专门处理用户提供的示例代码中的特定格式问题
        code = fixSpecificFormatIssues(code);

        //修复关键字和标识符
        code = fixKeywordsAndIdentifiers(code);

        //修复花括号和括号
        code = fixBracesAndParentheses(code);

        // 修复语句结束
        code = fixStatementEndings(code);

        //重建代码结构
        code = rebuildCodeStructure(code);

        // 最终清理和美化
        code = finalPolish(code);

        return code;
    }
    
    // 专门处理用户提供的示例代码中的特定格式问题
    private String fixSpecificFormatIssues(String code) {
        // 1. 修复类名拼写错误
        code = code.replace("Buttonint erface", "ButtonInterface")
                   .replace("int erface", "Interface");
        
        // 2. 修复方法调用错误
        code = code.replace("System.out.print ln", "System.out.println");
        
        // 3. 修复多余的空格
        code = code.replace("  ", " ")
                   .replace("   ", " ")
                   .replace("    ", " ")
                   .replace("     ", " ");
        
        // 4. 修复括号周围的空格
        code = code.replace("( ", "(")
                   .replace(" )", ")")
                   .replace("{ ", "{")
                   .replace(" }", "}")
                   .replace("[ ", "[")
                   .replace(" ]", "]")
                   .replace("= ", "=")
                   .replace(" =", "=");
        
        // 5. 修复分号周围的空格
        code = code.replace(" ;", ";")
                   .replace("; ", ";");
        
        // 6. 修复点号周围的空格
        code = code.replace(" .", ".")
                   .replace(". ", ".");
        
        // 7. 修复逗号周围的空格
        code = code.replace(" ,", ",")
                   .replace(", ", ",");
        
        return code;
    }
    
    // 专门处理"???"乱码
    private String fixQuestionMarkGarbage(String code) {
        // 如果代码中包含"???"乱码，尝试修复
        if (code.contains("???")) {
            // 1. 尝试GBK到UTF-8的转换
            try {
                byte[] bytes = code.getBytes("GBK");
                code = new String(bytes, "UTF-8");
            } catch (Exception e) {
                // 如果转换失败，尝试其他方法
            }
            
            // 2. 尝试GB2312到UTF-8的转换
            try {
                byte[] bytes = code.getBytes("GB2312");
                code = new String(bytes, "UTF-8");
            } catch (Exception e) {
                // 如果转换失败，尝试其他方法
            }
            
            // 3. 尝试其他常见编码
            try {
                byte[] bytes = code.getBytes("Windows-1252");
                code = new String(bytes, "UTF-8");
            } catch (Exception e) {
                // 如果转换失败，尝试其他方法
            }
            
            // 4. 如果仍然存在"???"，将其移除
            code = code.replace("???", "");
        }
        
        // 5. 移除连续的空格
        code = code.replaceAll("\\s{2,}", " ");
        
        // 6. 确保注释中的中文可以正确显示
        code = code.replaceAll("//\\s*\\?{3,}", "// ");
        
        return code;
    }

    // 基础清理
    private String basicCleanup(String code) {
        // 增强版基础清理和编码修复
        // 1. 移除null字符
        code = code.replace("\u0000", "");
        
        // 2. 移除控制字符（保留换行、回车、制表符）
        code = code.replaceAll("[\\p{Cntrl}&&[^\\n\\r\\t]]", "");
        
        // 3. 移除特殊Unicode字符（BOM、替换字符、私有使用区域）
        code = code.replaceAll("[\\uFEFF\\uFFFD\\uE000-\\uF8FF]", "");
        
        // 4. 移除所有无法打印的ASCII字符（除了空格和控制字符）
        code = code.replaceAll("[\\x00-\\x1F\\x7F]", "");
        
        // 5. 移除代码块标记
        code = code.replaceAll("```(java)?", "").trim();

        // 6. 移除开头的null字符串
        while (code.startsWith("null")) {
            code = code.substring(4).trim();
        }
        
        // 7. 移除多余的换行符
        code = code.replaceAll("\\n{3,}", "\n\n");
        
        // 8. 修复编码问题
        try {
            // 尝试多种编码转换来修复乱码
            byte[] bytes = code.getBytes("ISO-8859-1");
            code = new String(bytes, "UTF-8");
        } catch (Exception e) {
            // 如果转换失败，尝试其他编码
            try {
                byte[] bytes = code.getBytes("Windows-1252");
                code = new String(bytes, "UTF-8");
            } catch (Exception ex) {
                // 如果所有转换都失败，使用原始内容
            }
        }

        return code;
    }

    // 修复关键字和标识符
    private String fixKeywordsAndIdentifiers(String code) {
        // 修复Java关键字
        String[] keywords = {
                "public", "private", "protected", "class", "extends", "implements",
                "void", "static", "final", "int", "String", "double", "float",
                "boolean", "char", "byte", "short", "long", "if", "else",
                "for", "while", "do", "switch", "case", "default", "break",
                "continue", "return", "try", "catch", "finally", "throw",
                "throws", "import", "package", "new", "this", "super",
                "instanceof", "interface", "abstract", "synchronized",
                "volatile", "transient", "native", "strictfp", "enum",
                "assert", "const", "goto"
        };

        // 在关键字后添加空格
        for (String keyword : keywords) {
            code = code.replaceAll("(?i)" + keyword + "(?![\\s\\(\\{;])", keyword + " ");
        }

        // 修复常见的关键字组合
        code = code.replaceAll("publicclass", "public class");
        code = code.replaceAll("publicstatic", "public static");
        code = code.replaceAll("publicvoid", "public void");
        code = code.replaceAll("privatevoid", "private void");
        code = code.replaceAll("protectedvoid", "protected void");
        code = code.replaceAll("extendsApplication", "extends Application");
        code = code.replaceAll("newButton", "new Button");
        code = code.replaceAll("newStage", "new Stage");
        code = code.replaceAll("newScene", "new Scene");
        code = code.replaceAll("setOnAction", "setOnAction");

        return code;
    }

    // 修复花括号和括号
    private String fixBracesAndParentheses(String code) {
        // 修复左花括号
        code = code.replaceAll("(?<=\\w)\\{", " {");
        code = code.replaceAll("(?<=\\))\\{", " {");
        code = code.replaceAll("(?<=;)\\{", " {");

        // 修复右花括号
        code = code.replaceAll("(?<!\\s)\\}", " }");
        code = code.replaceAll("\\}(?!\\s|$)", " } ");

        // 只在控制结构(如if, for, while)后添加空格
        code = code.replaceAll("(?<=if|for|while|switch|catch|synchronized)\\(", " (");
        code = code.replaceAll("(?<=if|for|while|switch|catch|synchronized)\\(\\s*", " (");

        // 只在控制结构后添加空格
        code = code.replaceAll("\\)(?=\\s*\\{)", " ");

        return code;
    }

    // 修复语句结束
    private String fixStatementEndings(String code) {
        // 修复分号
        code = code.replaceAll(";(?!\\s|$)", "; ");
        code = code.replaceAll("\\s+;", ";");

        // 修复逗号
        code = code.replaceAll(",(?!\\s)", ", ");
        code = code.replaceAll("\\s+,", ",");

        // 修复点号
        code = code.replaceAll("\\s*\\.\\s*", ".");

        // 修复赋值操作符
        code = code.replaceAll("(?<!==|!=|<=|>=|\\+\\+|--)=(?!=)", " = ");
        code = code.replaceAll("(?<![\\s\\+\\-\\*/%&|^<>!])==(?![=])", " == ");
        code = code.replaceAll("(?<![\\s!])!=(?![=])", " != ");

        // 修复其他操作符
        String[] operators = {"\\+", "-", "\\*", "/", "%", "<=", ">=", "&&", "\\|\\|", "\\+\\+", "--"};
        for (String op : operators) {
            code = code.replaceAll("(?<![\\s" + op + "])" + op + "(?![\\s" + op + "])", " " + op + " ");
        }

        return code;
    }

    // 重建代码结构
    private String rebuildCodeStructure(String code) {
        StringBuilder result = new StringBuilder();
        int indentLevel = 0;
        boolean inImportBlock = true;
        boolean inMultiLineComment = false;
        boolean inStringLiteral = false;
        char stringDelimiter = '\0';

        // 首先进行初步的行分割，确保每个主要代码块都有换行
        // 在关键字、方法名等前面添加换行（如果不是行首）
        code = code.replaceAll("(?<!\\n)(public|private|protected|@Override)", "\n$1");
        // 确保左花括号后面换行
        code = code.replaceAll("\\{(?![\\n])", "{\n");
        
        // 确保右花括号前面换行（除非是在同一行有左花括号）
        code = code.replaceAll("(?<!\\n)(?<!\\{)\\}", "\n}");
        code = code.replaceAll("\\}(?!\\n|$)", "}\n");
        
        // 确保分号后面换行
        code = code.replaceAll(";\\s*(?!\\n)", ";\n");
        
        // 处理注释和代码在同一行的情况，将代码和注释分开到不同行
        code = code.replaceAll("(\\S)(\\/\\/.+?)(?=\\n|$)", "$1\n$2");
        
        // 处理lambda表达式的右大括号和右括号在同一行的情况
        code = code.replaceAll("(\\})\\s*\\)", "}\n)");
        
        // 处理特殊情况，确保方法调用有正确的格式
        code = code.replaceAll("(\\w+)\\.(\\w+)\\s*\\(", "$1.$2(");
        code = code.replaceAll("\\)\\s*\\.", ").");
        
        // 分割成多行
        String[] lines = code.split("\n");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            // 处理字符串字面量
            boolean localInString = inStringLiteral;
            char localDelimiter = stringDelimiter;
            for (int i = 0; i < trimmed.length(); i++) {
                char c = trimmed.charAt(i);
                if (!localInString && (c == '"' || c == '\'')) {
                    localInString = true;
                    localDelimiter = c;
                } else if (localInString && c == localDelimiter && (i == 0 || trimmed.charAt(i-1) != '\\')) {
                    localInString = false;
                    localDelimiter = '\0';
                }
            }

            // 如果不是在字符串中，处理格式
            if (!localInString) {
                // 处理多行注释
                if (trimmed.contains("/*")) {
                    inMultiLineComment = true;
                }
                if (trimmed.contains("*/")) {
                    inMultiLineComment = false;
                }

                // 如果不是在多行注释中，计算缩进
                if (!inMultiLineComment) {
                    // 处理类名拼写错误（如Buttonint erface -> ButtonInterface）
                    trimmed = trimmed.replaceAll("Buttonint erface", "ButtonInterface");
                    trimmed = trimmed.replaceAll("(\\w)(int erface)", "$1$2");
                    trimmed = trimmed.replaceAll("int erface", "Interface");
                    
                    // 修复方法调用错误（如print ln -> println）
                    trimmed = trimmed.replaceAll("System\\.out\\.print ln", "System.out.println");
                    
                    // 根据内容调整缩进级别
                    if (trimmed.contains("}") && !trimmed.contains("{")) {
                        indentLevel = Math.max(0, indentLevel - 1);
                    }

                    // 添加适当的缩进
                    String indent = "    ".repeat(indentLevel);
                    result.append(indent).append(trimmed).append("\n");

                    // 更新缩进级别
                    if (trimmed.contains("{") && !trimmed.contains("}")) {
                        indentLevel++;
                    }

                    // 更新状态标志和添加空行
                    if (trimmed.startsWith("import")) {
                        if (!inImportBlock) {
                            result.append("\n");
                            inImportBlock = true;
                        }
                    } else {
                        inImportBlock = false;
                        if (trimmed.startsWith("public class")) {
                            result.append("\n");
                        } else if (trimmed.startsWith("@Override")) {
                            result.append("\n");
                        } else if (trimmed.startsWith("public static void main")) {
                            result.append("\n");
                        }
                    }
                } else {
                    // 多行注释保持原样，但添加适当的缩进
                    String indent = "    ".repeat(indentLevel);
                    result.append(indent).append(trimmed).append("\n");
                }
            } else {
                // 字符串字面量保持原样，但添加适当的缩进
                String indent = "    ".repeat(indentLevel);
                result.append(indent).append(trimmed).append("\n");
            }

            // 更新状态
            inStringLiteral = localInString;
            stringDelimiter = localDelimiter;
        }

        return result.toString();
    }

    // 最终清理和美化
    private String finalPolish(String code) {
        StringBuilder result = new StringBuilder();
        String[] lines = code.split("\\n");
        boolean lastLineEmpty = false;
        int currentIndent = 0;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                // 保留空行但不多于一个
                if (!lastLineEmpty && result.length() > 0) {
                    result.append("\n");
                    lastLineEmpty = true;
                }
                continue;
            }

            // 修复注释
            if (trimmed.startsWith("//")) {
                // 确保注释后有空格
                if (trimmed.length() > 2 && trimmed.charAt(2) != ' ') {
                    trimmed = trimmed.substring(0, 2) + " " + trimmed.substring(2);
                }
            }

            // 修复常见的代码模式
            trimmed = fixCommonPatterns(trimmed);

            // 计算正确的缩进级别
            if (trimmed.contains("}") && !trimmed.contains("{")) {
                currentIndent = Math.max(0, currentIndent - 1);
            }

            // 添加正确的缩进
            String indent = "    ".repeat(currentIndent);
            result.append(indent).append(trimmed).append("\n");
            lastLineEmpty = false;

            // 更新缩进级别
            if (trimmed.contains("{") && !trimmed.contains("}")) {
                currentIndent++;
            }
        }

        // 移除结尾多余的换行
        if (result.length() > 0 && result.toString().endsWith("\n")) {
            result.setLength(result.length() - 1);
        }

        // 移除连续的空行
        String finalCode = result.toString();
        finalCode = finalCode.replaceAll("\n{3,}", "\n\n");

        return finalCode;
    }

    // 修复常见的代码模式
    private String fixCommonPatterns(String line) {
        // 修复@Override注解
        line = line.replace("@Overridepublic", "@Override\npublic");
        
        // 修复常见的JavaFX模式
        line = line.replace("setOnAction(e->", "setOnAction(e ->");
        line = line.replace("setOnAction(event->", "setOnAction(event ->");
        
        // 修复lambda表达式格式
        line = line.replace("->{", "-> {");
        line = line.replace("};", "};");
        
        // 修复数组初始化
        line = line.replace("newString[]", "new String[]");
        line = line.replace("newInteger[]", "new Integer[]");
        
        // 修复集合初始化
        line = line.replace("newArrayList", "new ArrayList");
        line = line.replace("newHashMap", "new HashMap");
        line = line.replace("newVBox", "new VBox");
        line = line.replace("newHBox", "new HBox");
        line = line.replace("newBorderPane", "new BorderPane");
        
        // 修复常见的方法调用
        line = line.replace("setTitle(", "setTitle(");
        line = line.replace("setScene(", "setScene(");
        line = line.replace("setLayoutX(", "setLayoutX(");
        line = line.replace("setLayoutY(", "setLayoutY(");
        line = line.replace("setPrefWidth(", "setPrefWidth(");
        line = line.replace("setPrefHeight(", "setPrefHeight(");
        line = line.replace("setPromptText(", "setPromptText(");
        line = line.replace("setStyle(", "setStyle(");
        
        // 修复属性设置
        line = line.replace("setAlignment(", "setAlignment(");
        line = line.replace("setPadding(", "setPadding(");
        line = line.replace("setSpacing(", "setSpacing(");
        line = line.replace("setMargin(", "setMargin(");
        
        // 修复方法声明（这些可能已经在fixKeywordsAndIdentifiers中处理过，但作为冗余保护）
        line = line.replace("publicvoid", "public void");
        line = line.replace("privatevoid", "private void");
        
        
        line = line.replace("Stagestage", "Stage stage");
        line = line.replace("Scenescene", "Scene scene");

        // 修复方法调用链
        line = line.replace("btn.setText", "btn.setText");
        line = line.replace("btn.setOnAction", "btn.setOnAction");
        line = line.replace("stage.setTitle", "stage.setTitle");
        line = line.replace("stage.setScene", "stage.setScene");
        line = line.replace("stage.show", "stage.show");

        // 修复常见表达式
        line = line.replace("newButton()", "new Button()");
        line = line.replace("newStage()", "new Stage()");
        line = line.replace("newScene()", "new Scene()");

        return line;
    }
}
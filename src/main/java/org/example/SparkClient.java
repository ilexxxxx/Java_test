package org.example;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import okhttp3.Protocol;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.tools.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class SparkClient {

    // ===== 星火 API 配置 =====
    private static final String HOST_URL = "https://spark-api.xf-yun.com/v1/x1";
    private static final String DOMAIN = "spark-x";
    private static final String APP_ID = "c40df7c9";
    private static final String API_SECRET = "YzI0N2E1YzhhYzhiZTM2OWQyOWY3MmNh";
    private static final String API_KEY = "4b20a2a894a67bcc7a419fb193350b11";

    private static final StringBuilder responseBuilder = new StringBuilder();

    // ================== 对外入口 ==================
    public static String generateCode(String prompt) {
        responseBuilder.setLength(0);

        try {
            String authUrl = getAuthUrl(HOST_URL, API_KEY, API_SECRET);
            String wsUrl = authUrl.replace("http://", "ws://")
                                  .replace("https://", "wss://");

            OkHttpClient client = new OkHttpClient.Builder()
                    .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                    .build();

            Request request = new Request.Builder()
                    .url(wsUrl)
                    .build();

            CountDownLatch latch = new CountDownLatch(1);

            client.newWebSocket(request, new WebSocketListener() {

                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    webSocket.send(buildRequest(prompt).toJSONString());
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    try {
                        JSONObject json = JSON.parseObject(text);
                        JSONObject header = json.getJSONObject("header");

                        if (header.getIntValue("code") != 0) {
                            latch.countDown();
                            return;
                        }

                        JSONArray texts = json
                                .getJSONObject("payload")
                                .getJSONObject("choices")
                                .getJSONArray("text");

                        if (texts != null && !texts.isEmpty()) {
                            String content = texts.getJSONObject(0).getString("content");
                            if (content != null) {
                                responseBuilder.append(content);
                            }
                        }

                        if (header.getIntValue("status") == 2) {
                            latch.countDown();
                            webSocket.close(1000, "done");
                        }

                    } catch (Exception e) {
                        latch.countDown();
                    }
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    latch.countDown();
                }
            });

            latch.await(30, java.util.concurrent.TimeUnit.SECONDS);

            String code = responseBuilder.toString()
                    .replace("```java", "")
                    .replace("```", "")
                    .trim();

            if (code.isEmpty()) {
                return "AI未生成有效代码";
            }

            // ⭐ 核心：生成后自动语法检查
            return compileAndValidate(code);

        } catch (Exception e) {
            return "生成失败：" + e.getMessage();
        }
    }

    // ================== 自动编译校验 ==================
    private static String compileAndValidate(String code) {
        try {
            String className = extractClassName(code);
            if (className == null) {
                return "// 编译失败：未找到 class 定义\n\n" + code;
            }

            File tempDir = new File(System.getProperty("java.io.tmpdir"), "ai_javafx_check");
            tempDir.mkdirs();

            File javaFile = new File(tempDir, className + ".java");
            try (FileWriter writer = new FileWriter(javaFile, StandardCharsets.UTF_8)) {
                writer.write(code);
            }

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                return "// 编译失败：未检测到 JDK（不是 JRE）\n\n" + code;
            }

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);

            Iterable<? extends JavaFileObject> units =
                    fileManager.getJavaFileObjects(javaFile);

            JavaCompiler.CompilationTask task =
                    compiler.getTask(null, fileManager, diagnostics, null, null, units);

            boolean success = task.call();
            fileManager.close();

            if (!success) {
                StringBuilder error = new StringBuilder();
                error.append("// ===== 编译失败 =====\n");
                for (Diagnostic<?> d : diagnostics.getDiagnostics()) {
                    error.append("行 ").append(d.getLineNumber())
                         .append(" : ").append(d.getMessage(null)).append("\n");
                }
                error.append("\n").append(code);
                return error.toString();
            }

            return code;

        } catch (Exception e) {
            return "// 编译异常：" + e.getMessage() + "\n\n" + code;
        }
    }

    // ================== 提取类名 ==================
    private static String extractClassName(String code) {
        for (String line : code.split("\n")) {
            line = line.trim();
            if (line.startsWith("public class ")) {
                return line.split("\\s+")[2];
            }
        }
        return null;
    }

    // ================== 构建请求 ==================
    private static JSONObject buildRequest(String prompt) {
        JSONObject root = new JSONObject();

        root.put("header", new JSONObject()
                .fluentPut("app_id", APP_ID)
                .fluentPut("uid", UUID.randomUUID().toString().substring(0, 10)));

        root.put("parameter", new JSONObject()
                .fluentPut("chat", new JSONObject()
                        .fluentPut("domain", DOMAIN)
                        .fluentPut("temperature", 0.2)
                        .fluentPut("max_tokens", 4096)));

        JSONArray text = new JSONArray();

        text.add(new JSONObject()
                .fluentPut("role", "system")
                .fluentPut("content",
                        "你是一个JavaFX专家。" +
                        "只返回【完整、可直接编译运行】的JavaFX代码。" +
                        "必须包含 import、class、main。" +
                        "禁止解释、禁止伪代码。"));

        text.add(new JSONObject()
                .fluentPut("role", "user")
                .fluentPut("content", prompt));

        root.put("payload", new JSONObject()
                .fluentPut("message", new JSONObject()
                        .fluentPut("text", text)));

        return root;
    }

    // ================== 鉴权 ==================
    private static String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        URL url = new URL(hostUrl);
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = sdf.format(new Date());

        String sign = "host: " + url.getHost() + "\n" +
                "date: " + date + "\n" +
                "GET " + url.getPath() + " HTTP/1.1";

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = Base64.getEncoder().encodeToString(mac.doFinal(sign.getBytes(StandardCharsets.UTF_8)));

        String auth = String.format(
                "api_key=\"%s\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"%s\"",
                apiKey, signature);

        return Objects.requireNonNull(HttpUrl.parse("https://" + url.getHost() + url.getPath()))
                .newBuilder()
                .addQueryParameter("authorization",
                        Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8)))
                .addQueryParameter("date", date)
                .addQueryParameter("host", url.getHost())
                .build()
                .toString();
    }
}
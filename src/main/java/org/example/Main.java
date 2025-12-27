package org.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {


        // 创建控制器和界面
        UIController controller = new UIController();
        Scene scene = new Scene(controller.createView(), 1000, 700);

        // 配置主窗口
        primaryStage.setTitle("AI JavaFX 代码生成器");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

        // 显示窗口
        primaryStage.show();

        // 窗口关闭确认
        primaryStage.setOnCloseRequest(event -> {
            System.out.println("程序关闭 - AI JavaFX 代码生成器");
        });
    }

    public static void main(String[] args) {
        // 添加启动日志
        System.out.println("AI JavaFX 代码生成器启动");

        // 检查API配置
        checkAPIConfig();

        // 启动JavaFX应用
        launch(args);
    }

    private static void checkAPIConfig() {
        try {
            // 使用反射访问SparkClient中的静态字段
            java.lang.reflect.Field appIdField = SparkClient.class.getDeclaredField("APP_ID");
            java.lang.reflect.Field apiKeyField = SparkClient.class.getDeclaredField("API_KEY");
            
            // 设置字段可访问
            appIdField.setAccessible(true);
            apiKeyField.setAccessible(true);
            
            String appId = (String) appIdField.get(null);
            String apiKey = (String) apiKeyField.get(null);

            // 检查是否是默认值（表示未配置）
            boolean isDefaultConfig = appId.equals("c40df7c9") &&
                    apiKey.equals("4b20a2a894a67bcc7a419fb193350b11");

            if (isDefaultConfig) {
                System.out.println("  警告：请配置有效的API密钥");
                System.out.println("请在 SparkClient.java 中更新您的API配置：");

            } else {
                System.out.println(" API配置检查通过");
            }

        } catch (NoSuchFieldException e) {
            System.out.println("  无法访问SparkClient配置字段: " + e.getMessage());
        } catch (IllegalAccessException e) {
            System.out.println("  无法访问SparkClient私有字段: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  API配置检查异常: " + e.getMessage());
        }
    }
}
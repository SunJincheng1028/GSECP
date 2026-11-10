package com.gsecp.ai;

/**
 * 大模型服务抽象。迭代 0 只定义接口并使用空实现；迭代 3 增加 DeepSeekAiClient，
 * 由 ai_service_config 与 AI 全局开关决定是否启用，业务层无需改动。
 */
public interface AiClient {

    boolean isEnabled();

    String chat(String prompt);
}

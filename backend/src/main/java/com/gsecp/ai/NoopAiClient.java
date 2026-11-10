package com.gsecp.ai;

import com.gsecp.common.BusinessException;
import com.gsecp.common.ResultCode;
import org.springframework.stereotype.Component;

@Component
public class NoopAiClient implements AiClient {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public String chat(String prompt) {
        throw new BusinessException(ResultCode.CONFLICT, "AI 功能尚未启用，请在迭代 3 配置大模型服务");
    }
}

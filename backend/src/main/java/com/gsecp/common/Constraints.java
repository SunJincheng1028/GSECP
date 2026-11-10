package com.gsecp.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * 步骤交付项的约束配置（来自 constraint_json）。解析失败时回退到默认值，不影响主流程。
 */
public record Constraints(
        int maxSizeMb,
        List<String> formats,
        int minWords,
        int maxWords,
        List<String> urlHosts
) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static Constraints defaults() {
        return new Constraints(50, List.of(), 1, 20000, List.of());
    }

    public static Constraints from(String json) {
        if (json == null || json.isBlank() || "null".equalsIgnoreCase(json)) {
            return defaults();
        }
        try {
            JsonNode node = MAPPER.readTree(json);
            return new Constraints(
                    node.path("maxSizeMb").asInt(50),
                    strings(node, "formats"),
                    node.path("minWords").asInt(1),
                    node.path("maxWords").asInt(20000),
                    strings(node, "urlHosts"));
        } catch (Exception e) {
            return defaults();
        }
    }

    private static List<String> strings(JsonNode node, String field) {
        JsonNode array = node.path(field);
        if (!array.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        array.forEach(item -> values.add(item.asText().toLowerCase()));
        return List.copyOf(values);
    }
}

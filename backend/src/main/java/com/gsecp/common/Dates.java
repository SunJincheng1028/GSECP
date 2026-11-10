package com.gsecp.common;

public final class Dates {

    private Dates() {
    }

    /** 把前端 datetime-local 值规范成 MySQL DATETIME 可接受的格式。 */
    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String result = value.trim().replace('T', ' ');
        if (result.length() == 16) {
            result += ":00";
        }
        return result;
    }
}

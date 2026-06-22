package com.buckle.inventory.util;

import java.util.regex.Pattern;

public class ShelfPositionValidator {

    private static final Pattern SHELF_POSITION_PATTERN = Pattern.compile("^[A-Z]-\\d{2}-\\d{2}$");

    public static final String FORMAT_HINT = "格式：A-01-02（字母-两位数字-两位数字）";

    public static boolean isValid(String shelfPosition) {
        if (shelfPosition == null || shelfPosition.trim().isEmpty()) {
            return true;
        }
        return SHELF_POSITION_PATTERN.matcher(shelfPosition.trim()).matches();
    }

    public static String validate(String shelfPosition, String fieldName) {
        if (!isValid(shelfPosition)) {
            return fieldName + "格式不正确，" + FORMAT_HINT;
        }
        return null;
    }
}

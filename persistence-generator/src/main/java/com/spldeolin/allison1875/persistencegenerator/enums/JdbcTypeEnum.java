package com.spldeolin.allison1875.persistencegenerator.enums;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Deolin 2020-07-12
 */
@Getter
@AllArgsConstructor
public enum JdbcTypeEnum {

    VARCHAR("varchar", null, String.class),

    CHAR("char", null, String.class),

    TEXT("text", null, String.class),

    LONGTEXT("longtext", null, String.class),

    TINYINT("tinyint", null, Byte.class),

    INT("int", null, Integer.class),

    BIGINT("bigint", null, Long.class),

    TINYINT_1(null, "tinyint(1)", Boolean.class),

    DATE("date", null, Date.class),

    TIME("time", null, Date.class),

    DATETIME("datetime", null, Date.class),

    TIMESTAMP("timestamp", null, Date.class),

    DECIMAL("decimal", null, BigDecimal.class);

    private final String dataType;

    private final String columnType;

    private final Class<?> javaType;

    public static JdbcTypeEnum ofDataType(String dataType) {
        if (dataType == null) {
            throw new IllegalArgumentException("illegal dataType [null]");
        }
        return Arrays.stream(values()).filter(one -> dataType.equalsIgnoreCase(one.getDataType())).findFirst()
                .orElse(null);
    }

    public static JdbcTypeEnum likeColumnType(String columnType) {
        if (columnType == null) {
            throw new IllegalArgumentException("illegal columnType [null]");
        }

        return Arrays.stream(values()).filter(one -> StringUtils.containsIgnoreCase(columnType, one.getColumnType()))
                .findFirst().orElse(null);
    }

}

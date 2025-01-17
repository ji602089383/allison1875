package com.spldeolin.allison1875.persistencegenerator.handle;

import java.math.BigDecimal;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import com.google.inject.Singleton;
import com.spldeolin.allison1875.base.ast.AstForest;
import com.spldeolin.allison1875.persistencegenerator.facade.javabean.JavaTypeNamingDto;
import com.spldeolin.allison1875.persistencegenerator.javabean.InformationSchemaDto;

/**
 * @author Deolin 2021-03-23
 */
@Singleton
public class JdbcTypeHandle {

    public JavaTypeNamingDto jdbcType2javaType(InformationSchemaDto columnMeta, AstForest astForest) {
        String columnType = columnMeta.getColumnType();
        String dataType = columnMeta.getDataType();
        if (columnType == null || dataType == null) {
            throw new IllegalArgumentException("illegal argument.");
        }
        if (StringUtils.containsIgnoreCase(columnType, "tinyint(1)")) {
            return new JavaTypeNamingDto().setClass(Boolean.class);
        }
        if (StringUtils.equalsAnyIgnoreCase(dataType, "varchar", "char", "text", "longtext")) {
            return new JavaTypeNamingDto().setClass(String.class);
        }
        if ("tinyint".equals(dataType)) {
            return new JavaTypeNamingDto().setClass(Byte.class);
        }
        if ("int".equals(dataType)) {
            return new JavaTypeNamingDto().setClass(Integer.class);
        }
        if ("bigint".equals(dataType)) {
            return new JavaTypeNamingDto().setClass(Long.class);
        }
        if ("date".equals(dataType)) {
            return new JavaTypeNamingDto().setClass(Date.class);
        }
        if ("time".equals(dataType)) {
            return new JavaTypeNamingDto().setClass(Date.class);
        }
        if ("datetime".equals(dataType)) {
            return new JavaTypeNamingDto().setClass(Date.class);
        }
        if ("timestamp".equals(dataType)) {
            return new JavaTypeNamingDto().setClass(Date.class);
        }
        if ("decimal".equals(dataType)) {
            return new JavaTypeNamingDto().setClass(BigDecimal.class);
        }
        return null;
    }

}
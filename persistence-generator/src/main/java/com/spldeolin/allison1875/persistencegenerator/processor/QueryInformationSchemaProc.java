package com.spldeolin.allison1875.persistencegenerator.processor;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Collection;
import java.util.stream.Collectors;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.spldeolin.allison1875.base.util.CollectionUtils;
import com.spldeolin.allison1875.persistencegenerator.PersistenceGeneratorConfig;
import com.spldeolin.allison1875.persistencegenerator.javabean.InformationSchemaDto;
import lombok.extern.log4j.Log4j2;

/**
 * @author Deolin 2020-07-12
 */
@Singleton
@Log4j2
public class QueryInformationSchemaProc {

    @Inject
    private PersistenceGeneratorConfig config;

    public Collection<InformationSchemaDto> process() {
//        Collection<InformationSchemaDto> result = Lists.newArrayList();

        try (Connection conn = DriverManager.getConnection(config.getJdbcUrl(), config.getUserName(),
                config.getPassword())) {
            String sql = Resources.toString(Resources.getResource("information_schema.sql"), StandardCharsets.UTF_8);
            String part = "IS NOT NULL";
            Collection<String> tables = config.getTables();
            if (CollectionUtils.isNotEmpty(tables)) {
                tables = tables.stream().map(one -> "'" + one + "'").collect(Collectors.toList());
                part = Joiner.on(',').appendTo(new StringBuilder("IN ("), tables).append(")").toString();
            }
            sql = sql.replace("${tableNames}", part);
            sql = sql.replace("${tableSchema}", "'" + config.getSchema() + "'");

            Result<Record> records = DSL.using(conn, SQLDialect.MYSQL).fetch(sql);
            return records.into(InformationSchemaDto.class);
//            for (Record record : records) {
//                InformationSchemaDto dto = new InformationSchemaDto();
//                dto.setTableName(record.getValue("TABLE_NAME", String.class));
//                dto.setTableComment(record.getValue("TABLE_COMMENT", String.class));
//                dto.setColumnName(record.getValue("COLUMN_NAME", String.class));
//                dto.setDataType(record.getValue("DATA_TYPE", String.class));
//                dto.setColumnType(record.getValue("COLUMN_TYPE", String.class));
//                dto.setColumnComment(record.getValue("COLUMN_COMMENT", String.class));
//                dto.setColumnKey(record.getValue("COLUMN_KEY", String.class));
//                dto.setCharacterMaximumLength(record.getValue("CHARACTER_MAXIMUM_LENGTH", Long.class));
//                dto.setIsNullable(record.getValue("IS_NULLABLE", String.class));
//                dto.setColumnDefault(record.getValue("COLUMN_DEFAULT", String.class));
//            }
        } catch (Exception e) {
            log.error("QueryInformationSchemaProc.process", e);
            return Lists.newArrayList();
        }
    }

}
package com.spldeolin.allison1875.persistencegenerator.processor.mapperxml;

import static com.spldeolin.allison1875.base.constant.BaseConstant.SINGLE_INDENT;

import java.util.Collection;
import java.util.List;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.spldeolin.allison1875.base.constant.BaseConstant;
import com.spldeolin.allison1875.persistencegenerator.PersistenceGeneratorConfig;
import com.spldeolin.allison1875.persistencegenerator.facade.javabean.PropertyDto;
import com.spldeolin.allison1875.persistencegenerator.javabean.KeyMethodNameDto;
import com.spldeolin.allison1875.persistencegenerator.javabean.PersistenceDto;

/**
 * 根据外键删除
 *
 * 表中每有几个外键，这个Proc就生成几个方法，以_id结尾的字段算作外键
 *
 * @author Deolin 2020-07-19
 */
@Singleton
public class DeleteByKeyXmlProc {

    @Inject
    private PersistenceGeneratorConfig persistenceGeneratorConfig;

    public Collection<String> process(PersistenceDto persistence, Collection<KeyMethodNameDto> KeyAndMethodNames) {
        if (persistenceGeneratorConfig.getDisableDeleteByKey() || !persistence.getIsDeleteFlagExist()) {
            return Lists.newArrayList();
        }
        Collection<String> result = Lists.newArrayList();
        for (KeyMethodNameDto KeyAndMethodName : KeyAndMethodNames) {
            List<String> xmlLines = Lists.newArrayList();
            PropertyDto key = KeyAndMethodName.getKey();
            xmlLines.add(String.format("<update id=\"%s\" parameterType=\"%s\">", KeyAndMethodName.getMethodName(),
                    key.getJavaType().getQualifier().replaceFirst("java\\.lang\\.", "")));
            xmlLines.add(SINGLE_INDENT + BaseConstant.FORMATTER_OFF_MARKER);
            xmlLines.add(SINGLE_INDENT + "UPDATE `" + persistence.getTableName() + "`");
            xmlLines.add(SINGLE_INDENT + "SET " + persistenceGeneratorConfig.getDeletedSql());
            xmlLines.add(SINGLE_INDENT + "WHERE `" + key.getColumnName() + "` = #{" + key.getPropertyName() + "}");
            xmlLines.add(SINGLE_INDENT + BaseConstant.FORMATTER_ON_MARKER);
            xmlLines.add("</update>");
            result.addAll(xmlLines);
            result.add("");
        }
        return result;
    }

}
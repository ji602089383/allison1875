package com.spldeolin.allison1875.persistencegenerator.processor.mapperxml;

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
 * 根据外键查询，表中每有几个外键，这个Proc就生成几个方法
 *
 * @author Deolin 2020-07-19
 */
@Singleton
public class QueryByKeyXmlProc {

    @Inject
    private PersistenceGeneratorConfig persistenceGeneratorConfig;

    public Collection<String> process(PersistenceDto persistence, Collection<KeyMethodNameDto> keyAndMethodNames) {
        if (persistenceGeneratorConfig.getDisableQueryByKey()) {
            return null;
        }
        Collection<String> result = Lists.newArrayList();
        for (KeyMethodNameDto keyAndMethodName : keyAndMethodNames) {
            List<String> xmlLines = Lists.newArrayList();
            PropertyDto key = keyAndMethodName.getKey();
            xmlLines.add(String.format("<select id=\"%s\" parameterType=\"%s\" resultMap=\"all\">",
                    keyAndMethodName.getMethodName(),
                    key.getJavaType().getQualifier().replaceFirst("java\\.lang\\.", "")));
            xmlLines.add(BaseConstant.SINGLE_INDENT + BaseConstant.FORMATTER_OFF_MARKER);
            xmlLines.add(BaseConstant.SINGLE_INDENT + "SELECT");
            xmlLines.add(BaseConstant.DOUBLE_INDENT + "<include refid=\"all\"/>");
            xmlLines.add(BaseConstant.SINGLE_INDENT + "FROM `" + persistence.getTableName() + "`");
            xmlLines.add(BaseConstant.SINGLE_INDENT + "WHERE TRUE");
            if (persistence.getIsDeleteFlagExist()) {
                xmlLines.add(BaseConstant.SINGLE_INDENT + "  AND " + persistenceGeneratorConfig.getNotDeletedSql());
            }
            xmlLines.add(BaseConstant.SINGLE_INDENT + "  AND `" + key.getColumnName() + "` = #{" + key.getPropertyName()
                    + "}");
            xmlLines.add(BaseConstant.SINGLE_INDENT + BaseConstant.FORMATTER_ON_MARKER);
            xmlLines.add("</select>");
            result.addAll(xmlLines);
            result.add("");
        }
        return result;
    }

}
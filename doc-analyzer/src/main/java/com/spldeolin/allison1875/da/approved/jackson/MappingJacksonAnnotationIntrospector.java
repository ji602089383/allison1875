package com.spldeolin.allison1875.da.approved.jackson;

import java.lang.reflect.Field;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.google.common.collect.Table;
import com.spldeolin.allison1875.base.util.JsonUtils;
import com.spldeolin.allison1875.da.approved.dto.CodeAndDescriptionDto;
import lombok.AllArgsConstructor;

/**
 * @author Deolin 2020-06-02
 */
@AllArgsConstructor
public class MappingJacksonAnnotationIntrospector extends JacksonAnnotationIntrospector {

    private static final long serialVersionUID = -6279240404102583448L;

    private final Table<String, String, String> enumDescriptions;

    private final Table<String, String, String> propertyDescriptions;

    private Field getFirstPropertyField(Class<?> enumType) {
        for (Field declaredField : enumType.getDeclaredFields()) {
            if (declaredField.getType() != enumType) {
                return declaredField;
            }
        }
        return null;
    }

    @Override
    public String[] findEnumValues(Class<?> enumType, Enum<?>[] enumValues, String[] names) {
        String[] result = new String[enumValues.length];

        Field codeField = getFirstPropertyField(enumType);
        if (codeField == null) {
            // has no field any more.
            return super.findEnumValues(enumType, enumValues, names);
        }

        Field descriptionField = null;
        try {
            descriptionField = enumType.getDeclaredField("description");
            descriptionField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            try {
                descriptionField = enumType.getDeclaredField("desc");
                descriptionField.setAccessible(true);
            } catch (NoSuchFieldException ignore) {
                // just enough
            }
        }

        codeField.setAccessible(true);
        for (int i = 0; i < enumValues.length; i++) {
            try {
                String code = codeField.get(enumValues[i]).toString();
                CodeAndDescriptionDto cad = new CodeAndDescriptionDto();
                cad.setCode(code);
                if (descriptionField != null) {
                    cad.setDescription(descriptionField.get(enumValues[i]).toString());
                } else {
                    cad.setDescription(
                            enumDescriptions.get(enumType.getName().replace('$', '.'), enumValues[i].name()));
                }
                result[i] = JsonUtils.toJson(cad);
            } catch (IllegalAccessException e) {
                // impossible unless bug
                return super.findEnumValues(enumType, enumValues, names);
            }
        }
        return result;
    }

    @Override
    public String findPropertyDescription(Annotated ann) {
        if (ann instanceof AnnotatedField) {
            AnnotatedField annf = (AnnotatedField) ann;
            String className = annf.getDeclaringClass().getName().replace('$', '.');
            String fieldName = annf.getName();
            String result = propertyDescriptions.get(className, fieldName);
            return result;
        }
        return "{}";
    }

}

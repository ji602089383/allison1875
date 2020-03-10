package com.spldeolin.allison1875.base.util;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.spldeolin.allison1875.base.classloader.WarOrFatJarClassLoaderFactory;
import com.spldeolin.allison1875.base.util.exception.JsonSchemasException;
import lombok.extern.log4j.Log4j2;

/**
 * @author Deolin 2020-03-01
 */
@Log4j2
public class JsonSchemas {

    private JsonSchemas() {
        throw new UnsupportedOperationException("Never instantiate me.");
    }

    private static final JsonSchemaGenerator defaultJsonSchemaGenerator;

    static {
        defaultJsonSchemaGenerator = new JsonSchemaGenerator(Jsons.initObjectMapper(new ObjectMapper()));
    }

    public static JsonSchema generateSchema(String qualifierForClassLoader) throws JsonSchemasException {
        try {
            JavaType javaType = new TypeFactory(null) {

                private static final long serialVersionUID = 2221941743132252200L;

                @Override
                public ClassLoader getClassLoader() {
                    return WarOrFatJarClassLoaderFactory.getClassLoader();
                }
            }.constructFromCanonical(qualifierForClassLoader);
            return defaultJsonSchemaGenerator.generateSchema(javaType);
        } catch (Exception e) {
            log.warn("qualifierForClassLoader={}", qualifierForClassLoader, e);
            throw new JsonSchemasException();
        }
    }

}

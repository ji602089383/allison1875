package com.spldeolin.allison1875.da.core.domain;

import java.util.Collection;
import com.spldeolin.allison1875.da.core.enums.FieldType;
import com.spldeolin.allison1875.da.core.enums.NumberFormatType;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * @author Deolin 2019-12-02
 */
@Data
@Accessors(fluent = true)
@ToString(exclude = {"parentField"}) // StackOverflowError
public class BodyFieldDomain {

    private BodyFieldDomain parentField;

    private String fieldName;

    /**
     * @see FieldType
     * @see ApiDomain#pathVariableFields() string, number, boolean
     * @see ApiDomain#requestParamFields() string, number, boolean
     */
    private FieldType jsonType;

    private String stringFormat;

    private NumberFormatType numberFormat;

    /**
     * notNull absent & notEmpty absent & notBlank absent = TRUE
     */
    private Boolean nullable;

    private Collection<ValidatorDomain> validators;

    private String description;

    /**
     * com.topaiebiz.rapgen2.enums.TypeName#object
     * com.topaiebiz.rapgen2.enums.TypeName#objectArray
     */
    private Collection<BodyFieldDomain> fields;

}
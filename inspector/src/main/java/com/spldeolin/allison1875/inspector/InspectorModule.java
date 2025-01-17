package com.spldeolin.allison1875.inspector;

import java.util.Collection;
import com.google.common.collect.Lists;
import com.google.inject.TypeLiteral;
import com.spldeolin.allison1875.base.ancestor.Allison1875MainProcessor;
import com.spldeolin.allison1875.base.ancestor.Allison1875Module;
import com.spldeolin.allison1875.base.util.ValidateUtils;
import com.spldeolin.allison1875.inspector.processor.Inspector;
import com.spldeolin.allison1875.inspector.statute.Statute;
import lombok.ToString;

/**
 * @author Deolin 2020-12-07
 */
@ToString
public class InspectorModule extends Allison1875Module {

    private final InspectorConfig inspectorConfig;

    public InspectorModule(InspectorConfig inspectorConfig) {
        this.inspectorConfig = inspectorConfig;
    }

    @Override
    protected void configure() {
        // bind config
        ValidateUtils.ensureValid(inspectorConfig);
        bind(InspectorConfig.class).toInstance(inspectorConfig);
        // bind statutes
        bind(new TypeLiteral<Collection<Statute>>() {
        }).toInstance(Lists.newArrayList());
    }

    @Override
    public Class<? extends Allison1875MainProcessor> provideMainProcessorType() {
        return Inspector.class;
    }

}
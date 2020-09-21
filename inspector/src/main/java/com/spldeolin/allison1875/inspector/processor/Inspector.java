package com.spldeolin.allison1875.inspector.processor;

import java.util.Collection;
import com.google.common.collect.Lists;
import com.spldeolin.allison1875.base.ancestor.Allison1875MainProcessor;
import com.spldeolin.allison1875.base.ast.AstForest;
import com.spldeolin.allison1875.base.ast.AstForestContext;
import com.spldeolin.allison1875.inspector.dto.LawlessDto;
import com.spldeolin.allison1875.inspector.dto.PardonDto;
import com.spldeolin.allison1875.inspector.statute.Statute;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author Deolin 2020-08-31
 */
@Accessors(fluent = true)
public class Inspector implements Allison1875MainProcessor {

    @Setter
    private Collection<Statute> statutes = Lists.newArrayList();

    @Override
    public void process(AstForest astForest) {
        AstForestContext.setCurrent(astForest);

        DetectPardonProc pardonDetectProc = new DetectPardonProc().process();
        Collection<PardonDto> pardons = pardonDetectProc.pardons();

        JudgeByStatutesProc judgeByStatutesProc = new JudgeByStatutesProc().statutes(statutes).pardons(pardons)
                .process();
        Collection<LawlessDto> lawlesses = judgeByStatutesProc.lawlesses();

        new ReportLawlessProc().lawlesses(lawlesses).process();
    }

}
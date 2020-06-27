package com.spldeolin.allison1875.docanalyzer.markdown;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import com.spldeolin.allison1875.base.exception.FreeMarkerPrintExcpetion;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * @author Deolin 2020-02-17
 */
public class FreeMarkerPrinter {

    public static void printToFile(EndpointVo ftl, File output) throws FreeMarkerPrintExcpetion {
        Configuration cfg = new Configuration(Configuration.getVersion());
        cfg.setClassForTemplateLoading(FreeMarkerPrinter.class, "/");
        cfg.setDefaultEncoding("utf-8");
        try (Writer out = new BufferedWriter(new FileWriter(output))) {
            Template template = cfg.getTemplate("simple-md-output.ftl");
            template.process(ftl, out);
        } catch (IOException | TemplateException e) {
            throw new FreeMarkerPrintExcpetion(e);
        }
    }

    public static String printAsString(EndpointVo ftl) throws FreeMarkerPrintExcpetion {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_30);
        cfg.setClassForTemplateLoading(FreeMarkerPrinter.class, "/");
        cfg.setDefaultEncoding("utf-8");

        try (StringWriter stringWriter = new StringWriter()) {
            Template template = cfg.getTemplate("simple-md-output.ftl");
            template.process(ftl, stringWriter);
            return stringWriter.toString();
        } catch (IOException | TemplateException e) {
            throw new FreeMarkerPrintExcpetion(e);
        }
    }

}
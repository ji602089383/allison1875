package com.spldeolin.allison1875.base.util.ast;

import java.io.File;
import java.util.Collection;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.CompilationUnit.Storage;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.spldeolin.allison1875.base.exception.StorageAbsentException;
import com.spldeolin.allison1875.base.util.FileBackupUtils;

/**
 * 将CompilationUnit保存到硬盘
 *
 * @author Deolin 2020-01-26
 */
public class Saves {

    /**
     * 以代码格式化的形式进行保存
     */
    public static void save(CompilationUnit cu) {
        Storage storage = cu.getStorage().orElseThrow(StorageAbsentException::new);
        File file = storage.getDirectory().resolve(storage.getFileName()).toFile();
        if (file.exists()) {
            FileBackupUtils.backup(file);
        }
        storage.save();
    }

    /**
     * 以代码格式化的形式进行保存
     */
    public static void save(Collection<CompilationUnit> cus) {
        cus.forEach(Saves::save);
    }

    /**
     * 以保持代码源格式的形式进行保存
     */
    public static void originalSave(CompilationUnit cu) {
        Storage storage = cu.getStorage().orElseThrow(StorageAbsentException::new);
        File file = storage.getDirectory().resolve(storage.getFileName()).toFile();
        if (file.exists()) {
            FileBackupUtils.backup(file);
        }
        storage.save(LexicalPreservingPrinter::print);
    }

}

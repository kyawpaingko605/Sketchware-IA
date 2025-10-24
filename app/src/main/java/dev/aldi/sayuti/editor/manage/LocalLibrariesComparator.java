package dev.aldi.sayuti.editor.manage;

import java.io.File;
import java.util.Comparator;
import pro.sketchware.utility.TranslationFunction;

public class LocalLibrariesComparator implements Comparator<File> {
    @Override
    public int compare(File first, File second) {
        return first.getName().compareTo(second.getName());
    }
}

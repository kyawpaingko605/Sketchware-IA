package com.besome.sketch.beans;

import java.util.ArrayList;
import pro.sketchware.utility.TranslationFunction;

public class MoreBlockCollectionBean extends SelectableBean {
    public ArrayList<BlockBean> blocks;
    public String name;
    public String spec;

    public MoreBlockCollectionBean(String str, String str2, ArrayList<BlockBean> arrayList) {
        name = str;
        spec = str2;
        blocks = arrayList;
    }
}

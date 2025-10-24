package com.besome.sketch.beans;

import a.a.a.nA;
import pro.sketchware.utility.TranslationFunction;

public class CollapsibleBean extends nA {
    public int buttonPressed = -1;
    public boolean isCollapsed = true;
    public boolean isConfirmation = false;
    public boolean isSelected = false;

    public void initValue() {
        isCollapsed = true;
        isConfirmation = false;
        isSelected = false;
        buttonPressed = -1;
    }
}

package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import java.util.List;

/**
 * Represents a breathing technique with multiple forms
 */
public class BreathingTechnique {
    private final String name;
    private final List<BreathingForm> forms;

    public BreathingTechnique(String name, List<BreathingForm> forms) {
        this.name = name;
        this.forms = forms;
    }

    public String getName() {
        return name;
    }

    public List<BreathingForm> getForms() {
        return forms;
    }

    public BreathingForm getForm(int index) {
        if (index >= 0 && index < forms.size()) {
            return forms.get(index);
        }
        return null;
    }

    public int getFormCount() {
        return forms.size();
    }
    
}

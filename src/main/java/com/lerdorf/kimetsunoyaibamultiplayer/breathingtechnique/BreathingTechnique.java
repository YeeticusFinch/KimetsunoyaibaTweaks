package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import java.util.List;

/**
 * Represents a breathing technique with multiple forms
 */
public class BreathingTechnique {
    private final String name;
    private final List<BreathingForm> forms;
    private final String techniqueColor;  // Minecraft color code for technique name (e.g., "§6" for gold)
    private final String formColor;       // Minecraft color code for form name (e.g., "§b" for aqua)

    public BreathingTechnique(String name, List<BreathingForm> forms) {
        this(name, forms, "", ""); // Default: no color
    }

    public BreathingTechnique(String name, List<BreathingForm> forms, String techniqueColor, String formColor) {
        this.name = name;
        this.forms = forms;
        this.techniqueColor = techniqueColor != null ? techniqueColor : "";
        this.formColor = formColor != null ? formColor : "";
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

    /**
     * Get the color code for the technique name display.
     * @return Minecraft color code (e.g., "§6" for gold) or empty string for no color
     */
    public String getTechniqueColor() {
        return techniqueColor;
    }

    /**
     * Get the color code for the form name display.
     * @return Minecraft color code (e.g., "§b" for aqua) or empty string for no color
     */
    public String getFormColor() {
        return formColor;
    }

}

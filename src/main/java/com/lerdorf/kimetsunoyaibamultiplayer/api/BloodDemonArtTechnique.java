package com.lerdorf.kimetsunoyaibamultiplayer.api;

import java.util.List;

/**
 * Represents a blood demon art with multiple forms.
 */
public class BloodDemonArtTechnique {
    private final String name;
    private final List<BloodDemonArtForm> forms;
    private final int displayColor;

    public BloodDemonArtTechnique(String name, List<BloodDemonArtForm> forms, int displayColor) {
        this.name = name;
        this.forms = forms;
        this.displayColor = displayColor;
    }

    public String getName() {
        return name;
    }

    public List<BloodDemonArtForm> getForms() {
        return forms;
    }

    public BloodDemonArtForm getForm(int index) {
        if (index < 0 || index >= forms.size()) {
            return null;
        }
        return forms.get(index);
    }

    public int getFormCount() {
        return forms.size();
    }

    public int getDisplayColor() {
        return displayColor;
    }
}

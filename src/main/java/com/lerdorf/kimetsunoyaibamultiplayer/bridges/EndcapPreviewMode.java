package com.lerdorf.kimetsunoyaibamultiplayer.bridges;

public enum EndcapPreviewMode {
    NONE,
    REGULAR,
    SHORT;

    public EndcapPreviewMode next() {
        EndcapPreviewMode[] values = EndcapPreviewMode.values();
        return values[(ordinal() + 1) % values.length];
    }
}

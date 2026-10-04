package com.gregtechceu.gtceu.data.pattern;

public enum StructureDefinitionSource {

    JSON("json"),
    BINARY_ZSTD("binary");

    private final String serializedName;

    StructureDefinitionSource(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public static StructureDefinitionSource fromDefinitionType(StructureDefinitionType type) {
        return switch (type) {
            case JSON -> JSON;
            case BINARY_ZSTD -> BINARY_ZSTD;
        };
    }
}

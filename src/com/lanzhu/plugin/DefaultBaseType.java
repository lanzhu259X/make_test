package com.lanzhu.plugin;

public enum DefaultBaseType {

    BYTE("byte", "0"),
    BYTE_O("Byte", "0"),
    SHORT("short", "0"),
    SHORT_O("Short", "0"),
    CHAR("char", "\'A\'"),
    CHARACTER("Character", "\'B\'"),
    INT("int", "0"),
    INTEGER("Integer", "1"),
    LONG("long", "1L"),
    LONG_O("Long", "2L"),
    FLOAT("float", "1.0"),
    FLOAT_O("Float", "2.0"),
    DOUBLE("double", "10.01"),
    DOUBLE_O("Double", "20.02"),
    BOOLEAN("boolean", "true"),
    BOOLEAN_O("Boolean", "false"),

    DATE("Date", "new Date()");


    private String code;
    private String value;
    DefaultBaseType(String code, String value) {
        this.code = code;
        this.value = value;
    }

    public static DefaultBaseType getByCode(String code) {
        if (code == null || code.trim().equals("")) {
            return null;
        }
        String codeStr = code.trim();
        for (DefaultBaseType type : DefaultBaseType.values()) {
            if (type.getCode().equals(codeStr)) {
                return type;
            }
        }
        return null;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

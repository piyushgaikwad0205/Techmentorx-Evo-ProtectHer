package com.example.sos;

public class HelplineModel {
    private String id;
    private String name;
    private String number;
    private String keyword;
    private String customMessage;

    public HelplineModel(String id, String name, String number, String keyword, String customMessage) {
        this.id = id;
        this.name = name;
        this.number = number;
        this.keyword = keyword;
        this.customMessage = customMessage;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getCustomMessage() {
        return customMessage;
    }

    public void setCustomMessage(String customMessage) {
        this.customMessage = customMessage;
    }
}

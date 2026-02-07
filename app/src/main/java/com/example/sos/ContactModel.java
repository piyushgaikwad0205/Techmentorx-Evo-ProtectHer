package com.example.sos;

public class ContactModel {
    String id, name, number;

    public ContactModel(String id, String name, String number, String keyword) {
        this.id = id;
        this.name = name;
        this.number = number;
        this.keyword = keyword;
    }

    String keyword;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public ContactModel() {
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
}

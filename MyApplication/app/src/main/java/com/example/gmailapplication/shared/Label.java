package com.example.gmailapplication.shared;

public class Label {
    public String id;     // UUID מהשרת
    public String name;   // שם התווית

    public Label() {}

    public Label(String name) {
        this.name = name;
    }

    public Label(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return name != null ? name : "";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Label label = (Label) obj;
        return (id != null ? id.equals(label.id) : label.id == null);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
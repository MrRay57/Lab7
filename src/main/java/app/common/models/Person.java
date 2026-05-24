package app.common.models;

import app.common.models.validators.Validatable;


/**
 * Класс, представляющий оператора фильма
 */
public class Person implements Validatable {
    private static final long serialVersionUID = 1L;

    private String name;
    private int height;
    private Color eyeColor;
    private Country nationality;
    private Location location;

    public Person(String name, int height, Color eyeColor, Country nationality, Location location) {
        this.name = name;
        this.height = height;
        this.eyeColor = eyeColor;
        this.nationality = nationality;
        this.location = location;
    }

    public String getName() { return name; }
    public int getHeight() { return height; }
    public Color getEyeColor() { return eyeColor; }
    public Country getNationality() { return nationality; }
    public Location getLocation() { return location; }

    @Override
    public boolean validate() {
        if (name == null || name.trim().isEmpty()) return false;
        if (height <= 0) return false;
        if (nationality == null) return false;
        if (location == null || !location.validate()) return false;
        return true;
    }

    @Override
    public String toString() {
        return "Person{name='" + name + "', height=" + height +
                ", eyeColor=" + eyeColor + ", nationality=" + nationality +
                ", location=" + location + "}";
    }
}
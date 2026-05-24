package app.common.models;

import app.common.models.validators.Validatable;


/**
 * Класс, описывающий координаты фильма
 */
public class Coordinates implements Validatable {
    private static final long serialVersionUID = 1L;

    private Long x;
    private double y;

    public Coordinates(Long x, double y) {
        this.x = x;
        this.y = y;
    }

    public Long getX() { return x; }
    public double getY() { return y; }

    @Override
    public boolean validate() {
        if (x == null) return false;
        if (x > 626) return false;
        if (Double.isNaN(y) || Double.isInfinite(y)) return false;
        return true;
    }

    @Override
    public String toString() {
        return "Coordinates{x=" + x + ", y=" + y + "}";
    }
}
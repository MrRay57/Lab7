package app.common.models;

import app.common.models.validators.Validatable;

import java.time.ZonedDateTime;

/**
 * Класс, представляющий фильм
 */
public class Movie implements Validatable, Comparable<Movie> {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private Coordinates coordinates;
    private java.time.ZonedDateTime creationDate;
    private long oscarsCount;
    private Float totalBoxOffice;
    private int length;
    private MovieGenre genre;
    private Person operator;

    public Movie(Long id, String name, Coordinates coordinates, ZonedDateTime creationDate,
            long oscarsCount, Float totalBoxOffice, int length,
            MovieGenre genre, Person operator) {
        this.id = id;
        this.name = name;
        this.coordinates = coordinates;
        this.creationDate = creationDate;
        this.oscarsCount = oscarsCount;
        this.totalBoxOffice = totalBoxOffice;
        this.length = length;
        this.genre = genre;
        this.operator = operator;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public ZonedDateTime getCreationDate() {
        return creationDate;
    }

    public long getOscarsCount() {
        return oscarsCount;
    }

    public Float getTotalBoxOffice() {
        return totalBoxOffice;
    }

    public int getLength() {
        return length;
    }

    public MovieGenre getGenre() {
        return genre;
    }

    public Person getOperator() {
        return operator;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCreationDate(ZonedDateTime creationDate) {
        this.creationDate = creationDate;
    }

    @Override
    public boolean validate() {
        if (id == null || id <= 0)
            return false;
        if (name == null || name.trim().isEmpty())
            return false;
        if (coordinates == null || !coordinates.validate())
            return false;
        if (creationDate == null)
            return false;
        if (oscarsCount <= 0)
            return false;
        if (totalBoxOffice == null || totalBoxOffice <= 0)
            return false;
        if (length <= 0)
            return false;
        if (operator != null && !operator.validate())
            return false;
        return true;
    }

    @Override
    public int compareTo(Movie other) {
        int nameCompare = this.name.compareTo(other.name);
        if (nameCompare != 0) {
            return nameCompare;
        }

        if (this.id == null && other.id == null)
            return 0;
        if (this.id == null)
            return -1;
        if (other.id == null)
            return 1;

        return this.id.compareTo(other.id);
    }

    @Override
    public String toString() {
        return "Movie{id=" + id + ", name='" + name + '\'' +
                ", coordinates=" + coordinates + ", creationDate=" + creationDate +
                ", oscarsCount=" + oscarsCount + ", totalBoxOffice=" + totalBoxOffice +
                ", length=" + length + ", genre=" + genre + ", operator=" + operator + "}";
    }
}
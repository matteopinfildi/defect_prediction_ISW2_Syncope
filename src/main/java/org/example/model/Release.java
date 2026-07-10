package org.example.model;
import java.time.LocalDate;

public class Release implements Comparable<Release> {
    public String name;
    public LocalDate date;

    public Release(String name, String dateStr) {
        this.name = name;
        this.date = LocalDate.parse(dateStr);
    }

    @Override
    public int compareTo(Release o) {
        return this.date.compareTo(o.date);
    }
}
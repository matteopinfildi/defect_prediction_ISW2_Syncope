package org.example.model;
import java.time.LocalDateTime;

public class JiraTicket {
    public String id;
    public LocalDateTime ovDate;
    public LocalDateTime fvDate;
    public LocalDateTime ivDateAv; // Da Jira
    public LocalDateTime estimatedIvDate;

    public JiraTicket(String id) { this.id = id; }
}
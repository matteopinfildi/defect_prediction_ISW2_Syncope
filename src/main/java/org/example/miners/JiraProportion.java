package org.example.miners;

import org.example.model.JiraTicket;
import org.example.model.Release;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class JiraProportion {

    public static Map<String, JiraTicket> fetchAndEstimate(String project, List<Release> targetReleases) throws Exception {
        Map<String, JiraTicket> tickets = new HashMap<>();
        HttpClient client = HttpClient.newHttpClient();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        System.out.println("   [PROPORTION] Scaricamento di TUTTE le release storiche per il calcolo degli indici...");
        String versionsUrl = "https://issues.apache.org/jira/rest/api/2/project/" + project + "/versions";
        HttpRequest versionsRequest = HttpRequest.newBuilder().uri(URI.create(versionsUrl)).build();
        HttpResponse<String> versionsResponse = client.send(versionsRequest, HttpResponse.BodyHandlers.ofString());

        JSONArray versionsArray = new JSONArray(versionsResponse.body());
        List<Release> allReleases = new ArrayList<>();

        for (int i = 0; i < versionsArray.length(); i++) {
            JSONObject v = versionsArray.getJSONObject(i);
            if (v.has("releaseDate") && v.has("released") && v.getBoolean("released")) {
                allReleases.add(new Release(v.getString("name"), v.getString("releaseDate")));
            }
        }
        Collections.sort(allReleases);
        System.out.println("   [PROPORTION] Trovate " + allReleases.size() + " release storiche.");

        String jql = "project=\"" + project + "\" AND issueType=\"Bug\" AND (status=\"closed\" OR status=\"resolved\") AND resolution=\"fixed\"";
        String encodedJql = URLEncoder.encode(jql, StandardCharsets.UTF_8);
        String url = "https://issues.apache.org/jira/rest/api/2/search?jql=" + encodedJql + "&fields=key,created,resolutiondate,versions&maxResults=1000";

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JSONArray issues = new JSONObject(response.body()).getJSONArray("issues");

        for (int i = 0; i < issues.length(); i++) {
            JSONObject issue = issues.getJSONObject(i);
            JSONObject fields = issue.getJSONObject("fields");
            JiraTicket ticket = new JiraTicket(issue.getString("key"));

            if (!fields.isNull("created")) ticket.ovDate = LocalDateTime.parse(fields.getString("created"), formatter);
            if (!fields.isNull("resolutiondate")) ticket.fvDate = LocalDateTime.parse(fields.getString("resolutiondate"), formatter);

            if (!fields.isNull("versions")) {
                JSONArray affectedVersions = fields.getJSONArray("versions");
                for (int j = 0; j < affectedVersions.length(); j++) {
                    JSONObject v = affectedVersions.getJSONObject(j);
                    if (!v.isNull("releaseDate")) {
                        LocalDateTime rd = LocalDateTime.parse(v.getString("releaseDate") + "T00:00:00.000+0000", formatter);
                        if (ticket.ivDateAv == null || rd.isBefore(ticket.ivDateAv)) {
                            ticket.ivDateAv = rd;
                        }
                    }
                }
            }
            if (ticket.ovDate != null && ticket.fvDate != null) {
                tickets.put(ticket.id, ticket);
            }
        }

        double sum = 0.0;
        int count = 0;

        for (JiraTicket t : tickets.values()) {
            int ivIdx = getIvIndex(t.ivDateAv, allReleases);
            int ovIdx = getOpeningVersionIndex(t.ovDate, allReleases);
            int fvIdx = getFixedVersionIndex(t.fvDate, allReleases);

            if (ivIdx != -1 && ovIdx != -1 && fvIdx != -1 && ivIdx <= ovIdx && ivIdx < fvIdx) {
                double proportion = (double) (fvIdx - ivIdx) / (double) (fvIdx - ovIdx);
                sum += proportion;
                count++;
            }
        }

        double pAverage = (count == 0) ? 0.0 : (sum / count);
        System.out.println("   [PROPORTION] P-Total (Media Indici) calcolato: " + String.format(Locale.US, "%.3f", pAverage));

        for (JiraTicket t : tickets.values()) {
            if (t.ivDateAv != null) {
                t.estimatedIvDate = t.ivDateAv;
            } else {
                int ovIdx = getOpeningVersionIndex(t.ovDate, allReleases);
                int fvIdx = getFixedVersionIndex(t.fvDate, allReleases);

                if (ovIdx == -1 || fvIdx == -1 || fvIdx <= ovIdx) {
                    t.estimatedIvDate = t.ovDate;
                } else {
                    int estimatedIvIndex = (int) Math.round(fvIdx - pAverage * (fvIdx - ovIdx));

                    if (estimatedIvIndex < 0) estimatedIvIndex = 0;
                    if (estimatedIvIndex > ovIdx) estimatedIvIndex = ovIdx;

                    t.estimatedIvDate = allReleases.get(estimatedIvIndex).date.atStartOfDay();
                }
            }
        }
        return tickets;
    }


    private static int getOpeningVersionIndex(LocalDateTime creationDate, List<Release> releases) {
        if (creationDate == null) return -1;
        int bestIndex = -1;
        for (int i = 0; i < releases.size(); i++) {
            if (!releases.get(i).date.isAfter(creationDate.toLocalDate())) {
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static int getFixedVersionIndex(LocalDateTime resolutionDate, List<Release> releases) {
        if (resolutionDate == null) return -1;
        for (int i = 0; i < releases.size(); i++) {
            if (releases.get(i).date.isAfter(resolutionDate.toLocalDate())) {
                return i;
            }
        }
        return -1;
    }

    private static int getIvIndex(LocalDateTime affectedDate, List<Release> releases) {
        if (affectedDate == null) return -1;
        for (int i = 0; i < releases.size(); i++) {
            if (releases.get(i).date.equals(affectedDate.toLocalDate())) {
                return i;
            }
        }
        return getOpeningVersionIndex(affectedDate, releases);
    }
}
package org.example.miners;

import org.example.model.DatasetRecord;
import org.example.model.JiraTicket;
import org.example.model.Release;
import org.example.utils.GitCommandRunner;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SZZLabeler {

    public static class BuggyFile {
        public String className;
        public LocalDateTime buggyDate;
        public LocalDateTime fixDate;
        public JiraTicket ticket;
        public String ticketId;

        public BuggyFile(String className, LocalDateTime buggyDate, LocalDateTime fixDate, JiraTicket ticket, String ticketId) {
            this.className = className;
            this.buggyDate = buggyDate;
            this.fixDate = fixDate;
            this.ticket = ticket;
            this.ticketId = ticketId;
        }
    }

    public static List<BuggyFile> extractAllBugs(String repoPath, Map<String, JiraTicket> tickets, List<String> logLines) {
        List<BuggyFile> allBugs = new ArrayList<>();

        Pattern ticketPattern = Pattern.compile("\\b[A-Z][A-Z0-9]+-\\d+\\b", Pattern.CASE_INSENSITIVE);
        Pattern hunkPattern = Pattern.compile("^@@\\s+-(\\d+)(?:,(\\d+))?\\s+\\+\\d+(?:,\\d+)?\\s+@@.*$");

        int commitCounter = 0;
        for (String line : logLines) {
            commitCounter++;

            if (commitCounter % 500 == 0) {
                System.out.println("   [PROGRESSO] Analizzati " + commitCounter + " commit su " + logLines.size() + "...");
            }

            String[] parts = line.split("\\|", 3);
            if (parts.length < 3) continue;

            String fixHash = parts[0];
            String subject = parts[1];
            String dateString = parts[2];

            Matcher m = ticketPattern.matcher(subject);
            if (!m.find()) continue;

            String tId = m.group().toUpperCase();
            if (!tickets.containsKey(tId)) continue;

            JiraTicket ticket = tickets.get(tId);
            LocalDateTime fixDate;
            try {
                fixDate = LocalDateTime.parse(dateString.substring(0, 19), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e) { continue; }

            String parentHash;
            try {
                List<String> parentOutput = GitCommandRunner.runCommand(repoPath, "git", "rev-list", "--parents", "-n", "1", fixHash);
                if (parentOutput.isEmpty()) continue;
                String[] parentParts = parentOutput.get(0).trim().split("\\s+");
                if (parentParts.length < 2) continue;
                parentHash = parentParts[1];
            } catch (Exception e) { continue; }

            List<String> diffLines;
            try {
                diffLines = GitCommandRunner.runCommand(repoPath, "git", "diff", "-U0", parentHash, fixHash);
            } catch (Exception e) { continue; }

            String currentFile = "";
            for (String diffLine : diffLines) {
                if (diffLine.startsWith("--- a/")) {
                    currentFile = diffLine.substring(6);
                } else if (diffLine.startsWith("@@") && isProductionJavaClass(currentFile)) {
                    Matcher hm = hunkPattern.matcher(diffLine);
                    if (hm.matches()) {
                        int startLine = Integer.parseInt(hm.group(1));
                        int count = (hm.group(2) == null) ? 1 : Integer.parseInt(hm.group(2));
                        int endLine = startLine + count - 1;

                        List<String> blame;
                        try {
                            blame = GitCommandRunner.runCommand(repoPath, "git", "blame", "-w", "-l", "-L", startLine + "," + endLine, parentHash, "--", currentFile);
                        } catch (Exception e) { continue; }

                        if (blame.isEmpty()) continue;

                        for (String blameLine : blame) {
                            try {
                                String buggyHash = blameLine.split("\\s+")[0].replace("^", "");
                                List<String> buggyDateOut = GitCommandRunner.runCommand(repoPath, "git", "show", "-s", "--format=%cI", buggyHash);
                                if (buggyDateOut.isEmpty()) continue;

                                LocalDateTime buggyDate = LocalDateTime.parse(buggyDateOut.get(0).substring(0, 19), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                                String javaClass = extractJavaClass(currentFile);
                                allBugs.add(new BuggyFile(javaClass, buggyDate, fixDate, ticket, tId));
                            } catch (Exception e) { continue; }
                        }
                    }
                }
            }
        }
        System.out.println("SZZ completato. Trovati " + allBugs.size() + " difetti totali.");
        return allBugs;
    }

    public static void labelBugginess(Map<String, DatasetRecord> recordsMap, Release release, List<BuggyFile> allBugs) {
        Map<String, String> shortNameMap = new HashMap<>();
        for (String fullName : recordsMap.keySet()) {
            String shortName = fullName.substring(fullName.lastIndexOf('.') + 1);
            shortNameMap.put(shortName, fullName);
        }

        for (BuggyFile bug : allBugs) {
            if (bug.buggyDate.toLocalDate().isAfter(release.date) ||
                    !bug.fixDate.toLocalDate().isAfter(release.date) ||
                    (bug.ticket.estimatedIvDate != null && release.date.isBefore(bug.ticket.estimatedIvDate.toLocalDate()))) {
                continue; // se si entra qua, la classe non è buggy
            }

            if (recordsMap.containsKey(bug.className)) {
                recordsMap.get(bug.className).setBugginess(1);
            }
            else {
                String bugShortName = bug.className.substring(bug.className.lastIndexOf('.') + 1);
                if (shortNameMap.containsKey(bugShortName)) {
                    String fullClassName = shortNameMap.get(bugShortName);
                    recordsMap.get(fullClassName).setBugginess(1);
                }
            }
        }
    }

    private static boolean isProductionJavaClass(String path) {
        String normalized = path.replace("\\", "/").trim();
        return normalized.endsWith(".java") && normalized.contains("/src/main/java/")
                && !normalized.contains("/src/test/") && !normalized.contains("/testDependencies/");
    }

    private static String extractJavaClass(String path) {
        String normalized = path.replace("\\", "/").trim();
        int index = normalized.indexOf("src/main/java/");
        if (index != -1) return normalized.substring(index + 14).replace("/", ".").replace(".java", "");
        return normalized.replace("/", ".").replace(".java", "");
    }

    public static void saveBugsToCache(List<BuggyFile> bugs, String cachePath) {
        try (java.io.FileWriter fw = new java.io.FileWriter(cachePath);
             java.io.PrintWriter pw = new java.io.PrintWriter(fw)) {
            for (BuggyFile bug : bugs) {
                pw.println(bug.className + "|" + bug.buggyDate + "|" + bug.fixDate + "|" + bug.ticketId);
            }
            System.out.println("Dati SZZ salvati in: " + cachePath);
        } catch (Exception e) {
            System.err.println("Errore nel salvataggio della cache: " + e.getMessage());
        }
    }

    public static List<BuggyFile> loadBugsFromCache(String cachePath, Map<String, JiraTicket> tickets) {
        List<BuggyFile> bugs = new ArrayList<>();
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(cachePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 4) {
                    String className = parts[0];
                    LocalDateTime buggyDate = LocalDateTime.parse(parts[1]);
                    LocalDateTime fixDate = LocalDateTime.parse(parts[2]);
                    String ticketId = parts[3];

                    JiraTicket ticket = tickets.get(ticketId);
                    if (ticket != null) {
                        bugs.add(new BuggyFile(className, buggyDate, fixDate, ticket, ticketId));
                    }
                }
            }
            System.out.println("Trovato.");
            return bugs;
        } catch (Exception e) {
            return null;
        }
    }

}
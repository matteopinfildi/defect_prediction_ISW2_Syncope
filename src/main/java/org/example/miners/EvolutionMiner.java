package org.example.miners;

import org.example.model.DatasetRecord;
import org.example.model.Release;
import org.example.utils.GitCommandRunner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class EvolutionMiner {

    public static void extractMetrics(String repoPath, Map<String, DatasetRecord> recordsMap, Release release, LocalDate previousReleaseDate) {
        String format = "COMMIT@@@%H@@@%cI@@@%aN@@@%s";

        List<String> logLines = GitCommandRunner.runCommand(repoPath,
                "git", "log", "--numstat", "--no-renames", "--reverse",
                "--before=" + release.date.toString() + "T23:59:59", "--format=" + format);

        Pattern fixPattern = Pattern.compile("(?i)(fix|bug|issue|syncope-\\d+)");

        String currentCommit = "";
        LocalDateTime currentCommitDate = null;
        String currentAuthor = "";
        boolean isFix = false;

        List<String[]> currentCommitStats = new ArrayList<>();

        for (int i = 0; i <= logLines.size(); i++) {
            String line = (i < logLines.size()) ? logLines.get(i) : "COMMIT@@@END";

            if (line.startsWith("COMMIT@@@")) {
                if (!currentCommitStats.isEmpty() && currentCommitDate != null) {
                    int changeSetSize = currentCommitStats.size();
                    LocalDate commitLocalDate = currentCommitDate.toLocalDate();

                    for (String[] stat : currentCommitStats) {
                        String path = stat[2].replace("\\", "/");
                        if (path.startsWith("\"") && path.endsWith("\"")) {
                            path = path.substring(1, path.length() - 1);
                        }

                        if (!path.endsWith(".java")) continue;

                        String className = path.replace(".java", "").replace("/", ".");
                        int srcMainJavaIdx = className.indexOf("src.main.java.");
                        if (srcMainJavaIdx != -1) {
                            className = className.substring(srcMainJavaIdx + 14);
                        }

                        DatasetRecord rec = null;
                        if (recordsMap.containsKey(className)) {
                            rec = recordsMap.get(className);
                        } else {
                            for (String key : recordsMap.keySet()) {
                                if (className.endsWith(key) || key.endsWith(className)) {
                                    rec = recordsMap.get(key);
                                    break;
                                }
                            }
                        }

                        if (rec != null) {
                            int added = 0;
                            int deleted = 0;
                            try {
                                added = Integer.parseInt(stat[0]);
                                deleted = Integer.parseInt(stat[1]);
                            } catch (NumberFormatException e) {
                                continue;
                            }
                            int touched = added + deleted;

                            if (rec.firstCommitDate == null || currentCommitDate.isBefore(rec.firstCommitDate)) {
                                rec.firstCommitDate = currentCommitDate;
                            }
                            if (rec.lastCommitDate == null || currentCommitDate.isAfter(rec.lastCommitDate)) {
                                rec.lastCommitDate = currentCommitDate;
                            }
                            if (isFix) {
                                rec.setNFix(rec.getNFix() + 1);
                            }

                            rec.setNr(rec.getNr() + 1);
                            rec.getAuthors().add(currentAuthor);
                            rec.setChurnTotal(rec.getChurnTotal() + touched);
                            if (touched > rec.getMaxChurn()) {
                                rec.setMaxChurn(touched);
                            }

                            boolean isCommitInCurrentRelease = previousReleaseDate == null || commitLocalDate.isAfter(previousReleaseDate);

                            if (isCommitInCurrentRelease) {
                                rec.revs++;
                                rec.added += added;
                                rec.setLocTouched(rec.getLocTouched() + touched);
                                rec.churn += (added - deleted);
                                if (isFix) rec.setFix(rec.getFix() + 1);

                                rec.touchedPerCommit.add(touched);
                                rec.changeSets.add(changeSetSize);
                            }
                        }
                    }
                }

                if (i < logLines.size()) {
                    String[] parts = line.split("@@@", 5);
                    if (parts.length >= 5) {
                        currentCommit = parts[1];
                        try {
                            currentCommitDate = LocalDateTime.parse(parts[2].substring(0, 19), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        } catch (Exception e) {
                            currentCommitDate = null;
                        }
                        currentAuthor = parts[3];
                        isFix = fixPattern.matcher(parts[4]).find();
                        currentCommitStats.clear();
                    }
                }
            } else if (!line.trim().isEmpty() && !line.startsWith("-")) {
                String[] stats = line.split("\\s+");
                if (stats.length >= 3 && !stats[0].equals("-") && !stats[1].equals("-")) {
                    currentCommitStats.add(new String[]{stats[0], stats[1], stats[2]});
                }
            }
        }

        for (DatasetRecord rec : recordsMap.values()) {
            if (rec.firstCommitDate != null) {
                long weeks = ChronoUnit.WEEKS.between(rec.firstCommitDate.toLocalDate(), release.date);
                rec.setAge((int) Math.max(0, weeks));
            }
            if (rec.lastCommitDate != null) {
                long weeks = ChronoUnit.WEEKS.between(rec.lastCommitDate.toLocalDate(), release.date);
                rec.setAoc((int) Math.max(0, weeks));
            }

            if (rec.revs > 0) {
                rec.setAla((double) rec.added / rec.revs);
                rec.setAvc((double) rec.churn / rec.revs);

                double sumChangeSet = 0;
                for (int cs : rec.changeSets) sumChangeSet += cs;
                rec.setAcss(sumChangeSet / rec.revs);

                rec.setNf(rec.changeSets.stream().max(Integer::compare).orElse(0));

                int totTouched = rec.getLocTouched();
                if (totTouched > 0) {
                    double ent = 0.0;
                    for (int t : rec.touchedPerCommit) {
                        if (t > 0) {
                            double p = (double) t / totTouched;
                            ent -= p * (Math.log(p) / Math.log(2));
                        }
                    }
                    rec.setEntropy(ent);
                }
            }
        }
    }
}

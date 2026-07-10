package org.example.miners;

import org.example.model.DatasetRecord;
import org.example.utils.GitCommandRunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Map;

public class PMDMiner {

    public static void extractSmells(String repoPath, Map<String, DatasetRecord> recordsMap, String pmdExecutablePath) {
        String reportPath = repoPath + File.separator + "pmd_report.csv";
        File reportFile = new File(reportPath);

        if (reportFile.exists()) reportFile.delete();
        String rulesets = "category/java/design.xml,category/java/errorprone.xml";
        System.out.println("   [PMD] Avvio scansione PMD..");

        List<String> output = GitCommandRunner.runCommand(repoPath,
                pmdExecutablePath, "check", "-d", repoPath, "-R", rulesets, "-f", "csv", "-r", reportPath);

        if (!reportFile.exists() || reportFile.length() == 0) {
            System.err.println("   [ERRORE PMD] Il report non è stato generato!");
            System.err.println("   Output dell'errore: " + output);
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(reportFile))) {
            String line;
            boolean firstLine = true;
            int smellsFound = 0;

            while ((line = br.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; }

                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String fileInfo = parts[2].replace("\"", "");
                    String matchClass = fileInfo.substring(Math.max(fileInfo.lastIndexOf('\\'), fileInfo.lastIndexOf('/')) + 1).replace(".java", "");

                    for (Map.Entry<String, DatasetRecord> entry : recordsMap.entrySet()) {
                        if (entry.getKey().endsWith(matchClass)) {
                            entry.getValue().setNSmells(entry.getValue().getNSmells() + 1);
                            smellsFound++;
                        }
                    }
                }
            }
            System.out.println("   [PMD] Analisi completata. Smell totali trovati: " + smellsFound);
        } catch (Exception e) {
            System.err.println("   [ERRORE PMD] Errore lettura report: " + e.getMessage());
        } finally {
            if (reportFile.exists()) reportFile.delete();
        }
    }
}

package org.example;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.example.miners.*;
import org.example.model.*;
import org.example.utils.*;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.util.*;

public class App {
    public static void main(String[] args) {
        String repoPath = "C:\\syncope_mining";
        String csvPath = "C:\\Users\\Dell\\OneDrive\\Desktop\\Dataset_Java_Final_Definitivo_AAA.csv";
        String pmdBatPath = "C:\\Users\\Dell\\Downloads\\pmd-dist-7.24.0-bin\\pmd-bin-7.24.0\\bin\\pmd.bat";

        List<Release> releases = Arrays.asList(
                new Release("syncope-1.0.0-incubating", "2012-08-06"),
                new Release("syncope-1.0.1-incubating", "2012-08-29"),
                new Release("syncope-1.0.3-incubating", "2012-09-30"),
                new Release("syncope-1.0.2-incubating", "2012-10-02"),
                new Release("syncope-1.0.4", "2012-12-10"),
                new Release("syncope-1.0.5", "2013-01-23"),
                new Release("syncope-1.0.6", "2013-02-27"),
                new Release("syncope-1.0.7", "2013-03-26"),
                new Release("syncope-1.1.0", "2013-04-05"),
                new Release("syncope-1.0.8", "2013-04-18"),
                new Release("syncope-1.1.1", "2013-04-29"),
                new Release("syncope-1.1.2", "2013-06-11"),
                new Release("syncope-1.1.3", "2013-07-12"),
                new Release("syncope-1.1.4", "2013-09-27"),
                new Release("syncope-1.1.5", "2013-11-28"),
                new Release("syncope-1.1.6", "2014-02-22"),
                new Release("syncope-1.1.7", "2014-04-11"),
                new Release("syncope-1.1.8", "2014-07-03"),
                new Release("syncope-1.2.0-M1", "2014-09-08"),
                new Release("syncope-1.2.0", "2014-10-03"),
                new Release("syncope-1.2.1", "2014-11-17"),
                new Release("syncope-1.2.2", "2015-01-30"),
                new Release("syncope-1.2.3", "2015-03-20"),
                new Release("syncope-1.2.4", "2015-05-05"),
                new Release("syncope-1.2.5", "2015-07-28"),
                new Release("syncope-1.2.6", "2015-11-05"),
                new Release("syncope-1.2.7", "2016-01-15")
        );

        List<DatasetRecord> totalDataset = new ArrayList<>();

        try {
            System.out.println("1. Scaricamento Jira e Calcolo PROPORTION METHOD in corso...");
            Map<String, JiraTicket> validTickets = JiraProportion.fetchAndEstimate("SYNCOPE", releases);
            System.out.println("Trovati " + validTickets.size() + " ticket validi e calcolata la stima IV.");

            System.out.println("  Inizio SZZ");
            GitCommandRunner.runCommand(repoPath, "git", "reset", "--hard");
            GitCommandRunner.runCommand(repoPath, "git", "clean", "-fd");

            List<String> checkoutResult = GitCommandRunner.runCommand(repoPath, "git", "checkout", "master");
            if (checkoutResult.toString().toLowerCase().contains("error")) {
                GitCommandRunner.runCommand(repoPath, "git", "checkout", "trunk");
            }

            String cachePath = "C:\\Users\\Dell\\OneDrive\\Desktop\\szz_bugs_cache.txt";
            File cacheFile = new File(cachePath);
            List<SZZLabeler.BuggyFile> allBugs;

            if (cacheFile.exists()) {
                allBugs = SZZLabeler.loadBugsFromCache(cachePath, validTickets);
            } else {
                System.out.println("   [ Nessuna cache trovata.");
                List<String> allLogLines = GitCommandRunner.runCommand(repoPath, "git", "log", "--all", "--format=%H|%s|%cI");
                System.out.println("   [SZZ] Caricati " + allLogLines.size() + " commit storici.");

                allBugs = SZZLabeler.extractAllBugs(repoPath, validTickets, allLogLines);
                SZZLabeler.saveBugsToCache(allBugs, cachePath);
            }


            LocalDate previousReleaseDate = null;

            for (Release release : releases) {
                System.out.println("ELABORAZIONE RELEASE: " + release.name);

                GitCommandRunner.runCommand(repoPath, "git", "reset", "--hard");
                GitCommandRunner.runCommand(repoPath, "git", "clean", "-fd");
                GitCommandRunner.runCommand(repoPath, "git", "checkout", release.name);

                Map<String, DatasetRecord> releaseMap = new HashMap<>();

                System.out.println("-> Calcolo metriche.");
                CKMiner.runCK(repoPath, releaseMap, release.name);
                EvolutionMiner.extractMetrics(repoPath, releaseMap, release, previousReleaseDate);
                PMDMiner.extractSmells(repoPath, releaseMap, pmdBatPath);

                GitCommandRunner.runCommand(repoPath, "git", "reset", "--hard");
                GitCommandRunner.runCommand(repoPath, "git", "clean", "-fd");

                checkoutResult = GitCommandRunner.runCommand(repoPath, "git", "checkout", "master");
                if (checkoutResult.toString().toLowerCase().contains("error")) {
                    GitCommandRunner.runCommand(repoPath, "git", "checkout", "trunk");
                }

                System.out.println("-> Calcolo Bugginess.");
                SZZLabeler.labelBugginess(releaseMap, release, allBugs);

                totalDataset.addAll(releaseMap.values());

                previousReleaseDate = release.date;
            }

            GitCommandRunner.runCommand(repoPath, "git", "checkout", "master");

            System.out.println("Scrittura del file CSV in corso: " + csvPath);

            String finalCsvPath = csvPath;
            try {
                new FileWriter(csvPath, true).close();
            } catch (Exception e) {
                finalCsvPath = csvPath.replace(".csv", "_BACKUP_SALVATAGGIO.csv");
                System.err.println("Nessun panico: salvo i dati in -> " + finalCsvPath + "\n");
            }

            try (FileWriter out = new FileWriter(finalCsvPath);
                 CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.builder().setHeader(
                         "Release", "Class_Name", "LOC", "LOC_TOUCHED", "AVC", "ALA", "ACSS", "AGE",
                         "AOC", "NFIX", "FIX", "WMC", "DIT", "CBO", "RFC", "LCOM", "NF", "ENTROPY",
                         "NSMELLS", "NR", "NAUTH", "CHURN", "MAX_CHURN", "Bugginess").build())) {

                int bugTrovati = 0;
                for (DatasetRecord r : totalDataset) {
                    printer.printRecord(
                            r.getRelease(), r.getClassName(),
                            r.getLoc(), r.getLocTouched(),
                            String.format(Locale.US, "%.2f", r.getAvc()),
                            String.format(Locale.US, "%.2f", r.getAla()),
                            String.format(Locale.US, "%.2f", r.getAcss()),
                            r.getAge(), r.getAoc(), r.getNFix(), r.getFix(),
                            r.getWmc(), r.getDit(), r.getCbo(), r.getRfc(), r.getLcom(),
                            r.getNf(),
                            String.format(Locale.US, "%.4f", r.getEntropy()),
                            r.getNSmells(),
                            r.getNr(),
                            r.getAuthors().size(),
                            r.getChurnTotal(),
                            r.getMaxChurn(),

                            r.getBugginess()
                    );
                    if (r.getBugginess() == 1) bugTrovati++;
                }
                System.out.println("FINITO! Istanze difettose trovate in totale: " + bugTrovati);
                System.out.println("Righe totali scritte nel dataset: " + totalDataset.size());
                System.out.println("Salvataggio completato con successo in: " + finalCsvPath);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
package org.example.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class GitCommandRunner {
    public static List<String> runCommand(String workingDir, String... command) {
        List<String> output = new ArrayList<>();
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(new File(workingDir));
            builder.redirectErrorStream(true);
            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.add(line);
                }
            }
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }
}

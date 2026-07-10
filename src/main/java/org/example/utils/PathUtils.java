package org.example.utils;

public class PathUtils {
    public static String gitPathToJavaClass(String gitPath) {
        if (gitPath == null || !gitPath.endsWith(".java")) return "";

        String normalized = gitPath.replace("\\", "/");
        if (normalized.contains("src/main/java/")) {
            normalized = normalized.substring(normalized.indexOf("src/main/java/") + 14);
        } else if (normalized.contains("core/src/main/java/")) {
            normalized = normalized.substring(normalized.indexOf("core/src/main/java/") + 19);
        } else if (normalized.contains("console/src/main/java/")) {
            normalized = normalized.substring(normalized.indexOf("console/src/main/java/") + 22);
        }

        normalized = normalized.replace(".java", "");
        return normalized.replace("/", ".");
    }
}
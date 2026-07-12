package org.example.miners;

import com.github.mauricioaniche.ck.CK;
import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKNotifier;
import org.example.model.DatasetRecord;

import java.util.Map;

public class CKMiner {
    public static void runCK(String repoPath, Map<String, DatasetRecord> recordsMap, String releaseName) {
        new CK().calculate(repoPath, new CKNotifier() {
            @Override
            public void notify(CKClassResult result) {
                if (result.getClassName().contains("$")) return;

                String filePath = result.getFile().replace("\\", "/").toLowerCase();
                String className = result.getClassName();

                if (filePath.contains("/test/") || className.endsWith("Test") || className.endsWith("ITCase")) {
                    return;
                }

                DatasetRecord record = recordsMap.computeIfAbsent(className,
                        k -> new DatasetRecord(releaseName, k));

                record.setWmc(result.getWmc());
                record.setDit(result.getDit());
                record.setCbo(result.getCbo());
                record.setRfc(result.getRfc());
                record.setLcom(result.getLcom());
                record.setLoc(result.getLoc());
            }

            @Override
            public void notifyError(String sourceFilePath, Exception e) {
                // errori silenziati
            }
        });
    }
}
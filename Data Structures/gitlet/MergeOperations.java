package gitlet;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** MergeOperations class for Gitlet.
 *  @author Erin Lee
 */
public class MergeOperations {

    /**
     * Returns the split point of the merge between CURRENTLASTCOMMIT and
     * GIVENLASTCOMMIT.
     */
    static String getSplitPointHeadMerged(Commit currentLastCommit,
                                          Commit givenLastCommit) {
        Map<String, Integer> currentCommitHistory = new HashMap<>();
        MergeOperations.addToCommitHistory(
                currentCommitHistory, currentLastCommit.getHash(), 1,
                Gitlet.COMMITS_FOLDER);

        Map<String, Integer> givenCommitHistory = new HashMap<>();
        MergeOperations.addToCommitHistory(
                givenCommitHistory, givenLastCommit.getHash(), 1,
                Gitlet.COMMITS_FOLDER);

        HashMap<String, Integer> commonSplitPoints = new HashMap<>();

        Set<String> givenCommitHashes = givenCommitHistory.keySet();
        for (String givenCommitHash : givenCommitHashes) {
            if (currentCommitHistory.containsKey(givenCommitHash)) {
                commonSplitPoints.put(givenCommitHash,
                        currentCommitHistory.get(givenCommitHash)
                                + givenCommitHistory.get(givenCommitHash));
            }
        }

        String splitPointHead = null;
        if (!commonSplitPoints.isEmpty()) {
            LinkedHashMap<String, Integer> commonOrderedSplitPoints =
                    sortByValue(commonSplitPoints);
            splitPointHead =
                    commonOrderedSplitPoints.keySet().iterator().next();
        }
        return splitPointHead;
    }

    /**
     * Returns merge of CURRENTCOMMIT and GIVENCOMMIT on CURRENTSTAGE given a
     * SPLITPOINTCOMMIT.
     */
    static Commit merge(Stage currentStage, Commit currentCommit,
                        Commit givenCommit, Commit splitPointCommit) {
        currentStage = mergeFiles(currentStage, currentCommit, givenCommit,
                splitPointCommit);
        Commit commit = new Commit(currentStage, currentCommit, givenCommit);
        if (currentStage.isConflicted()) {
            System.out.println("Encountered a merge conflict.");
        }
        commit.save();
        currentStage.applyCommit();
        return commit;
    }

    /**
     * Returns a new stage that is the result of merging CURRENTCOMMIT and
     * GIVENCOMMIT that split at SPLITPOINTCOMMIT in the CURRENTSTAGE.
     */
    private static Stage mergeFiles(Stage currentStage, Commit currentCommit,
                        Commit givenCommit, Commit splitPointCommit) {
        Map<String, String> givenFileInfos = givenCommit.getFileInfos();
        if (!givenFileInfos.isEmpty()) {
            Set<String> givenFileNames = givenFileInfos.keySet();
            for (String givenFileName: givenFileNames) {
                String givenFileHash = givenFileInfos.get(givenFileName);
                String currentFileHash =
                        currentCommit.getFileHash(givenFileName);
                String splitPointFileHash =
                        splitPointCommit.getFileHash(givenFileName);
                if (splitPointFileHash != null) {
                    if (!givenFileHash.equals(splitPointFileHash)) {
                        if (currentFileHash != null
                                && currentFileHash.equals(splitPointFileHash)) {
                            currentStage.checkout(givenFileHash);
                            currentStage.add(givenFileName, givenFileHash);
                        } else if (givenFileHash.equals(currentFileHash)) {
                            continue;
                        } else {
                            replacedConflictedFile(currentStage, givenFileName,
                                    currentFileHash, givenFileHash);
                        }
                    }
                } else {
                    if (currentFileHash == null) {
                        currentStage.checkout(givenFileHash);
                        currentStage.add(givenFileName, givenFileHash);
                    } else {
                        replacedConflictedFile(currentStage, givenFileName,
                                currentFileHash, givenFileHash);
                    }
                }
            }
        }
        Map<String, String> currentFileInfos = currentCommit.getFileInfos();
        if (!currentFileInfos.isEmpty()) {
            Set<String> currentFileNames = currentFileInfos.keySet();
            for (String currentFileName: currentFileNames) {
                String currentFileHash = currentFileInfos.get(currentFileName);
                String givenFileHash = givenCommit.getFileHash(currentFileName);
                String splitPointFileHash =
                        splitPointCommit.getFileHash(currentFileName);
                if (givenFileHash != null) {
                    continue;
                }
                if (splitPointFileHash != null) {
                    if (currentFileHash.equals(splitPointFileHash)) {
                        currentStage.unstage(currentFileName);
                    } else {
                        replacedConflictedFile(currentStage, currentFileName,
                                currentFileHash, givenFileHash);
                    }
                }
            }
        }
        return currentStage;
    }

    /**
     * Replace the contents of a file with FILENAME conflicted in
     * CURRENTFILEHASH AND GIVENFILEHASH on CURRENTSTAGE.
     */
    private static void replacedConflictedFile(
            Stage currentStage, String fileName,
            String currentFileHash, String givenFileHash) {
        String currentContent = "";
        if (currentFileHash != null) {
            Blob currentBlob = Blob.load(currentFileHash);
            if (currentBlob != null) {
                currentContent += new String(
                        currentBlob.getFileContents(), StandardCharsets.UTF_8);
            }
        }

        String givenContent = "";
        if (givenFileHash != null) {
            Blob givenBlob = Blob.load(givenFileHash);
            if (givenBlob != null) {
                givenContent += new String(
                        givenBlob.getFileContents(), StandardCharsets.UTF_8);
            }
        }

        File contentFile = new File(fileName);
        Utils.writeContents(contentFile,
                "<<<<<<< HEAD\n"
                        + currentContent
                        +  "=======\n"
                        + givenContent
                        + ">>>>>>>\n");

        Blob blob = new Blob(contentFile);
        blob.save();
        String newFileHash = blob.getHash();

        currentStage.add(fileName, newFileHash);
        currentStage.addConflict(fileName, newFileHash, currentFileHash,
                givenFileHash);
    }

    /**
     * Returns the COMMITHISTORY of a starting point COMMITHASH to DISTANCE
     * into the given COMMITFOLDER.
     */
    public static void addToCommitHistory(Map<String, Integer> commitHistory,
            String commitHash, int distance, File commitFolder) {
        if (commitHash != null) {
            commitHistory.put(commitHash, distance);
            Commit commit = Commit.load(commitHash, commitFolder);
            addToCommitHistory(commitHistory, commit.getParent(),
                    distance + 1, commitFolder);
            addToCommitHistory(commitHistory, commit.getGivenParent(),
                    distance + 1, commitFolder);
        }
    }

    /**
     * Returns the sorted version of the given MAP.
     */
    private static LinkedHashMap<String, Integer> sortByValue(
            Map<String, Integer> map) {
        List<String> mapKeys = new ArrayList<>(map.keySet());
        List<Integer> mapValues = new ArrayList<>(map.values());
        Collections.sort(mapValues);
        Collections.sort(mapKeys);

        LinkedHashMap<String, Integer> sortedMap = new LinkedHashMap<>();

        Iterator<Integer> valueIt = mapValues.iterator();
        while (valueIt.hasNext()) {
            Integer val = valueIt.next();
            Iterator<String> keyIt = mapKeys.iterator();

            while (keyIt.hasNext()) {
                String key = keyIt.next();
                Integer comp1 = map.get(key);
                Integer comp2 = val;

                if (comp1.equals(comp2)) {
                    keyIt.remove();
                    sortedMap.put(key, val);
                    break;
                }
            }
        }
        return sortedMap;
    }
}

package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;


/** Stage class for Gitlet.
 *  @author Erin Lee
 */
public class Stage implements Serializable {

    /**
     * Creates a gitlet staging area of a branch with given BRANCHNAME.
     */
    public Stage(String branchName) {
        _branchName = branchName;
        _tracked = new HashMap<>();
        _added = new TreeMap<>();
        _removed = new TreeMap<>();
        _conflicted = new TreeMap<>();
    }

    /**
     * Sets the name of the branch to BRANCHNAME.
     */
    void setBranchName(String branchName) {
        _branchName = branchName;
    }

    /**
     * Return the name of branch.
     */
    String getBranchName() {
        return _branchName;
    }

    /**
     * Sets name of merged branch to MERGEDBRANCHNAME.
     */
    void setMergedBranchName(String mergedBranchName) {
        _mergedBranchName = mergedBranchName;
    }

    /**
     * Return the name of branch.
     */
    String getMergedBranchName() {
        return _mergedBranchName;
    }

    /**
     * Return the tracked blobs.
     */
    HashMap<String, String> getTracked() {
        return _tracked;
    }

    /**
     * Return the union of added and modified.
     */
    TreeMap<String, String> getAdded() {
        return _added;
    }

    /**
     * Returns the removed.
     */
    TreeMap<String, String> getRemoved() {
        return _removed;
    }

    /**
     * Returns the conflicted.
     */
    TreeMap<String, String[]> getConflicted() {
        return _conflicted;
    }

    /**
     *  Returns true if the staging area has changed.
     */
    boolean isChanged() {
        return _removed.size() + _added.size() != 0;
    }

    /**
     * Returns true if the staging area is conflicted.
     */
    boolean isConflicted() {
        return !_conflicted.isEmpty();
    }

    /**
     * Checks out a particular file of the given FILEHASH.
     */
    void checkout(String fileHash) {
        Blob blob = Blob.load(fileHash);
        if (blob == null) {
            return;
        }

        String fileName = blob.getFileName();
        File file = new File(Gitlet.CWD, blob.getFileName());
        Utils.writeContents(file, blob.getFileContents());

        _added.remove(fileName);
        _removed.remove(fileName);
    }

    /**
     * Checks out all the files tracked by the given COMMIT.
     * Removes tracked files that are not present in that commit.
     * Also moves the current branch's head to that commit node
     */
    void checkout(Commit commit) {
        List<String> fileNames = Utils.plainFilenamesIn(Gitlet.CWD);
        for (String fileName: fileNames) {
            if (!getTracked().containsKey(fileName)
                    && !getAdded().containsKey(fileName)) {
                throw new GitletException("There is an untracked file in the "
                    + "way; delete it, or add and commit it first.");
            }
        }
        reset(commit);
    }

    /**
     * Resets the given COMMIT.
     */
    void reset(Commit commit) {
        List<String> fileNames = Utils.plainFilenamesIn(Gitlet.CWD);
        for (String fileName: fileNames) {
            if (!commit.containsFile(fileName)) {
                Utils.restrictedDelete(fileName);
            }
        }

        Map<String, String> fileInfos = commit.getFileInfos();
        Set<String> commitFileNames = fileInfos.keySet();
        for (String fileName: commitFileNames) {
            Blob blob = Blob.load(fileInfos.get(fileName));
            File newFile = new File(Gitlet.CWD, blob.getFileName());
            Utils.writeContents(newFile, blob.getFileContents());
        }

        _tracked = commit.getTrackedFileInfos();
        _added = commit.getAddedFileInfos();
        _removed = commit.getRemovedFileInfos();
        applyCommit();
    }

    /**
     * Adds a blob with FILENAME and BLOBHASH to the staging area.
     * Staging an already-staged file overwrites the previous entry in the
     * staging area with the new contents.
     * If the current working version of the file is identical to the version
     * in the current commit, do not stage it to be added, and remove it from
     * the staging area if it is already there (as can happen when a file is
     * changed, added, and then changed back). The file will no longer be
     * staged for removal (see gitlet rm), if it was at the time of the command.
     */
    void add(String fileName, String blobHash) {
        if (_removed.containsKey(fileName)) {
            _removed.remove(fileName);
        } else {
            String trackedBlobHash = _tracked.get(fileName);
            if (trackedBlobHash == null) {
                _added.put(fileName, blobHash);
            } else if (trackedBlobHash.equals(blobHash)) {
                if (_added.containsKey(fileName)) {
                    _added.remove(fileName);
                }
            } else {
                _added.put(fileName, blobHash);
            }
        }
    }

    /**
     * Unstage the file with FILENAME if it is currently staged for addition.
     * If the file is tracked in the current commit, stage it for removal.
     */
    void unstage(String fileName) {
        if (!_tracked.containsKey(fileName) && !_added.containsKey(fileName)) {
            throw new GitletException("No reason to remove the file.");
        }

        if (_added.containsKey(fileName)) {
            _added.remove(fileName);
        }

        if (_tracked.containsKey(fileName)) {
            _removed.put(fileName, _tracked.get(fileName));
            Utils.restrictedDelete(fileName);
        }
    }

    /**
     * Puts conflicted commit with FILENAME, NEWFILEHASH, CURRENTFILEHASH, and
     * GIVENFILEHASH into the conflicted folder.
     */
    void addConflict(String fileName, String newFileHash,
                     String currentFileHash, String givenFileHash) {
        _conflicted.put(fileName,
                new String[] { newFileHash, currentFileHash, givenFileHash });
    }

    /**
     * Applies commit to stage.
     */
    void applyCommit() {
        if (!_added.isEmpty()) {
            _tracked.putAll(_added);
        }
        if (!_removed.isEmpty()) {
            Set<String> removedFileNames = _removed.keySet();
            for (String fileName: removedFileNames) {
                _tracked.remove(fileName);
            }
        }
        clear();
    }

    /**
     * Clears the stage.
     */
    private void clear() {
        _added.clear();
        _removed.clear();
        _conflicted.clear();
    }

    /**
     * Saves the staged file.
     */
    void save() {
        Utils.writeObject(Utils.join(Gitlet.STAGE_FOLDER,
                _branchName.replace('/', '-')), this);
    }

    /**
     * Returns the stage of branch with BRANCHNAME.
     */
    static Stage load(String branchName) {
        File file = Utils.join(Gitlet.STAGE_FOLDER,
                branchName.replace('/', '-'));
        if (!file.exists()) {
            return null;
        }
        return Utils.readObject(file, Stage.class);
    }

    /**
     * Prints the staged files on the stage.
     */
    void printStagedFiles() {
        Set<String> addedFileNames = _added.keySet();
        for (String fileName : addedFileNames) {
            if (checkFileModified(fileName, _added.get(fileName)) == 0) {
                System.out.println(fileName);
            }
        }
    }

    /**
     * Prints the files that have been removed from the stage.
     */
    void printRemovedFiles() {
        Set<String> removedFileNames = _removed.keySet();
        for (String filename : removedFileNames) {
            System.out.println(filename);
        }
    }

    /**
     * Prints files that are not staged for removal, but tracked in the current
     * commit and deleted from the working directory.
     */
    void printDeletedButNotStagedFiles() {
        ArrayList<String> deleted = new ArrayList<>();
        List<String> workingFileNames = Utils.plainFilenamesIn(Gitlet.CWD);
        Set<String> trackedFileNames = _tracked.keySet();
        for (String trackedFileName: trackedFileNames) {
            if (!workingFileNames.contains(trackedFileName)
                    && !_removed.containsKey(trackedFileName)) {
                deleted.add(trackedFileName + " (deleted)");
            }
        }
        Collections.sort(deleted);
        for (String fileName : deleted) {
            System.out.println(fileName);
        }
    }

    /**
     * Prints files tracked in the current commit, changed in the working
     * directory, but not staged or staged for addition, but with different
     * contents than in the working directory.
     */
    void printModifiedButNotStagedFiles() {
        ArrayList<String> modified = new ArrayList<>();
        List<String> workingFileNames = Utils.plainFilenamesIn(Gitlet.CWD);
        Set<String> trackedFileNames = _tracked.keySet();
        for (String trackedFileName: trackedFileNames) {
            if (workingFileNames.contains(trackedFileName)
                    && !_added.containsKey(trackedFileName)
                    && (checkFileModified(trackedFileName,
                    _tracked.get(trackedFileName)) == 1)) {
                modified.add(trackedFileName + " (modified)");
            }
        }
        Collections.sort(modified);
        for (String fileName : modified) {
            System.out.println(fileName);
        }
    }

    /**
     * Prints files present in the working directory but neither staged for
     * addition nor tracked.
     * This includes files that have been staged for removal, but then
     * re-created without Gitlet's knowledge.
     */
    void printUntrackedFiles() {
        ArrayList<String> untracked = new ArrayList<>();
        List<String> workingFileNames = Utils.plainFilenamesIn(Gitlet.CWD);
        for (String fileName: workingFileNames) {
            if (!getAdded().containsKey(fileName)
                    && !getTracked().containsKey(fileName)) {
                untracked.add(fileName);
            }
        }
        Collections.sort(untracked);
        for (String fileName : untracked) {
            System.out.println(fileName);
        }
        System.out.println();
    }

    /**
     * Returns int determining if a file with FILENAME and BLOBHASH has been
     * deleted or modified.
     *  -1: deleted.
     *  0: not modified (hash changed).
     *  1: modified.
     */
    private int checkFileModified(String fileName, String blobHash) {
        File file = new File(fileName);
        String fileHash = Utils.sha1(fileName, Utils.readContents(file));
        int modified = 0;
        if (!file.exists()) {
            modified = -1;
        } else if (!fileHash.equals(blobHash)) {
            modified = 1;
        }
        return modified;
    }

    /** String name of branch. */
    private String _branchName;

    /** String name of merged branch. */
    private String _mergedBranchName;

    /** Files tracked in the current commit. */
    private HashMap<String, String> _tracked;

    /** Added files. i.e. files to be added. */
    private TreeMap<String, String> _added;

    /** Removed files. */
    private TreeMap<String, String> _removed;

    /** FileName: [fileHash of merged, fileHash of current branch, fileHash of
     * given branch] if file has been deleted, hash is null. */
    private TreeMap<String, String[]> _conflicted;
}

package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.List;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.Locale;


/** Commit class for Gitlet.
 *  @author Erin Lee
 */
public class Commit implements Serializable {

    /**
     * A new commit in the current system with a string MESSAGE, a stage of
     * STAGE, and a parent PARENT.
     */
    Commit(String message, Stage stage, String parent) {
        if (parent == null) {
            _timestamp = new Date(0);
        } else {
            _timestamp = new Date();
        }
        _message = message;
        if (stage == null) {
            _trackedFileInfos = new HashMap<>();
            _addedFileInfos = new TreeMap<>();
            _removedFileInfos = new TreeMap<>();
        } else {
            _trackedFileInfos = stage.getTracked();
            _addedFileInfos = stage.getAdded();
            _removedFileInfos = stage.getRemoved();
        }
        _hash = Utils.sha1(_timestamp.toString(),
                (_message != null ? _message : ""),
                (_trackedFileInfos.toString()),
                (_addedFileInfos.toString()),
                (_removedFileInfos.toString()));
        _parent = parent;
    }

    /**
     * A new commit merged with CURRENTCOMMIT and GIVENCOMMIT in the stage
     * STAGE.
     */
    Commit(Stage stage, Commit currentCommit, Commit givenCommit) {
        _timestamp = new Date();
        _message = "Merged " + stage.getMergedBranchName()
                + " into " + stage.getBranchName() + ".";
        _trackedFileInfos = stage.getTracked();
        _addedFileInfos = stage.getAdded();
        _removedFileInfos = stage.getRemoved();

        _hash = Utils.sha1(_timestamp.toString(),
                (_message != null ? _message : ""),
                (_trackedFileInfos.toString()),
                (_addedFileInfos.toString()),
                (_removedFileInfos.toString()));
        _parent = currentCommit.getHash();
        _givenParent = givenCommit.getHash();
    }

    /**
     * Returns the HashMap that maps a tracked filename to its sha1 value.
     */
    HashMap<String, String> getTrackedFileInfos() {
        return _trackedFileInfos;
    }

    /**
     * Returns the HashMap that maps an added filename to its sha1 value.
     */
    TreeMap<String, String> getAddedFileInfos() {
        return _addedFileInfos;
    }

    /**
     * Returns the HashMap that maps a removed filename to its sha1 value.
     */
    TreeMap<String, String> getRemovedFileInfos() {
        return _removedFileInfos;
    }

    /**
     * Returns the file infos of the commit.
     */
    HashMap<String, String> getFileInfos() {
        HashMap<String, String> fileInfos = new HashMap<>();
        if (!_trackedFileInfos.isEmpty()) {
            Set<String> trackedFileNames = _trackedFileInfos.keySet();
            for (String fileName: trackedFileNames) {
                if (!_removedFileInfos.containsKey(fileName)) {
                    fileInfos.put(fileName, _trackedFileInfos.get(fileName));
                }
            }
        }
        if (!_addedFileInfos.isEmpty()) {
            Set<String> addedFileNames = _addedFileInfos.keySet();
            for (String fileName: addedFileNames) {
                fileInfos.put(fileName, _addedFileInfos.get(fileName));
            }
        }
        return fileInfos;
    }

    /**
     * Returns the HashCode for this particular commit.
     */
    String getHash() {
        return _hash;
    }

    /**
     * Returns the HashCode for this parent commit.
     */
    String getParent() {
        return _parent;
    }

    /**
     * Returns the HashCode for this given parent commit.
     */
    String getGivenParent() {
        return _givenParent;
    }

    /** Returns the message for this particular commit. */
    String getMessage() {
        return _message;
    }

    /**
     * Saves a commit to the commits folder.
     */
    void save() {
        Utils.writeObject(Utils.join(Gitlet.COMMITS_FOLDER, _hash), this);
    }

    /**
     * Saves a commit to the COMMITFOLDER.
     */
    void save(File commitFolder) {
        Utils.writeObject(Utils.join(commitFolder, _hash), this);
    }

    /**
     * Returns a commit with the given HASH.
     */
    static Commit load(String hash) {
        return load(hash, null);
    }

    /**
     * Returns a commit read into COMMITFOLDER with the given HASH.
     */
    static Commit load(String hash, File commitFolder) {
        if (commitFolder == null) {
            commitFolder = Gitlet.COMMITS_FOLDER;
        }
        File file = Utils.join(commitFolder, hash);
        if (!file.exists()) {
            if (hash.length() >= 6) {
                List<String> commitFiles = Utils.plainFilenamesIn(commitFolder);
                for (String commitFile : commitFiles) {
                    if (commitFile.startsWith(hash)) {
                        return Utils.readObject(Utils.join(commitFolder,
                                commitFile), Commit.class);
                    }
                }
                return null;
            } else {
                return null;
            }
        }
        return Utils.readObject(file, Commit.class);
    }

    /**
     * Returns true if the commit references a file with FILENAME.
     */
    boolean containsFile(String fileName) {
        return !_removedFileInfos.containsKey(fileName)
                && (_addedFileInfos.containsKey(fileName)
                || (_trackedFileInfos.containsKey(fileName)));
    }

    /**
     * Returns the hash of the file with the given FILENAME.
     */
    String getFileHash(String fileName) {
        if (_removedFileInfos.containsKey(fileName)) {
            return null;
        }
        String fileHash = _addedFileInfos.get(fileName);
        if (fileHash != null) {
            return fileHash;
        }
        return _trackedFileInfos.get(fileName);
    }

    /**
     * Returns the hash of the commit of the given MESSAGE.
     */
    static List<String> getCommitHashes(String message) {
        List<String> commitHashes = new ArrayList<>();
        List<String> allCommitHashes =
                Utils.plainFilenamesIn(Gitlet.COMMITS_FOLDER);
        for (String commitHash : allCommitHashes) {
            Commit commit = Commit.load(commitHash);
            if (commit.getMessage().equals(message)) {
                commitHashes.add(commitHash);
            }
        }
        return commitHashes;
    }

    /**
     * Returns true if this commit is merged.
     */
    boolean isMerged() {
        return _givenParent != null;
    }

    /**
     * Prints the date and time of the commit.
     */
    void print() {
        Calendar calendar = Calendar.getInstance(Locale.US);
        calendar.setTime(_timestamp);

        System.out.println("===");
        System.out.println("commit " + _hash);
        System.out.format("Date: %1$ta %1$tb %1$td %1$tT %1$tY %1$tz",
                calendar);
        System.out.println();
        System.out.println(_message);
        System.out.println();
    }

    /**
     * Returns true if this commit equals the OTHER commit.
     */
    boolean equals(Commit other) {
        return other.getHash().compareTo(_hash) == 0;
    }

    /**
     * Returns true if this hash equals OTHERHASH.
     */
    boolean equals(String otherHash) {
        return otherHash.compareTo(_hash) == 0;
    }

    @Override
    public String toString() {
        return "Commit{"
                + "timestamp=" + _timestamp
                + ", message='" + _message
                + ", _trackedFileInfos=" + _trackedFileInfos
                + ", _addedFileInfos=" + _addedFileInfos
                + ", _removedFileInfos=" + _removedFileInfos
                + "}";
    }

    /** Date of this commit. */
    protected Date _timestamp;

    /** The commit message of this commit. */
    protected String _message;

    /** Files tracked by its parent. */
    protected HashMap<String, String> _trackedFileInfos;

    /** Files that were staged for addition. */
    protected TreeMap<String, String> _addedFileInfos;

    /** Files that were staged for removal. */
    protected TreeMap<String, String> _removedFileInfos;

    /** The hash value of the commit using sha1. */
    protected String _hash;

    /** The parent commit of this commit. */
    protected String _parent;

    /** The head of the given branch on the command line to be merged. */
    private String _givenParent;

}

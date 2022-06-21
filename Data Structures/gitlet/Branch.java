package gitlet;

import java.io.File;

/** Branch class for Gitlet.
 *  @author Erin Lee
 */
public class Branch {

    /**
     * A new branch in the current system including a NAME and a HEAD commit.
     */
    Branch(String name, String head) {
        _name = name;
        _head = head;
    }

    /**
     * Returns the name of the branch.
     */
    String getName() {
        return _name;
    }

    /**
     * Sets the head of the branch as HEAD.
     */
    void setHead(String head) {
        _head = head;
    }

    /**
     * Returns the head of the branch.
     */
    String getHead() {
        return _head;
    }

    /**
     * Saves the head of the branch.
     */
    void saveHead() {
        saveHead(Gitlet.HEADS_FOLDER);
    }

    /**
     * Saves the HEADSFOLDER of the branch.
     */
    void saveHead(File headsFolder) {
        Utils.writeContents(Utils.join(headsFolder,
                _name.replace('/', '-')), _head);
    }

    /**
     * Saves the HEAD of the branch.
     */
    void saveHead(String head) {
        _head = head;
        saveHead();
    }

    /**
     * Saves the HEAD and HEADSFOLDER of the branch.
     */
    void saveHead(String head, File headsFolder) {
        _head = head;
        saveHead(headsFolder);
    }

    /**
     * Returns information of a branch with the given NAME.
     */
    static Branch load(String name) {
        return load(name, Gitlet.HEADS_FOLDER);
    }

    /**
     * Returns information of a branch with the given NAME and HEADSFOLDER.
     */
    static Branch load(String name, File headsFolder) {
        File file = Utils.join(headsFolder,
                name.replace('/', '-'));
        if (!file.exists()) {
            return null;
        }
        String head = Utils.readContentsAsString(file);
        if (head == null) {
            return null;
        }
        return new Branch(name, head);
    }

    /**
     * Returns current branch information including name and pointer of head
     * from the repository.
     */
    static Branch load() {
        String name = Utils.readContentsAsString(Gitlet.HEAD_FILE);
        return load(name);
    }

    /**
     * Returns removal of a branch with the given NAME.
     */
    static boolean remove(String name) {
        File headFile = Utils.join(Gitlet.HEADS_FOLDER,
                name.replace('/', '-'));
        File stageFile = Utils.join(Gitlet.STAGE_FOLDER,
                name.replace('/', '-'));
        if (!headFile.exists()) {
            return false;
        }
        if (!stageFile.exists()) {
            return false;
        }
        return headFile.delete() && stageFile.delete();
    }

    /**
     * Returns true if a branch with the given NAME exists.
     */
    static boolean exists(String name) {
        File file = Utils.join(Gitlet.HEADS_FOLDER,
                name.replace('/', '-'));
        return file.exists();
    }

    /** String name of branch. */
    private String _name;

    /** String name of head of branch. */
    private String _head;
}

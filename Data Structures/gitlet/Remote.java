package gitlet;

import java.io.File;
import java.io.Serializable;

/** Remote class for Gitlet.
 *  @author Erin Lee
 */
public class Remote implements Serializable {

    /**
     * A new branch in remote including a NAME and a DIRECTORY.
     */
    Remote(String name, String directory) {
        _name = name;
        _directory = directory;
    }

    /**
     * Returns the name of the remote.
     */
    String getName() {
        return _name;
    }

    /**
     * Returns the directory of the remote.
     */
    String getDirectory() {
        return _directory;
    }

    /**
     * Returns gitlet folder.
     */
    File getGitletFolder() {
        return new File(_directory);
    }

    /**
     * Returns heads folder.
     */
    File getHeadsFolder() {
        return Utils.join(getGitletFolder(), "heads");
    }

    /**
     * Returns commits folder.
     */
    File getCommitsFolder() {
        return Utils.join(getGitletFolder(), "commits");
    }

    /**
     * Returns objects folder.
     */
    File getObjectsFolder() {
        return Utils.join(getGitletFolder(), "objects");
    }

    /**
     * Saves the HEAD of the branch with BRANCHNAME.
     */
    void saveBranchHead(String branchName, String head) {
        Utils.writeContents(
                Utils.join(getHeadsFolder(), branchName), head);
    }

    /** String name of branch. */
    private String _name;

    /** String name of branch. */
    private String _directory;

}

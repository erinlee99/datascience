package gitlet;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Gitlet class for Gitlet.
 *  @author Erin Lee
 */
public class Gitlet {

    /** Initial branch. */
    private static final String INIT_BRANCH = "master";

    /** Current working directory. */
    static final File CWD = new File(".");

    /** Gitlet folder. */
    static final File GITLET_FOLDER = new File(CWD, ".gitlet");

    /** Head of branch. */
    static final File HEAD_FILE = new File(GITLET_FOLDER, "HEAD");

    /** Folder of heads. */
    static final File HEADS_FOLDER = new File(GITLET_FOLDER, "heads");

    /** Folder of objects. */
    static final File OBJS_FOLDER = new File(GITLET_FOLDER, "objects");

    /** Folder of staged files. */
    static final File STAGE_FOLDER = new File(GITLET_FOLDER, "stage");

    /** Folder of commits. */
    static final File COMMITS_FOLDER = new File(GITLET_FOLDER, "commits");

    /** Folder of config. */
    static final File CONFIG_FILE = new File(GITLET_FOLDER, "config");

    /** Head of remote branch. */
    static final File FETCH_HEAD_FILE = new File(GITLET_FOLDER, "FETCH_HEAD");

    /**
     * Creates a new Gitlet version-control system in the current directory.
     * This system will automatically start with one commit: a commit that
     * contains no files and has the commit message initial commit (just like
     * that, with no punctuation). It will have a single branch: master,
     * which initially points to this initial commit, and master will be the
     * current branch. The timestamp for this initial commit will be 00:00:00
     * UTC, Thursday, 1 January 1970 in whatever format you choose for dates
     * (this is called "The (Unix) Epoch", represented internally by the time
     * 0.) Since the initial commit in all repositories created by Gitlet
     * will have exactly the same content, it follows that all repositories
     * will automatically share this commit (they will all have the same UID)
     * and all commits in all repositories will trace back to it.
     */
    void init() {
        if (GITLET_FOLDER.exists()) {
            System.out.println("A gitlet version-control system already "
                    + "exists in the current directory.");
            return;
        }

        GITLET_FOLDER.mkdir();
        HEADS_FOLDER.mkdir();
        STAGE_FOLDER.mkdir();
        OBJS_FOLDER.mkdir();
        COMMITS_FOLDER.mkdir();

        Commit commit = new Commit("initial commit", null, null);
        commit.save();

        String branchName = INIT_BRANCH;
        String commitHash = commit.getHash();
        createBranch(branchName, commitHash);
        Utils.writeContents(HEAD_FILE, branchName);
        new Stage(branchName).save();

        new Config().save();
    }


    /**
     * Adds a copy of the file with a given FILENAME as it currently exists to
     * the staging area For this reason, adding a file is also called staging
     * the file for addition. Staging an already-staged file overwrites the
     * previous entry in the staging area with the new contents. The staging
     * area should be somewhere in .gitlet.
     * If the current working version of the file is identical to the
     * version in the current commit, do not stage it to be added, and remove
     * it from the staging area if it is already there (as can happen when a
     * file is changed, added, and then changed back). The file will no
     * longer be staged for removal (see gitlet rm), if it was at the time of
     * the command.
     * Take ARGS as command.
     */
    void add(String... args) {
        if (args.length != 2) {
            throw new GitletException("Incorrect operands.");
        }
        String fileName = args[1];

        File file = new File(fileName);
        if (!file.exists()) {
            throw new GitletException("File does not exist.");
        }

        Branch branch = Branch.load();
        Stage stage = Stage.load(branch.getName());

        Blob blob = new Blob(file);
        blob.save();

        stage.add(file.getName(), blob.getHash());

        stage.save();
    }

    /** Saves a snapshot of certain files in the current commit with a given
     * MESSAGE and staging area so they can be restored at a later time,
     * creating a new commit.
     * The commit is said to be tracking the saved files. By default, each
     * commit's snapshot of files will be exactly the same as its parent
     * commit's snapshot of files; it will keep versions of files exactly as
     * they are, and not update them. A commit will only update the contents
     * of files it is tracking that have been staged for addition at the time
     * of commit, in which case the commit will now include the version of
     * the file that was staged instead of the version it got from its parent
     * . A commit will save and start tracking any files that were staged for
     * addition but weren't tracked by its parent. Finally, files tracked in
     * the current commit may be untracked in the new commit as a result
     * being staged for removal by the rm command (below).
     * Take ARGS as command.
     */
    void commit(String... args) {
        if (args.length == 1) {
            throw new GitletException("Please enter a commit message.");
        } else if (args.length != 2) {
            throw new GitletException("Incorrect operands.");
        }
        String message = args[1];
        if (message.equals(null)  || message.equals("")) {
            throw new GitletException("Please enter a commit message.");
        }
        Branch branch = Branch.load();
        Stage stage = Stage.load(branch.getName());
        if (!stage.isChanged()) {
            throw new GitletException("No changes added to the commit.");
        }

        Commit commit = new Commit(message, stage, branch.getHead());
        commit.save();
        stage.applyCommit();

        stage.save();
        branch.saveHead(commit.getHash());
    }

    /**
     * Unstage the file with FILENAME if it is currently staged for addition.
     * If the file is tracked in the current commit, stage it for removal and
     * remove the file from the working directory if the user has not already
     * done so (do not remove it unless it is tracked in the current commit).
     * Take ARGS as command.
     */
    void rm(String... args) {
        if (args.length != 2) {
            throw new GitletException("Incorrect operands.");
        }
        String fileName = args[1];
        Branch branch = Branch.load();
        Stage stage = Stage.load(branch.getName());

        stage.unstage(fileName);

        stage.save();
    }

    /**
     * Starting at the current head commit, display information about each
     * commit backwards along the commit tree until the initial commit,
     * following the first parent commit links, ignoring any second parents
     * found in merge commits. (In regular Git, this is what you get with git
     * log --first-parent). This set of commit nodes is called the commit's
     * history. For every node in this history, the information it should
     * display is the commit id, the time the commit was made, and the commit
     * message.
     */
    void log() {
        Branch branch = Branch.load();
        String commitHash = branch.getHead();
        while (commitHash != null) {
            Commit commit = Commit.load(commitHash);
            commit.print();
            commitHash = commit.getParent();
        }
    }

    /**
     * Like log, except displays information about all commits ever made. The
     * order of the commits does not matter.
     */
    void globalLog() {
        List<String> commitHashes = Utils.plainFilenamesIn(COMMITS_FOLDER);
        for (String commitHash: commitHashes) {
            Commit commit = Commit.load(commitHash);
            commit.print();
        }
    }

    /**
     * Prints out the ids of all commits that have the given COMMITMESSAGE,
     * one per line. If there are multiple such commits, it prints the ids
     * out on separate lines. The commit message is a single operand; to
     * indicate a multiword message, put the operand in quotation marks, as
     * for the commit command below.
     * Take ARGS as command.
     */
    void find(String... args) {
        if (args.length != 2) {
            throw new GitletException("Incorrect operands.");
        }
        String commitMessage = args[1];
        List<String> commitHashes = Commit.getCommitHashes(commitMessage);
        if (commitHashes.isEmpty()) {
            throw new GitletException("Found no commit with that message.");
        } else {
            for (String commitHash : commitHashes) {
                System.out.println(commitHash);
            }
        }
    }

    /**
     * Displays what branches currently exist, and marks the current branch
     * with a *. Also displays what files have been staged for addition or
     * removal. An example of the exact format it should follow is as follows.
     */
    void status() {
        Branch branch = Branch.load();
        Stage stage = Stage.load(branch.getName());

        System.out.println("=== Branches ===");
        printBranches(branch.getName());
        System.out.println();

        System.out.println("=== Staged Files ===");
        stage.printStagedFiles();
        System.out.println();

        System.out.println("=== Removed Files ===");
        stage.printRemovedFiles();
        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===");
        stage.printDeletedButNotStagedFiles();
        stage.printModifiedButNotStagedFiles();
        System.out.println();

        System.out.println("=== Untracked Files ===");
        stage.printUntrackedFiles();
        System.out.println();
    }

    /**
     * Prints what branches currently exist, and marks the CURRENTBRANCHNAME
     * with a *.
     * Entries should be listed in lexicographic order, using the Java
     * string-comparison order (the asterisk doesn't count).
     */
    private void printBranches(String currentBranchName) {
        for (String fileName: Utils.plainFilenamesIn(HEADS_FOLDER)) {
            if (fileName.equals(currentBranchName)) {
                System.out.print('*');
            }
            System.out.println(fileName);
        }
    }

    /**
     * Take ARGS as command for checkout.
     */
    void checkout(String... args) {
        if (args.length == 2) {
            checkoutBranch(args[1]);
        } else if (args.length == 3 & args[1].equals("--")) {
            checkoutFile(null, args[2]);
        } else if (args.length == 4 & args[2].equals("--")) {
            checkoutFile(args[1], args[3]);
        } else {
            throw new GitletException("Incorrect operands.");
        }
    }

    /**
     * Takes the version of the file with the given FILENAME as it exists in
     * the commit with the given COMMITHASH, and puts it in the working
     * directory, overwriting the version of the file that's already there if
     * there is one. The new version of the file is not staged.
     */
    void checkoutFile(String commitHash, String fileName) {
        Branch branch = Branch.load();
        if (commitHash == null) {
            commitHash = branch.getHead();
        }

        Commit commit = Commit.load(commitHash);
        if (commit == null) {
            throw new GitletException("No commit with that id exists.");
        }

        String fileHash = commit.getFileHash(fileName);
        if (fileHash == null) {
            throw new GitletException("File does not exist in that commit.");
        }

        Stage stage = Stage.load(branch.getName());
        stage.checkout(fileHash);
        stage.save();
    }

    /**
     * Takes all files in the commit at the head of the BRANCHNAME, and
     * puts them in the working directory, overwriting the versions of the
     * files that are already there if they exist.
     * Also, at the end of this command, the given branch will now be
     * considered the current branch (HEAD).
     * Any files that are tracked in the current branch but are not present
     * in the checked-out branch are deleted.
     * The staging area is cleared, unless the checked-out branch is the
     * current branch.
     */
    void checkoutBranch(String branchName) {
        Branch givenBranch = Branch.load(branchName);
        if (givenBranch == null) {
            throw new GitletException("No such branch exists.");
        }

        Branch currentBranch = Branch.load();
        Commit currentCommit = Commit.load(currentBranch.getHead());
        if (givenBranch.getName().equals(currentBranch.getName())) {
            throw new GitletException(
                    "No need to checkout the current branch.");
        }

        String givenCommitHash = givenBranch.getHead();
        Commit givenCommit = Commit.load(givenCommitHash);
        Stage stage = Stage.load(currentBranch.getName());
        stage.checkout(givenCommit);

        stage.setBranchName(branchName);
        stage.save();

        Utils.writeContents(HEAD_FILE, branchName);
    }

    /**
     * Creates a new branch with the BRANCHNAME, and points it at the current
     * head node. A branch is nothing more than a name for a reference (a
     * SHA-1 identifier) to a commit node. This command does NOT immediately
     * switch to the newly created branch (just as in real Git). Before you
     * ever call branch, your code should be running with a default branch
     * called "master".
     * Take ARGS as command.
     */
    void branch(String... args) {
        if (args.length != 2) {
            throw new GitletException("Incorrect operands.");
        }
        String branchName = args[1];
        if (Branch.exists(branchName)) {
            throw new GitletException("A branch with that name already "
                + "exists.");
        }
        Branch branch = Branch.load();
        Stage stage = Stage.load(branch.getName());
        stage.setBranchName(branchName);
        createBranch(branchName, branch.getHead());
        stage.save();
    }

    /**
     * Deletes the branch with the given BRANCHNAME. This only means to delete
     * the pointer associated with the branch; it does not mean to delete all
     * commits that were created under the branch, or anything like that.
     * Take ARGS as command.
     */
    void rmBranch(String... args) {
        if (args.length != 2) {
            throw new GitletException("Incorrect operands.");
        }
        String branchName = args[1];
        if (!Branch.exists(branchName)) {
            throw new GitletException("A branch with that name does not "
                + "exist.");
        }
        Branch branch = Branch.load();
        if (branchName.equals(branch.getName())) {
            throw new GitletException("Cannot remove the current branch.");
        }
        Branch.remove(branchName);
    }

    /**
     * Checks out all the files tracked by the given commit COMMITHASH. Removes
     * tracked files that are not present in that commit. Also moves the current
     * branch's head to that commit node. See the intro for an example of
     * what happens to the head pointer after using reset. The [commit id]
     * may be abbreviated as for checkout. The staging area is cleared. The
     * command is essentially checkout of an arbitrary commit that also
     * changes the current branch head.
     * Take ARGS as command.
     */
    void reset(String... args) {
        if (args.length != 2) {
            throw new GitletException("Incorrect operands.");
        }
        String commitHash = args[1];
        Commit commit = Commit.load(commitHash);
        if (commit == null) {
            throw new GitletException("No commit with that id exists.");
        }

        Branch branch = Branch.load();
        Stage stage = Stage.load(branch.getName());
        stage.checkout(commit);
        stage.save();

        branch.saveHead(commitHash);
    }

    /**
     * Merges files from the given branch GIVENBRANCHNAME into the current
     * branch.
     * Take ARGS as command.
     */
    void merge(String... args) {
        if (args.length != 2) {
            throw new GitletException("Incorrect operands.");
        }
        String givenBranchName = args[1];
        Branch currentBranch = Branch.load();
        Stage currentStage = Stage.load(currentBranch.getName());
        currentStage.setMergedBranchName(givenBranchName);

        if (currentStage.isChanged()) {
            throw new GitletException("You have uncommitted changes.");
        }

        Branch givenBranch = Branch.load(givenBranchName);
        if (givenBranch == null) {
            throw new GitletException("A branch with that name does not "
                + "exist.");
        }

        if (givenBranchName.equals(currentBranch.getName())) {
            throw new GitletException("Cannot merge a branch with itself.");
        }

        Commit givenLastCommit = Commit.load(givenBranch.getHead());
        Commit currentLastCommit = Commit.load(currentBranch.getHead());

        List<String> fileNames = Utils.plainFilenamesIn(CWD);
        for (String fileName: fileNames) {
            if (!currentStage.getTracked().containsKey(fileName)
                    && givenLastCommit.getFileInfos().containsKey(fileName)) {
                throw new GitletException("There is an untracked file in the "
                    + "way; delete it, or add and commit it first.");
            }
        }

        String splitPointHead = MergeOperations.getSplitPointHeadMerged(
                currentLastCommit, givenLastCommit);
        if (splitPointHead == null) {
            return;
        }

        Commit splitPointCommit = Commit.load(splitPointHead);

        if (splitPointCommit.equals(givenLastCommit)) {
            throw new GitletException("Given branch is an ancestor of the "
                + "current branch.");
        }
        if (splitPointCommit.equals(currentLastCommit)) {
            checkoutBranch(givenBranchName);
            throw new GitletException("Current branch is fast-forwarded.");
        }

        Commit mergeCommit = MergeOperations.merge(currentStage,
                currentLastCommit, givenLastCommit, splitPointCommit);

        currentStage.save();
        currentBranch.saveHead(mergeCommit.getHash());
    }

    /**
     * Saves the given login information under the given REMOTENAME.
     * Attempts to push or pull from the given remote name will then attempt
     * to use this .gitlet directory REMOTEDIRNAME.
     * By writing, e.g., java gitlet.Main add-remote other .
     * ./testing/otherdir/.gitlet you can provide tests of remotes that will
     * work from all locations
     * Take ARGS as command.
     */
    void addRemote(String... args) {
        if (args.length != 3) {
            throw new GitletException("Incorrect operands.");
        }
        String remoteName = args[1];
        String remoteDirName = args[2];

        Config config = Config.load();
        if (config.findRemote(remoteName) != null) {
            throw new GitletException(
                    "A remote with that name already exists.");
        }

        Remote remote = new Remote(remoteName, remoteDirName);
        config.addRemote(remote);
        config.save();
    }


    /**
     * Removes the remote with name REMOTENAME.
     * Take ARGS as command.
     */
    void rmRemote(String... args) {
        if (args.length != 2) {
            throw new GitletException("Incorrect operands.");
        }
        String remoteName = args[1];

        Config config = Config.load();
        if (config.findRemote(remoteName) == null) {
            throw new GitletException(
                    "A remote with that name does not exist.");
        }

        config.removeRemote(remoteName);
        config.save();
    }

    /**
     *  Attempts to append the current branch's commits to the end of the
     *  given branch REMOTEBRANCHNAME at the given remote REMOTENAME.
     *  Take ARGS as command.
     */
    void push(String... args) {
        if (args.length != 3) {
            throw new GitletException("Incorrect operands.");
        }
        String remoteName = args[1];
        String branchName = args[2];
        Config config = Config.load();
        Remote remote = config.findRemote(remoteName);
        String remoteDir = remote.getDirectory();
        if (!new File(remoteDir).exists()) {
            throw new GitletException("Remote directory not found.");
        }
        Branch currentBranch = Branch.load();
        String head = currentBranch.getHead();
        String givenBranchName = remoteName + "/" + branchName;
        Branch givenBranch = Branch.load(givenBranchName);
        if (givenBranch == null) {
            throw new GitletException("Please pull down remote changes before"
                + " pushing.");
        }
        HashMap<String, Integer> commitHistory = new HashMap<>();
        MergeOperations.addToCommitHistory(commitHistory,
                head, 1, COMMITS_FOLDER);
        File remoteCommitsFolder = remote.getCommitsFolder();
        File remoteObjsFolder = remote.getObjectsFolder();
        Set<String> commitHashes = commitHistory.keySet();
        for (String commitHash : commitHashes) {
            Commit commit = Commit.load(commitHash, COMMITS_FOLDER);
            if (commit != null) {
                commit.save(remoteCommitsFolder);
                HashMap<String, String> fileInfos = commit.getFileInfos();
                Set<String> fileNameHashSet = fileInfos.keySet();
                for (String fileName : fileNameHashSet) {
                    Blob blob = Blob.load(fileInfos.get(fileName), OBJS_FOLDER);
                    if (blob != null) {
                        blob.save(remoteObjsFolder);
                    }
                }
            }
        }
        Commit commit = Commit.load(head, remoteCommitsFolder);
        Map<String, String> fileInfos = commit.getFileInfos();
        Set<String> fileNameHashSet = fileInfos.keySet();
        for (String fileName : fileNameHashSet) {
            Blob blob = Blob.load(fileInfos.get(fileName), remoteObjsFolder);
            if (blob != null) {
                Utils.writeContents(Utils.join(
                    new File(remote.getDirectory()).getParentFile(), fileName),
                    blob.getFileContents());
            }
        }
        removeRemoteFile(remote, commit);
        remote.saveBranchHead(branchName, head);
    }

    /**
     * Remove remote files with REMOTE and COMMIT
     */
    void removeRemoteFile(Remote remote, Commit commit) {
        Map<String, String> removedFileInfos = commit.getRemovedFileInfos();
        Set<String> removedFileINameHashSet = removedFileInfos.keySet();
        for (String removedFileName : removedFileINameHashSet) {
            File file = Utils.join(new File(remote.getDirectory())
                    .getParentFile(), removedFileName);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    /**
     * Fetches with REMOTENAME and REMOTEBRANCHNAME.
     * Take ARGS as command.
     */
    void fetch(String... args) {
        if (args.length != 3) {
            throw new GitletException("Incorrect operands.");
        }
        String remoteName = args[1];
        String branchName = args[2];

        Config config = Config.load();
        Remote remote = config.findRemote(remoteName);
        String remoteDir = remote.getDirectory();
        if (!new File(remoteDir).exists()) {
            throw new GitletException("Remote directory not found.");
        }

        Branch remoteBranch = Branch.load(branchName, remote.getHeadsFolder());
        if (remoteBranch == null) {
            throw new GitletException("That remote does not have that branch.");
        }
        String head = remoteBranch.getHead();

        String givenBranchName = remoteName + "/" + branchName;
        Branch givenBranch = new Branch(givenBranchName, head);

        fetch(remote, head, givenBranch);
    }

    /**
     * Fetches with given REMOTE name, HEAD and GIVENBRANCH.
     */
    private void fetch(Remote remote, String head, Branch givenBranch) {
        File remoteCommitsFolder = remote.getCommitsFolder();
        File remoteObjsFolder = remote.getObjectsFolder();

        HashMap<String, Integer> commitHistory = new HashMap<>();
        MergeOperations.addToCommitHistory(commitHistory, head, 1,
                remoteCommitsFolder);

        Set<String> commitHashes = commitHistory.keySet();
        for (String commitHash : commitHashes) {
            Commit commit = Commit.load(commitHash, remoteCommitsFolder);
            if (commit != null) {
                commit.save(COMMITS_FOLDER);
                HashMap<String, String> fileInfos = commit.getFileInfos();
                Set<String> fileNameHashSet = fileInfos.keySet();
                for (String fileName : fileNameHashSet) {
                    Blob blob = Blob.load(fileInfos.get(fileName),
                            remoteObjsFolder);
                    if (blob != null) {
                        blob.save(OBJS_FOLDER);
                    }
                }
            }

        }
        givenBranch.saveHead(head);
        Utils.writeContents(FETCH_HEAD_FILE, givenBranch.getName());
    }

    /**
     * Pulls with REMOTENAME and REMOTEBRANCHNAME.
     * Take ARGS as command.
     */
    void pull(String... args) {
        if (args.length != 3) {
            throw new GitletException("Incorrect operands.");
        }
        String remoteName = args[1];
        String branchName = args[2];
        Branch currentBranch = Branch.load();
        Stage currentStage = Stage.load(currentBranch.getName());
        Config config = Config.load();
        Remote remote = config.findRemote(remoteName);
        String remoteDir = remote.getDirectory();
        if (!new File(remoteDir).exists()) {
            throw new GitletException("Remote directory not found.");
        }

        Branch remoteBranch = Branch.load(branchName, remote.getHeadsFolder());
        if (remoteBranch == null) {
            throw new GitletException("That remote does not have that branch.");
        }
        String head = remoteBranch.getHead();
        String givenBranchName = remoteName + "/" + branchName;
        Branch givenBranch = new Branch(givenBranchName, head);
        currentStage.setMergedBranchName(givenBranchName);
        fetch(remote, head, givenBranch);
        Commit givenLastCommit = Commit.load(head);
        Commit currentLastCommit = Commit.load(currentBranch.getHead());
        List<String> fileNames = Utils.plainFilenamesIn(CWD);
        for (String fileName: fileNames) {
            if (!currentStage.getTracked().containsKey(fileName)
                    && givenLastCommit.getFileInfos().containsKey(fileName)) {
                throw new GitletException("There is an untracked file in the "
                        + "way; delete it, or add and commit it first.");
            }
        }
        String splitPointHead = MergeOperations.getSplitPointHeadMerged(
                currentLastCommit, givenLastCommit);
        if (splitPointHead == null) {
            return;
        }
        Commit splitPointCommit = Commit.load(splitPointHead);
        if (splitPointCommit.equals(givenLastCommit)) {
            throw new GitletException("Given branch is an ancestor of the "
                    + "current branch.");
        }
        if (splitPointCommit.equals(currentLastCommit)) {
            checkoutBranch(givenBranchName);
            throw new GitletException("Current branch is fast-forwarded.");
        }
        Commit mergeCommit = MergeOperations.merge(currentStage,
                currentLastCommit, givenLastCommit, splitPointCommit);
        currentStage.save();
        currentBranch.saveHead(mergeCommit.getHash());
    }

    /**
     * Creates a new branch with NAME and HEAD.
     */
    private void createBranch(String name, String head) {
        Branch branch = new Branch(name, head);
        branch.saveHead();
    }


}


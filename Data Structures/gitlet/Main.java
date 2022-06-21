package gitlet;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Erin Lee
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> ....
     *  java gitlet.Main add hello.txt */
    public static void main(String... args) {
        if (args.length == 0) {
            System.err.println("Please enter a command.");
            return;
        }
        try {
            Gitlet gitlet = new Gitlet();
            String command = args[0];
            if (!command.equals("init") && !Gitlet.GITLET_FOLDER.exists()) {
                throw new GitletException(
                        "Not in an initialized Gitlet directory.");
            }
            switch (command) {
            case "init":
                gitlet.init(); break;
            case "add":
                gitlet.add(args); break;
            case "commit":
                gitlet.commit(args); break;
            case "rm":
                gitlet.rm(args); break;
            case "log":
                gitlet.log(); break;
            case "global-log":
                gitlet.globalLog(); break;
            case "find":
                gitlet.find(args); break;
            case "status":
                gitlet.status(); break;
            case "checkout":
                gitlet.checkout(args); break;
            case "branch":
                gitlet.branch(args); break;
            case "rm-branch":
                gitlet.rmBranch(args); break;
            case "reset":
                gitlet.reset(args); break;
            case "merge":
                gitlet.merge(args); break;
            case "add-remote":
                gitlet.addRemote(args); break;
            case "rm-remote":
                gitlet.rmRemote(args); break;
            case "push":
                gitlet.push(args); break;
            case "fetch":
                gitlet.fetch(args); break;
            case "pull":
                gitlet.pull(args); break;
            default:
                System.err.println("No command with that name exists.");
            }
        } catch (GitletException e) {
            System.err.println(e.getMessage());
        }
        return;
    }
}

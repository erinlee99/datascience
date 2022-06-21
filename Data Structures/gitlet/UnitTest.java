package gitlet;

import ucb.junit.textui;
import org.junit.Test;


import static org.junit.Assert.*;

/**
 * The suite of all JUnit tests for the gitlet package.
 *
 * @author
 */
public class UnitTest {

    /**
     * Run the JUnit tests in the loa package. Add xxxTest.class entries to
     * the arguments of runClasses to run other JUnit tests.
     */
    public static void main(String[] ignored) {
        System.exit(textui.runClasses(UnitTest.class));
    }

    /**
     * A dummy test to avoid complaint.
     */
    @Test
    public void placeholderTest() {
    }


    @Test
    public void setup() {
        Gitlet gitlet = new Gitlet();
        gitlet.init();
        Utils.writeContents(Utils.join(Gitlet.CWD, "f.txt"), "wug.txt");
        Utils.writeContents(Utils.join(Gitlet.CWD, "g.txt"), "notwug.txt");
        gitlet.add("g.txt");
        gitlet.add("f.txt");
        gitlet.commit("Two files");
    }

}



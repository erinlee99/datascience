package gitlet;

import java.io.File;
import java.io.Serializable;


/** Blob class for Gitlet.
 *  @author Erin Lee
 */
public class Blob implements Serializable {

    /**
     * A new blob in the current system with FILE.
     */
    Blob(File file) {
        _fileName = file.getName();
        _fileContents = Utils.readContents(file);
        _hash = Utils.sha1(_fileName, _fileContents);
    }

    /**
     * Returns the string name of the file.
     */
    String getFileName() {
        return _fileName;
    }

    /**
     * Returns the contents of the file in the blob.
     */
    byte[] getFileContents() {
        return _fileContents;
    }

    /**
     * Returns the hash of the blob.
     */
    String getHash() {
        return _hash;
    }

    /**
     * Saves the blob.
     */
    void save() {
        Utils.writeObject(Utils.join(Gitlet.OBJS_FOLDER, _hash), this);
    }

    /**
     * Saves the blob that corresponds to the given OBJECTFOLDER.
     */
    void save(File objectFolder) {
        Utils.writeObject(Utils.join(objectFolder, _hash), this);
    }

    /**
     * Returns blob with string HASH.
     */
    static Blob load(String hash) {
        return load(hash, null);
    }

    /**
     * Returns blob that corresponds to the given string HASH and OBJECTFOLDER.
     */
    static Blob load(String hash, File objectFolder) {
        if (objectFolder == null) {
            objectFolder = Gitlet.OBJS_FOLDER;
        }
        File file = Utils.join(objectFolder, hash);
        if (!file.exists()) {
            return null;
        }
        return Utils.readObject(file, Blob.class);
    }


    /**
     * Returns true if this blob equals the OTHER blob.
     */
    boolean equals(Blob other) {
        return other.getHash().compareTo(_hash) == 0;
    }

    /**
     * Returns true if this hash equals OTHERHASH.
     */
    boolean equals(String otherHash) {
        return otherHash.compareTo(_hash) == 0;
    }

    /** String name of file. */
    private String _fileName;

    /** Byte array of file contents. */
    private byte[] _fileContents;

    /** String hash. */
    private String _hash;

}


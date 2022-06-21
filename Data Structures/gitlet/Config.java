package gitlet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/** Config class for Gitlet.
 *  @author Erin Lee
 */
public class Config implements Serializable {

    /**
     * Creates a new config.
     */
    Config() {
        _remotes = new HashMap<>();
    }

    /**
     * Returns remotes.
     */
    Map<String, Remote> getRemotes() {
        return _remotes;
    }

    /**
     * Sets remotes in REMOTES.
     */
    void setRemotes(Map<String, Remote> remotes) {
        this._remotes = _remotes;
    }

    /**
     * Returns the remote found by REMOTENAME.
     */
    Remote findRemote(String remoteName) {
        return _remotes.get(remoteName);
    }

    /**
     * Adds the REMOTE.
     */
    void addRemote(Remote remote) {
        _remotes.put(remote.getName(), remote);
    }

    /**
     * Removes the remote with the given REMOTENAME.
     */
    void removeRemote(String remoteName) {
        _remotes.remove(remoteName);
    }

    /**
     * Saves the config.
     */
    void save() {
        Utils.writeObject(Gitlet.CONFIG_FILE, this);
    }

    /**
     * Returns the config.
     */
    static Config load() {
        return Utils.readObject(Gitlet.CONFIG_FILE, Config.class);
    }

    /** Private map remotes. */
    private Map<String, Remote> _remotes;

}

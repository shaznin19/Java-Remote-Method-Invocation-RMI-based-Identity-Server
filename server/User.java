package p4.server;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * The {@code User} class represents a simple user object.
 * <p>
 *
 * @author Sheikh Md Mushfiqur Rahman
 * @version 1.0
 */
public class User implements Serializable {
    String loginName;
    String realName;
    String encryptedPassword;
    UUID uuid;
    String ipAddress;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    /**
     * Retrieves the login name of the user.
     *
     * @return The login name of the user.
     */
    public String getLoginName() {
        return loginName;
    }

    /**
     * Retrieves the real name of the user.
     *
     * @return The real name of the user.
     */
    public String getRealName() {
        return realName;
    }

    /**
     * Retrieves the encrypted password of the user.
     *
     * @return The encrypted password of the user.
     */
    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    /**
     * Retrieves the IP address of the user.
     *
     * @return The IP address of the user.
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Retrieves the creation time of the user.
     *
     * @return the creation time
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Retrieves the time of last update
     *
     * @return the last updation time
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Constructs a new User object with the specified attributes.
     *
     * @param loginName         The login name for the user.
     * @param realName          The real name of the user.
     * @param encryptedPassword The encrypted password of the user.
     * @param uuid              The UUID (Universally Unique Identifier) of the user.
     * @param ipAddress         The IP address from which the user accessed the system.
     * @param createdAt         The timestamp when the user account was created.
     * @param updatedAt         The timestamp when the user account was last updated.
     */
    public User(String loginName, String realName, String encryptedPassword, UUID uuid, String ipAddress,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.loginName = loginName;
        this.realName = realName;
        this.encryptedPassword = encryptedPassword;
        this.uuid = uuid;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Sets the login name for the user and updates the last update timestamp.
     *
     * @param loginName The new login name for the user.
     */
    public void setLoginName(String loginName, LocalDateTime updatedAt) {
        this.loginName = loginName;
        this.updatedAt = updatedAt;
    }

    /**
     * return a string format of user's information
     *
     * @return
     */
    @Override
    public String toString() {
        return "{ uuid: " + uuid + ",login name: " + loginName + ", real name: " + realName + "}";
    }

    /**
     * Returns the UUID of the user.
     *
     * @return The UUID of the user.
     */
    public UUID getUuid() {
        return uuid;
    }

    public static User userFromMap(Map<String, String> userMap) {
        String loginName = userMap.get("loginName");
        String realName = userMap.get("realName");
        String encryptedPassword = userMap.get("encryptedPassword");
        UUID uuid = UUID.fromString(userMap.get("uuid"));
        String ipAddress = userMap.get("ipAddress");
        LocalDateTime createdAt = LocalDateTime.parse(userMap.get("createdAt"));
        LocalDateTime updatedAt = LocalDateTime.parse(userMap.get("updatedAt"));
        User user = new User(loginName, realName, encryptedPassword, uuid, ipAddress, createdAt, updatedAt);
        return user;
    }
}

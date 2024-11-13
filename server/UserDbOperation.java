package p4.server;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The UserDbOperation class represents a database operation related to user entities.
 * It provides methods to generate string representations of the object tailored for
 * different types of database operations such as create, delete, and modify.
 * @author Sheikh Md Mushfiqur Rahman & Shaznin Sultana
 * @version 1.0
 *
 */
public class UserDbOperation {
    String operationType;
    UUID uuid;
    String loginName;
    String newLoginName;
    String realName;
    String password;
    String ipAddress;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    long operationTimestamp;

    /**
     * Constructor to creata operation
     * @param operationType type of of operation
     * @param uuid user uuid
     * @param loginName user login name
     * @param newLoginName user new login name
     * @param realName user real name
     * @param password user password
     * @param ipAddress user ipaddress
     * @param createdAt user created at
     * @param updatedAt user updated at
     * @param operationTimestamp operation timestamp
     */
    public UserDbOperation(String operationType, UUID uuid, String loginName, String newLoginName, String realName, String password, String ipAddress, LocalDateTime createdAt, LocalDateTime updatedAt, long operationTimestamp) {
        this.operationType = operationType;
        this.uuid = uuid;
        this.loginName = loginName;
        this.newLoginName = newLoginName;
        this.realName = realName;
        this.password = password;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.operationTimestamp = operationTimestamp;
    }

    /**
     * Generates a string representation of the object tailored for a UserDb operation.
     *
     * @return a string representing the object with details specific to a UserDb operation.
     */
    @Override
    public String toString() {
        return "UserDbOperation{" +
                "operationName='" + operationType + '\'' +
                ", uuid=" + uuid +
                ", loginName='" + loginName + '\'' +
                ", newLoginName='" + newLoginName + '\'' +
                ", realName='" + realName + '\'' +
                ", password='" + password + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", operationTimestamp=" + operationTimestamp +
                '}';
    }

    /**
     * Gets the operation type
     * @return the operation type.
     */
    public String getOperationType() {
        return operationType;
    }

    /**
     * Gets the uuid
     * @return the uuid.
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Gets the login name associated with the operation.
     *
     * @return the login name.
     */
    public String getLoginName() {
        return loginName;
    }

    /**
     * Gets the real name associated with the operation.
     *
     * @return the real name.
     */
    public String getRealName() {
        return realName;
    }

     /**
     * Gets the password associated with the operation.
     *
     * @return the password.
     */
    public String getPassword() {
        return password;
    }

     /**
     * Gets the IP address associated with the operation.
     *
     * @return the IP address.
     */
    public String getIpAddress() {
        return ipAddress;
    }

     /**
     * Gets the creation timestamp associated with the operation.
     *
     * @return the creation timestamp.
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the update timestamp associated with the operation.
     *
     * @return the update timestamp.
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Gets the operation timestamp associated with the operation.
     *
     * @return the operation timestamp.
     */
    public long getOperationTimestamp() {
        return operationTimestamp;
    }

    /**
     * Gets the new login name associated with the operation.
     *
     * @return the new login name.
     */
    public String getNewLoginName() {
        return newLoginName;
    }

    /**
     * Generates a string representation of the object tailored for a create operation.
     *
     * @return a string representing the object with details specific to a create operation.
     */
    public String toStringForCreateOperation() {
        return "UserDbOperation{" +
                "operationType='" + operationType + '\'' +
                ", uuid=" + uuid +
                ", loginName='" + loginName + '\'' +
                ", realName='" + realName + '\'' +
                ", password='" + password + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", createdAt=" + createdAt +
                ", operationTimestamp=" + operationTimestamp +
                '}';
    }

    /**
     * Generates a string representation of the object tailored for a delete operation.
     *
     * @return a string representing the object with details specific to a delete operation.
     */
    public String toStringForDeleteOperation() {
        return "UserDbOperation{" +
                "uuid=" + uuid +
                ", operationType='" + operationType + '\'' +
                ", operationTimestamp=" + operationTimestamp +
                '}';
    }

    /**
     * Generates a string representation of the object tailored for a modify operation.
     *
     * @return a string representing the object with details specific to a modify operation.
     */
    public String toStringForModifyOperation() {
        return "UserDbOperation{" +
                "loginName='" + loginName + '\'' +
                ", newLoginName='" + newLoginName + '\'' +
                ", operationType='" + operationType + '\'' +
                ", uuid=" + uuid +
                ", updatedAt=" + updatedAt +
                ", operationTimestamp=" + operationTimestamp +
                '}';
    }
}

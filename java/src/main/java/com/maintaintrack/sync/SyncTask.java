package com.maintaintrack.sync;

/**
 * Represents a single pending sync operation.
 * Serialized to the SYNC_QUEUE SQLite table for crash recovery.
 */
public class SyncTask {

    public enum Operation { INSERT, UPDATE, DELETE }

    private int       id;
    private String    tableName;
    private int       localId;
    private String    payload;   // JSON string
    private Operation operation;
    private String    timestamp;
    private int       retryCount;

    public SyncTask() {}

    public SyncTask(String tableName, int localId,
                    String payload, Operation operation) {
        this.tableName  = tableName;
        this.localId    = localId;
        this.payload    = payload;
        this.operation  = operation;
        this.timestamp  = java.time.LocalDateTime.now().toString();
        this.retryCount = 0;
    }

    // ── Getters & Setters ────────────────────────────────────
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public int getLocalId() { return localId; }
    public void setLocalId(int localId) { this.localId = localId; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public Operation getOperation() { return operation; }
    public void setOperation(Operation operation) { this.operation = operation; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
}

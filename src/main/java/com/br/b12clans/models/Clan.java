package com.br.b12clans.models;

import java.sql.Timestamp;
import java.util.UUID;

public class Clan {
    
    private final int id;
    private final String name;
    private final String tag;
    private final UUID ownerUuid;
    private final Timestamp createdAt;
    
    public Clan(int id, String name, String tag, UUID ownerUuid, Timestamp createdAt) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.ownerUuid = ownerUuid;
        this.createdAt = createdAt;
    }
    
    public int getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getTag() {
        return tag;
    }
    
    public UUID getOwnerUuid() {
        return ownerUuid;
    }
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    
    @Override
    public String toString() {
        return "Clan{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", tag='" + tag + '\'' +
                ", ownerUuid=" + ownerUuid +
                ", createdAt=" + createdAt +
                '}';
    }
}

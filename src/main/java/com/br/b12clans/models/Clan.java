package com.br.b12clans.models;

import java.sql.Timestamp;
import java.util.Objects;
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

    // ##### MÉTODOS ADICIONADOS PARA COMPARAÇÃO CORRETA #####
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Clan clan = (Clan) o;
        return id == clan.id; // Clãs são iguais se tiverem o mesmo ID
    }

    @Override
    public int hashCode() {
        return Objects.hash(id); // O hashCode é baseado apenas no ID
    }
    // ######################################################
}
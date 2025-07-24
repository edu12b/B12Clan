package com.br.b12clans.models;

public class PlayerData {
    private int kills;
    private int deaths;

    public PlayerData(int kills, int deaths) {
        this.kills = kills;
        this.deaths = deaths;
    }

    // Getters
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public double getKdr() {
        if (deaths == 0) {
            return kills;
        }
        return (double) kills / deaths;
    }

    // Setters (para atualizar o cache)
    public void incrementKills(int amount) { this.kills += amount; }
    public void incrementDeaths(int amount) { this.deaths += amount; }
}
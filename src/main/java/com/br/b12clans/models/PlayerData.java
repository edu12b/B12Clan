package com.br.b12clans.models;

public class PlayerData {
    private int kills;
    private int deaths;
    private String role; // <-- CAMPO ADICIONADO

    public PlayerData(int kills, int deaths, String role) {
        this.kills = kills;
        this.deaths = deaths;
        this.role = role;
    }

    // Getters
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public String getRole() { return role; } // <-- GETTER ADICIONADO
    public double getKdr() {
        if (deaths == 0) {
            return kills;
        }
        return (double) kills / deaths;
    }

    // Setters
    public void incrementKills(int amount) { this.kills += amount; }
    public void incrementDeaths(int amount) { this.deaths += amount; }
    public void setRole(String role) { this.role = role; } // <-- SETTER ADICIONADO
}
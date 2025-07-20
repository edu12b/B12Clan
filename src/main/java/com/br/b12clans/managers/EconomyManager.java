package com.br.b12clans.managers;

import com.br.b12clans.Main;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {

    private final Main plugin;
    private Economy economy;
    private boolean vaultEnabled;

    public EconomyManager(Main plugin) {
        this.plugin = plugin;
        this.vaultEnabled = setupEconomy();
    }

    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault não encontrado! Sistema bancário do clã será desabilitado.");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("Nenhum plugin de economia encontrado! Sistema bancário do clã será desabilitado.");
            return false;
        }

        economy = rsp.getProvider();
        plugin.getLogger().info("Integração com economia (" + economy.getName() + ") habilitada!");
        return true;
    }

    public boolean isEnabled() {
        return vaultEnabled && economy != null;
    }

    public double getPlayerBalance(Player player) {
        if (!isEnabled()) return 0.0;
        return economy.getBalance(player);
    }

    public boolean hasEnough(Player player, double amount) {
        if (!isEnabled()) return false;
        return economy.has(player, amount);
    }

    public boolean withdrawPlayer(Player player, double amount) {
        if (!isEnabled()) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean depositPlayer(Player player, double amount) {
        if (!isEnabled()) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public String format(double amount) {
        if (!isEnabled()) return String.format("%.2f", amount);
        return economy.format(amount);
    }

    public String getCurrencyName() {
        if (!isEnabled()) return "dinheiro";
        return economy.currencyNameSingular();
    }

    public String getCurrencyNamePlural() {
        if (!isEnabled()) return "dinheiro";
        return economy.currencyNamePlural();
    }
}

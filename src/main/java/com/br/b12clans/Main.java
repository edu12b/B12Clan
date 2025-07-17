package com.br.b12clans;

import com.br.b12clans.commands.ClanCommand;
import com.br.b12clans.database.DatabaseManager;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.placeholders.ClanPlaceholder;
import com.br.b12clans.listeners.PlayerListener;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private DatabaseManager databaseManager;
    private ClanManager clanManager;
    private MessagesManager messagesManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            getLogger().severe("Falha crítica ao inicializar o banco de dados! O B12Clans será desabilitado.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.messagesManager = new MessagesManager(this);
        this.clanManager = new ClanManager(this);

        registerCommands();
        registerEvents();
        registerPlaceholders();

        getLogger().info("B12Clans foi habilitado com sucesso!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("B12Clans foi desabilitado!");
    }

    private void registerCommands() {
        ClanCommand clanCommandExecutor = new ClanCommand(this, clanManager, messagesManager);
        getCommand("clan").setExecutor(clanCommandExecutor);
        getCommand("clan").setTabCompleter(clanCommandExecutor);
    }

    private void registerPlaceholders() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ClanPlaceholder(this, clanManager).register();
            getLogger().info("PlaceholderAPI integração registrada!");
        } else {
            getLogger().warning("PlaceholderAPI não encontrado! Placeholders não estarão disponíveis.");
        }
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ClanManager getClanManager() {
        return clanManager;
    }

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }
}
package com.br.b12clans;

import com.br.b12clans.chat.ClanChatManager;
import com.br.b12clans.chat.DiscordManager;
import com.br.b12clans.commands.ClanChatCommand;
import com.br.b12clans.commands.AllyChatCommand;
import com.br.b12clans.commands.DiscordCommand;
import com.br.b12clans.commands.ClanCommand;
import com.br.b12clans.database.DatabaseManager;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.placeholders.ClanPlaceholder;
import com.br.b12clans.listeners.PlayerListener;
import com.br.b12clans.listeners.KDRListener;
import com.br.b12clans.utils.MessagesManager;
import com.br.b12clans.listeners.ChatListener;
import com.br.b12clans.managers.EconomyManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private DatabaseManager databaseManager;
    private ClanManager clanManager;
    private MessagesManager messagesManager;
    private ClanChatManager clanChatManager;
    private DiscordManager discordManager;
    private EconomyManager economyManager;

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
        this.clanChatManager = new ClanChatManager(this);
        this.discordManager = new DiscordManager(this);
        this.economyManager = new EconomyManager(this);

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
        if (discordManager != null) {
            discordManager.shutdown();
        }
        getLogger().info("B12Clans foi desabilitado!");
    }

    private void registerCommands() {
        ClanCommand clanCommandExecutor = new ClanCommand(this, clanManager, messagesManager);
        ClanChatCommand clanChatCommand = new ClanChatCommand(this, clanManager, clanChatManager, messagesManager);
        AllyChatCommand allyChatCommand = new AllyChatCommand(this, clanManager, clanChatManager, messagesManager);
        DiscordCommand discordCommand = new DiscordCommand(this, discordManager, messagesManager);

        getCommand("clan").setExecutor(clanCommandExecutor);
        getCommand("clan").setTabCompleter(clanCommandExecutor);
        getCommand(".").setExecutor(clanChatCommand);
        getCommand(".").setTabCompleter(clanChatCommand);
        getCommand("ally").setExecutor(allyChatCommand);
        getCommand("ally").setTabCompleter(allyChatCommand);
        getCommand("discord").setExecutor(discordCommand);
        getCommand("discord").setTabCompleter(discordCommand);
        getCommand("vincular").setExecutor(discordCommand);
        getCommand("desvincular").setExecutor(discordCommand);

        // Registrar comandos bancários como aliases do comando principal
        getCommand("bank").setExecutor(clanCommandExecutor);
        getCommand("bank").setTabCompleter(clanCommandExecutor);
        getCommand("banco").setExecutor(clanCommandExecutor);
        getCommand("banco").setTabCompleter(clanCommandExecutor);
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
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new KDRListener(this), this);
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

    public ClanChatManager getClanChatManager() {
        return clanChatManager;
    }

    public DiscordManager getDiscordManager() {
        return discordManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }
}

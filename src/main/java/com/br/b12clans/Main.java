package com.br.b12clans;

import com.br.b12clans.chat.ClanChatManager;
import com.br.b12clans.chat.DiscordManager;
import com.br.b12clans.commands.AllyChatCommand;
import com.br.b12clans.commands.ClanChatCommand;
import com.br.b12clans.commands.ClanCommand;
import com.br.b12clans.commands.DiscordCommand;
import com.br.b12clans.database.DatabaseManager;
import com.br.b12clans.listeners.ChatListener;
import com.br.b12clans.listeners.KDRListener;
import com.br.b12clans.listeners.PlayerListener;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.managers.CommandManager;
import com.br.b12clans.managers.EconomyManager;
import com.br.b12clans.placeholders.ClanPlaceholder;
import com.br.b12clans.utils.AsyncHandler; // <-- MOVIDO PARA O LUGAR CORRETO
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends JavaPlugin {

    private DatabaseManager databaseManager;
    private ClanManager clanManager;
    private MessagesManager messagesManager;
    private ClanChatManager clanChatManager;
    private DiscordManager discordManager;
    private EconomyManager economyManager;
    private CommandManager commandManager;
    private ExecutorService threadPool;
    private AsyncHandler asyncHandler;
    // A LINHA DE IMPORT FOI REMOVIDA DAQUI

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.threadPool = Executors.newCachedThreadPool();

        this.databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            getLogger().severe("Falha crítica ao inicializar o banco de dados! O B12Clans será desabilitado.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.messagesManager = new MessagesManager(this);
        this.commandManager = new CommandManager(this);
        this.asyncHandler = new AsyncHandler(this); // <-- Esta linha agora funciona

        this.clanManager = new ClanManager(this);
        this.clanChatManager = new ClanChatManager(this);
        this.discordManager = new DiscordManager(this);
        this.economyManager = new EconomyManager(this);

        registerCommands();
        registerEvents();
        registerPlaceholders();

        // ##### TAREFA DE LIMPEZA AGENDADA #####
        long cleanupInterval = 20 * 60 * 5;
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            clanManager.cleanupExpiredRequests();
        }, cleanupInterval, cleanupInterval);
        // #####################################

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
        if (threadPool != null) {
            threadPool.shutdown();
        }
        getLogger().info("B12Clans foi desabilitado!");
    }

    private void registerCommands() {
        getCommand("clan").setExecutor(new ClanCommand(this));

        DiscordCommand discordCommand = new DiscordCommand(this);
        getCommand("discord").setExecutor(discordCommand);
        getCommand("vincular").setExecutor(discordCommand);
        getCommand("desvincular").setExecutor(discordCommand);

        List<String> clanChatAliases = commandManager.getChatCommandAliases("clan-chat");
        if (clanChatAliases != null && !clanChatAliases.isEmpty()) {
            ClanChatCommand clanChatCommand = new ClanChatCommand(this);
            for (String alias : clanChatAliases) {
                PluginCommand command = getCommand(alias);
                if (command != null) {
                    command.setExecutor(clanChatCommand);
                    command.setTabCompleter(clanChatCommand);
                } else {
                    getLogger().warning("Comando '" + alias + "' definido no commands.yml mas não encontrado no plugin.yml!");
                }
            }
        } else {
            getLogger().warning("Nenhum alias encontrado para 'clan-chat' no commands.yml!");
        }

        List<String> allyChatAliases = commandManager.getChatCommandAliases("ally-chat");
        if (allyChatAliases != null && !allyChatAliases.isEmpty()) {
            AllyChatCommand allyChatCommand = new AllyChatCommand(this);
            for (String alias : allyChatAliases) {
                PluginCommand command = getCommand(alias);
                if (command != null) {
                    command.setExecutor(allyChatCommand);
                    command.setTabCompleter(allyChatCommand);
                } else {
                    getLogger().warning("Comando '" + alias + "' definido no commands.yml mas não encontrado no plugin.yml!");
                }
            }
        } else {
            getLogger().warning("Nenhum alias encontrado para 'ally-chat' no commands.yml!");
        }
    }

    private void registerPlaceholders() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ClanPlaceholder(this).register();
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

    // Getters para os managers
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public ClanManager getClanManager() { return clanManager; }
    public MessagesManager getMessagesManager() { return messagesManager; }
    public ClanChatManager getClanChatManager() { return clanChatManager; }
    public DiscordManager getDiscordManager() { return discordManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public CommandManager getCommandManager() { return commandManager; }
    public ExecutorService getThreadPool() { return threadPool; }
    public AsyncHandler getAsyncHandler() { return asyncHandler; }
}
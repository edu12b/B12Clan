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

        // --- ORDEM DE INICIALIZAÇÃO CORRIGIDA ---

        // 1. Inicializa o DatabaseManager PRIMEIRO e ACIMA DE TUDO.
        this.databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            // Se a base de dados falhar, o plugin para aqui e desativa-se.
            getLogger().severe("Falha crítica ao inicializar o banco de dados! O B12Clans será desabilitado.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 2. Agora que o banco de dados está 100% pronto, inicializamos o resto.
        this.messagesManager = new MessagesManager(this);
        this.clanManager = new ClanManager(this);

        // 3. Com todos os gerenciadores prontos, registramos os comandos e eventos.
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
        getCommand("clan").setExecutor(new ClanCommand(this, clanManager, messagesManager));
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
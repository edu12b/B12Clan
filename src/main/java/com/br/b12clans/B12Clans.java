package com.br.b12clans;

import com.br.b12clans.commands.ClanCommand;
import com.br.b12clans.database.DatabaseManager;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.placeholders.ClanPlaceholder;
import com.br.b12clans.listeners.PlayerListener;
import org.bukkit.plugin.java.JavaPlugin;

public class B12Clans extends JavaPlugin {
    
    private static B12Clans instance;
    private DatabaseManager databaseManager;
    private ClanManager clanManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Salvar configuração padrão
        saveDefaultConfig();
        
        // Inicializar banco de dados
        this.databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            getLogger().severe("Falha ao conectar com o banco de dados! Desabilitando plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Inicializar gerenciadores
        this.clanManager = new ClanManager(this);
        
        // Registrar comandos
        registerCommands();

        // Registrar eventos
        registerEvents();
        
        // Registrar PlaceholderAPI
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
        getCommand("clan").setExecutor(new ClanCommand(this));
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
    }
    
    public static B12Clans getInstance() {
        return instance;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public ClanManager getClanManager() {
        return clanManager;
    }
}

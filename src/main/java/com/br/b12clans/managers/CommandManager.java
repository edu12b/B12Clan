package com.br.b12clans.managers;

import com.br.b12clans.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class CommandManager {

    private final Main plugin;
    private FileConfiguration commandConfig;
    private final File commandConfigFile;

    public CommandManager(Main plugin) {
        this.plugin = plugin;
        this.commandConfigFile = new File(plugin.getDataFolder(), "commands.yml");
        saveDefaultConfig();
        reloadConfig(); // Carrega imediatamente no construtor
    }

    public void reloadConfig() {
        commandConfig = YamlConfiguration.loadConfiguration(commandConfigFile);

        try (InputStreamReader defaultConfigStream = new InputStreamReader(plugin.getResource("commands.yml"), StandardCharsets.UTF_8)) {
            if (defaultConfigStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(defaultConfigStream);
                commandConfig.setDefaults(defaultConfig);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível carregar o commands.yml padrão.", e);
        }
    }

    public FileConfiguration getConfig() {
        if (commandConfig == null) {
            reloadConfig();
        }
        return commandConfig;
    }

    public void saveDefaultConfig() {
        if (!commandConfigFile.exists()) {
            plugin.saveResource("commands.yml", false);
        }
    }
    public List<String> getChatCommandAliases(String chatType) {
        // chatType será "clan-chat" ou "ally-chat"
        return getConfig().getStringList("chat-commands." + chatType);
    }

    public List<String> getAliasesFor(String commandKey) {
        return getConfig().getStringList("main-commands." + commandKey);
    }

    public List<String> getActionAliasesFor(String commandKey, String actionKey) {
        return getConfig().getStringList("sub-command-actions." + commandKey + "." + actionKey);
    }

    public Set<String> getCommandKeys() {
        if (getConfig().getConfigurationSection("main-commands") == null) {
            return Collections.emptySet();
        }
        return getConfig().getConfigurationSection("main-commands").getKeys(false);
    }
}
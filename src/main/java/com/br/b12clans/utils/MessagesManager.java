package com.br.b12clans.utils;

import com.br.b12clans.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class MessagesManager {

    private final Main plugin;
    private FileConfiguration langConfig;
    private String prefix;

    public MessagesManager(Main plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        File langFile = new File(plugin.getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            plugin.saveResource("lang.yml", false);
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);
        prefix = ChatColor.translateAlternateColorCodes('&', langConfig.getString("prefix", ""));
    }

    public void sendMessage(CommandSender sender, String path, String... placeholders) {
        String message = langConfig.getString(path);
        if (message == null || message.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Mensagem não encontrada: " + path);
            return;
        }

        // Aplica placeholders internos
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace(placeholders[i], placeholders[i + 1]);
            }
        }

        // Aplica placeholders do PAPI se o sender for um jogador
        if (sender instanceof Player) {
            // Lógica PAPI (se necessário no futuro)
            // message = PlaceholderAPI.setPlaceholders((Player) sender, message);
        }

        // Traduz cores e envia com o prefixo (se não for uma mensagem de ajuda/info)
        boolean isHelpOrInfo = path.startsWith("help-") || path.startsWith("info-") || path.startsWith("ver-");
        if (!message.trim().isEmpty() && !isHelpOrInfo) {
            sender.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', message));
        } else {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    // Método sobrecarregado para enviar mensagens sem placeholders
    public void sendMessage(CommandSender sender, String path) {
        sendMessage(sender, path, new String[0]);
    }
}
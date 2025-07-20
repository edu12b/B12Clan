package com.br.b12clans.utils;

import com.br.b12clans.Main;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

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

    public String getMessage(String path, String... placeholders) {
        // --- CORREÇÃO APLICADA AQUI ---
        // Garante que todas as buscas são feitas dentro da seção "messages".
        String fullPath = "messages." + path;
        String message = langConfig.getString(fullPath);

        if (message == null) {
            return ChatColor.RED + "Mensagem não encontrada: " + fullPath;
        }

        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace(placeholders[i], placeholders[i + 1]);
            }
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public void sendMessage(CommandSender sender, String path, String... placeholders) {
        String message = getMessage(path, placeholders);

        // Aplica placeholders do PAPI se o PAPI estiver ativo e o sender for um jogador
        if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI") && sender instanceof Player) {
            message = PlaceholderAPI.setPlaceholders((Player) sender, message);
        }

        // Verifica se a mensagem deve ter o prefixo
        boolean noPrefix = path.startsWith("help-") || path.startsWith("info-") || path.startsWith("ver-") || path.equals("clan-created-success");
        if (!prefix.isEmpty() && !noPrefix) {
            sender.sendMessage(prefix + message);
        } else {
            sender.sendMessage(message);
        }
    }

    public void sendMessage(CommandSender sender, String path) {
        sendMessage(sender, path, new String[0]);
    }
}

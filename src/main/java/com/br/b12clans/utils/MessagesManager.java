// ARQUIVO: src/main/java/com/br/b12clans/utils/MessagesManager.java
package com.br.b12clans.utils;

import com.br.b12clans.Main;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessagesManager {

    private final Main plugin;
    private FileConfiguration langConfig;
    private String prefix;

    private static final Pattern HEX_PATTERN = Pattern.compile("&#[a-fA-F0-9]{6}");

    public MessagesManager(Main plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    // MÉTODO ATUALIZADO
    public void loadMessages() {
        File langFile = new File(plugin.getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            plugin.getLogger().info("lang.yml não encontrado, criando arquivo padrão...");
            plugin.saveResource("lang.yml", false); // false para não sobrescrever se já existir
        }

        // Carrega a configuração do arquivo na pasta do servidor
        langConfig = YamlConfiguration.loadConfiguration(langFile);

        // Adiciona uma verificação de versão para garantir que o lang.yml está atualizado
        if (!langConfig.isSet("version") || langConfig.getDouble("version") < 1.1) {
            plugin.getLogger().warning("Seu arquivo lang.yml parece estar desatualizado!");
            plugin.getLogger().warning("Considere renomear seu lang.yml antigo e deixar o plugin gerar um novo.");
        }

        prefix = translateColors(langConfig.getString("prefix", ""));
    }

    public String getMessage(String path, String... placeholders) {
        String fullPath = "messages." + path;
        String message = langConfig.getString(fullPath);

        if (message == null) {
            return ChatColor.RED + "Mensagem não encontrada: " + fullPath;
        }

        // Usamos StringBuilder para evitar criar múltiplos objetos String
        StringBuilder sb = new StringBuilder(message);

        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                String placeholder = placeholders[i];
                String value = placeholders[i + 1];
                int index;
                // Usamos um loop while para garantir que TODAS as ocorrências do placeholder sejam substituídas
                while ((index = sb.indexOf(placeholder)) != -1) {
                    sb.replace(index, index + placeholder.length(), value);
                }
            }
        }
        return sb.toString();
    }

    public void sendMessage(CommandSender sender, String path, String... placeholders) {
        String message = getMessage(path, placeholders);

        if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI") && sender instanceof Player) {
            message = PlaceholderAPI.setPlaceholders((Player) sender, message);
        }

        message = translateColors(message);

        boolean noPrefix = path.startsWith("help-") || path.startsWith("info-") || path.startsWith("ver-") || path.startsWith("kdr-") || path.equals("clan-created-success");

        if (!prefix.isEmpty() && !noPrefix) {
            sender.sendMessage(prefix + " " + message);
        } else {
            sender.sendMessage(message);
        }
    }

    /**
     * Traduz códigos de cor, incluindo o formato hexadecimal &#RRGGBB.
     * Esta é a versão manual e mais compatível.
     * @param text O texto para colorir.
     * @return O texto com as cores do Minecraft aplicadas.
     */
    public String translateColors(String text) {
        if (text == null) {
            return "";
        }

        // Primeiro, traduzir cores hexadecimais (&#RRGGBB) para o formato do Spigot (§x§R§R§G§G§B§B)
        String hexTranslated = HEX_PATTERN.matcher(text).replaceAll(match -> {
            String hex = match.group().substring(2); // Remove o "&#"
            StringBuilder magic = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                magic.append('§').append(c);
            }
            return magic.toString();
        });

        // Depois, traduzir os códigos de cor normais (&c, &l, etc.)
        return ChatColor.translateAlternateColorCodes('&', hexTranslated);
    }
}
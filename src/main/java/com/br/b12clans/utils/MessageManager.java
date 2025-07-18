package com.br.b12clans.utils;

import com.br.b12clans.Main;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.regex.Pattern;

public class MessageManager {
    
    private final Main plugin;
    private FileConfiguration langConfig;
    private File langFile;
    
    // Padrão para cores hexadecimais
    private static final Pattern HEX_PATTERN = Pattern.compile("&#[a-fA-F0-9]{6}");
    
    public MessageManager(Main plugin) {
        this.plugin = plugin;
        loadLanguageFile();
    }
    
    private void loadLanguageFile() {
        langFile = new File(plugin.getDataFolder(), "lang.yml");
        
        if (!langFile.exists()) {
            plugin.saveResource("lang.yml", false);
        }
        
        langConfig = YamlConfiguration.loadConfiguration(langFile);
        plugin.getLogger().info("Arquivo de idioma carregado com sucesso!");
    }
    
    public void reloadLanguageFile() {
        if (langFile.exists()) {
            langConfig = YamlConfiguration.loadConfiguration(langFile);
            plugin.getLogger().info("Arquivo de idioma recarregado!");
        }
    }
    
    public String getMessage(String path, Object... args) {
        String message = langConfig.getString(path);
        
        if (message == null) {
            plugin.getLogger().warning("Mensagem não encontrada: " + path);
            return "§cMensagem não encontrada: " + path;
        }
        
        // Aplicar variáveis se fornecidas
        if (args.length > 0) {
            message = MessageFormat.format(message, args);
        }
        
        // Traduzir cores
        return translateColors(message);
    }
    
    public String translateColors(String message) {
        if (message == null) return null;
        
        // Primeiro traduzir cores hexadecimais
        String hexTranslated = HEX_PATTERN.matcher(message).replaceAll(match -> {
            String hex = match.group().substring(2); // Remove &#
            StringBuilder magic = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                magic.append("§").append(c);
            }
            return magic.toString();
        });
        
        // Depois traduzir cores normais
        return ChatColor.translateAlternateColorCodes('&', hexTranslated);
    }
    
    // Métodos de conveniência para mensagens comuns
    public String getNoPermission() {
        return getMessage("general.no-permission");
    }
    
    public String getPlayerOnly() {
        return getMessage("general.player-only");
    }
    
    public String getClanCreated() {
        return getMessage("clan.created");
    }
    
    public String getClanCreatedDetails(String name, String tag, String leader) {
        return getMessage("clan.created-details", name, tag, leader);
    }
    
    public String getAlreadyInClan(String clanName) {
        return getMessage("clan.already-in-clan", clanName);
    }
    
    public String getInvalidName() {
        return getMessage("clan.invalid-name");
    }
    
    public String getInvalidTag() {
        return getMessage("clan.invalid-tag");
    }
    
    public String getInvalidTagDetails(String cleanTag) {
        return getMessage("clan.invalid-tag-details", cleanTag);
    }
    
    public String getTagTooLong(int currentLength) {
        return getMessage("clan.tag-too-long", currentLength);
    }
    
    public String getTagTooLongTip() {
        return getMessage("clan.tag-too-long-tip");
    }
    
    public String getNoClan() {
        return getMessage("clan.no-clan");
    }
    
    public String getCheckingAvailability() {
        return getMessage("clan.checking-availability");
    }
    
    public String getClanExists() {
        return getMessage("clan.clan-exists");
    }
    
    public String getCreationFailed() {
        return getMessage("clan.creation-failed");
    }
    
    public String getPreviewBeforeCreate(String renderedTag, String cleanTag, int size) {
        return getMessage("clan.preview-before-create", renderedTag, cleanTag, size);
    }
    
    public String getUsageCrear() {
        return getMessage("commands.usage-criar");
    }
    
    public String getUsageVer() {
        return getMessage("commands.usage-ver");
    }
    
    public String getHelpHeader() {
        return getMessage("commands.help-header");
    }
    
    public String getHelpFooter() {
        return getMessage("commands.help-footer");
    }
    
    public String getHelpCriar() {
        return getMessage("commands.help-criar");
    }
    
    public String getHelpInfo() {
        return getMessage("commands.help-info");
    }
    
    public String getHelpVer() {
        return getMessage("commands.help-ver");
    }
    
    // Métodos para informações do clã
    public String getInfoHeader() {
        return getMessage("clan.info-header");
    }
    
    public String getInfoFooter() {
        return getMessage("clan.info-footer");
    }
    
    public String getInfoName(String name) {
        return getMessage("clan.info-name", name);
    }
    
    public String getInfoTag(String tag) {
        return getMessage("clan.info-tag", tag);
    }
    
    public String getInfoTagClean(String cleanTag) {
        return getMessage("clan.info-tag-clean", cleanTag);
    }
    
    public String getInfoTagSize(int size) {
        return getMessage("clan.info-tag-size", size);
    }
    
    public String getInfoCreated(String date) {
        return getMessage("clan.info-created", date);
    }
    
    // Métodos para preview de tag
    public String getPreviewHeader() {
        return getMessage("clan.preview-header");
    }
    
    public String getPreviewFooter() {
        return getMessage("clan.preview-footer");
    }
    
    public String getPreviewOriginal(String original) {
        return getMessage("clan.preview-original", original);
    }
    
    public String getPreviewRendered(String rendered) {
        return getMessage("clan.preview-rendered", rendered);
    }
    
    public String getPreviewClean(String clean) {
        return getMessage("clan.preview-clean", clean);
    }
    
    public String getPreviewSize(int size) {
        return getMessage("clan.preview-size", size);
    }
    
    public String getPreviewValid() {
        return getMessage("clan.preview-valid");
    }
    
    public String getPreviewInvalid() {
        return getMessage("clan.preview-invalid");
    }
    
    public String getPreviewInvalidReasonLong() {
        return getMessage("clan.preview-invalid-reason-long");
    }
    
    public String getPreviewInvalidReasonContent() {
        return getMessage("clan.preview-invalid-reason-content");
    }
}

package com.br.b12clans.commands;

import com.br.b12clans.B12Clans;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.models.Clan;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClanCommand implements CommandExecutor, TabCompleter {
    
    private final B12Clans plugin;
    private final ClanManager clanManager;
    
    public ClanCommand(B12Clans plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem usar este comando!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "criar":
                handleCreateCommand(player, args);
                break;
            case "info":
                handleInfoCommand(player, args);
                break;
            default:
                sendHelp(player);
                break;
        }
        
        return true;
    }
    
    private void handleCreateCommand(Player player, String[] args) {
        if (!player.hasPermission("b12clans.criar")) {
            player.sendMessage("§cVocê não tem permissão para criar clãs!");
            return;
        }
        
        if (args.length < 3) {
            player.sendMessage("§cUso correto: /clan criar <nome> <tag>");
            player.sendMessage("§7Exemplo: /clan criar MeuClan &#FF0000[MC]");
            return;
        }
        
        String name = args[1];
        String tag = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        
        // Validações
        if (!clanManager.isValidClanName(name)) {
            player.sendMessage("§cNome inválido! Use apenas letras, números e _ (3-16 caracteres)");
            return;
        }
        
        if (!clanManager.isValidClanTag(tag)) {
            player.sendMessage("§cTag inválida! Use apenas letras e números (2-6 caracteres sem cores)");
            return;
        }
        
        // Verificar se o jogador já tem clã
        Clan existingClan = clanManager.getPlayerClan(player.getUniqueId());
        if (existingClan != null) {
            player.sendMessage("§cVocê já faz parte do clã: §f" + existingClan.getName());
            return;
        }
        
        player.sendMessage("§eVerificando disponibilidade...");
        
        // Verificar se clã já existe
        plugin.getDatabaseManager().clanExists(name, tag)
            .thenAccept(exists -> {
                if (exists) {
                    player.sendMessage("§cJá existe um clã com esse nome ou tag!");
                    return;
                }
                
                // Criar clã
                plugin.getDatabaseManager().createClan(name, tag, player.getUniqueId(), player.getName())
                    .thenAccept(success -> {
                        if (success) {
                            player.sendMessage("§a§lCLÃ CRIADO!");
                            player.sendMessage("§7Nome: §f" + name);
                            player.sendMessage("§7Tag: " + clanManager.translateColors(tag));
                            player.sendMessage("§7Líder: §f" + player.getName());
                            
                            // Recarregar dados do jogador
                            clanManager.loadPlayerClan(player.getUniqueId());
                        } else {
                            player.sendMessage("§cErro ao criar o clã! Tente novamente.");
                        }
                    });
            });
    }
    
    private void handleInfoCommand(Player player, String[] args) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        
        if (clan == null) {
            player.sendMessage("§cVocê não faz parte de nenhum clã!");
            return;
        }
        
        player.sendMessage("§6§l=== INFORMAÇÕES DO CLÃ ===");
        player.sendMessage("§7Nome: §f" + clan.getName());
        player.sendMessage("§7Tag: " + clanManager.translateColors(clan.getTag()));
        player.sendMessage("§7Criado em: §f" + clan.getCreatedAt().toString());
        player.sendMessage("§6§l========================");
    }
    
    private void sendHelp(Player player) {
        player.sendMessage("§6§l=== B12CLANS - AJUDA ===");
        player.sendMessage("§e/clan criar <nome> <tag> §7- Criar um novo clã");
        player.sendMessage("§e/clan info §7- Ver informações do seu clã");
        player.sendMessage("§6§l=======================");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("criar", "info"));
        }
        
        return completions;
    }
}

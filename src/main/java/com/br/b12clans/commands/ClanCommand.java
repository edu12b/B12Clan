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
            case "ver":
                handleVerCommand(player, args);
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
            player.sendMessage("§7Use /clan ver <tag> para testar sua tag primeiro!");
            return;
        }
        
        String name = args[1];
        String tag = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        
        // Validações
        if (!clanManager.isValidClanName(name)) {
            player.sendMessage("§cNome inválido! Use apenas letras, números e _ (2-32 caracteres)");
            return;
        }

        if (!clanManager.isValidClanTag(tag)) {
            if (clanManager.isTagTooLong(tag)) {
                int currentLength = clanManager.getExpandedTagLength(tag);
                player.sendMessage("§cTag muito longa! Tamanho atual: " + currentLength + " caracteres (máximo: 1000)");
                player.sendMessage("§7Dica: Use menos cores ou uma tag mais curta");
            } else {
                String cleanTag = clanManager.getCleanTag(tag);
                player.sendMessage("§cTag inválida! Conteúdo limpo: '" + cleanTag + "'");
                player.sendMessage("§7Use apenas letras, números e símbolos básicos (1-16 caracteres sem cores)");
                player.sendMessage("§7Símbolos permitidos: [ ] ( ) - _");
            }
            return;
        }

        // Mostrar preview da tag antes de criar
        String expandedTag = clanManager.translateColors(tag);
        String cleanTag = clanManager.getCleanTag(tag);
        player.sendMessage("§7Preview da tag: " + expandedTag);
        player.sendMessage("§7Conteúdo limpo: §f" + cleanTag + " §7(Tamanho expandido: " + expandedTag.length() + " caracteres)");
        
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
        
        String cleanTag = clanManager.getCleanTag(clan.getTag());
        String expandedTag = clanManager.translateColors(clan.getTag());
        
        player.sendMessage("§6§l=== INFORMAÇÕES DO CLÃ ===");
        player.sendMessage("§7Nome: §f" + clan.getName());
        player.sendMessage("§7Tag: " + expandedTag);
        player.sendMessage("§7Tag limpa: §f" + cleanTag);
        player.sendMessage("§7Tamanho expandido: §f" + expandedTag.length() + " caracteres");
        player.sendMessage("§7Criado em: §f" + clan.getCreatedAt().toString());
        player.sendMessage("§6§l========================");
    }
    
    private void handleVerCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUso: /clan ver <tag>");
            player.sendMessage("§7Exemplo: /clan ver &#FF0000[MC]");
            return;
        }
        
        String tag = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        String expandedTag = clanManager.translateColors(tag);
        String cleanTag = clanManager.getCleanTag(tag);
        
        player.sendMessage("§6§l=== VISUALIZAR TAG ===");
        player.sendMessage("§7Tag original: §f" + tag);
        player.sendMessage("§7Tag renderizada: " + expandedTag);
        player.sendMessage("§7Conteúdo limpo: §f" + cleanTag);
        player.sendMessage("§7Tamanho expandido: §f" + expandedTag.length() + " caracteres");
        
        if (clanManager.isValidClanTag(tag)) {
            player.sendMessage("§a✓ Tag válida!");
        } else {
            player.sendMessage("§c✗ Tag inválida!");
            if (clanManager.isTagTooLong(tag)) {
                player.sendMessage("§c  Motivo: Muito longa (máximo: 1000 caracteres)");
            } else {
                player.sendMessage("§c  Motivo: Conteúdo inválido (1-16 caracteres limpos)");
            }
        }
        player.sendMessage("§6§l==================");
    }
    
    private void sendHelp(Player player) {
        player.sendMessage("§6§l=== B12CLANS - AJUDA ===");
        player.sendMessage("§e/clan criar <nome> <tag> §7- Criar um novo clã");
        player.sendMessage("§e/clan info §7- Ver informações do seu clã");
        player.sendMessage("§e/clan ver <tag> §7- Visualizar como uma tag ficará");
        player.sendMessage("§6§l=======================");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("criar", "info", "ver"));
        }
        
        return completions;
    }
}

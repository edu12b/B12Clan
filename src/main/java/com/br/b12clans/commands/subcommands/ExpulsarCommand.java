// ARQUIVO: src/main/java/com/br/b12clans/commands/subcommands/ExpulsarCommand.java
package com.br.b12clans.commands.subcommands;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.models.Clan;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ExpulsarCommand implements SubCommand {

    private final Main plugin;
    private final ClanManager clanManager;
    private final MessagesManager messages;

    public ExpulsarCommand(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.messages = plugin.getMessagesManager();
    }

    @Override
    public String getName() {
        return "expulsar";
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public void execute(Player player, String[] args) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        if (args.length < 1) {
            messages.sendMessage(player, "kick-usage");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            messages.sendMessage(player, "player-not-found", "%player_name%", args[0]);
            return;
        }

        UUID targetUuid = target.getUniqueId();
        if (targetUuid.equals(player.getUniqueId())) {
            messages.sendMessage(player, "cannot-kick-self");
            return;
        }

        // Inicia a cadeia de verificação assíncrona
        plugin.getDatabaseManager().getMemberRoleAsync(clan.getId(), player.getUniqueId())
                .thenComposeAsync(kickerRole -> {
                    // Primeira etapa: Pega o cargo de quem expulsa.
                    // Agora, precisamos pegar o cargo do alvo para comparar.
                    return plugin.getDatabaseManager().getMemberRoleAsync(clan.getId(), targetUuid)
                            .thenComposeAsync(targetRole -> {
                                // Segunda etapa: Temos os dois cargos, agora fazemos a validação.
                                if (!canKick(kickerRole, targetRole)) {
                                    // Se não puder expulsar, rejeitamos a operação com um erro específico.
                                    return CompletableFuture.failedFuture(new IllegalAccessException("Sem permissão de hierarquia"));
                                }
                                // Se puder expulsar, continuamos para a próxima etapa: remover o membro.
                                return plugin.getDatabaseManager().removeClanMemberAsync(clan.getId(), targetUuid);
                            }, plugin.getThreadPool());

                }, plugin.getThreadPool())
                .thenAccept(success -> {
                    // Etapa final: Executa na thread principal se todas as etapas anteriores deram certo.
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (success) {
                            clanManager.unloadPlayerClan(targetUuid);
                            messages.sendMessage(player, "kick-success", "%player_name%", target.getName());

                            if (target.isOnline()) {
                                messages.sendMessage(target.getPlayer(), "you-were-kicked", "%clan_name%", clan.getName());
                            }

                            clanManager.broadcastToClan(clan, "member-kicked", "%player_name%", target.getName(), "%kicker_name%", player.getName());

                            if (plugin.getConfig().getBoolean("discord.enabled", false)) {
                                plugin.getDiscordManager().onMemberLeft(clan, target);
                            }
                        } else {
                            messages.sendMessage(player, "generic-error");
                        }
                    });
                })
                .exceptionally(error -> {
                    // ##### CÓDIGO ANTIGO REMOVIDO E SUBSTITUÍDO POR UMA ÚNICA LINHA #####
                    plugin.getAsyncHandler().handleException(player, error, "generic-error");
                    return null;
                });
    }

    /**
     * Um método auxiliar para manter a lógica de permissão de expulsão limpa.
     */
    private boolean canKick(String kickerRole, String targetRole) {
        if (kickerRole == null || targetRole == null) {
            return false; // Jogador não está no clã
        }

        if (targetRole.equals("OWNER")) {
            return false; // Ninguém pode expulsar o dono
        }

        switch (kickerRole) {
            case "OWNER":
                return true; // Dono pode expulsar qualquer um (exceto ele mesmo, já verificado)
            case "VICE_LEADER":
                // Vice-líder só pode expulsar admins e membros
                return targetRole.equals("ADMIN") || targetRole.equals("MEMBER");
            case "ADMIN":
                // Admin só pode expulsar membros
                return targetRole.equals("MEMBER");
            default:
                return false;
        }
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        if (args.length == 1) {
            // Sugere jogadores online para simplificar
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> !name.equals(player.getName()))
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
package com.br.b12clans.commands;

import com.br.b12clans.Main;
import com.br.b12clans.database.ClanExistenceStatus;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.models.Clan;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ClanCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final ClanManager clanManager;
    private final MessagesManager messages;

    public ClanCommand(Main plugin, ClanManager clanManager, MessagesManager messagesManager) {
        this.plugin = plugin;
        this.clanManager = clanManager;
        this.messages = messagesManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            messages.sendMessage(sender, "player-only");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            // Comandos básicos
            case "criar":
                handleCreateCommand(player, args);
                break;
            case "info":
                handleInfoCommand(player, args);
                break;
            case "ver":
                handleVerCommand(player, args);
                break;
            case "convidar":
                handleInviteCommand(player, args);
                break;
            case "aceitar":
                handleAcceptCommand(player, args);
                break;
            case "negar":
                handleDenyCommand(player, args);
                break;
            case "sair":
                handleLeaveCommand(player, args);
                break;
            case "expulsar":
                handleKickCommand(player, args);
                break;
            case "deletar":
                handleDeleteCommand(player, args);
                break;
            case "promover":
                handlePromoteCommand(player, args);
                break;
            case "rebaixar":
                handleDemoteCommand(player, args);
                break;
            case "titulo":
                handleTitleCommand(player, args);
                break;

            // Novos comandos por hierarquia
            case "description":
                handleDescriptionCommand(player, args);
                break;
            case "home":
                handleHomeCommand(player, args);
                break;
            case "kdr":
                handleKDRCommand(player, args);
                break;
            case "modtag":
                handleModTagCommand(player, args);
                break;
            case "ally":
                handleAllyCommand(player, args);
                break;
            case "rival":
                handleRivalCommand(player, args);
                break;
            case "fee":
                handleFeeCommand(player, args);
                break;
            case "setbanner":
                handleSetBannerCommand(player, args);
                break;

            // Comandos bancários em inglês
            case "bank":
                handleBankCommand(player, args);
                break;

            // Comandos bancários em português
            case "banco":
                handleBancoCommand(player, args);
                break;

            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    // ========================================
    // COMANDOS BÁSICOS (já existentes)
    // ========================================

    private void handleCreateCommand(Player player, String[] args) {
        if (!player.hasPermission("b12clans.criar")) {
            messages.sendMessage(player, "no-permission");
            return;
        }
        if (args.length < 3) {
            messages.sendMessage(player, "create-usage");
            messages.sendMessage(player, "create-example");
            messages.sendMessage(player, "create-tag-test-tip");
            return;
        }
        String name = args[1];
        String tag = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (!clanManager.isValidClanName(name)) {
            messages.sendMessage(player, "invalid-name");
            return;
        }
        if (!clanManager.isValidClanTag(tag)) {
            if (clanManager.isTagTooLong(tag)) {
                messages.sendMessage(player, "tag-too-long");
            } else {
                messages.sendMessage(player, "invalid-tag-rules");
                messages.sendMessage(player, "invalid-tag-symbols");
            }
            return;
        }
        Clan existingClan = clanManager.getPlayerClan(player.getUniqueId());
        if (existingClan != null) {
            messages.sendMessage(player, "already-in-clan", "%clan_name%", existingClan.getName());
            return;
        }
        messages.sendMessage(player, "checking-availability");
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            ClanExistenceStatus status = plugin.getDatabaseManager().clanExists(name, tag);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                switch (status) {
                    case EXISTS:
                        messages.sendMessage(player, "clan-exists");
                        break;
                    case DATABASE_ERROR:
                        messages.sendMessage(player, "clan-creation-failed");
                        break;
                    case DOES_NOT_EXIST:
                        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                            boolean success = plugin.getDatabaseManager().createClan(name, tag, player.getUniqueId(), player.getName());
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                if (success) {
                                    clanManager.loadPlayerClan(player.getUniqueId());
                                    messages.sendMessage(player, "clan-created-success");
                                    messages.sendMessage(player, "clan-created-name", "%name%", clanManager.formatDisplayName(name));
                                    messages.sendMessage(player, "clan-created-tag", "%tag%", clanManager.translateColors(tag));
                                    messages.sendMessage(player, "clan-created-leader", "%leader%", player.getName());

                                    // Adicionar ao Discord se estiver habilitado e verificado
                                    if (plugin.getConfig().getBoolean("discord.enabled", false)) {
                                        // Aguardar um pouco para garantir que o clã foi carregado
                                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                            Clan newClan = clanManager.getPlayerClan(player.getUniqueId());
                                            if (newClan != null) {
                                                plugin.getDiscordManager().onClanCreated(newClan);
                                            }
                                        }, 20L); // 1 segundo de delay
                                    }
                                } else {
                                    messages.sendMessage(player, "clan-creation-failed");
                                }
                            });
                        });
                        break;
                }
            });
        });
    }

    private void handleInfoCommand(Player player, String[] args) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }
        String cleanTag = clanManager.getCleanTag(clan.getTag());
        String expandedTag = clanManager.translateColors(clan.getTag());
        messages.sendMessage(player, "info-header");
        messages.sendMessage(player, "info-name", "%name%", clanManager.formatDisplayName(clan.getName()));
        messages.sendMessage(player, "info-tag", "%tag%", expandedTag);
        messages.sendMessage(player, "info-clean-tag", "%clean_tag%", cleanTag);
        messages.sendMessage(player, "info-expanded-size", "%length%", String.valueOf(expandedTag.length()));
        messages.sendMessage(player, "info-created-at", "%date%", clan.getCreatedAt().toString());
        messages.sendMessage(player, "info-footer");
    }

    private void handleVerCommand(Player player, String[] args) {
        if (args.length < 2) {
            messages.sendMessage(player, "ver-usage");
            return;
        }
        String tag = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        String expandedTag = clanManager.translateColors(tag);
        String cleanTag = clanManager.getCleanTag(tag);
        messages.sendMessage(player, "ver-header");
        messages.sendMessage(player, "ver-original-tag", "%tag%", tag);
        messages.sendMessage(player, "ver-rendered-tag", "%tag_rendered%", expandedTag);
        messages.sendMessage(player, "info-clean-tag", "%clean_tag%", cleanTag);
        messages.sendMessage(player, "info-expanded-size", "%length%", String.valueOf(expandedTag.length()));
        if (clanManager.isValidClanTag(tag)) {
            messages.sendMessage(player, "ver-valid");
        } else {
            messages.sendMessage(player, "ver-invalid");
            if (clanManager.isTagTooLong(tag)) {
                messages.sendMessage(player, "ver-invalid-reason-long");
            } else {
                messages.sendMessage(player, "ver-invalid-reason-content");
            }
        }
        messages.sendMessage(player, "ver-footer");
    }

    private void handleInviteCommand(Player player, String[] args) {
        Clan inviterClan = clanManager.getPlayerClan(player.getUniqueId());
        if (inviterClan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String role = plugin.getDatabaseManager().getMemberRole(inviterClan.getId(), player.getUniqueId());

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (role == null || !(role.equals("OWNER") || role.equals("VICE_LEADER") || role.equals("ADMIN"))) {
                    messages.sendMessage(player, "invite-no-permission");
                    return;
                }

                if (args.length < 2) {
                    messages.sendMessage(player, "invite-usage");
                    return;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    messages.sendMessage(player, "player-not-found", "%player_name%", args[1]);
                    return;
                }

                if (target.getUniqueId().equals(player.getUniqueId())) {
                    messages.sendMessage(player, "cannot-invite-self");
                    return;
                }

                Clan targetClan = clanManager.getPlayerClan(target.getUniqueId());
                if (targetClan != null) {
                    messages.sendMessage(player, "target-already-in-clan", "%player_name%", target.getName());
                    return;
                }

                clanManager.addInvite(target.getUniqueId(), inviterClan.getId());
                messages.sendMessage(player, "invite-sent", "%player_name%", target.getName());
                messages.sendMessage(target, "invite-received",
                        "%clan_name%", inviterClan.getName(),
                        "%clan_tag%", clanManager.getCleanTag(inviterClan.getTag()));
            });
        });
    }

    private void handleAcceptCommand(Player player, String[] args) {
        if (args.length < 2) {
            messages.sendMessage(player, "accept-usage");
            return;
        }
        Integer invitedClanId = clanManager.getPendingInvite(player.getUniqueId());
        if (invitedClanId == null) {
            messages.sendMessage(player, "no-pending-invite");
            return;
        }
        clanManager.getClanById(invitedClanId).thenAcceptAsync(clan -> {
            if (clan == null || !clanManager.getCleanTag(clan.getTag()).equalsIgnoreCase(args[1])) {
                messages.sendMessage(player, "no-pending-invite");
                return;
            }
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                boolean success = plugin.getDatabaseManager().addClanMember(clan.getId(), player.getUniqueId(), player.getName());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        clanManager.removeInvite(player.getUniqueId());
                        clanManager.loadPlayerClan(player.getUniqueId());
                        messages.sendMessage(player, "invite-accepted", "%clan_name%", clan.getName());
                        clanManager.broadcastToClan(clan.getId(), messages.getMessage("player-joined-clan-broadcast", "%player_name%", player.getName()));

                        // Adicionar ao Discord se estiver habilitado e verificado
                        if (plugin.getConfig().getBoolean("discord.enabled", false)) {
                            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                plugin.getDiscordManager().onPlayerJoinedClan(player.getUniqueId());
                            }, 20L); // 1 segundo de delay
                        }
                    } else {
                        messages.sendMessage(player, "generic-error");
                    }
                });
            });
        }, runnable -> plugin.getServer().getScheduler().runTask(plugin, runnable));
    }

    private void handleDenyCommand(Player player, String[] args) {
        if (args.length < 2) {
            messages.sendMessage(player, "deny-usage");
            return;
        }
        Integer invitedClanId = clanManager.getPendingInvite(player.getUniqueId());
        if (invitedClanId == null) {
            messages.sendMessage(player, "no-pending-invite");
            return;
        }
        clanManager.getClanById(invitedClanId).thenAcceptAsync(clan -> {
            if (clan != null && clanManager.getCleanTag(clan.getTag()).equalsIgnoreCase(args[1])) {
                clanManager.removeInvite(player.getUniqueId());
                messages.sendMessage(player, "invite-denied", "%clan_name%", clan.getName());
            } else {
                messages.sendMessage(player, "no-pending-invite");
            }
        }, runnable -> plugin.getServer().getScheduler().runTask(plugin, runnable));
    }

    private void handleLeaveCommand(Player player, String[] args) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }
        if (clan.getOwnerUuid().equals(player.getUniqueId())) {
            messages.sendMessage(player, "cannot-leave-as-owner");
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = plugin.getDatabaseManager().removeClanMember(clan.getId(), player.getUniqueId());
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (success) {
                    clanManager.unloadPlayerClan(player.getUniqueId());
                    messages.sendMessage(player, "leave-success", "%clan_name%", clan.getName());

                    // Remover do tópico do Discord se estiver verificado
                    if (plugin.getConfig().getBoolean("discord.enabled", false)) {
                        plugin.getDiscordManager().removePlayerFromClanThread(player.getUniqueId(), clan);
                    }
                } else {
                    messages.sendMessage(player, "generic-error");
                }
            });
        });
    }

    private void handleKickCommand(Player player, String[] args) {
        Clan kickerClan = clanManager.getPlayerClan(player.getUniqueId());
        if (kickerClan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        if (args.length < 2) {
            messages.sendMessage(player, "kick-usage");
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String kickerRole = plugin.getDatabaseManager().getMemberRole(kickerClan.getId(), player.getUniqueId());

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (kickerRole == null || !(kickerRole.equals("OWNER") || kickerRole.equals("VICE_LEADER") || kickerRole.equals("ADMIN"))) {
                    messages.sendMessage(player, "kick-no-permission");
                    return;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (!target.hasPlayedBefore() && !target.isOnline()) {
                    messages.sendMessage(player, "player-not-found", "%player_name%", args[1]);
                    return;
                }

                UUID targetUuid = target.getUniqueId();
                if (targetUuid.equals(player.getUniqueId())) {
                    messages.sendMessage(player, "cannot-kick-self");
                    return;
                }

                if (kickerClan.getOwnerUuid().equals(targetUuid)) {
                    messages.sendMessage(player, "cannot-kick-owner");
                    return;
                }

                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    String targetRole = plugin.getDatabaseManager().getMemberRole(kickerClan.getId(), targetUuid);

                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (targetRole == null) {
                            messages.sendMessage(player, "player-not-in-your-clan", "%player_name%", target.getName());
                            return;
                        }

                        // Vice-líderes não podem expulsar outros vice-líderes ou admins
                        if (kickerRole.equals("VICE_LEADER") && (targetRole.equals("VICE_LEADER") || targetRole.equals("ADMIN"))) {
                            messages.sendMessage(player, "kick-no-permission");
                            return;
                        }

                        // Admins não podem expulsar vice-líderes ou outros admins
                        if (kickerRole.equals("ADMIN") && (targetRole.equals("VICE_LEADER") || targetRole.equals("ADMIN"))) {
                            messages.sendMessage(player, "kick-no-permission");
                            return;
                        }

                        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                            boolean success = plugin.getDatabaseManager().removeClanMember(kickerClan.getId(), targetUuid);
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                if (success) {
                                    clanManager.unloadPlayerClan(targetUuid);
                                    messages.sendMessage(player, "kick-success", "%player_name%", target.getName());
                                    if (target.isOnline()) {
                                        messages.sendMessage(target.getPlayer(), "you-were-kicked", "%clan_name%", kickerClan.getName());
                                    }

                                    // Remover do tópico do Discord se estiver verificado
                                    if (plugin.getConfig().getBoolean("discord.enabled", false)) {
                                        plugin.getDiscordManager().removePlayerFromClanThread(targetUuid, kickerClan);
                                    }
                                } else {
                                    messages.sendMessage(player, "generic-error");
                                }
                            });
                        });
                    });
                });
            });
        });
    }

    private void handleDeleteCommand(Player player, String[] args) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }
        if (!clan.getOwnerUuid().equals(player.getUniqueId())) {
            messages.sendMessage(player, "not-owner");
            return;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            messages.sendMessage(player, "delete-confirm");
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = plugin.getDatabaseManager().deleteClan(clan.getId());
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (success) {
                    clanManager.unloadPlayerClan(player.getUniqueId());
                    messages.sendMessage(player, "delete-success", "%clan_name%", clan.getName());

                    // Arquivar tópico do Discord se estiver habilitado
                    if (plugin.getConfig().getBoolean("discord.enabled", false)) {
                        plugin.getDiscordManager().archiveClanThread(clan);
                    }
                } else {
                    messages.sendMessage(player, "generic-error");
                }
            });
        });
    }

    private void handlePromoteCommand(Player player, String[] args) {
        if (!player.hasPermission("b12clans.promover")) {
            messages.sendMessage(player, "no-permission");
            return;
        }
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String promoterRole = plugin.getDatabaseManager().getMemberRole(clan.getId(), player.getUniqueId());

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (promoterRole == null || !promoterRole.equals("OWNER")) {
                    messages.sendMessage(player, "no-permission-to-manage-roles");
                    return;
                }

                if (args.length < 2) {
                    messages.sendMessage(player, "promote-usage");
                    return;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (!target.hasPlayedBefore() && !target.isOnline()) {
                    messages.sendMessage(player, "player-not-found", "%player_name%", args[1]);
                    return;
                }

                UUID targetUuid = target.getUniqueId();
                if (targetUuid.equals(player.getUniqueId())) {
                    messages.sendMessage(player, "cannot-manage-self");
                    return;
                }

                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    String targetRole = plugin.getDatabaseManager().getMemberRole(clan.getId(), targetUuid);
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (targetRole == null) {
                            messages.sendMessage(player, "player-not-member", "%player_name%", target.getName());
                            return;
                        }

                        String newRole = null;
                        String roleMessage = null;

                        switch (targetRole) {
                            case "MEMBER":
                                newRole = "ADMIN";
                                roleMessage = "Admin";
                                break;
                            case "ADMIN":
                                newRole = "VICE_LEADER";
                                roleMessage = "Vice-líder";
                                break;
                            case "VICE_LEADER":
                                messages.sendMessage(player, "player-already-max-role", "%player_name%", target.getName());
                                return;
                            case "OWNER":
                                messages.sendMessage(player, "cannot-manage-owner");
                                return;
                        }

                        final String finalNewRole = newRole;
                        final String finalRoleMessage = roleMessage;

                        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                            boolean success = plugin.getDatabaseManager().updateMemberRole(clan.getId(), targetUuid, finalNewRole);
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                if (success) {
                                    messages.sendMessage(player, "promote-success", "%player_name%", target.getName(), "%role%", finalRoleMessage);
                                    if (target.isOnline()) {
                                        messages.sendMessage(target.getPlayer(), "got-promoted", "%clan_name%", clan.getName(), "%role%", finalRoleMessage);
                                    }
                                } else {
                                    messages.sendMessage(player, "generic-error");
                                }
                            });
                        });
                    });
                });
            });
        });
    }

    private void handleDemoteCommand(Player player, String[] args) {
        if (!player.hasPermission("b12clans.rebaixar")) {
            messages.sendMessage(player, "no-permission");
            return;
        }
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String demoterRole = plugin.getDatabaseManager().getMemberRole(clan.getId(), player.getUniqueId());

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (demoterRole == null || !demoterRole.equals("OWNER")) {
                    messages.sendMessage(player, "no-permission-to-manage-roles");
                    return;
                }

                if (args.length < 2) {
                    messages.sendMessage(player, "demote-usage");
                    return;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (!target.hasPlayedBefore() && !target.isOnline()) {
                    messages.sendMessage(player, "player-not-found", "%player_name%", args[1]);
                    return;
                }

                UUID targetUuid = target.getUniqueId();
                if (targetUuid.equals(clan.getOwnerUuid())) {
                    messages.sendMessage(player, "cannot-manage-owner");
                    return;
                }

                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    String targetRole = plugin.getDatabaseManager().getMemberRole(clan.getId(), targetUuid);
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (targetRole == null) {
                            messages.sendMessage(player, "player-not-member", "%player_name%", target.getName());
                            return;
                        }

                        String newRole = null;
                        String roleMessage = null;

                        switch (targetRole) {
                            case "VICE_LEADER":
                                newRole = "ADMIN";
                                roleMessage = "Admin";
                                break;
                            case "ADMIN":
                                newRole = "MEMBER";
                                roleMessage = "Membro";
                                break;
                            case "MEMBER":
                                messages.sendMessage(player, "player-already-min-role", "%player_name%", target.getName());
                                return;
                            case "OWNER":
                                messages.sendMessage(player, "cannot-manage-owner");
                                return;
                        }

                        final String finalNewRole = newRole;
                        final String finalRoleMessage = roleMessage;

                        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                            boolean success = plugin.getDatabaseManager().updateMemberRole(clan.getId(), targetUuid, finalNewRole);
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                if (success) {
                                    messages.sendMessage(player, "demote-success", "%player_name%", target.getName(), "%role%", finalRoleMessage);
                                    if (target.isOnline()) {
                                        messages.sendMessage(target.getPlayer(), "got-demoted", "%clan_name%", clan.getName(), "%role%", finalRoleMessage);
                                    }
                                } else {
                                    messages.sendMessage(player, "generic-error");
                                }
                            });
                        });
                    });
                });
            });
        });
    }

    private void handleTitleCommand(Player player, String[] args) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String role = plugin.getDatabaseManager().getMemberRole(clan.getId(), player.getUniqueId());

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (role == null || !(role.equals("OWNER") || role.equals("VICE_LEADER") || role.equals("ADMIN"))) {
                    messages.sendMessage(player, "no-permission-to-set-title");
                    return;
                }

                if (args.length < 2) {
                    messages.sendMessage(player, "title-usage");
                    return;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (!target.hasPlayedBefore() && !target.isOnline()) {
                    messages.sendMessage(player, "player-not-found", "%player_name%", args[1]);
                    return;
                }

                String title = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : null;

                if (title != null && title.length() > 50) {
                    messages.sendMessage(player, "title-too-long");
                    return;
                }

                String coloredTitle = (title != null) ? clanManager.translateColors(title) : "";

                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    String targetRole = plugin.getDatabaseManager().getMemberRole(clan.getId(), target.getUniqueId());
                    if (targetRole == null) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            messages.sendMessage(player, "player-not-member", "%player_name%", target.getName());
                        });
                        return;
                    }

                    boolean success = plugin.getDatabaseManager().updateMemberTitle(clan.getId(), target.getUniqueId(), title);

                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (success) {
                            if (title == null || title.isEmpty()) {
                                messages.sendMessage(player, "title-cleared", "%player_name%", target.getName());
                            } else {
                                messages.sendMessage(player, "title-set", "%player_name%", target.getName(), "%title%", coloredTitle);
                            }
                        } else {
                            messages.sendMessage(player, "generic-error");
                        }
                    });
                });
            });
        });
    }

    // ========================================
    // NOVOS COMANDOS POR HIERARQUIA
    // ========================================

    private void handleDescriptionCommand(Player player, String[] args) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        if (args.length == 1) {
            // Mostrar descrição atual
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                String description = plugin.getDatabaseManager().getClanDescription(clan.getId());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (description == null || description.trim().isEmpty()) {
                        messages.sendMessage(player, "description-not-set");
                    } else {
                        messages.sendMessage(player, "description-current", "%description%", clanManager.translateColors(description));
                    }
                });
            });
            return;
        }

        // Alterar descrição (apenas vice-líderes e líderes)
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String role = plugin.getDatabaseManager().getMemberRole(clan.getId(), player.getUniqueId());

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (role == null || !(role.equals("OWNER") || role.equals("VICE_LEADER"))) {
                    messages.sendMessage(player, "description-no-permission");
                    return;
                }

                String description = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                if (description.length() > 200) {
                    messages.sendMessage(player, "description-too-long");
                    return;
                }

                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean success = plugin.getDatabaseManager().updateClanDescription(clan.getId(), description);
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (success) {
                            messages.sendMessage(player, "description-updated", "%description%", clanManager.translateColors(description));
                        } else {
                            messages.sendMessage(player, "generic-error");
                        }
                    });
                });
            });
        });
    }

    private void handleHomeCommand(Player player, String[] args) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        if (args.length == 1) {
            // Teleportar para home
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                Object[] homeData = plugin.getDatabaseManager().getClanHome(clan.getId());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (homeData == null) {
                        messages.sendMessage(player, "home-not-set");
                        return;
                    }

                    String worldName = (String) homeData[0];
                    double x = (Double) homeData[1];
                    double y = (Double) homeData[2];
                    double z = (Double) homeData[3];
                    float yaw = (Float) homeData[4];
                    float pitch = (Float) homeData[5];

                    Location homeLocation = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
                    if (homeLocation.getWorld() == null) {
                        messages.sendMessage(player, "home-world-not-found");
                        return;
                    }

                    player.teleport(homeLocation);
                    messages.sendMessage(player, "home-teleported");
                });
            });
            return;
        }

        String subAction = args[1].toLowerCase();
        if (subAction.equals("set")) {
            // Definir home (apenas líderes e vice-líderes se permitido)
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                String role = plugin.getDatabaseManager().getMemberRole(clan.getId(), player.getUniqueId());

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    boolean canSetHome = false;
                    if (role != null) {
                        if (role.equals("OWNER")) {
                            canSetHome = true;
                        } else if (role.equals("VICE_LEADER") && plugin.getConfig().getBoolean("permissions.vice-leader-can-set-home", true)) {
                            canSetHome = true;
                        }
                    }

                    if (!canSetHome) {
                        messages.sendMessage(player, "home-set-no-permission");
                        return;
                    }

                    Location loc = player.getLocation();
                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                        boolean success = plugin.getDatabaseManager().setClanHome(clan.getId(),
                                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            if (success) {
                                messages.sendMessage(player, "home-set-success");
                            } else {
                                messages.sendMessage(player, "generic-error");
                            }
                        });
                    });
                });
            });
        } else if (subAction.equals("clear")) {
            // Limpar home (apenas líderes)
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                String role = plugin.getDatabaseManager().getMemberRole(clan.getId(), player.getUniqueId());

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (role == null || !role.equals("OWNER")) {
                        messages.sendMessage(player, "home-clear-no-permission");
                        return;
                    }

                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                        boolean success = plugin.getDatabaseManager().clearClanHome(clan.getId());
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            if (success) {
                                messages.sendMessage(player, "home-cleared");
                            } else {
                                messages.sendMessage(player, "generic-error");
                            }
                        });
                    });
                });
            });
        } else {
            messages.sendMessage(player, "home-usage");
        }
    }

    private void handleKDRCommand(Player player, String[] args) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            int[] kdrData = plugin.getDatabaseManager().getPlayerKDR(player.getUniqueId());
            int kills = kdrData[0];
            int deaths = kdrData[1];

            double kdr = deaths == 0 ? (kills > 0 ? kills : 0.0) : (double) kills / deaths;

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                messages.sendMessage(player, "kdr-header");
                messages.sendMessage(player, "kdr-ratio", "%kdr%", String.format("%.2f", kdr));
                messages.sendMessage(player, "kdr-details", "%kills%", String.valueOf(kills), "%deaths%", String.valueOf(deaths));
                messages.sendMessage(player, "kdr-footer");
            });
        });
    }

    private void handleModTagCommand(Player player, String[] args) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        // Apenas líderes podem modificar a tag
        if (!clan.getOwnerUuid().equals(player.getUniqueId())) {
            messages.sendMessage(player, "modtag-no-permission");
            return;
        }

        if (args.length < 2) {
            messages.sendMessage(player, "modtag-usage");
            return;
        }

        String newTag = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        // Validar nova tag
        if (!clanManager.isValidClanTag(newTag)) {
            if (clanManager.isTagTooLong(newTag)) {
                messages.sendMessage(player, "tag-too-long");
            } else {
                messages.sendMessage(player, "invalid-tag-rules");
                messages.sendMessage(player, "invalid-tag-symbols");
            }
            return;
        }

        // Verificar se a tag já existe
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Clan existingClan = plugin.getDatabaseManager().getClanByTag(newTag);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (existingClan != null && existingClan.getId() != clan.getId()) {
                    messages.sendMessage(player, "tag-already-exists");
                    return;
                }

                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean success = plugin.getDatabaseManager().updateClanTag(clan.getId(), newTag);
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (success) {
                            // Recarregar clã no cache
                            clanManager.loadPlayerClan(player.getUniqueId());
                            messages.sendMessage(player, "modtag-success", "%old_tag%", clanManager.translateColors(clan.getTag()), "%new_tag%", clanManager.translateColors(newTag));
                        } else {
                            messages.sendMessage(player, "generic-error");
                        }
                    });
                });
            });
        });
    }

    private void handleAllyCommand(Player player, String[] args) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        if (args.length < 3) {
            messages.sendMessage(player, "ally-usage");
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String role = plugin.getDatabaseManager().getMemberRole(clan.getId(), player.getUniqueId());

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                boolean canManageAllies = false;
                if (role != null) {
                    if (role.equals("OWNER")) {
                        canManageAllies = true;
                    } else if (role.equals("VICE_LEADER") && plugin.getConfig().getBoolean("permissions.vice-leader-can-manage-allies", true)) {
                        canManageAllies = true;
                    }
                }

                if (!canManageAllies) {
                    messages.sendMessage(player, "ally-no-permission");
                    return;
                }

                String action = args[1].toLowerCase();
                String targetTag = args[2];

                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    Clan targetClan = plugin.getDatabaseManager().getClanByTag(targetTag);
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (targetClan == null) {
                            messages.sendMessage(player, "clan-not-found", "%tag%", targetTag);
                            return;
                        }

                        if (targetClan.getId() == clan.getId()) {
                            messages.sendMessage(player, "cannot-ally-self");
                            return;
                        }

                        if (action.equals("add")) {
                            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                                boolean alreadyAllies = plugin.getDatabaseManager().areAllies(clan.getId(), targetClan.getId());
                                if (alreadyAllies) {
                                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                                        messages.sendMessage(player, "already-allies", "%clan_name%", targetClan.getName());
                                    });
                                    return;
                                }

                                boolean success = plugin.getDatabaseManager().addAlly(clan.getId(), targetClan.getId());
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    if (success) {
                                        messages.sendMessage(player, "ally-added", "%clan_name%", targetClan.getName());
                                    } else {
                                        messages.sendMessage(player, "generic-error");
                                    }
                                });
                            });
                        } else if (action.equals("remove")) {
                            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                                boolean areAllies = plugin.getDatabaseManager().areAllies(clan.getId(), targetClan.getId());
                                if (!areAllies) {
                                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                                        messages.sendMessage(player, "not-allies", "%clan_name%", targetClan.getName());
                                    });
                                    return;
                                }

                                boolean success = plugin.getDatabaseManager().removeAlly(clan.getId(), targetClan.getId());
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    if (success) {
                                        messages.sendMessage(player, "ally-removed", "%clan_name%", targetClan.getName());
                                    } else {
                                        messages.sendMessage(player, "generic-error");
                                    }
                                });
                            });
                        } else {
                            messages.sendMessage(player, "ally-usage");
                        }
                    });
                });
            });
        });
    }

    private void handleRivalCommand(Player player, String[] args) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        if (args.length < 3) {
            messages.sendMessage(player, "rival-usage");
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String role = plugin.getDatabaseManager().getMemberRole(clan.getId(), player.getUniqueId());

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                boolean canManageRivals = false;
                if (role != null) {
                    if (role.equals("OWNER")) {
                        canManageRivals = true;
                    } else if (role.equals("VICE_LEADER") && plugin.getConfig().getBoolean("permissions.vice-leader-can-manage-rivals", true)) {
                        canManageRivals = true;
                    }
                }

                if (!canManageRivals) {
                    messages.sendMessage(player, "rival-no-permission");
                    return;
                }

                String action = args[1].toLowerCase();
                String targetTag = args[2];

                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    Clan targetClan = plugin.getDatabaseManager().getClanByTag(targetTag);
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (targetClan == null) {
                            messages.sendMessage(player, "clan-not-found", "%tag%", targetTag);
                            return;
                        }

                        if (targetClan.getId() == clan.getId()) {
                            messages.sendMessage(player, "cannot-rival-self");
                            return;
                        }

                        if (action.equals("add")) {
                            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                                boolean alreadyRivals = plugin.getDatabaseManager().areRivals(clan.getId(), targetClan.getId());
                                if (alreadyRivals) {
                                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                                        messages.sendMessage(player, "already-rivals", "%clan_name%", targetClan.getName());
                                    });
                                    return;
                                }

                                boolean success = plugin.getDatabaseManager().addRival(clan.getId(), targetClan.getId());
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    if (success) {
                                        messages.sendMessage(player, "rival-added", "%clan_name%", targetClan.getName());
                                    } else {
                                        messages.sendMessage(player, "generic-error");
                                    }
                                });
                            });
                        } else if (action.equals("remove")) {
                            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                                boolean areRivals = plugin.getDatabaseManager().areRivals(clan.getId(), targetClan.getId());
                                if (!areRivals) {
                                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                                        messages.sendMessage(player, "not-rivals", "%clan_name%", targetClan.getName());
                                    });
                                    return;
                                }

                                boolean success = plugin.getDatabaseManager().removeRival(clan.getId(), targetClan.getId());
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    if (success) {
                                        messages.sendMessage(player, "rival-removed", "%clan_name%", targetClan.getName());
                                    } else {
                                        messages.sendMessage(player, "generic-error");
                                    }
                                });
                            });
                        } else {
                            messages.sendMessage(player, "rival-usage");
                        }
                    });
                });
            });
        });
    }

    private void handleFeeCommand(Player player, String[] args) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        // Apenas líderes podem definir taxas
        if (!clan.getOwnerUuid().equals(player.getUniqueId())) {
            messages.sendMessage(player, "fee-no-permission");
            return;
        }

        if (args.length < 3 || !args[1].equalsIgnoreCase("set")) {
            messages.sendMessage(player, "fee-usage");
            return;
        }

        try {
            double amount = Double.parseDouble(args[2]);
            if (amount < 0) {
                messages.sendMessage(player, "fee-invalid-amount");
                return;
            }

            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                boolean success = plugin.getDatabaseManager().setClanFee(clan.getId(), amount);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        messages.sendMessage(player, "fee-set-success", "%amount%", String.format("%.2f", amount));
                    } else {
                        messages.sendMessage(player, "generic-error");
                    }
                });
            });
        } catch (NumberFormatException e) {
            messages.sendMessage(player, "fee-invalid-number");
        }
    }

    private void handleSetBannerCommand(Player player, String[] args) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        // Apenas líderes podem definir banner
        if (!clan.getOwnerUuid().equals(player.getUniqueId())) {
            messages.sendMessage(player, "setbanner-no-permission");
            return;
        }

        // Por enquanto, apenas uma mensagem informativa
        // A implementação completa de banners requer mais complexidade
        messages.sendMessage(player, "setbanner-not-implemented");
    }

    // ========================================
    // COMANDOS BANCÁRIOS (INGLÊS)
    // ========================================

    private void handleBankCommand(Player player, String[] args) {
        com.br.b12clans.models.Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        if (!plugin.getEconomyManager().isEnabled()) {
            messages.sendMessage(player, "economy-not-available");
            return;
        }

        if (args.length < 2) {
            messages.sendMessage(player, "bank-usage");
            return;
        }

        String subCommand = args[1].toLowerCase();

        switch (subCommand) {
            case "status":
            case "balance":
                handleBankStatus(player, clan);
                break;
            case "deposit":
                handleBankDeposit(player, clan, args);
                break;
            case "withdraw":
                handleBankWithdraw(player, clan, args);
                break;
            default:
                messages.sendMessage(player, "bank-usage");
                break;
        }
    }

    // ========================================
    // COMANDOS BANCÁRIOS (PORTUGUÊS)
    // ========================================

    private void handleBancoCommand(Player player, String[] args) {
        com.br.b12clans.models.Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        if (!plugin.getEconomyManager().isEnabled()) {
            messages.sendMessage(player, "economy-not-available");
            return;
        }

        if (args.length < 2) {
            messages.sendMessage(player, "banco-usage");
            return;
        }

        String subCommand = args[1].toLowerCase();

        switch (subCommand) {
            case "status":
            case "saldo":
                handleBankStatus(player, clan);
                break;
            case "depositar":
                handleBankDeposit(player, clan, args);
                break;
            case "sacar":
            case "retirar":
                handleBankWithdraw(player, clan, args);
                break;
            default:
                messages.sendMessage(player, "banco-usage");
                break;
        }
    }

    // ========================================
    // MÉTODOS AUXILIARES BANCÁRIOS
    // ========================================

    private void handleBankStatus(Player player, com.br.b12clans.models.Clan clan) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            double balance = plugin.getDatabaseManager().getClanBankBalance(clan.getId());
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                messages.sendMessage(player, "bank-status-header");
                messages.sendMessage(player, "bank-status-balance",
                        "%balance%", plugin.getEconomyManager().format(balance),
                        "%clan_name%", clan.getName());
                messages.sendMessage(player, "bank-status-footer");
            });
        });
    }

    private void handleBankDeposit(Player player, com.br.b12clans.models.Clan clan, String[] args) {
        if (args.length < 3) {
            messages.sendMessage(player, "bank-deposit-usage");
            return;
        }

        String amountStr = args[2].toLowerCase();
        double amount;

        if (amountStr.equals("all") || amountStr.equals("tudo")) {
            amount = plugin.getEconomyManager().getPlayerBalance(player);
            if (amount <= 0) {
                messages.sendMessage(player, "bank-no-money-to-deposit");
                return;
            }
        } else {
            try {
                amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    messages.sendMessage(player, "bank-invalid-amount");
                    return;
                }
            } catch (NumberFormatException e) {
                messages.sendMessage(player, "bank-invalid-number");
                return;
            }
        }

        if (!plugin.getEconomyManager().hasEnough(player, amount)) {
            messages.sendMessage(player, "bank-insufficient-funds",
                    "%amount%", plugin.getEconomyManager().format(amount));
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean economySuccess = plugin.getEconomyManager().withdrawPlayer(player, amount);
            if (!economySuccess) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    messages.sendMessage(player, "bank-economy-error");
                });
                return;
            }

            boolean bankSuccess = plugin.getDatabaseManager().depositToClanBank(clan.getId(), amount);
            if (!bankSuccess) {
                // Reverter transação da economia
                plugin.getEconomyManager().depositPlayer(player, amount);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    messages.sendMessage(player, "bank-database-error");
                });
                return;
            }

            double newBalance = plugin.getDatabaseManager().getClanBankBalance(clan.getId());
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                messages.sendMessage(player, "bank-deposit-success",
                        "%amount%", plugin.getEconomyManager().format(amount),
                        "%balance%", plugin.getEconomyManager().format(newBalance));
            });
        });
    }

    private void handleBankWithdraw(Player player, com.br.b12clans.models.Clan clan, String[] args) {
        // Verificar permissões
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String role = plugin.getDatabaseManager().getMemberRole(clan.getId(), player.getUniqueId());

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (role == null || !(role.equals("OWNER") || role.equals("VICE_LEADER") || role.equals("ADMIN"))) {
                    messages.sendMessage(player, "bank-withdraw-no-permission");
                    return;
                }

                if (args.length < 3) {
                    messages.sendMessage(player, "bank-withdraw-usage");
                    return;
                }

                String amountStr = args[2].toLowerCase();

                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    double currentBalance = plugin.getDatabaseManager().getClanBankBalance(clan.getId());
                    double amount;

                    if (amountStr.equals("all") || amountStr.equals("tudo")) {
                        amount = currentBalance;
                        if (amount <= 0) {
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                messages.sendMessage(player, "bank-no-money-to-withdraw");
                            });
                            return;
                        }
                    } else {
                        try {
                            amount = Double.parseDouble(amountStr);
                            if (amount <= 0) {
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    messages.sendMessage(player, "bank-invalid-amount");
                                });
                                return;
                            }
                        } catch (NumberFormatException e) {
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                messages.sendMessage(player, "bank-invalid-number");
                            });
                            return;
                        }
                    }

                    if (amount > currentBalance) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            messages.sendMessage(player, "bank-insufficient-clan-funds",
                                    "%amount%", plugin.getEconomyManager().format(amount),
                                    "%balance%", plugin.getEconomyManager().format(currentBalance));
                        });
                        return;
                    }

                    boolean bankSuccess = plugin.getDatabaseManager().withdrawFromClanBank(clan.getId(), amount);
                    if (!bankSuccess) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            messages.sendMessage(player, "bank-database-error");
                        });
                        return;
                    }

                    boolean economySuccess = plugin.getEconomyManager().depositPlayer(player, amount);
                    if (!economySuccess) {
                        // Reverter transação do banco
                        plugin.getDatabaseManager().depositToClanBank(clan.getId(), amount);
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            messages.sendMessage(player, "bank-economy-error");
                        });
                        return;
                    }

                    double newBalance = plugin.getDatabaseManager().getClanBankBalance(clan.getId());
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        messages.sendMessage(player, "bank-withdraw-success",
                                "%amount%", plugin.getEconomyManager().format(amount),
                                "%balance%", plugin.getEconomyManager().format(newBalance));
                    });
                });
            });
        });
    }

    private void sendHelp(Player player) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        String role = null;

        if (clan != null) {
            if (clan.getOwnerUuid().equals(player.getUniqueId())) {
                role = "OWNER";
            } else {
                // Buscar role do banco de forma síncrona para o help
                role = plugin.getDatabaseManager().getMemberRole(clan.getId(), player.getUniqueId());
            }
        }

        messages.sendMessage(player, "help-header");

        // Comandos básicos disponíveis para todos
        messages.sendMessage(player, "help-line-create");
        messages.sendMessage(player, "help-line-info");
        messages.sendMessage(player, "help-line-ver");

        if (clan != null) {
            // Comandos de membros
            messages.sendMessage(player, "help-section-member");
            messages.sendMessage(player, "help-line-home");
            messages.sendMessage(player, "help-line-description-view");
            messages.sendMessage(player, "help-line-kdr");
            messages.sendMessage(player, "help-line-bank-status");
            messages.sendMessage(player, "help-line-sair");

            // Comandos de vice-líder e admin
            if (role != null && (role.equals("VICE_LEADER") || role.equals("ADMIN") || role.equals("OWNER"))) {
                messages.sendMessage(player, "help-section-vice-leader");
                messages.sendMessage(player, "help-line-invite");
                messages.sendMessage(player, "help-line-expulsar");
                messages.sendMessage(player, "help-line-titulo");
                messages.sendMessage(player, "help-line-description-set");
                messages.sendMessage(player, "help-line-bank-deposit");
                messages.sendMessage(player, "help-line-bank-withdraw");

                if (plugin.getConfig().getBoolean("permissions.vice-leader-can-set-home", true)) {
                    messages.sendMessage(player, "help-line-home-set");
                }
                if (plugin.getConfig().getBoolean("permissions.vice-leader-can-manage-allies", true)) {
                    messages.sendMessage(player, "help-line-ally");
                }
                if (plugin.getConfig().getBoolean("permissions.vice-leader-can-manage-rivals", true)) {
                    messages.sendMessage(player, "help-line-rival");
                }
            }

            // Comandos de líder
            if (role != null && role.equals("OWNER")) {
                messages.sendMessage(player, "help-section-leader");
                messages.sendMessage(player, "help-line-promover");
                messages.sendMessage(player, "help-line-rebaixar");
                messages.sendMessage(player, "help-line-deletar");
                messages.sendMessage(player, "help-line-modtag");
                messages.sendMessage(player, "help-line-home-clear");
                messages.sendMessage(player, "help-line-fee");
                messages.sendMessage(player, "help-line-setbanner");
                messages.sendMessage(player, "help-line-ally");
                messages.sendMessage(player, "help-line-rival");
            }
        } else {
            messages.sendMessage(player, "help-line-accept");
            messages.sendMessage(player, "help-line-deny");
        }

        messages.sendMessage(player, "help-footer");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();

        Player player = (Player) sender;
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList(
                    "criar", "info", "ver", "convidar", "aceitar", "negar", "sair", "expulsar",
                    "deletar", "promover", "rebaixar", "titulo", "description", "home", "kdr",
                    "modtag", "ally", "rival", "fee", "setbanner", "bank", "banco"
            );
            for (String sub : subCommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
            return completions;
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            // Tab completion específico para cada comando
            switch (subCommand) {
                case "home":
                    if ("set".startsWith(args[1].toLowerCase())) completions.add("set");
                    if ("clear".startsWith(args[1].toLowerCase())) completions.add("clear");
                    break;
                case "ally":
                case "rival":
                    if ("add".startsWith(args[1].toLowerCase())) completions.add("add");
                    if ("remove".startsWith(args[1].toLowerCase())) completions.add("remove");
                    break;
                case "fee":
                    if ("set".startsWith(args[1].toLowerCase())) completions.add("set");
                    break;
                case "deletar":
                    if ("confirm".startsWith(args[1].toLowerCase())) completions.add("confirm");
                    break;
                case "bank":
                    if ("status".startsWith(args[1].toLowerCase())) completions.add("status");
                    if ("balance".startsWith(args[1].toLowerCase())) completions.add("balance");
                    if ("deposit".startsWith(args[1].toLowerCase())) completions.add("deposit");
                    if ("withdraw".startsWith(args[1].toLowerCase())) completions.add("withdraw");
                    break;
                case "banco":
                    if ("status".startsWith(args[1].toLowerCase())) completions.add("status");
                    if ("saldo".startsWith(args[1].toLowerCase())) completions.add("saldo");
                    if ("depositar".startsWith(args[1].toLowerCase())) completions.add("depositar");
                    if ("sacar".startsWith(args[1].toLowerCase())) completions.add("sacar");
                    if ("retirar".startsWith(args[1].toLowerCase())) completions.add("retirar");
                    break;
                default:
                    // Para comandos que precisam de nomes de jogadores
                    if (Arrays.asList("convidar", "expulsar", "promover", "rebaixar", "titulo").contains(subCommand)) {
                        String partialName = args[1].toLowerCase();
                        return Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .filter(name -> name.toLowerCase().startsWith(partialName))
                                .collect(Collectors.toList());
                    }
                    break;
            }
            return completions;
        }

        if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            String action = args[1].toLowerCase();

            if ((subCommand.equals("bank") && (action.equals("deposit") || action.equals("withdraw"))) ||
                    (subCommand.equals("banco") && (action.equals("depositar") || action.equals("sacar") || action.equals("retirar")))) {
                if ("all".startsWith(args[2].toLowerCase())) completions.add("all");
                if ("tudo".startsWith(args[2].toLowerCase())) completions.add("tudo");
            }
        }

        return Collections.emptyList();
    }
}

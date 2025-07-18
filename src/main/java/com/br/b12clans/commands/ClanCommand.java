package com.br.b12clans.commands;

import com.br.b12clans.Main;
import com.br.b12clans.database.ClanExistenceStatus;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.models.Clan;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.Bukkit;
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
            default:
                sendHelp(player);
                break;
        }

        return true;
    }

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
                                    messages.sendMessage(player, "clan-created-name", "%name%", name);
                                    messages.sendMessage(player, "clan-created-tag", "%tag%", clanManager.translateColors(tag));
                                    messages.sendMessage(player, "clan-created-leader", "%leader%", player.getName());
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
        messages.sendMessage(player, "info-name", "%name%", clan.getName());
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
                if (role == null || !(role.equals("OWNER") || role.equals("ADMIN"))) {
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
                if (kickerRole == null || !(kickerRole.equals("OWNER") || kickerRole.equals("ADMIN"))) {
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

                        if (kickerRole.equals("ADMIN") && targetRole.equals("ADMIN")) {
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
        if (!clan.getOwnerUuid().equals(player.getUniqueId())) {
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
                if (targetRole.equals("ADMIN")) {
                    messages.sendMessage(player, "player-already-that-role", "%player_name%", target.getName());
                    return;
                }
                if (targetRole.equals("OWNER")) {
                    messages.sendMessage(player, "cannot-manage-owner");
                    return;
                }
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean success = plugin.getDatabaseManager().updateMemberRole(clan.getId(), targetUuid, "ADMIN");
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (success) {
                            messages.sendMessage(player, "promote-success", "%player_name%", target.getName());
                            if (target.isOnline()) {
                                messages.sendMessage(target.getPlayer(), "got-promoted", "%clan_name%", clan.getName());
                            }
                        } else {
                            messages.sendMessage(player, "generic-error");
                        }
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
        if (!clan.getOwnerUuid().equals(player.getUniqueId())) {
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
                if (targetRole.equals("MEMBER")) {
                    messages.sendMessage(player, "player-already-that-role", "%player_name%", target.getName());
                    return;
                }
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean success = plugin.getDatabaseManager().updateMemberRole(clan.getId(), targetUuid, "MEMBER");
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (success) {
                            messages.sendMessage(player, "demote-success", "%player_name%", target.getName());
                            if (target.isOnline()) {
                                messages.sendMessage(target.getPlayer(), "got-demoted", "%clan_name%", clan.getName());
                            }
                        } else {
                            messages.sendMessage(player, "generic-error");
                        }
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
                if (role == null || !(role.equals("OWNER") || role.equals("ADMIN"))) {
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

    private void sendHelp(Player player) {
        messages.sendMessage(player, "help-header");
        messages.sendMessage(player, "help-line-create");
        messages.sendMessage(player, "help-line-info");
        messages.sendMessage(player, "help-line-ver");
        messages.sendMessage(player, "help-line-invite");
        messages.sendMessage(player, "help-line-accept");
        messages.sendMessage(player, "help-line-deny");
        messages.sendMessage(player, "help-line-sair");
        messages.sendMessage(player, "help-line-expulsar");
        messages.sendMessage(player, "help-line-deletar");
        messages.sendMessage(player, "help-line-promover");
        messages.sendMessage(player, "help-line-rebaixar");
        messages.sendMessage(player, "help-line-titulo");
        messages.sendMessage(player, "help-footer");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();

        Player player = (Player) sender;
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("criar", "info", "ver", "convidar", "aceitar", "negar", "sair", "expulsar", "deletar", "promover", "rebaixar", "titulo");
            for (String sub : subCommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
            return completions;
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (Arrays.asList("convidar", "expulsar", "promover", "rebaixar", "titulo").contains(subCommand)) {
                String partialName = args[1].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(partialName))
                        .collect(Collectors.toList());
            }

            if (Arrays.asList("aceitar", "negar").contains(subCommand)) {
                Integer invitedClanId = clanManager.getPendingInvite(player.getUniqueId());
                if (invitedClanId != null) {
                    Clan clan = plugin.getDatabaseManager().getClanById(invitedClanId);
                    if (clan != null) {
                        String cleanTag = clanManager.getCleanTag(clan.getTag());
                        if (cleanTag.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(cleanTag);
                        }
                    }
                }
                return completions;
            }

            if (subCommand.equals("deletar")) {
                if ("confirm".startsWith(args[1].toLowerCase())) {
                    completions.add("confirm");
                }
                return completions;
            }
        }
        return Collections.emptyList();
    }
}
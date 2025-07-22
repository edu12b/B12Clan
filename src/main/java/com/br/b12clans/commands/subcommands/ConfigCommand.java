// ARQUIVO: src/main/java/com/br/b12clans/commands/subcommands/ConfigCommand.java
package com.br.b12clans.commands.subcommands;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.managers.EconomyManager;
import com.br.b12clans.models.Clan;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ConfigCommand implements SubCommand {

    private final Main plugin;
    private final ClanManager clanManager;
    private final MessagesManager messages;
    private final EconomyManager economyManager;

    public ConfigCommand(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.messages = plugin.getMessagesManager();
        this.economyManager = plugin.getEconomyManager();
    }

    @Override
    public String getName() {
        return "config";
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

        if (!clan.getOwnerUuid().equals(player.getUniqueId())) {
            messages.sendMessage(player, "config-no-permission");
            return;
        }

        if (args.length < 1) {
            messages.sendMessage(player, "config-usage");
            return;
        }

        String action = args[0].toLowerCase();
        String[] actionArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (action) {
            case "tag":
                handleModTag(player, clan, actionArgs);
                break;
            case "fee":
                handleFee(player, clan, actionArgs);
                break;
            case "banner":
                handleSetBanner(player, clan, actionArgs);
                break;
            default:
                messages.sendMessage(player, "config-usage");
                break;
        }
    }

    // ##### MÉTODO CORRIGIDO #####
    private void handleModTag(Player player, Clan clan, String[] args) {
        if (args.length < 1) {
            messages.sendMessage(player, "modtag-usage");
            return;
        }
        String newTag = String.join(" ", args);
        if (!clanManager.isValidClanTag(newTag)) {
            messages.sendMessage(player, "invalid-tag-rules");
            return;
        }

        // Guarda a tag antiga antes de qualquer coisa
        final String oldTag = clan.getTag();

        plugin.getDatabaseManager().getClanByTagAsync(newTag)
                .thenComposeAsync(existingClan -> {
                    if (existingClan != null && existingClan.getId() != clan.getId()) {
                        return CompletableFuture.failedFuture(new IllegalAccessException("Tag já existe"));
                    }
                    return plugin.getDatabaseManager().updateClanTagAsync(clan.getId(), newTag);
                }, plugin.getThreadPool())
                .thenAccept(success -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if(success) {
                            clanManager.loadPlayerClan(player.getUniqueId());
                            // Agora passamos a oldTag e a newTag para a mensagem
                            messages.sendMessage(player, "modtag-success",
                                    "%old_tag%", clanManager.translateColors(oldTag),
                                    "%new_tag%", clanManager.translateColors(newTag));
                        } else {
                            messages.sendMessage(player, "generic-error");
                        }
                    });
                })
                .exceptionally(error -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (error.getCause() instanceof IllegalAccessException) {
                            messages.sendMessage(player, "tag-already-exists");
                        } else {
                            messages.sendMessage(player, "generic-error");
                        }
                    });
                    return null;
                });
    }

    private void handleFee(Player player, Clan clan, String[] args) {
        if (args.length < 1) {
            messages.sendMessage(player, "fee-usage");
            return;
        }
        try {
            double amount = Double.parseDouble(args[0]);
            if (amount < 0) {
                messages.sendMessage(player, "fee-invalid-amount");
                return;
            }
            plugin.getDatabaseManager().setClanFeeAsync(clan.getId(), amount)
                    .thenAccept(success -> {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            if (success) {
                                messages.sendMessage(player, "fee-set-success", "%amount%", economyManager.format(amount));
                            } else {
                                messages.sendMessage(player, "generic-error");
                            }
                        });
                    });
        } catch (NumberFormatException e) {
            messages.sendMessage(player, "fee-invalid-number");
        }
    }

    private void handleSetBanner(Player player, Clan clan, String[] args) {
        messages.sendMessage(player, "setbanner-not-implemented");
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("tag", "fee", "banner").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
package com.br.b12clans.commands.subcommands;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.managers.CommandManager;
import com.br.b12clans.managers.EconomyManager;
import com.br.b12clans.models.Clan;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayOutputStream;
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
    private final CommandManager commandManager; // <-- Dependência adicionada

    public ConfigCommand(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.messages = plugin.getMessagesManager();
        this.economyManager = plugin.getEconomyManager();
        this.commandManager = plugin.getCommandManager(); // <-- Inicialização
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

        // ##### LÓGICA ATUALIZADA AQUI #####
        // Compara a ação do jogador com as listas de aliases do commands.yml
        if (commandManager.getActionAliasesFor("config", "tag").contains(action)) {
            handleModTag(player, clan, actionArgs);
        } else if (commandManager.getActionAliasesFor("config", "fee").contains(action)) {
            handleFee(player, clan, actionArgs);
        } else if (commandManager.getActionAliasesFor("config", "banner").contains(action)) {
            handleSetBanner(player, clan);
        } else {
            messages.sendMessage(player, "config-usage");
        }
    }

    private void handleSetBanner(Player player, Clan clan) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.getType() == Material.AIR || !(itemInHand.getItemMeta() instanceof BannerMeta)) {
            messages.sendMessage(player, "setbanner-must-hold-banner");
            return;
        }

        String bannerData = itemStackToBase64(itemInHand);

        if (bannerData == null) {
            messages.sendMessage(player, "generic-error");
            return;
        }

        plugin.getDatabaseManager().updateClanBannerAsync(clan.getId(), bannerData)
                .thenAccept(success -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (success) {
                            messages.sendMessage(player, "setbanner-success");
                        } else {
                            messages.sendMessage(player, "generic-error");
                        }
                    });
                });
    }

    private String itemStackToBase64(ItemStack itemStack) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(itemStack);
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao serializar ItemStack para Base64: " + e.getMessage());
            return null;
        }
    }

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

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        if (args.length == 1) {
            List<String> allActions = commandManager.getActionAliasesFor("config", "tag");
            allActions.addAll(commandManager.getActionAliasesFor("config", "fee"));
            allActions.addAll(commandManager.getActionAliasesFor("config", "banner"));

            return allActions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
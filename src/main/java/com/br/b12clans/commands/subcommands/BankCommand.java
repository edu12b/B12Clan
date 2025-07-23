package com.br.b12clans.commands.subcommands;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.managers.CommandManager;
import com.br.b12clans.managers.EconomyManager;
import com.br.b12clans.models.Clan;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class BankCommand implements SubCommand {

    private final Main plugin;
    private final ClanManager clanManager;
    private final MessagesManager messages;
    private final EconomyManager economyManager;
    private final CommandManager commandManager; // <-- Dependência adicionada

    public BankCommand(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.messages = plugin.getMessagesManager();
        this.economyManager = plugin.getEconomyManager();
        this.commandManager = plugin.getCommandManager(); // <-- Inicialização
    }

    @Override
    public String getName() {
        return "bank";
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public void execute(Player player, String[] args) {
        if (!economyManager.isEnabled()) {
            messages.sendMessage(player, "economy-disabled");
            return;
        }

        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        if (args.length < 1) {
            messages.sendMessage(player, "bank-usage");
            return;
        }

        String action = args[0].toLowerCase();
        String[] actionArgs = Arrays.copyOfRange(args, 1, args.length);

        // ##### LÓGICA ATUALIZADA AQUI #####
        if (commandManager.getActionAliasesFor("bank", "deposit").contains(action)) {
            handleDeposit(player, clan, actionArgs);
        } else if (commandManager.getActionAliasesFor("bank", "withdraw").contains(action)) {
            handleWithdraw(player, clan, actionArgs);
        } else if (commandManager.getActionAliasesFor("bank", "balance").contains(action)) {
            handleBalance(player, clan);
        } else {
            messages.sendMessage(player, "bank-usage");
        }
    }

    private void handleDeposit(Player player, Clan clan, String[] args) {
        if (args.length < 1) {
            messages.sendMessage(player, "bank-deposit-usage");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            messages.sendMessage(player, "bank-invalid-number");
            return;
        }
        if (amount <= 0) {
            messages.sendMessage(player, "bank-invalid-amount");
            return;
        }
        if (!economyManager.hasEnough(player, amount)) {
            messages.sendMessage(player, "bank-insufficient-funds", "%amount%", economyManager.format(amount));
            return;
        }
        if (!economyManager.withdrawPlayer(player, amount)) {
            messages.sendMessage(player, "bank-economy-error");
            return;
        }
        plugin.getDatabaseManager().depositToClanBankAsync(clan.getId(), amount)
                .thenAccept(success -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (success) {
                            messages.sendMessage(player, "bank-deposit-success", "%amount%", economyManager.format(amount));
                        } else {
                            economyManager.depositPlayer(player, amount);
                            messages.sendMessage(player, "bank-database-error");
                        }
                    });
                })
                .exceptionally(error -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        economyManager.depositPlayer(player, amount);
                        messages.sendMessage(player, "bank-database-error");
                    });
                    plugin.getLogger().warning("Erro ao depositar no banco: " + error.getMessage());
                    return null;
                });
    }

    private void handleWithdraw(Player player, Clan clan, String[] args) {
        if (args.length < 1) {
            messages.sendMessage(player, "bank-withdraw-usage");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            messages.sendMessage(player, "bank-invalid-number");
            return;
        }
        if (amount <= 0) {
            messages.sendMessage(player, "bank-invalid-amount");
            return;
        }
        plugin.getDatabaseManager().getMemberRoleAsync(clan.getId(), player.getUniqueId())
                .thenComposeAsync(role -> {
                    if (role == null || !(role.equals("OWNER") || role.equals("VICE_LEADER") || role.equals("ADMIN"))) {
                        return CompletableFuture.failedFuture(new IllegalAccessException("Sem permissão"));
                    }
                    return plugin.getDatabaseManager().withdrawFromClanBankAsync(clan.getId(), amount);
                }, plugin.getThreadPool())
                .thenAccept(success -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (success) {
                            if(economyManager.depositPlayer(player, amount)) {
                                messages.sendMessage(player, "bank-withdraw-success", "%amount%", economyManager.format(amount));
                            } else {
                                plugin.getDatabaseManager().depositToClanBankAsync(clan.getId(), amount);
                                messages.sendMessage(player, "bank-economy-error");
                            }
                        } else {
                            messages.sendMessage(player, "bank-insufficient-clan-funds");
                        }
                    });
                })
                .exceptionally(error -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (error.getCause() instanceof IllegalAccessException) {
                            messages.sendMessage(player, "bank-withdraw-no-permission");
                        } else {
                            messages.sendMessage(player, "generic-error");
                            plugin.getLogger().warning("Erro ao sacar do banco: " + error.getMessage());
                        }
                    });
                    return null;
                });
    }

    private void handleBalance(Player player, Clan clan) {
        plugin.getDatabaseManager().getClanBankBalanceAsync(clan.getId())
                .thenAccept(balance -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        messages.sendMessage(player, "bank-status-balance", "%balance%", economyManager.format(balance));
                    });
                })
                .exceptionally(error -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> messages.sendMessage(player, "generic-error"));
                    plugin.getLogger().warning("Erro ao buscar saldo do banco: " + error.getMessage());
                    return null;
                });
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        if (args.length == 1) {
            List<String> allActions = commandManager.getActionAliasesFor("bank", "deposit");
            allActions.addAll(commandManager.getActionAliasesFor("bank", "withdraw"));
            allActions.addAll(commandManager.getActionAliasesFor("bank", "balance"));

            return allActions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
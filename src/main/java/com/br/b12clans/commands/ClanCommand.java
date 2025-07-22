// ARQUIVO: src/main/java/com/br/b12clans/commands/ClanCommand.java
package com.br.b12clans.commands;

import com.br.b12clans.Main;
import com.br.b12clans.commands.subcommands.*;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.stream.Collectors;

public class ClanCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final MessagesManager messages;
    private final Map<String, SubCommand> subCommands = new HashMap<>();
    private final CargoCommand cargoCommand;
    private final RelacionamentoCommand relacionamentoCommand;
    private final BankCommand bankCommand;
    private final ConfigCommand configCommand;

    public ClanCommand(Main plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessagesManager();
        this.cargoCommand = new CargoCommand(plugin);
        this.relacionamentoCommand = new RelacionamentoCommand(plugin);
        this.bankCommand = new BankCommand(plugin);
        this.configCommand = new ConfigCommand(plugin);
        registerSubCommands();
    }

    private void registerSubCommands() {
        subCommands.put("criar", new CriarCommand(plugin));
        subCommands.put("info", new InfoCommand(plugin));
        subCommands.put("convite", new ConviteCommand(plugin));
        subCommands.put("sair", new SairCommand(plugin));
        subCommands.put("expulsar", new ExpulsarCommand(plugin));
        subCommands.put("promover", cargoCommand);
        subCommands.put("rebaixar", cargoCommand);
        subCommands.put("deletar", new DeletarCommand(plugin));
        subCommands.put("titulo", new TituloCommand(plugin));
        subCommands.put("description", new DescriptionCommand(plugin));
        subCommands.put("ally", relacionamentoCommand);
        subCommands.put("rival", relacionamentoCommand);
        subCommands.put("bank", bankCommand);
        subCommands.put("banco", bankCommand);
        subCommands.put("config", configCommand);
        subCommands.put("home", new HomeCommand(plugin));
        subCommands.put("ver", new VerCommand(plugin));
        subCommands.put("kdr", new KDRCommand(plugin)); // <-- NOSSO ÚLTIMO COMANDO!

        // A refatoração está completa!
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            messages.sendMessage(sender, "player-only");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            messages.sendMessage(player, "help-header");
            return true;
        }
        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);
        if (subCommand == null) {
            messages.sendMessage(player, "help-header");
            return true;
        }
        String permission = subCommand.getPermission();
        if (permission != null && !player.hasPermission(permission)) {
            messages.sendMessage(player, "no-permission");
            return true;
        }
        String[] subCommandArgs = Arrays.copyOfRange(args, 1, args.length);
        if (subCommand instanceof CargoCommand) {
            boolean isPromoting = subCommandName.equals("promover");
            ((CargoCommand) subCommand).handleCommand(player, subCommandArgs, isPromoting);
        } else if (subCommand instanceof RelacionamentoCommand) {
            boolean isAlly = subCommandName.equals("ally");
            ((RelacionamentoCommand) subCommand).handleCommand(player, subCommandArgs, isAlly);
        } else {
            subCommand.execute(player, subCommandArgs);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }
        Player player = (Player) sender;
        if (args.length == 1) {
            return subCommands.keySet().stream()
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
                    .filter(name -> {
                        SubCommand sub = subCommands.get(name);
                        return sub.getPermission() == null || player.hasPermission(sub.getPermission());
                    })
                    .collect(Collectors.toList());
        }
        SubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand != null) {
            if (subCommand.getPermission() == null || player.hasPermission(subCommand.getPermission())) {
                String[] subCommandArgs = Arrays.copyOfRange(args, 1, args.length);
                return subCommand.onTabComplete(player, subCommandArgs);
            }
        }
        return Collections.emptyList();
    }
}
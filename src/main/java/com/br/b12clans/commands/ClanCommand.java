package com.br.b12clans.commands;

import com.br.b12clans.Main;
import com.br.b12clans.commands.subcommands.*;
import com.br.b12clans.managers.CommandManager;
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
    private final CommandManager commandManager;
    private final Map<String, SubCommand> subCommands = new HashMap<>();
    private final AjudaCommand ajudaCommand;

    public ClanCommand(Main plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessagesManager();
        this.commandManager = plugin.getCommandManager();
        this.ajudaCommand = new AjudaCommand(plugin);
        registerSubCommands();
    }

    private void registerSubCommands() {
        // 1. Mapeia o nome interno (chave do yml) para a sua classe de comando
        Map<String, SubCommand> commandMap = new HashMap<>();
        CargoCommand cargoCmd = new CargoCommand(plugin); // Instancia uma vez para reutilizar

        commandMap.put("create", new CriarCommand(plugin));
        commandMap.put("info", new InfoCommand(plugin));
        commandMap.put("help", ajudaCommand);
        commandMap.put("invite", new ConviteCommand(plugin));
        commandMap.put("leave", new SairCommand(plugin));
        commandMap.put("kick", new ExpulsarCommand(plugin));
        commandMap.put("promote", cargoCmd);
        commandMap.put("demote", cargoCmd);
        commandMap.put("title", new TituloCommand(plugin));
        commandMap.put("config", new ConfigCommand(plugin));
        commandMap.put("description", new DescriptionCommand(plugin));
        commandMap.put("home", new HomeCommand(plugin));
        commandMap.put("ally", new AllyCommand(plugin));
        commandMap.put("rival", new RivalCommand(plugin));
        commandMap.put("view", new VerCommand(plugin));
        commandMap.put("kdr", new KDRCommand(plugin));
        commandMap.put("delete", new DeletarCommand(plugin));
        commandMap.put("bank", new BankCommand(plugin));
        commandMap.put("friendlyfire", new FriendlyFireCommand(plugin));
        commandMap.put("relations", new RelationsCommand(plugin));
        commandMap.put("toggleinvite", new ToggleInviteCommand(plugin));
        commandMap.put("togglealliance", new ToggleAllianceCommand(plugin));


        // 2. Pega as chaves dos comandos do commands.yml (create, info, ally, etc.)
        Set<String> commandKeys = commandManager.getCommandKeys();
        if (commandKeys.isEmpty()) {
            plugin.getLogger().severe("A seção 'main-commands:' não foi encontrada ou está vazia no commands.yml!");
            plugin.getLogger().severe("Verifique se o arquivo existe e não tem erros de indentação.");
            return;
        }

        // 3. Para cada chave, pega seus aliases e registra no mapa principal que o servidor usa
        for (String commandKey : commandKeys) {
            SubCommand subCommand = commandMap.get(commandKey);
            if (subCommand != null) {
                List<String> aliases = commandManager.getAliasesFor(commandKey);
                for (String alias : aliases) {
                    subCommands.put(alias.toLowerCase(), subCommand);
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            messages.sendMessage(sender, "player-only");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            ajudaCommand.execute(player, new String[0]);
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            ajudaCommand.execute(player, new String[0]);
            return true;
        }

        String permission = subCommand.getPermission();
        if (permission != null && !player.hasPermission(permission)) {
            messages.sendMessage(player, "no-permission");
            return true;
        }
        String[] subCommandArgs = Arrays.copyOfRange(args, 1, args.length);

        // Lógica especial para CargoCommand, corrigida para funcionar com aliases
        if (subCommand instanceof CargoCommand) {
            // Verifica se o alias digitado pertence à lista de aliases de "promote"
            boolean isPromoting = commandManager.getAliasesFor("promote").contains(subCommandName);
            ((CargoCommand) subCommand).handleCommand(player, subCommandArgs, isPromoting);
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
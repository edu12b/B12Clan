// ARQUIVO: src/main/java/com/br/b12clans/commands/subcommands/VerCommand.java
package com.br.b12clans.commands.subcommands;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VerCommand implements SubCommand {

    private final Main plugin;
    private final ClanManager clanManager;
    private final MessagesManager messages;

    public VerCommand(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.messages = plugin.getMessagesManager();
    }

    @Override
    public String getName() {
        return "ver";
    }

    @Override
    public String getPermission() {
        // Este comando geralmente não precisa de permissão, é uma ferramenta de ajuda.
        return null;
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 1) {
            messages.sendMessage(player, "view-tag-usage");
            return;
        }

        String tagToView = String.join(" ", args);
        String coloredTag = clanManager.translateColors(tagToView);

        messages.sendMessage(player, "view-tag-result", "%tag%", coloredTag);
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        // Este comando não tem um tab-complete útil.
        return Collections.emptyList();
    }
}
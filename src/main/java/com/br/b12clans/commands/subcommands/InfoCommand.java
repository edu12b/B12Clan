// ARQUIVO: src/main/java/com/br/b12clans/commands/subcommands/InfoCommand.java

package com.br.b12clans.commands.subcommands;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.models.Clan;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class InfoCommand implements SubCommand {

    // Dependências que a classe precisa para funcionar
    private final ClanManager clanManager;
    private final MessagesManager messages;

    // Construtor que recebe o plugin principal para pegar as dependências
    public InfoCommand(Main plugin) {
        this.clanManager = plugin.getClanManager();
        this.messages = plugin.getMessagesManager();
    }

    @Override
    public String getName() {
        return "info"; // O nome do nosso subcomando
    }

    @Override
    public String getPermission() {
        // O comando /clan info não precisa de uma permissão especial além da principal (b12clans.use)
        // Então retornamos null. A verificação da permissão principal já foi feita na ClanCommand.
        return null;
    }

    @Override
    public void execute(Player player, String[] args) {
        // Toda a lógica que estava no método "handleInfoCommand" foi movida para cá.

        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            messages.sendMessage(player, "no-clan");
            return;
        }

        String cleanTag = clanManager.getCleanTag(clan.getTag());
        String expandedTag = clanManager.translateColors(clan.getTag());

        // Envia as mensagens de informação para o jogador
        messages.sendMessage(player, "info-header");
        messages.sendMessage(player, "info-name", "%name%", clanManager.formatDisplayName(clan.getName()));
        messages.sendMessage(player, "info-tag", "%tag%", expandedTag);
        messages.sendMessage(player, "info-clean-tag", "%clean_tag%", cleanTag);
        messages.sendMessage(player, "info-expanded-size", "%length%", String.valueOf(expandedTag.length()));
        messages.sendMessage(player, "info-created-at", "%date%", clan.getCreatedAt().toString());
        messages.sendMessage(player, "info-footer");
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        // O comando /clan info não tem argumentos, então retornamos uma lista vazia.
        return Collections.emptyList();
    }
}
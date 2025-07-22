// ARQUIVO: src/main/java/com/br/b12clans/commands/subcommands/SubCommand.java
package com.br.b12clans.commands.subcommands;

import org.bukkit.entity.Player;

import java.util.List;

public interface SubCommand {

    /**
     * Executa a lógica do subcomando.
     * @param player O jogador que executou o comando.
     * @param args Os argumentos passados após o nome do subcomando.
     */
    void execute(Player player, String[] args);

    /**
     * Retorna o nome do subcomando.
     * @return O nome (ex: "criar", "info").
     */
    String getName();

    /**
     * Retorna a permissão necessária para executar este subcomando.
     * @return A string da permissão, ou null se não houver.
     */
    String getPermission();

    /**
     * Fornece sugestões de tab-completion para este subcomando.
     * @param player O jogador que está usando o tab.
     * @param args Os argumentos atuais.
     * @return Uma lista de sugestões.
     */
    List<String> onTabComplete(Player player, String[] args);
}
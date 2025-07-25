package com.br.b12clans.utils; // ou o pacote que preferir

import com.br.b12clans.Main;
import org.bukkit.entity.Player;

public class AsyncHandler {

    private final Main plugin;
    private final MessagesManager messages;

    public AsyncHandler(Main plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessagesManager();
    }

    /**
     * Lida com exceções de CompletableFuture de forma padronizada.
     * Envia uma mensagem de erro para o jogador e registra no console.
     * @param player O jogador que executou o comando.
     * @param throwable O erro capturado pelo .exceptionally().
     * @param defaultMessageKey A chave da mensagem padrão em lang.yml caso o erro não tenha uma mensagem específica.
     */
    public void handleException(Player player, Throwable throwable, String defaultMessageKey) {
        // Volta para a thread principal para enviar a mensagem
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            String messageKey = defaultMessageKey;
            // Tenta obter uma mensagem de erro mais específica da causa da exceção
            if (throwable.getCause() != null && throwable.getCause().getMessage() != null) {
                // Verifica se a mensagem da causa é uma chave válida em lang.yml
                // (Isso é opcional, mas uma boa prática para não enviar mensagens de debug ao jogador)
                // Por simplicidade aqui, vamos apenas usar a mensagem da causa se ela existir.
                messageKey = throwable.getCause().getMessage();
            }

            // Envia a mensagem para o jogador
            messages.sendMessage(player, messageKey);

            // Loga o erro detalhado no console para o admin do servidor
            plugin.getLogger().warning("Erro em operação assíncrona para " + player.getName() + " (" + player.getUniqueId() + "): " + throwable.getMessage());
            // Se quiser o stack trace completo para debug, descomente a linha abaixo
            // throwable.printStackTrace();
        });
    }
}
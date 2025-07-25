package com.br.b12clans.listeners;

import com.br.b12clans.Main;
import com.br.b12clans.managers.ClanManager;
import com.br.b12clans.models.Clan;
import com.br.b12clans.utils.MessagesManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.List;
import java.util.logging.Level;

public class FriendlyFireListener implements Listener {

    private final Main plugin;
    private final ClanManager clanManager;
    private final MessagesManager messages;

    public FriendlyFireListener(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager(); // <-- Esta linha provavelmente está faltando ou incorreta
        this.messages = plugin.getMessagesManager();
    }

    private void debug(String message) {
        plugin.getLogger().log(Level.INFO, "[FF DEBUG] " + message);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        debug("Evento de dano detectado. Atacante: " + attacker.getName() + ", Vítima: " + victim.getName());

        Clan victimClan = clanManager.getPlayerClan(victim.getUniqueId());
        Clan attackerClan = clanManager.getPlayerClan(attacker.getUniqueId());

        if (victimClan == null || attackerClan == null) {
            debug("Um dos jogadores não está em um clã. Ignorando.");
            return;
        }

        debug("Atacante está no clã '" + attackerClan.getName() + "', Vítima está no clã '" + victimClan.getName() + "'.");

        // --- Verificação de dano no mesmo clã ---
        if (plugin.getConfig().getBoolean("friendly-fire.prevent-clan-damage", true) && attackerClan.getId() == victimClan.getId()) {
            debug("Verificando dano no mesmo clã...");
            Boolean isDisabled = clanManager.isFriendlyFireDisabledFromCache(attackerClan.getId());
            debug("Status do FF do clã no cache: " + isDisabled);

            if (isDisabled != null && isDisabled) {
                debug("CANCELANDO o dano. Motivo: Mesmo clã com FF desativado.");
                event.setCancelled(true);
                messages.sendMessage(attacker, "ff-action-blocked");
            }
            return;
        }

        // --- Verificação de dano em clãs aliados ---
        if (plugin.getConfig().getBoolean("friendly-fire.prevent-ally-damage", true)) {
            debug("Verificando dano entre aliados...");
            Boolean attackerFF = clanManager.isFriendlyFireDisabledFromCache(attackerClan.getId());
            Boolean victimFF = clanManager.isFriendlyFireDisabledFromCache(victimClan.getId());
            List<Integer> attackerAllies = clanManager.getClanAlliesFromCache(attackerClan.getId());

            debug("Status FF do Atacante (cache): " + attackerFF);
            debug("Status FF da Vítima (cache): " + victimFF);
            debug("Lista de aliados do Atacante (cache): " + attackerAllies);

            if (attackerFF == null || victimFF == null || attackerAllies == null) {
                debug("Uma das informações necessárias não está no cache. Ignorando o ataque desta vez.");
                return;
            }

            boolean areAllies = attackerAllies.contains(victimClan.getId());
            debug("São aliados? " + areAllies);

            boolean protectionActive = attackerFF || victimFF;
            debug("Proteção de aliança está ativa (um dos clãs desativou o FF)? " + protectionActive);

            if (protectionActive && areAllies) {
                debug("CANCELANDO o dano. Motivo: São aliados e a proteção está ativa.");
                event.setCancelled(true);
                messages.sendMessage(attacker, "ff-action-blocked");
            } else {
                debug("Dano PERMITIDO. Condições não foram atendidas.");
            }
        }
    }
}
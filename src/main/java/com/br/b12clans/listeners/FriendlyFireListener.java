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

public class FriendlyFireListener implements Listener {

    private final Main plugin;
    private final ClanManager clanManager;
    private final MessagesManager messages;

    public FriendlyFireListener(Main plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.messages = plugin.getMessagesManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        Clan victimClan = clanManager.getPlayerClan(victim.getUniqueId());
        Clan attackerClan = clanManager.getPlayerClan(attacker.getUniqueId());

        if (victimClan == null || attackerClan == null) {
            return;
        }

        // --- Verificação de dano no mesmo clã ---
        if (plugin.getConfig().getBoolean("friendly-fire.prevent-clan-damage", true) && attackerClan.getId() == victimClan.getId()) {
            Boolean isDisabled = clanManager.isFriendlyFireDisabledFromCache(attackerClan.getId());

            if (isDisabled == null) {
                // Se não está no cache, manda carregar em segundo plano para a próxima vez.
                clanManager.isFriendlyFireDisabled(attackerClan.getId());
                return; // O primeiro ataque passa, os próximos serão bloqueados.
            }

            if (isDisabled) {
                event.setCancelled(true);
                messages.sendMessage(attacker, "ff-action-blocked");
            }
            return;
        }

        // --- Verificação de dano em clãs aliados ---
        if (plugin.getConfig().getBoolean("friendly-fire.prevent-ally-damage", true)) {
            Boolean attackerFF = clanManager.isFriendlyFireDisabledFromCache(attackerClan.getId());
            Boolean victimFF = clanManager.isFriendlyFireDisabledFromCache(victimClan.getId());
            List<Integer> attackerAllies = clanManager.getClanAlliesFromCache(attackerClan.getId());

            // Se qualquer uma das informações não estiver no cache, carrega em segundo plano e permite o ataque desta vez.
            if (attackerFF == null) clanManager.isFriendlyFireDisabled(attackerClan.getId());
            if (victimFF == null) clanManager.isFriendlyFireDisabled(victimClan.getId());
            if (attackerAllies == null) clanManager.getClanAlliesAsync(attackerClan.getId());

            if (attackerFF == null || victimFF == null || attackerAllies == null) {
                return; // Permite o primeiro ataque enquanto os dados são carregados.
            }

            // Se ambos os clãs desativaram o FF e são aliados, cancela o dano.
            if (attackerFF && victimFF && attackerAllies.contains(victimClan.getId())) {
                event.setCancelled(true);
                messages.sendMessage(attacker, "ff-action-blocked");
            }
        }
    }
}
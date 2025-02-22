package ru.areshkin.realm;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.palmergames.bukkit.towny.TownyAPI;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RealmHidetag extends JavaPlugin implements Listener {

    private ProtocolManager protocolManager;
    private Map<UUID, Long> temporaryVisible = new HashMap<>();
    private Map<UUID, Boolean> permanentlyVisible = new HashMap<>();
    private int visibilityTime;

    @Override
    public void onEnable() {
        // Инициализация ProtocolLib
        protocolManager = ProtocolLibrary.getProtocolManager();

        // Регистрация событий
        getServer().getPluginManager().registerEvents(this, this);

        // Загрузка конфига
        saveDefaultConfig();
        loadConfigValues();

        // Скрытие ников для всех игроков при старте
        getServer().getOnlinePlayers().forEach(this::hidePlayerNametag);
    }

    private void loadConfigValues() {
        FileConfiguration config = getConfig();
        visibilityTime = config.getInt("visibility-time", 60); // 60 секунд по умолчанию
    }

    private void hidePlayerNametag(Player player) {
        if (!permanentlyVisible.containsKey(player.getUniqueId()) || !permanentlyVisible.get(player.getUniqueId())) {
            player.setCustomNameVisible(false);
            String townName = TownyAPI.getInstance().getTownName(player);
            String placeholder = townName != null ? "[" + townName + "] " + player.getName() : player.getName();
            player.setCustomName(PlaceholderAPI.setPlaceholders(player, placeholder));
        }
    }

    private void showPlayerNametag(Player player, boolean permanent) {
        player.setCustomNameVisible(true);
        if (permanent) {
            permanentlyVisible.put(player.getUniqueId(), true);
        } else {
            temporaryVisible.put(player.getUniqueId(), System.currentTimeMillis() + visibilityTime * 1000);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player target)) return;

        Player player = event.getPlayer();
        boolean holdingPaper = player.getInventory().getItemInMainHand().getType() == Material.PAPER;

        if (holdingPaper) {
            showPlayerNametag(target, true);
        } else {
            showPlayerNametag(target, false);
        }
    }

    @Override
    public void onDisable() {
        temporaryVisible.clear();
        permanentlyVisible.clear();
    }

    public void updateNametags() {
        long currentTime = System.currentTimeMillis();
        temporaryVisible.entrySet().removeIf(entry -> {
            if (currentTime > entry.getValue()) {
                Player p = getServer().getPlayer(entry.getKey());
                if (p != null) hidePlayerNametag(p);
                return true;
            }
            return false;
        });
    }
}
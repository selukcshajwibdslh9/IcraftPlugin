package org.icraftplugins.icraft2plugins;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public interface PhoneApp {
    String getTitle();
    void open(Player player);
    void handleClick(InventoryClickEvent event);
}
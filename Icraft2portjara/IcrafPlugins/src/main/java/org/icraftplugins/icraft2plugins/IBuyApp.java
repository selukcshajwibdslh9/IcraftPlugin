package org.icraftplugins.icraft2plugins;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IBuyApp {

    private final IcraftPlugins plugin;
    private final EconomyManager economyManager;
    private final String guiTitle = ChatColor.DARK_GREEN + "Магазин iBuy";

    public IBuyApp(IcraftPlugins plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
    }

    public String getTitle() {
        return guiTitle;
    }

    public void open(Player player) {
        int guiSize = plugin.getConfig().getInt("ibuy.gui_size", 27);
        if (guiSize < 9 || guiSize > 54 || guiSize % 9 != 0) {
            guiSize = 27;
        }

        Inventory inv = Bukkit.createInventory(null, guiSize, guiTitle);

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("ibuy.items");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                int slot = section.getInt(key + ".slot");
                if (slot < 0 || slot >= guiSize) continue;

                String name = ChatColor.translateAlternateColorCodes('&', section.getString(key + ".name", "Набор"));
                Material iconMat = Material.matchMaterial(section.getString(key + ".icon", "CHEST"));
                if (iconMat == null) iconMat = Material.CHEST;

                double price = section.getDouble(key + ".price", 10.0);
                String loreHeader = ChatColor.translateAlternateColorCodes('&', section.getString(key + ".lore_header", "&7Содержимое:"));

                ItemStack iconItem = new ItemStack(iconMat, 1);
                ItemMeta meta = iconItem.getItemMeta();

                if (meta != null) {
                    meta.setDisplayName(name);
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + "Цена: " + ChatColor.YELLOW + price + " монет");
                    lore.add(loreHeader);

                    List<Map<?, ?>> itemsList = section.getMapList(key + ".items");
                    for (Map<?, ?> itemData : itemsList) {
                        String matStr = (String) itemData.get("material");

                        // Безопасное получение числа из конфига через Number
                        int amount = 1;
                        if (itemData.containsKey("amount") && itemData.get("amount") instanceof Number) {
                            amount = ((Number) itemData.get("amount")).intValue();
                        }

                        String customName = itemData.containsKey("name") ? (String) itemData.get("name") : null;

                        Material mat = Material.matchMaterial(matStr != null ? matStr : "DIRT");
                        String itemNameToShow = (customName != null)
                                ? ChatColor.translateAlternateColorCodes('&', customName)
                                : (mat != null ? mat.name() : "Предмет");

                        lore.add(ChatColor.DARK_GRAY + " - " + ChatColor.WHITE + itemNameToShow + ChatColor.GRAY + " x" + amount);
                    }

                    lore.add("");
                    lore.add(ChatColor.GREEN + "Нажмите, чтобы заказать доставку!");
                    meta.setLore(lore);
                    iconItem.setItemMeta(meta);
                }

                inv.setItem(slot, iconItem);
            }
        }

        player.openInventory(inv);
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) {
            return;
        }

        String itemName = clicked.getItemMeta().getDisplayName();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("ibuy.items");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String name = ChatColor.translateAlternateColorCodes('&', section.getString(key + ".name", ""));
            if (name.equals(itemName)) {
                double price = section.getDouble(key + ".price", 0.0);

                if (!economyManager.hasMoney(player, price)) {
                    player.sendMessage(ChatColor.RED + "У вас недостаточно денег для покупки этого набора!");
                    player.closeInventory();
                    return;
                }

                List<ItemStack> itemsToDeliver = new ArrayList<>();
                List<Map<?, ?>> itemsList = section.getMapList(key + ".items");

                for (Map<?, ?> itemData : itemsList) {
                    String matStr = (String) itemData.get("material");

                    // Безопасное получение числа из конфига через Number
                    int amount = 1;
                    if (itemData.containsKey("amount") && itemData.get("amount") instanceof Number) {
                        amount = ((Number) itemData.get("amount")).intValue();
                    }

                    String customName = itemData.containsKey("name") ? (String) itemData.get("name") : null;

                    Material mat = Material.matchMaterial(matStr != null ? matStr : "DIRT");
                    if (mat == null) mat = Material.DIRT;

                    ItemStack itemStack = new ItemStack(mat, amount);
                    if (customName != null) {
                        ItemMeta m = itemStack.getItemMeta();
                        if (m != null) {
                            m.setDisplayName(ChatColor.translateAlternateColorCodes('&', customName));
                            itemStack.setItemMeta(m);
                        }
                    }
                    itemsToDeliver.add(itemStack);
                }

                economyManager.withdrawMoney(player, price);
                player.closeInventory();

                ParachuteDelivery.startDelivery(plugin, player, itemsToDeliver);
                break;
            }
        }
    }
}
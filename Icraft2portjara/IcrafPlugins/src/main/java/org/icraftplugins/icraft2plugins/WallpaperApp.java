package org.icraftplugins.icraft2plugins;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class WallpaperApp {

    private final IcraftPlugins plugin;
    private final String guiTitle = ChatColor.LIGHT_PURPLE + "Каталог обоев";
    private final Map<UUID, WallpaperData> playerWallpapers = new HashMap<>();

    private final String[] colorNames = {
            "Белое стекло", "Оранжевое стекло", "Пурпурное стекло", "Голубое стекло",
            "Желтое стекло", "Салатовое стекло", "Розовое стекло", "Коричневое стекло",
            "Светло-серое стекло", "Бирюзовое стекло", "Фиолетовое стекло", "Синее стекло",
            "Зеленое стекло", "Красное стекло", "Черное стекло", "Стандартное стекло", "Дополнительное стекло"
    };

    public WallpaperApp(IcraftPlugins plugin) {
        this.plugin = plugin;
    }

    public String getTitle() {
        return guiTitle;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, guiTitle);

        for (int i = 0; i < 17; i++) {
            ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) i);
            ItemMeta meta = glass.getItemMeta();
            meta.setDisplayName(ChatColor.WHITE + colorNames[i]);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Нажмите, чтобы установить этот цвет");
            meta.setLore(lore);
            glass.setItemMeta(meta);
            inv.setItem(i, glass);
        }

        ItemStack rainbowBtn = new ItemStack(Material.MAGMA_CREAM, 1);
        ItemMeta rMeta = rainbowBtn.getItemMeta();
        rMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Переливающиеся обои (Анимация)");
        List<String> rLore = new ArrayList<>();
        rLore.add(ChatColor.GRAY + "Нажмите для запуска смены цветов");
        rMeta.setLore(rLore);
        rainbowBtn.setItemMeta(rMeta);
        inv.setItem(28, rainbowBtn);

        ItemStack resetBtn = new ItemStack(Material.MILK_BUCKET, 1);
        ItemMeta resetMeta = resetBtn.getItemMeta();
        resetMeta.setDisplayName(ChatColor.RED + "Очистить обои");
        List<String> resetLore = new ArrayList<>();
        resetLore.add(ChatColor.GRAY + "Нажмите для сброса до стандартных");
        resetMeta.setLore(resetLore);
        resetBtn.setItemMeta(resetMeta);
        inv.setItem(30, resetBtn);

        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta bMeta = back.getItemMeta();
        bMeta.setDisplayName(ChatColor.RED + "Назад в меню");
        back.setItemMeta(bMeta);
        inv.setItem(35, back);

        player.openInventory(inv);
    }

    public void handleClose(Player player) {
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot == 35) {
            player.closeInventory();
            plugin.openPhoneMenu(player);
            return;
        }

        if (slot >= 0 && slot < 17) {
            playerWallpapers.put(player.getUniqueId(), new WallpaperData(false, (short) slot));
            player.sendMessage(ChatColor.GREEN + "[Обои] Установлены обои: " + colorNames[slot]);
            player.closeInventory();
            plugin.openPhoneMenu(player);
        } else if (slot == 28) {
            playerWallpapers.put(player.getUniqueId(), new WallpaperData(true, (short) 0));
            player.sendMessage(ChatColor.LIGHT_PURPLE + "[Обои] Включены переливающиеся обои!");
            player.closeInventory();
            plugin.openPhoneMenu(player);
        } else if (slot == 30) {
            playerWallpapers.remove(player.getUniqueId());
            player.sendMessage(ChatColor.YELLOW + "[Обои] Обои очищены и сброшены!");
            player.closeInventory();
            plugin.openPhoneMenu(player);
        }
    }

    public ItemStack getWallpaperItem(Player player, int slotIndex) {
        WallpaperData data = playerWallpapers.get(player.getUniqueId());

        if (data == null) {
            ItemStack def = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
            ItemMeta m = def.getItemMeta();
            m.setDisplayName(" ");
            def.setItemMeta(m);
            return def;
        }

        if (data.isRainbow) {
            short[] rainbowColors = {1, 4, 5, 3, 6, 10, 2, 14, 11, 13};
            int colorIndex = (int)((System.currentTimeMillis() / 250 + slotIndex) % rainbowColors.length);
            ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, rainbowColors[colorIndex]);
            ItemMeta m = item.getItemMeta();
            m.setDisplayName(ChatColor.LIGHT_PURPLE + "Анимированные обои");
            item.setItemMeta(m);
            return item;
        } else {
            ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, data.colorData);
            ItemMeta m = item.getItemMeta();
            m.setDisplayName(ChatColor.WHITE + "Обои");
            item.setItemMeta(m);
            return item;
        }
    }

    private static class WallpaperData {
        boolean isRainbow;
        short colorData;

        public WallpaperData(boolean isRainbow, short colorData) {
            this.isRainbow = isRainbow;
            this.colorData = colorData;
        }
    }
}
package org.icraftplugins.icraft2plugins;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ClockApp {

    private final IcraftPlugins plugin;
    private final String guiTitle = ChatColor.DARK_AQUA + "Часы и Время";

    public ClockApp(IcraftPlugins plugin) {
        this.plugin = plugin;
    }

    public String getTitle() {
        return guiTitle;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, guiTitle);

        // 0. Кнопка возврата в главное меню телефона
        ItemStack backItem = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.RED + "<< Назад в меню");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Вернуться в главное меню телефона");
            backMeta.setLore(lore);
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backItem);

        ItemStack clockItem = getItemStack(player);
        inv.setItem(11, clockItem);

        // 2. Иконка реального времени (Слот 15)
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        SimpleDateFormat dateFmt = new SimpleDateFormat("dd.MM.yyyy");
        String realTime = sdf.format(new Date());
        String realDate = dateFmt.format(new Date());

        ItemStack realClockItem = new ItemStack(Material.COMPASS, 1);
        ItemMeta realMeta = realClockItem.getItemMeta();
        if (realMeta != null) {
            realMeta.setDisplayName(ChatColor.AQUA + "Реальное время: " + realTime);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Дата: " + realDate);
            lore.add(ChatColor.GRAY + "Ваш часовой пояс (ПК)");
            realMeta.setLore(lore);
            realClockItem.setItemMeta(realMeta);
        }
        inv.setItem(15, realClockItem);

        player.openInventory(inv);
    }

    private static ItemStack getItemStack(Player player) {
        World world = player.getWorld();
        long time = world.getTime();

        // Переводим майнкрафтовское время в часы и минуты
        long hours = (time / 1000 + 6) % 24;
        long minutes = (time % 1000) * 60 / 1000;
        String mcTimeFormatted = String.format("%02d:%02d", hours, minutes);

        String timeOfDay = (time < 12300 || time > 23850) ? "День" : "Ночь";

        // 1. Иконка игрового времени (Слот 11)
        return getItemStack(mcTimeFormatted, timeOfDay, world);
    }

    private static ItemStack getItemStack(String mcTimeFormatted, String timeOfDay, World world) {
        ItemStack clockItem = new ItemStack(Material.WATCH, 1);
        ItemMeta clockMeta = clockItem.getItemMeta();
        if (clockMeta != null) {
            clockMeta.setDisplayName(ChatColor.YELLOW + "Игровое время: " + mcTimeFormatted);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Состояние: " + timeOfDay);
            lore.add(ChatColor.GRAY + "Мир: " + world.getName());
            lore.add("");
            lore.add(ChatColor.GREEN + "Нажмите, чтобы узнать точное время в чат!");
            clockMeta.setLore(lore);
            clockItem.setItemMeta(clockMeta);
        }
        return clockItem;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();

        // Кнопка «Назад в меню» (Слот 0)
        if (slot == 0) {
            plugin.openPhoneMenu(player);
            return;
        }

        if (clicked == null || !clicked.hasItemMeta()) return;

        // Если кликнули на игровые часы — выводим время в чат
        if (clicked.getType() == Material.WATCH) {
            World world = player.getWorld();
            long time = world.getTime();
            long hours = (time / 1000 + 6) % 24;
            long minutes = (time % 1000) * 60 / 1000;

            player.sendMessage(ChatColor.AQUA + "[Часы] Текущее время в игре: " + ChatColor.YELLOW + String.format("%02d:%02d", hours, minutes));
            player.closeInventory();
        }
    }
}
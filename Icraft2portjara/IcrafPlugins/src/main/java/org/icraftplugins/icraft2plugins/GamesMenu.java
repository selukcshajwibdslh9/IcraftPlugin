package org.icraftplugins.icraft2plugins;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GamesMenu {

    private final IcraftPlugins plugin;
    private final String guiTitle = ChatColor.GOLD + "Игры и Развлечения";
    private final SnakeApp snakeApp;
    private final FlappyApp flappyApp;
    private final SokobanApp sokobanApp; // 1. Объявляем поле

    // 2. Принимаем sokobanApp в конструкторе и сохраняем
    public GamesMenu(IcraftPlugins plugin, SnakeApp snakeApp, FlappyApp flappyApp, SokobanApp sokobanApp) {
        this.plugin = plugin;
        this.snakeApp = snakeApp;
        this.flappyApp = flappyApp;
        this.sokobanApp = sokobanApp; // <--- Фикс ошибки (присваиваем значение)
    }

    public String getTitle() {
        return guiTitle;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, guiTitle);

        // Кнопка выхода в главное меню телефона (Слот 0)
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

        // Иконка игры Змейка (Слот 11)
        ItemStack snakeIcon = new ItemStack(Material.APPLE, 1);
        ItemMeta snakeMeta = snakeIcon.getItemMeta();
        if (snakeMeta != null) {
            snakeMeta.setDisplayName(ChatColor.GREEN + "Змейка");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Классическая мини-игра");
            lore.add(ChatColor.YELLOW + "Нажмите, чтобы запустить!");
            snakeMeta.setLore(lore);
            snakeIcon.setItemMeta(snakeMeta);
        }
        inv.setItem(11, snakeIcon);

        // Иконка Flappy Bird (Слот 13)
        ItemStack flappyIcon = new ItemStack(Material.FEATHER, 1);
        ItemMeta flappyMeta = flappyIcon.getItemMeta();
        if (flappyMeta != null) {
            flappyMeta.setDisplayName(ChatColor.YELLOW + "Flappy Bird");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Пролети 30 труб и победи Марио!");
            flappyMeta.setLore(lore);
            flappyIcon.setItemMeta(flappyMeta);
        }
        inv.setItem(13, flappyIcon);

        // Иконка Сокобан (Слот 15)
        ItemStack sokobanIcon = new ItemStack(Material.BRICK, 1);
        ItemMeta sokobanMeta = sokobanIcon.getItemMeta();
        if (sokobanMeta != null) {
            sokobanMeta.setDisplayName(ChatColor.BLUE + "Тетрис");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Складывайте фигуры в линии");
            sokobanMeta.setLore(lore);
            sokobanIcon.setItemMeta(sokobanMeta);
        }
        inv.setItem(15, sokobanIcon);

        player.openInventory(inv);
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        // Кнопка «Назад в меню» (Слот 0)
        if (slot == 0) {
            plugin.openPhoneMenu(player);
            return;
        }

        // Клик по Змейке (Слот 11)
        if (slot == 11) {
            player.closeInventory();
            snakeApp.open(player);
        }

        // Клик по Flappy Bird (Слот 13)
        if (slot == 13) {
            player.closeInventory();
            flappyApp.open(player);
        }

        // Клик по Сокобану (Слот 15)
        if (slot == 15) {
            player.closeInventory();
            sokobanApp.open(player);
        }
    }
}
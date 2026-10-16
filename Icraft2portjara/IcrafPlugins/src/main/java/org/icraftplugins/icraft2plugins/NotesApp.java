package org.icraftplugins.icraft2plugins;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class NotesApp implements Listener {

    private final IcraftPlugins plugin;
    private final String guiTitle = ChatColor.DARK_PURPLE + "Заметки";

    private final Set<UUID> waitingForInput = new HashSet<>();
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    public NotesApp(IcraftPlugins plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public String getTitle() {
        return guiTitle;
    }

    public void open(Player player) {
        openPage(player, 0);
    }

    public void openPage(Player player, int page) {
        playerPages.put(player.getUniqueId(), page);
        Inventory inv = Bukkit.createInventory(null, 54, guiTitle);

        List<String> notes = plugin.getConfig().getStringList("notes." + player.getUniqueId());

        // --- Верхняя панель ---

        // Кнопка: Назад в телефон (Слот 0)
        ItemStack backItem = new ItemStack(Material.BARRIER); // или Material.BARRIER / Material.ARROW
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.RED + "<< Назад в меню");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Вернуться в главное меню телефона");
            backMeta.setLore(lore);
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backItem);

        // Кнопка: Создать заметку (Слот 4)
        ItemStack createItem = getItemStack();
        inv.setItem(4, createItem);

        // Кнопка: Очистить все заметки (Слот 8)
        ItemStack clearItem = getStack();
        inv.setItem(8, clearItem);

        // --- Отображение заметок ---
        int itemsPerPage = 36;
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, notes.size());

        int slot = 9;
        for (int i = startIndex; i < endIndex; i++) {
            ItemStack noteItem = new ItemStack(Material.BOOK);
            ItemMeta noteMeta = noteItem.getItemMeta();
            if (noteMeta != null) {
                noteMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Заметка #" + (i + 1));

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.YELLOW + "Текст:");

                String text = notes.get(i);
                for (String line : wrapText(text)) {
                    lore.add(ChatColor.WHITE + line);
                }

                lore.add("");
                lore.add(ChatColor.RED + "ПКМ - Удалить эту заметку");
                noteMeta.setLore(lore);
                noteItem.setItemMeta(noteMeta);
            }
            inv.setItem(slot++, noteItem);
        }

        // --- Переключение страниц ---
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta pMeta = prev.getItemMeta();
            if (pMeta != null) {
                pMeta.setDisplayName(ChatColor.YELLOW + "<< Предыдущая страница");
                prev.setItemMeta(pMeta);
            }
            inv.setItem(48, prev);
        }

        if (endIndex < notes.size()) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nMeta = next.getItemMeta();
            if (nMeta != null) {
                nMeta.setDisplayName(ChatColor.YELLOW + "Следующая страница >>");
                next.setItemMeta(nMeta);
            }
            inv.setItem(50, next);
        }

        player.openInventory(inv);
    }

    private static ItemStack getStack() {
        ItemStack clearItem = new ItemStack(Material.BARRIER);
        ItemMeta clearMeta = clearItem.getItemMeta();
        if (clearMeta != null) {
            clearMeta.setDisplayName(ChatColor.RED + "[X] Очистить все заметки");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Нажмите, чтобы безвозвратно");
            lore.add(ChatColor.GRAY + "удалить ВСЕ свои заметки.");
            clearMeta.setLore(lore);
            clearItem.setItemMeta(clearMeta);
        }
        return clearItem;
    }

    private static ItemStack getItemStack() {
        ItemStack createItem = new ItemStack(Material.PAPER);
        ItemMeta createMeta = createItem.getItemMeta();
        if (createMeta != null) {
            createMeta.setDisplayName(ChatColor.GREEN + "[+] Создать заметку");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Нажмите, чтобы ввести текст");
            lore.add(ChatColor.GRAY + "новой заметки в чат.");
            createMeta.setLore(lore);
            createItem.setItemMeta(createMeta);
        }
        return createItem;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        int currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);

        // Клик по кнопке «Назад в меню» (Слот 0)
        if (slot == 0) {
            plugin.openPhoneMenu(player);
            return;
        }

        // Клик по кнопке «Создать заметку» (Слот 4)
        if (slot == 4) {
            player.closeInventory();
            waitingForInput.add(player.getUniqueId());
            player.sendMessage(ChatColor.DARK_PURPLE + "[Заметки] " + ChatColor.YELLOW + "Введите текст заметки в чат (или напишите 'отмена'):");
            return;
        }

        // Клик по кнопке «Очистить все заметки» (Слот 8)
        if (slot == 8) {
            plugin.getConfig().set("notes." + player.getUniqueId(), null);
            plugin.saveConfig();
            player.sendMessage(ChatColor.RED + "[Заметки] Все ваши заметки были удалены!");
            openPage(player, 0);
            return;
        }

        // Переключение страниц
        if (slot == 48 && currentPage > 0) {
            openPage(player, currentPage - 1);
            return;
        }
        if (slot == 50) {
            List<String> notes = plugin.getConfig().getStringList("notes." + player.getUniqueId());
            if ((currentPage + 1) * 36 < notes.size()) {
                openPage(player, currentPage + 1);
            }
            return;
        }

        // Обработка клика по заметкам (Слоты 9–44)
        if (slot >= 9 && slot < 45) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() == Material.BOOK && clicked.hasItemMeta()) {
                int noteIndex = (currentPage * 36) + (slot - 9);
                List<String> notes = plugin.getConfig().getStringList("notes." + player.getUniqueId());

                if (noteIndex < notes.size()) {
                    if (event.isRightClick()) {
                        notes.remove(noteIndex);
                        plugin.getConfig().set("notes." + player.getUniqueId(), notes);
                        plugin.saveConfig();
                        player.sendMessage(ChatColor.RED + "[Заметки] Заметка #" + (noteIndex + 1) + " удалена!");
                        openPage(player, currentPage);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (waitingForInput.contains(player.getUniqueId())) {
            event.setCancelled(true);
            waitingForInput.remove(player.getUniqueId());

            String message = event.getMessage().trim();

            if (message.equalsIgnoreCase("отмена")) {
                player.sendMessage(ChatColor.RED + "[Заметки] Создание заметки отменено.");
                Bukkit.getScheduler().runTask(plugin, () -> open(player));
                return;
            }

            List<String> notes = plugin.getConfig().getStringList("notes." + player.getUniqueId());
            notes.add(message);
            plugin.getConfig().set("notes." + player.getUniqueId(), notes);
            plugin.saveConfig();

            player.sendMessage(ChatColor.GREEN + "[Заметки] Заметка успешно сохранена!");

            Bukkit.getScheduler().runTask(plugin, () -> open(player));
        }
    }

    private List<String> wrapText(String text) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > 30) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder();
            }
            if (currentLine.length() > 0) {
                currentLine.append(" ");
            }
            currentLine.append(word);
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        return lines;
    }
}
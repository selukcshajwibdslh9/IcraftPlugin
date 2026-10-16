package org.icraftplugins.icraft2plugins;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class MailApp implements PhoneApp {

    private final IcraftPlugins plugin;
    private final MailManager mailManager;
    private final String guiTitle = ChatColor.DARK_GREEN + "Почтовый клиент";
    private static final String DRAFT_LORE = ChatColor.DARK_PURPLE + "Черновик почтового письма";

    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private final Map<UUID, Boolean> viewingInbox = new HashMap<>();

    public MailApp(IcraftPlugins plugin, MailManager mailManager) {
        this.plugin = plugin;
        this.mailManager = mailManager;
    }

    @Override
    public String getTitle() {
        return guiTitle;
    }

    @Override
    public void open(Player player) {
        playerPages.put(player.getUniqueId(), 0);
        viewingInbox.put(player.getUniqueId(), true);
        openMailPage(player, 0, true);
    }

    public void openMailPage(Player player, int page, boolean isInbox) {
        Inventory gui = Bukkit.createInventory(null, 54, guiTitle);
        UUID uuid = player.getUniqueId();

        List<MailMessage> messages = isInbox ? mailManager.getInbox(uuid) : mailManager.getOutbox(player.getName());

        for (int i = 0; i < 54; i++) {
            gui.setItem(i, null);
        }

        int startIndex = page * 10;
        int endIndex = Math.min(startIndex + 10, messages.size());

        for (int i = 0; i < (endIndex - startIndex); i++) {
            MailMessage msg = messages.get(startIndex + i);
            ItemStack item = new ItemStack(Material.PAPER, 1);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (isInbox) {
                    meta.setDisplayName(ChatColor.YELLOW + "Письмо от: " + msg.getSenderName());
                } else {
                    meta.setDisplayName(ChatColor.AQUA + "Исходящее письмо");
                }
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Тема: " + msg.getTitle());
                lore.add(ChatColor.DARK_GRAY + "Нажмите, чтобы прочитать в чате");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            gui.setItem(10 + i, item);
        }

        // Нижняя панель управления
        setButton(gui, 45, Material.ARROW, ChatColor.RED + "Предыдущая страница");
        setButton(gui, 48, Material.BOOK_AND_QUILL, ChatColor.GREEN + "Написать письмо"); // ← Снова на месте!
        setButton(gui, 49, Material.BUCKET, ChatColor.RED + "Очистить входящие");
        setButton(gui, 50, Material.BARRIER, ChatColor.AQUA + "Выход в меню");

        if (isInbox) {
            setButton(gui, 52, Material.BOOK, ChatColor.GOLD + "Посмотреть исходящие");
        } else {
            setButton(gui, 52, Material.BOOK, ChatColor.GOLD + "Посмотреть входящие");
        }

        setButton(gui, 53, Material.ARROW, ChatColor.GREEN + "Следующая страница");

        player.openInventory(gui);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        UUID uuid = player.getUniqueId();
        boolean isInbox = viewingInbox.getOrDefault(uuid, true);

        if (slot == 50) { // Выход в меню
            plugin.openPhoneMenu(player);
        } else if (slot == 48) { // Написать письмо
            player.closeInventory();

            ItemStack book = new ItemStack(Material.BOOK_AND_QUILL, 1);
            BookMeta meta = (BookMeta) book.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.YELLOW + "Письмо (Черновик)");
                meta.setLore(Collections.singletonList(DRAFT_LORE));
                meta.addPage("Получатель: \n");
                book.setItemMeta(meta);
            }
            player.getInventory().addItem(book);
            player.sendMessage(ChatColor.GREEN + "[Почта] Укажите ник после 'Получатель:', напишите текст, тему и подпишите книгу!");

        } else if (slot == 49) { // Очистить входящие
            mailManager.clearInbox(uuid);
            player.sendMessage(ChatColor.RED + "[Почта] Ваши входящие сообщения очищены!");
            openMailPage(player, 0, true);
        } else if (slot == 52) { // Переключение Входящие / Исходящие
            boolean newMode = !isInbox;
            viewingInbox.put(uuid, newMode);
            playerPages.put(uuid, 0);
            openMailPage(player, 0, newMode);
        } else if (slot == 45) { // Назад
            int page = playerPages.getOrDefault(uuid, 0);
            if (page > 0) {
                page--;
                playerPages.put(uuid, page);
                openMailPage(player, page, isInbox);
            }
        } else if (slot == 53) { // Вперед
            int page = playerPages.getOrDefault(uuid, 0);
            playerPages.put(uuid, page + 1);
            openMailPage(player, page + 1, isInbox);
        } else if (slot >= 10 && slot < 20) { // Клик по письму
            int index = (playerPages.getOrDefault(uuid, 0) * 10) + (slot - 10);
            List<MailMessage> messages = isInbox ? mailManager.getInbox(uuid) : mailManager.getOutbox(player.getName());
            if (index < messages.size()) {
                MailMessage msg = messages.get(index);
                player.sendMessage(ChatColor.GOLD + "========== [ ПИСЬМО ] ==========");
                player.sendMessage(ChatColor.YELLOW + (isInbox ? "От: " + msg.getSenderName() : "Кому: " + Bukkit.getOfflinePlayer(msg.getRecipientUuid()).getName()));
                player.sendMessage(ChatColor.YELLOW + "Тема: " + msg.getTitle());
                player.sendMessage(ChatColor.WHITE + msg.getContent());
                player.sendMessage(ChatColor.GOLD + "================================");
            }
        }
    }

    public void handleBookSign(Player player, ItemStack signedBook, BookMeta meta) {
        if (meta == null) return;

        List<String> pages = meta.getPages();
        if (pages.isEmpty()) return;

        String firstPage = ChatColor.stripColor(pages.get(0));
        String recipientName = "";

        for (String line : firstPage.split("\n")) {
            String cleanLine = ChatColor.stripColor(line).trim();
            if (cleanLine.toLowerCase().startsWith("получатель")) {
                String[] parts = cleanLine.split(":", 2);
                if (parts.length > 1) {
                    recipientName = ChatColor.stripColor(parts[1]).trim();
                }
                break;
            }
        }

        if (recipientName.isEmpty()) {
            player.sendMessage(ChatColor.RED + "[Почта] Ошибка! Не указан получатель (формат: Получатель: Ник). Черновик удален.");
            removeDraftFromPlayer(player);
            return;
        }

        String title = ChatColor.stripColor(meta.hasTitle() ? meta.getTitle() : "Без темы");

        StringBuilder content = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) {
            String pageText = ChatColor.stripColor(pages.get(i));
            if (i == 0) {
                pageText = pageText.replaceAll("(?i)получатель\\s*:\\s*§?[0-9a-fk-or]*.*(\r?\n)?", "");
            }
            content.append(pageText).append("\n");
        }

        removeDraftFromPlayer(player);

        OfflinePlayer target = Bukkit.getOfflinePlayer(recipientName);
        UUID targetUuid = target.getUniqueId();

        MailMessage newMsg = new MailMessage(player.getName(), targetUuid, title, content.toString().trim());
        mailManager.addMessage(newMsg);

        player.sendMessage(ChatColor.GREEN + "Вы успешно отправили письмо игроку " + recipientName);
    }

    private void removeDraftFromPlayer(Player player) {
        ItemStack[] items = player.getInventory().getContents();
        for (int i = 0; i < items.length; i++) {
            ItemStack item = items[i];
            if (item != null && (item.getType() == Material.BOOK_AND_QUILL || item.getType() == Material.WRITTEN_BOOK)) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasLore() && meta.getLore().contains(DRAFT_LORE)) {
                    player.getInventory().setItem(i, null);
                }
            }
        }
    }

    public static boolean isMailDraft(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;
        return meta.getLore().contains(DRAFT_LORE);
    }

    private void setButton(Inventory gui, int slot, Material material, String displayName) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            item.setItemMeta(meta);
        }
        gui.setItem(slot, item);
    }
}
package org.icraftplugins.icraft2plugins;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class IcraftPlugins extends JavaPlugin implements Listener {
    private PaintApp paintApp;
    private WallpaperApp wallpaperApp;
    private SokobanApp sokobanApp;
    private FlappyApp flappyApp;
    private GamesMenu gamesMenu;
    private NotesApp notesApp;
    private ClockApp clockApp;
    private SnakeApp snakeApp; // <--- Сделай полем класса
    private final String phoneName = ChatColor.AQUA + "Телефон";
    private final String phoneGuiTitle = ChatColor.DARK_BLUE + "Меню телефона";
    private IBuyApp iBuyApp;
    private CalculatorApp calculatorApp;
    private MailApp mailApp;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.paintApp = new PaintApp(this);
        this.wallpaperApp = new WallpaperApp(this);
        this.sokobanApp = new SokobanApp(this);
        this.flappyApp = new FlappyApp(this);
        this.snakeApp = new SnakeApp(this); // <--- Присваивай сюда без `SnakeApp snakeApp`
        this.gamesMenu = new GamesMenu(this, snakeApp, flappyApp, sokobanApp);
        this.notesApp = new NotesApp(this);
        this.clockApp = new ClockApp(this);
        this.calculatorApp = new CalculatorApp(this);
        MailManager mailManager = new MailManager(this);
        this.mailApp = new MailApp(this, mailManager);
        EconomyManager economyManager = new EconomyManager(this);
        this.iBuyApp = new IBuyApp(this, economyManager);

        getServer().getPluginManager().registerEvents(this, this);
        registerPhoneRecipe();
        getLogger().info("IcraftPlugins успешно запущен!");
    }

    @Override
    public void onDisable() {
        getLogger().info("IcraftPlugins выключен.");
    }

    private void registerPhoneRecipe() {
        ItemStack phone = createPhoneItem();
        NamespacedKey key = new NamespacedKey(this, "icraft_phone");
        ShapedRecipe recipe = new ShapedRecipe(key, phone);

        recipe.shape("III", "ISI", "III");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('S', Material.THIN_GLASS);

        Bukkit.addRecipe(recipe);
    }

    private ItemStack createPhoneItem() {
        ItemStack phone = new ItemStack(Material.KNOWLEDGE_BOOK, 1);
        ItemMeta meta = phone.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(phoneName);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Устройство связи Icraft.");
            lore.add(ChatColor.YELLOW + "ПКМ, чтобы открыть");
            meta.setLore(lore);
            phone.setItemMeta(meta);
        }

        return phone;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && item != null) {
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                if (item.getItemMeta().getDisplayName().equals(phoneName)) {
                    event.setCancelled(true);
                    openPhoneMenu(player);
                }
            }
        }
    }

    public void openPhoneMenu(Player player) {
        Inventory phoneGui = Bukkit.createInventory(null, 54, phoneGuiTitle);

        // 1. Сначала заполняем ВСЕ слоты инвентаря выбранными обоями игрока!
        for (int i = 0; i < 54; i++) {
            phoneGui.setItem(i, wallpaperApp.getWallpaperItem(player, i));
        }

        // 2. Поверх обоев расставляем иконки приложений
        ItemStack calcIcon = new ItemStack(Material.COMMAND, 1);
        ItemMeta calcMeta = calcIcon.getItemMeta();
        if (calcMeta != null) {
            calcMeta.setDisplayName(ChatColor.GREEN + "Калькулятор");
            List<String> calcLore = new ArrayList<>();
            calcLore.add(ChatColor.GRAY + "Нажмите, чтобы запустить");
            calcMeta.setLore(calcLore);
            calcIcon.setItemMeta(calcMeta);
        }
        phoneGui.setItem(10, calcIcon);

        ItemStack mailIcon = new ItemStack(Material.BOOK, 1);
        ItemMeta mailMeta = mailIcon.getItemMeta();
        if (mailMeta != null) {
            mailMeta.setDisplayName(ChatColor.GOLD + "Почта");
            List<String> mailLore = new ArrayList<>();
            mailLore.add(ChatColor.GRAY + "Нажмите, чтобы открыть входящие");
            mailMeta.setLore(mailLore);
            mailIcon.setItemMeta(mailMeta);
        }
        phoneGui.setItem(12, mailIcon);

        ItemStack iBuyIcon = new ItemStack(Material.CHEST, 1);
        ItemMeta iBuyMeta = iBuyIcon.getItemMeta();
        if (iBuyMeta != null) {
            iBuyMeta.setDisplayName(ChatColor.GREEN + "Магазин iBuy");
            List<String> iBuyLore = new ArrayList<>();
            iBuyLore.add(ChatColor.GRAY + "Заказ товаров с доставкой на парашюте");
            iBuyMeta.setLore(iBuyLore);
            iBuyIcon.setItemMeta(iBuyMeta);
        }
        phoneGui.setItem(14, iBuyIcon);

        ItemStack clockIcon = new ItemStack(Material.WATCH, 1);
        ItemMeta clockMeta = clockIcon.getItemMeta();
        if (clockMeta != null) {
            clockMeta.setDisplayName(ChatColor.YELLOW + "Часы");
            List<String> clockLore = new ArrayList<>();
            clockLore.add(ChatColor.GRAY + "Узнать игровое и реальное время");
            clockMeta.setLore(clockLore);
            clockIcon.setItemMeta(clockMeta);
        }
        phoneGui.setItem(16, clockIcon);

        // Иконка Заметок в телефоне (слот 28)
        ItemStack notesIcon = new ItemStack(Material.BOOK_AND_QUILL, 1);
        ItemMeta notesMeta = notesIcon.getItemMeta();
        if (notesMeta != null) {
            notesMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Заметки");
            List<String> notesLore = new ArrayList<>();
            notesLore.add(ChatColor.GRAY + "Ваши личные записи в виртуальной книге");
            notesMeta.setLore(notesLore);
            notesIcon.setItemMeta(notesMeta);
        }
        phoneGui.setItem(28, notesIcon);

        // Иконка Игр в главном меню (Слот 30)
        ItemStack gamesIcon = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        ItemMeta gamesMeta = gamesIcon.getItemMeta();
        if (gamesMeta != null) {
            gamesMeta.setDisplayName(ChatColor.GOLD + "Игры");
            List<String> gamesLore = new ArrayList<>();
            gamesLore.add(ChatColor.GRAY + "Сборник мини-приложений");
            gamesMeta.setLore(gamesLore);
            gamesIcon.setItemMeta(gamesMeta);
        }
        phoneGui.setItem(30, gamesIcon);

        // Иконка каталога обоев (Слот 32)
        ItemStack wallpaperIcon = new ItemStack(Material.PAINTING, 1);
        ItemMeta wallpaperMeta = wallpaperIcon.getItemMeta();
        if (wallpaperMeta != null) {
            wallpaperMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Каталог обоев");
            List<String> wallpaperLore = new ArrayList<>();
            wallpaperLore.add(ChatColor.GRAY + "Выбор обоев и анимаций для телефона");
            wallpaperMeta.setLore(wallpaperLore);
            wallpaperIcon.setItemMeta(wallpaperMeta);
        }
        phoneGui.setItem(32, wallpaperIcon);

        ItemStack paintIcon = new ItemStack(Material.ITEM_FRAME, 1);
        ItemMeta paintMeta = paintIcon.getItemMeta();
        paintMeta.setDisplayName(ChatColor.DARK_GREEN + "Paint");
        List<String> paintLore = new ArrayList<>();
        paintLore.add(ChatColor.GRAY + "Рисование пиксель-артов");
        paintMeta.setLore(paintLore);
        paintIcon.setItemMeta(paintMeta);
        phoneGui.setItem(34, paintIcon);

        player.openInventory(phoneGui);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        ItemStack hotbarItem = null;

        if (event.getClick().name().startsWith("NUMBER_KEY")) {
            int hotbarButton = event.getHotbarButton();
            if (hotbarButton >= 0 && hotbarButton < 9) {
                hotbarItem = event.getWhoClicked().getInventory().getItem(hotbarButton);
            }
        }

        if (MailApp.isMailDraft(current) || MailApp.isMailDraft(cursor) || MailApp.isMailDraft(hotbarItem)) {
            if (event.isShiftClick() || event.getClick().name().startsWith("NUMBER_KEY") || event.getClick().name().contains("DROP") ||
                    (event.getClickedInventory() != null && event.getClickedInventory().getType() != InventoryType.PLAYER)) {

                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player) {
                    event.getWhoClicked().sendMessage(ChatColor.RED + "Нельзя перемещать или вытаскивать черновик письма!");
                }
                return;
            }
        }

        Inventory topInventory = event.getView().getTopInventory();
        if (topInventory != null && topInventory.getType() != InventoryType.CRAFTING && topInventory.getType() != InventoryType.CREATIVE) {
            if (MailApp.isMailDraft(current) || MailApp.isMailDraft(cursor)) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player) {
                    event.getWhoClicked().sendMessage(ChatColor.RED + "Нельзя класть черновик в сундуки!");
                }
                return;
            }
        }

        String title = event.getView().getTitle();

        if (title.equals(phoneGuiTitle)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            Player player = (Player) event.getWhoClicked();

            if (slot == 10) {
                player.closeInventory();
                calculatorApp.open(player);
            } else if (slot == 12) {
                player.closeInventory();
                mailApp.open(player);
            } else if (slot == 14) {
                player.closeInventory();
                iBuyApp.open(player);
            } else if (slot == 16) {
                player.closeInventory();
                clockApp.open(player);
            } else if (slot == 28) {
                player.closeInventory();
                notesApp.open(player);
            } else if (slot == 30) { // ИСПРАВЛЕНО с 20 на 30!
                player.closeInventory();
                gamesMenu.open(player);
            } else if (slot == 32) {
            player.closeInventory();
            wallpaperApp.open(player);
         } else if (slot == 34) {
            player.closeInventory();
            paintApp.open(player);
        }

        } else if (title.equals(calculatorApp.getTitle())) {
            calculatorApp.handleClick(event);
        } else if (title.equals(mailApp.getTitle())) {
            mailApp.handleClick(event);
        } else if (title.equals(iBuyApp.getTitle())) {
            iBuyApp.handleClick(event);
        } else if (title.equals(clockApp.getTitle())) {
            clockApp.handleClick(event);
        } else if (title.equals(notesApp.getTitle())) {
            notesApp.handleClick(event);
        } else if (title.equals(gamesMenu.getTitle())) {
            gamesMenu.handleClick(event);
        } else if (title.equals(snakeApp.getTitle())) {
        snakeApp.handleClick(event); // <--- ВОТ ЭТОГО НЕ ХВАТАЛО!
        } else if (title.equals(flappyApp.getTitle())) {
            flappyApp.handleClick(event); // Перехват кликов Flappy Bird
        }else if (title.equals(sokobanApp.getTitle())) {
                sokobanApp.handleClick(event);
        } else if (title.equals(wallpaperApp.getTitle())) {
            wallpaperApp.handleClick(event);
        } else if (title.equals(paintApp.getCanvasTitle())) {
        paintApp.handleCanvasClick(event);
    } else if (paintApp.isPaletteTitle(title)) {
        paintApp.handlePaletteClick(event);
    }
    }

    @EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(wallpaperApp.getTitle())) {
            if (event.getPlayer() instanceof Player) {
                wallpaperApp.handleClose((Player) event.getPlayer());
            }
        }if (event.getView().getTitle().equals(paintApp.getCanvasTitle()) || paintApp.isPaletteTitle(event.getView().getTitle())) {
            if (event.getPlayer() instanceof Player) {
                paintApp.handleClose((Player) event.getPlayer());
            }
        }
    }
    public void openGamesMenu(Player player) {
        if (gamesMenu != null) {
            gamesMenu.open(player);
        }
    }

    @EventHandler
    public void onBookEdit(PlayerEditBookEvent event) {
        if (event.isSigning()) {
            Player player = event.getPlayer();
            ItemStack itemInHand = player.getInventory().getItemInMainHand();

            if (MailApp.isMailDraft(itemInHand)) {
                mailApp.handleBookSign(player, itemInHand, event.getNewBookMeta());
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (MailApp.isMailDraft(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Нельзя выбрасывать черновик письма!");
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(MailApp::isMailDraft);
    }
}
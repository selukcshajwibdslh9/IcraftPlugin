package org.icraftplugins.icraft2plugins;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class SnakeApp implements Listener {

    private final IcraftPlugins plugin;
    private final String guiTitle = ChatColor.DARK_GREEN + "Змейка";
    private final Map<UUID, SnakeGame> activeGames = new HashMap<>();

    public SnakeApp(IcraftPlugins plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public String getTitle() {
        return guiTitle;
    }

    public void open(Player player) {
        stopGame(player);

        Inventory inv = Bukkit.createInventory(null, 54, guiTitle);
        SnakeGame game = new SnakeGame(player, inv);
        activeGames.put(player.getUniqueId(), game);

        player.openInventory(inv);
        game.start();
    }

    public void stopGame(Player player) {
        SnakeGame game = activeGames.remove(player.getUniqueId());
        if (game != null) {
            game.cancel();
        }
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (event.getClickedInventory() == null || event.getClickedInventory().getType() == org.bukkit.event.inventory.InventoryType.PLAYER) {
            return;
        }

        int slot = event.getSlot();

        // Кнопка возврата в меню игр (Слот 45)
        if (slot == 45) {
            stopGame(player);
            plugin.openGamesMenu(player);
            return;
        }

        SnakeGame game = activeGames.get(player.getUniqueId());
        if (game == null || game.isGameOver()) return;

        // Компактные слоты управления внизу слева:
        // 46: Вверх (^), 50: Влево (<), 51: Вниз (v), 52: Вправо (>)
        if (slot == 46) {
            game.setDirection(Direction.UP);
        } else if (slot == 47) {
            game.setDirection(Direction.DOWN);
        } else if (slot == 48) {
            game.setDirection(Direction.LEFT);
        } else if (slot == 49) {
            game.setDirection(Direction.RIGHT);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(guiTitle)) {
            if (event.getPlayer() instanceof Player) {
                stopGame((Player) event.getPlayer());
            }
        }
    }

    private enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    private class SnakeGame extends BukkitRunnable {
        private final Player player;
        private final Inventory inv;
        private final LinkedList<Integer> snake = new LinkedList<>();
        private Direction direction = Direction.RIGHT;
        private Direction nextDirection = Direction.RIGHT;
        private int appleSlot;
        private int score = 0;
        private boolean gameOver = false;

        public SnakeGame(Player player, Inventory inv) {
            this.player = player;
            this.inv = inv;

            snake.add(20);
            snake.add(19);
            snake.add(18);

            spawnApple();
            renderControls();
            render();
        }

        public void start() {
            // Увеличили задержку до 12 тиков (0.6 сек) — змейка стала плавной и не носится как безумная
            this.runTaskTimer(plugin, 12L, 12L);
        }

        public void setDirection(Direction dir) {
            if (direction == Direction.UP && dir == Direction.DOWN) return;
            if (direction == Direction.DOWN && dir == Direction.UP) return;
            if (direction == Direction.LEFT && dir == Direction.RIGHT) return;
            if (direction == Direction.RIGHT && dir == Direction.LEFT) return;
            this.nextDirection = dir;
        }

        public boolean isGameOver() {
            return gameOver;
        }

        private void spawnApple() {
            Random random = new Random();
            do {
                appleSlot = random.nextInt(45);
            } while (snake.contains(appleSlot));
        }

        @Override
        public void run() {
            if (gameOver) return;

            direction = nextDirection;
            int head = snake.getFirst();
            int row = head / 9;
            int col = head % 9;

            switch (direction) {
                case UP: row--; break;
                case DOWN: row++; break;
                case LEFT: col--; break;
                case RIGHT: col++; break;
            }

            if (row < 0 || row >= 5 || col < 0 || col >= 9) {
                endGame();
                return;
            }

            int newHead = row * 9 + col;

            if (snake.contains(newHead)) {
                endGame();
                return;
            }

            snake.addFirst(newHead);

            if (newHead == appleSlot) {
                score++;
                spawnApple();
            } else {
                snake.removeLast();
            }

            render();
        }

        private void endGame() {
            gameOver = true;
            cancel();

            ItemStack failItem = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
            ItemMeta meta = failItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.RED + "Игра окончена! Счёт: " + score);
                failItem.setItemMeta(meta);
            }
            for (int i = 0; i < 45; i++) {
                inv.setItem(i, failItem);
            }
            player.sendMessage(ChatColor.RED + "[Змейка] Игра окончена! Ваш счёт: " + ChatColor.YELLOW + score);
        }

        private void render() {
            ItemStack background = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
            ItemMeta bgMeta = background.getItemMeta();
            if (bgMeta != null) {
                bgMeta.setDisplayName(" ");
                background.setItemMeta(bgMeta);
            }

            for (int i = 0; i < 45; i++) {
                inv.setItem(i, background);
            }

            ItemStack apple = new ItemStack(Material.APPLE);
            ItemMeta appleMeta = apple.getItemMeta();
            if (appleMeta != null) {
                appleMeta.setDisplayName(ChatColor.RED + "Яблоко");
                apple.setItemMeta(appleMeta);
            }
            inv.setItem(appleSlot, apple);

            boolean isHead = true;
            for (int slot : snake) {
                short color = isHead ? (short) 5 : (short) 13;
                ItemStack part = new ItemStack(Material.STAINED_GLASS_PANE, 1, color);
                ItemMeta meta = part.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(isHead ? ChatColor.GREEN + "Голова (Счёт: " + score + ")" : ChatColor.DARK_GREEN + "Тело");
                    part.setItemMeta(meta);
                }
                inv.setItem(slot, part);
                isHead = false;
            }
        }

        private void renderControls() {
            // Кнопка выхода — Барьер (Слот 45)
            ItemStack back = new ItemStack(Material.BARRIER);
            ItemMeta bMeta = back.getItemMeta();
            if (bMeta != null) {
                bMeta.setDisplayName(ChatColor.RED + "<< Назад в меню игр");
                back.setItemMeta(bMeta);
            }
            inv.setItem(45, back);

            // Компактная панель управления впритык друг к другу (Слоты 46, 50, 51, 52)
            // [Выход(45)] [Вверх(46)] [пусто] ...
            // [Влево(50)] [Вниз(51)] [Вправо(52)]
            inv.setItem(46, createBtn("^ Вверх"));
            inv.setItem(47, createBtn("v Вниз"));
            inv.setItem(48, createBtn("< Влево"));
            inv.setItem(49, createBtn("> Вправо"));
        }

        private ItemStack createBtn(String name) {
            ItemStack btn = new ItemStack(Material.WOOD_BUTTON);
            ItemMeta meta = btn.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.YELLOW + name);
                btn.setItemMeta(meta);
            }
            return btn;
        }
    }
}
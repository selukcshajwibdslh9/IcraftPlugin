package org.icraftplugins.icraft2plugins;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class SokobanApp implements Listener {

    private final IcraftPlugins plugin;
    private final String guiTitle = ChatColor.DARK_PURPLE + "Тетрис";
    private final Map<UUID, TetrisGame> activeGames = new HashMap<>();

    public SokobanApp(IcraftPlugins plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public String getTitle() {
        return guiTitle;
    }

    public void open(Player player) {
        stopGame(player);
        Inventory inv = Bukkit.createInventory(null, 54, guiTitle);
        TetrisGame game = new TetrisGame(player, inv);
        activeGames.put(player.getUniqueId(), game);
        player.openInventory(inv);
        game.render();
    }

    public void stopGame(Player player) {
        TetrisGame game = activeGames.remove(player.getUniqueId());
        if (game != null) {
            game.destroy();
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

        if (slot == 45) { // Выход в меню
            stopGame(player);
            plugin.openGamesMenu(player);
            return;
        }

        TetrisGame game = activeGames.get(player.getUniqueId());
        if (game == null) return;

        if (slot == 46) { // Ресет
            game.restart();
            return;
        }

        // Управление впритык (50-53)
        if (slot == 48) {
            game.rotate();
        } else if (slot == 50) {
            game.moveRight();
        } else if (slot == 49) {
            game.moveLeft();
        } else if (slot == 51) {
            game.dropFast();
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

    private class TetrisGame {
        private final Player player;
        private final Inventory inv;
        private int score = 0;

        private final int[][] grid = new int[5][9];

        // Классические 7 тетромино
        private final int[][][][] TETROMINOES = {
                {{{0,0,0,0},{1,1,1,1},{0,0,0,0},{0,0,0,0}}, {{0,0,1,0},{0,0,1,0},{0,0,1,0},{0,0,1,0}}}, // I
                {{{1,0,0},{1,1,1},{0,0,0}}, {{0,1,1},{0,1,0},{0,1,0}}, {{0,0,0},{1,1,1},{0,0,1}}, {{0,1,0},{0,1,0},{1,1,0}}}, // J
                {{{0,0,1},{1,1,1},{0,0,0}}, {{0,1,0},{0,1,0},{0,1,1}}, {{0,0,0},{1,1,1},{1,0,0}}, {{1,1,0},{0,1,0},{0,1,0}}}, // L
                {{{1,1},{1,1}}}, // O
                {{{0,1,1},{1,1,0},{0,0,0}}, {{0,1,0},{0,1,1},{0,0,1}}}, // S
                {{{0,1,0},{1,1,1},{0,0,0}}, {{0,1,0},{0,1,1},{0,1,0}}, {{0,0,0},{1,1,1},{0,1,0}}, {{0,1,0},{1,1,0},{0,1,0}}}, // T
                {{{1,1,0},{0,1,1},{0,0,0}}, {{0,0,1},{0,1,1},{0,1,0}}}  // Z
        };

        private int currentType = 0;
        private int currentRot = 0;
        private int curX = 3;
        private int curY = 0;

        private boolean gameOver = false;
        private BukkitTask gameTask;
        private BukkitTask musicTask;
        private final Random rand = new Random();

        // Ноты 8-битной мелодии Тетриса (высота тона pitch)
        private final float[] MELODY_PITCHES = {
                1.26f, 0.94f, 1.06f, 1.18f, 1.06f, 0.94f, 0.84f, 0.84f, 1.06f, 1.26f, 1.18f, 1.06f, 0.94f,
                0.94f, 1.06f, 1.18f, 1.26f, 1.06f, 0.84f, 0.84f, 0.0f,
                1.18f, 1.41f, 1.58f, 1.41f, 1.26f, 1.06f, 1.26f, 1.18f, 1.06f, 0.94f,
                0.94f, 1.06f, 1.18f, 1.26f, 1.06f, 0.84f, 0.84f
        };
        private int musicIndex = 0;

        public TetrisGame(Player player, Inventory inv) {
            this.player = player;
            this.inv = inv;
            restart();

            // Авто-падение фигур (каждые 1.5 секунды / 30 тиков)
            gameTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (gameOver) return;
                tick();
            }, 0L, 30L);

            // Музыка Тетриса (нота играет каждые 3 тика ~ 150мс)
            musicTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (gameOver) return;
                playMusicTick();
            }, 0L, 3L);
        }

        private void playMusicTick() {
            float pitch = MELODY_PITCHES[musicIndex];
            if (pitch > 0.0f) {
                // Воспроизводим нотную арфу для 8-битного звучания
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_HARP, 0.6f, pitch);
            }
            musicIndex = (musicIndex + 1) % MELODY_PITCHES.length;
        }

        public void destroy() {
            if (gameTask != null) gameTask.cancel();
            if (musicTask != null) musicTask.cancel();
        }

        public void restart() {
            gameOver = false;
            score = 0;
            musicIndex = 0;
            for (int r = 0; r < 5; r++) {
                Arrays.fill(grid[r], 0);
            }
            spawnPiece();
            render();
        }

        private void spawnPiece() {
            currentType = rand.nextInt(TETROMINOES.length);
            currentRot = 0;
            curX = 3;
            curY = 0;

            if (!canFit(curX, curY, currentType, currentRot)) {
                gameOver = true;
                player.sendMessage(ChatColor.RED + "[Тетрис] Игра окончена! Ваш счёт: " + score);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERDRAGON_GROWL, 0.5f, 0.8f);
            }
        }

        private boolean canFit(int x, int y, int type, int rot) {
            int[][] shape = TETROMINOES[type][rot % TETROMINOES[type].length];
            for (int r = 0; r < shape.length; r++) {
                for (int c = 0; c < shape[r].length; c++) {
                    if (shape[r][c] != 0) {
                        int boardX = x + c;
                        int boardY = y + r;

                        if (boardX < 0 || boardX >= 9 || boardY < 0 || boardY >= 5) {
                            return false;
                        }
                        if (grid[boardY][boardX] != 0) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        public void tick() {
            if (canFit(curX, curY + 1, currentType, currentRot)) {
                curY++;
            } else {
                lockPiece();
                checkLines();
                spawnPiece();
            }
            render();
        }

        private void lockPiece() {
            int[][] shape = TETROMINOES[currentType][currentRot % TETROMINOES[currentType].length];
            for (int r = 0; r < shape.length; r++) {
                for (int c = 0; c < shape[r].length; c++) {
                    if (shape[r][c] != 0) {
                        int boardX = curX + c;
                        int boardY = curY + r;
                        if (boardY >= 0 && boardY < 5 && boardX >= 0 && boardX < 9) {
                            grid[boardY][boardX] = currentType + 1;
                        }
                    }
                }
            }
        }

        public void moveLeft() {
            if (gameOver) return;
            if (canFit(curX - 1, curY, currentType, currentRot)) {
                curX--;
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_SNARE, 0.3f, 1.2f);
                render();
            }
        }

        public void moveRight() {
            if (gameOver) return;
            if (canFit(curX + 1, curY, currentType, currentRot)) {
                curX++;
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_SNARE, 0.3f, 1.2f);
                render();
            }
        }

        public void rotate() {
            if (gameOver) return;
            int nextRot = (currentRot + 1) % TETROMINOES[currentType].length;
            if (canFit(curX, curY, currentType, nextRot)) {
                currentRot = nextRot;
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_HAT, 0.4f, 1.5f);
                render();
            }
        }

        public void dropFast() {
            if (gameOver) return;
            while (canFit(curX, curY + 1, currentType, currentRot)) {
                curY++;
            }
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BASEDRUM, 0.5f, 0.8f);
            tick();
        }

        private void checkLines() {
            for (int r = 0; r < 5; r++) {
                boolean full = true;
                for (int c = 0; c < 9; c++) {
                    if (grid[r][c] == 0) {
                        full = false;
                        break;
                    }
                }
                if (full) {
                    score += 100;
                    player.sendMessage(ChatColor.GREEN + "[Тетрис] Линия собрана! +100 очков");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.5f);
                    for (int row = r; row > 0; row--) {
                        System.arraycopy(grid[row - 1], 0, grid[row], 0, 9);
                    }
                    Arrays.fill(grid[0], 0);
                }
            }
        }

        public void render() {
            int[][] display = new int[5][9];
            for (int r = 0; r < 5; r++) {
                System.arraycopy(grid[r], 0, display[r], 0, 9);
            }

            if (!gameOver) {
                int[][] shape = TETROMINOES[currentType][currentRot % TETROMINOES[currentType].length];
                for (int r = 0; r < shape.length; r++) {
                    for (int c = 0; c < shape[r].length; c++) {
                        if (shape[r][c] != 0) {
                            int bx = curX + c;
                            int by = curY + r;
                            if (by >= 0 && by < 5 && bx >= 0 && bx < 9) {
                                display[by][bx] = 8;
                            }
                        }
                    }
                }
            }

            for (int r = 0; r < 5; r++) {
                for (int c = 0; c < 9; c++) {
                    int slot = r * 9 + c;
                    ItemStack item;
                    ItemMeta meta;

                    int val = display[r][c];
                    if (val == 8) {
                        item = new ItemStack(Material.GLOWSTONE);
                        meta = item.getItemMeta();
                        meta.setDisplayName(ChatColor.AQUA + "Фигура");
                    } else if (val > 0) {
                        item = new ItemStack(Material.STAINED_CLAY, 1, (short) 5);
                        meta = item.getItemMeta();
                        meta.setDisplayName(ChatColor.GOLD + "Блок");
                    } else {
                        item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
                        meta = item.getItemMeta();
                        meta.setDisplayName(" ");
                    }

                    item.setItemMeta(meta);
                    inv.setItem(slot, item);
                }
            }

            renderControls();
        }

        private void renderControls() {
            ItemStack back = new ItemStack(Material.BARRIER);
            ItemMeta bMeta = back.getItemMeta();
            bMeta.setDisplayName(ChatColor.RED + "<< Выход в меню");
            back.setItemMeta(bMeta);
            inv.setItem(45, back);

            ItemStack restart = new ItemStack(Material.REDSTONE);
            ItemMeta rMeta = restart.getItemMeta();
            rMeta.setDisplayName(ChatColor.YELLOW + "Счёт: " + score + " (Ресет)");
            restart.setItemMeta(rMeta);
            inv.setItem(46, restart);


            // Панель управления впритык (50-53)
            inv.setItem(48, createBtn("⟳ Поворот"));
            inv.setItem(50, createBtn("> Вправо"));
            inv.setItem(49, createBtn("< Влево"));
            inv.setItem(51, createBtn("v Вниз"));
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
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

public class FlappyApp implements Listener {

    private final IcraftPlugins plugin;
    private final String guiTitle = ChatColor.GOLD + "Flappy Bird";
    private final Map<UUID, FlappyGame> activeGames = new HashMap<>();

    public FlappyApp(IcraftPlugins plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public String getTitle() {
        return guiTitle;
    }

    public void open(Player player) {
        stopGame(player);

        Inventory inv = Bukkit.createInventory(null, 54, guiTitle);
        FlappyGame game = new FlappyGame(player, inv);
        activeGames.put(player.getUniqueId(), game);

        player.openInventory(inv);
        game.start();
    }

    public void stopGame(Player player) {
        FlappyGame game = activeGames.remove(player.getUniqueId());
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

        if (slot == 45) {
            stopGame(player);
            plugin.openGamesMenu(player);
            return;
        }

        FlappyGame game = activeGames.get(player.getUniqueId());
        if (game == null || game.isGameOver()) return;

        if (slot < 54) {
            game.jump();
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

    private class FlappyGame extends BukkitRunnable {
        private final Player player;
        private final Inventory inv;

        private double birdRow = 2.0;
        private double velocity = 0.0;

        private final List<Pipe> pipes = new ArrayList<>();
        private int tickCount = 0;
        private int passedPipes = 0;

        private boolean marioSpawned = false;
        private int marioCol = 8;
        private int marioRow = 2;
        private int fireBallCol = -1;
        private int fireBallRow = 2;
        private boolean gameOver = false;
        private boolean win = false;

        public FlappyGame(Player player, Inventory inv) {
            this.player = player;
            this.inv = inv;
            renderControls();
        }

        public void start() {
            this.runTaskTimer(plugin, 8L, 8L);
        }

        public void jump() {
            if (!gameOver && !win) {
                velocity = -0.85;
            }
        }

        public boolean isGameOver() {
            return gameOver || win;
        }

        @Override
        public void run() {
            if (gameOver || win) return;

            tickCount++;
            velocity += 0.4;
            birdRow += velocity;

            if (birdRow < 0 || birdRow >= 5) {
                endGame(false);
                return;
            }

            if (passedPipes < 30 && tickCount % 4 == 0) {
                if (passedPipes == 29 && pipes.isEmpty()) {
                    marioSpawned = true;
                    marioCol = 8;
                    marioRow = 2;
                } else {
                    Random rand = new Random();
                    int gapTop = rand.nextInt(3);
                    pipes.add(new Pipe(8, gapTop));
                }
            }

            Iterator<Pipe> iterator = pipes.iterator();
            while (iterator.hasNext()) {
                Pipe pipe = iterator.next();
                pipe.col--;
                if (pipe.col < 0) {
                    iterator.remove();
                    passedPipes++;
                }
            }

            if (marioSpawned && !win) {
                if (tickCount % 2 == 0 && marioCol > 4) {
                    marioCol--;
                }
                if (fireBallCol == -1 && marioCol <= 6) {
                    fireBallCol = marioCol - 1;
                    fireBallRow = marioRow;
                }
                if (fireBallCol != -1) {
                    fireBallCol--;
                    int bRowInt = (int) Math.round(birdRow);
                    if (fireBallCol == 2 && bRowInt == fireBallRow) {
                        endGame(false);
                        return;
                    }
                    if (fireBallCol < 0) {
                        fireBallCol = -1;
                    }
                }

                if (marioCol <= 1) {
                    endGame(true); // Теперь победа будет работать корректно!
                    return;
                }
            }

            int birdRowInt = (int) Math.round(birdRow);
            int birdCol = 2;

            for (Pipe pipe : pipes) {
                if (pipe.col == birdCol) {
                    if (birdRowInt < pipe.gapTop || birdRowInt > pipe.gapTop + 1) {
                        endGame(false);
                        return;
                    }
                }
            }

            render();
        }

        private void endGame(boolean victory) {
            gameOver = true;
            cancel();

            if (victory) {
                win = true;
                ItemStack winItem = new ItemStack(Material.GOLD_BLOCK);
                ItemMeta meta = winItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.GOLD + "ПОБЕДА НАД МАРИО!");
                    winItem.setItemMeta(meta);
                }

                for (int i = 0; i < 45; i++) {
                    inv.setItem(i, winItem);
                }

                player.getInventory().addItem(new ItemStack(Material.DIAMOND, 3));
                player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 10));
                player.sendMessage(ChatColor.GOLD + "[Flappy Bird] Победа! 3 алмаза и 10 золота зачислены в инвентарь!");
            } else {
                player.sendMessage(ChatColor.RED + "[Flappy Bird] Игра окончена! Труб пройдено: " + passedPipes + "/30");
            }
        }

        private void render() {
            ItemStack sky = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 3);
            ItemMeta skyMeta = sky.getItemMeta();
            skyMeta.setDisplayName(" ");
            sky.setItemMeta(skyMeta);

            for (int i = 0; i < 45; i++) {
                inv.setItem(i, sky);
            }

            ItemStack pipeItem = new ItemStack(Material.CACTUS);
            ItemMeta pipeMeta = pipeItem.getItemMeta();
            pipeMeta.setDisplayName(ChatColor.DARK_GREEN + "Труба (Кактус)");
            pipeItem.setItemMeta(pipeMeta);

            for (Pipe pipe : pipes) {
                if (pipe.col >= 0 && pipe.col < 9) {
                    for (int r = 0; r < pipe.gapTop; r++) {
                        inv.setItem(r * 9 + pipe.col, pipeItem);
                    }
                    for (int r = pipe.gapTop + 2; r < 5; r++) {
                        inv.setItem(r * 9 + pipe.col, pipeItem);
                    }
                }
            }

            if (marioSpawned) {
                ItemStack marioItem = new ItemStack(Material.WOOL, 1, (short) 14);
                ItemMeta mMeta = marioItem.getItemMeta();
                mMeta.setDisplayName(ChatColor.RED + "Босс Марио");
                marioItem.setItemMeta(mMeta);

                if (marioCol >= 0 && marioCol < 9) {
                    inv.setItem(marioRow * 9 + marioCol, marioItem);
                    if (marioRow + 1 < 5) {
                        inv.setItem((marioRow + 1) * 9 + marioCol, marioItem);
                    }
                }

                if (fireBallCol >= 0 && fireBallCol < 9) {
                    ItemStack fireItem = new ItemStack(Material.MAGMA_CREAM);
                    ItemMeta fMeta = fireItem.getItemMeta();
                    fMeta.setDisplayName(ChatColor.RED + "Огненный шар!");
                    fireItem.setItemMeta(fMeta);
                    inv.setItem(fireBallRow * 9 + fireBallCol, fireItem);
                }
            }

            int birdRowInt = Math.max(0, Math.min(4, (int) Math.round(birdRow)));
            ItemStack birdItem = new ItemStack(Material.FEATHER);
            ItemMeta birdMeta = birdItem.getItemMeta();
            birdMeta.setDisplayName(ChatColor.YELLOW + "Flappy Bird | Труб: " + passedPipes + "/30");
            birdItem.setItemMeta(birdMeta);
            inv.setItem(birdRowInt * 9 + 2, birdItem);
        }

        private void renderControls() {
            ItemStack back = new ItemStack(Material.BARRIER);
            ItemMeta bMeta = back.getItemMeta();
            bMeta.setDisplayName(ChatColor.RED + "<< Назад в меню игр");
            back.setItemMeta(bMeta);
            inv.setItem(45, back);

            ItemStack jumpBtn = new ItemStack(Material.SLIME_BALL);
            ItemMeta jMeta = jumpBtn.getItemMeta();
            jMeta.setDisplayName(ChatColor.GREEN + "КЛИКНИ ДЛЯ ПРЫЖКА");
            jumpBtn.setItemMeta(jMeta);

            for (int i = 46; i < 54; i++) {
                inv.setItem(i, jumpBtn);
            }
        }
    }

    private static class Pipe {
        int col;
        int gapTop;

        public Pipe(int col, int gapTop) {
            this.col = col;
            this.gapTop = gapTop;
        }
    }
}
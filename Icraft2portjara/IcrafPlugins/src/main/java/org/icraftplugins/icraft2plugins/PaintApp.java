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

public class PaintApp {

    private final IcraftPlugins plugin;
    private final String canvasTitle = ChatColor.DARK_GREEN + "Paint - Холст";
    private final String paletteTitle = ChatColor.DARK_AQUA + "Paint - Выбор цвета (Стр. ";

    private final Map<UUID, PaintSession> sessions = new HashMap<>();

    public PaintApp(IcraftPlugins plugin) {
        this.plugin = plugin;
    }

    public String getCanvasTitle() {
        return canvasTitle;
    }

    public boolean isPaletteTitle(String title) {
        return title != null && title.startsWith(ChatColor.DARK_AQUA + "Paint - Выбор цвета");
    }

    public void open(Player player) {
        PaintSession session = sessions.computeIfAbsent(player.getUniqueId(), k -> new PaintSession(player));
        player.openInventory(session.canvasInv);
        session.renderCanvas();
    }

    public void handleCanvasClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        PaintSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        if (slot >= 45 && slot < 54) {
            if (slot == 45) {
                player.closeInventory();
                plugin.openPhoneMenu(player);
            } else if (slot == 47) {
                session.openPalette(0);
            } else if (slot == 49) {
                session.toggleTool();
            } else if (slot == 51) {
                session.clearCanvas();
            }
            return;
        }

        if (slot >= 0 && slot < 45) {
            session.handleCanvasClick(slot);
        }
    }

    public void handlePaletteClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        PaintSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        if (slot == 45) {
            player.openInventory(session.canvasInv);
            return;
        }

        if (slot == 48 && session.palettePage > 0) {
            session.openPalette(session.palettePage - 1);
            return;
        }
        if (slot == 50 && session.palettePage < session.getMaxPages()) {
            session.openPalette(session.palettePage + 1);
            return;
        }

        if (slot >= 0 && slot < 45) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() != Material.AIR) {
                session.selectedMaterial = clicked.getType();
                session.selectedData = clicked.getDurability();
                player.sendMessage(ChatColor.GREEN + "[Paint] Выбран блок: " + clicked.getType().name() + " (Data: " + clicked.getDurability() + ")");
                player.openInventory(session.canvasInv);
                session.renderCanvas();
            }
        }
    }

    public void handleClose(Player player) {
    }

    private class PaintSession {
        private final Player player;
        private final Inventory canvasInv;

        private final Material[] canvasMaterials = new Material[45];
        private final short[] canvasData = new short[45];

        private Material selectedMaterial = Material.WOOL;
        private short selectedData = (short) 5;
        private ToolMode toolMode = ToolMode.BRUSH;

        private int palettePage = 0;
        private final List<ItemStack> allPaintableItems = new ArrayList<>();

        public PaintSession(Player player) {
            this.player = player;
            this.canvasInv = Bukkit.createInventory(null, 54, canvasTitle);

            Arrays.fill(canvasMaterials, Material.STAINED_GLASS_PANE);
            Arrays.fill(canvasData, (short) 0);

            loadPaletteItems();
        }

        private void loadPaletteItems() {
            // 1. Сначала добавляем все 16 цветов для ключевых строительных блоков
            String[] colorBlockNames = {
                    "WOOL", "STAINED_GLASS", "STAINED_GLASS_PANE",
                    "STAINED_CLAY", "CONCRETE", "CONCRETE_POWDER", "CARPET"
            };

            for (String blockName : colorBlockNames) {
                try {
                    Material mat = Material.valueOf(blockName);
                    for (short data = 0; data < 16; data++) {
                        allPaintableItems.add(new ItemStack(mat, 1, data));
                    }
                } catch (IllegalArgumentException ignored) {}
            }

            // 2. Добавляем декоративные головы (Игрок, Скелет, Зомби, Крипер, Дракон)
            try {
                Material skullMat = Material.valueOf("SKULL_ITEM");
                for (short data = 0; data < 6; data++) {
                    allPaintableItems.add(new ItemStack(skullMat, 1, data));
                }
            } catch (IllegalArgumentException ignored) {}

            // 3. Добавляем остальные безопасные блоки
            for (Material mat : Material.values()) {
                if (!mat.isBlock() || mat == Material.AIR) continue;
                String name = mat.name();
                if (name.contains("WOOL") || name.contains("GLASS") || name.contains("CLAY") ||
                        name.contains("CONCRETE") || name.contains("CARPET") || name.contains("SKULL")) {
                    continue; // уже добавили выше
                }
                allPaintableItems.add(new ItemStack(mat, 1));
            }
        }

        public int getMaxPages() {
            return Math.max(0, (allPaintableItems.size() - 1) / 45);
        }

        public void openPalette(int page) {
            this.palettePage = Math.max(0, Math.min(page, getMaxPages()));
            Inventory paletteInv = Bukkit.createInventory(null, 54, paletteTitle + (palettePage + 1) + ")");

            int start = palettePage * 45;
            int end = Math.min(start + 45, allPaintableItems.size());

            for (int i = start; i < end; i++) {
                paletteInv.setItem(i - start, allPaintableItems.get(i));
            }

            ItemStack back = new ItemStack(Material.BARRIER);
            ItemMeta bMeta = back.getItemMeta();
            bMeta.setDisplayName(ChatColor.RED + "Назад к холсту");
            back.setItemMeta(bMeta);
            paletteInv.setItem(45, back);

            if (palettePage > 0) {
                ItemStack prev = new ItemStack(Material.ARROW);
                ItemMeta pMeta = prev.getItemMeta();
                pMeta.setDisplayName(ChatColor.YELLOW + "Предыдущая страница");
                prev.setItemMeta(pMeta);
                paletteInv.setItem(48, prev);
            }

            if (palettePage < getMaxPages()) {
                ItemStack next = new ItemStack(Material.ARROW);
                ItemMeta nMeta = next.getItemMeta();
                nMeta.setDisplayName(ChatColor.YELLOW + "Следующая страница");
                next.setItemMeta(nMeta);
                paletteInv.setItem(50, next);
            }

            player.openInventory(paletteInv);
        }

        public void toggleTool() {
            if (toolMode == ToolMode.BRUSH) {
                toolMode = ToolMode.FILL;
                player.sendMessage(ChatColor.YELLOW + "[Paint] Инструмент: Заливка");
            } else if (toolMode == ToolMode.FILL) {
                toolMode = ToolMode.ERASER;
                player.sendMessage(ChatColor.LIGHT_PURPLE + "[Paint] Инструмент: Ластик");
            } else {
                toolMode = ToolMode.BRUSH;
                player.sendMessage(ChatColor.GREEN + "[Paint] Инструмент: Кисть");
            }
            renderCanvas();
        }

        public void clearCanvas() {
            Arrays.fill(canvasMaterials, Material.STAINED_GLASS_PANE);
            Arrays.fill(canvasData, (short) 0);
            player.sendMessage(ChatColor.RED + "[Paint] Холст полностью очищен!");
            renderCanvas();
        }

        public void handleCanvasClick(int slot) {
            if (toolMode == ToolMode.BRUSH) {
                canvasMaterials[slot] = selectedMaterial;
                canvasData[slot] = selectedData;
            } else if (toolMode == ToolMode.ERASER) {
                canvasMaterials[slot] = Material.STAINED_GLASS_PANE;
                canvasData[slot] = (short) 0;
            } else if (toolMode == ToolMode.FILL) {
                floodFill(slot, canvasMaterials[slot], canvasData[slot], selectedMaterial, selectedData);
            }
            renderCanvas();
        }

        private void floodFill(int slot, Material targetMat, short targetData, Material replacementMat, short replacementData) {
            if (targetMat == replacementMat && targetData == replacementData) return;
            if (canvasMaterials[slot] != targetMat || canvasData[slot] != targetData) return;

            Queue<Integer> queue = new LinkedList<>();
            queue.add(slot);

            while (!queue.isEmpty()) {
                int curr = queue.poll();
                int r = curr / 9;
                int c = curr % 9;

                if (canvasMaterials[curr] == targetMat && canvasData[curr] == targetData) {
                    canvasMaterials[curr] = replacementMat;
                    canvasData[curr] = replacementData;

                    if (r > 0) queue.add((r - 1) * 9 + c);
                    if (r < 4) queue.add((r + 1) * 9 + c);
                    if (c > 0) queue.add(r * 9 + (c - 1));
                    if (c < 8) queue.add(r * 9 + (c + 1));
                }
            }
        }

        public void renderCanvas() {
            for (int i = 0; i < 45; i++) {
                ItemStack pixel = new ItemStack(canvasMaterials[i], 1, canvasData[i]);
                ItemMeta meta = pixel.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.WHITE + "Пиксель " + (i + 1));
                    pixel.setItemMeta(meta);
                }
                canvasInv.setItem(i, pixel);
            }

            ItemStack backBtn = new ItemStack(Material.BARRIER);
            ItemMeta bMeta = backBtn.getItemMeta();
            bMeta.setDisplayName(ChatColor.RED + "Выход в меню");
            backBtn.setItemMeta(bMeta);
            canvasInv.setItem(45, backBtn);

            ItemStack colorBtn = new ItemStack(selectedMaterial, 1, selectedData);
            ItemMeta cMeta = colorBtn.getItemMeta();
            cMeta.setDisplayName(ChatColor.AQUA + "Выбрать цвет / блок");
            List<String> cLore = new ArrayList<>();
            cLore.add(ChatColor.GRAY + "Текущий: " + selectedMaterial.name() + " (Data: " + selectedData + ")");
            cMeta.setLore(cLore);
            colorBtn.setItemMeta(cMeta);
            canvasInv.setItem(47, colorBtn);

            Material toolIconMat = Material.FEATHER;
            String toolName = "Кисть";
            if (toolMode == ToolMode.FILL) {
                toolIconMat = Material.WATER_BUCKET;
                toolName = "Заливка";
            } else if (toolMode == ToolMode.ERASER) {
                toolIconMat = Material.SPONGE;
                toolName = "Ластик";
            }

            ItemStack toolBtn = new ItemStack(toolIconMat);
            ItemMeta tMeta = toolBtn.getItemMeta();
            tMeta.setDisplayName(ChatColor.GREEN + "Инструмент: " + toolName);
            toolBtn.setItemMeta(tMeta);
            canvasInv.setItem(49, toolBtn);

            ItemStack clearBtn = new ItemStack(Material.LAVA_BUCKET);
            ItemMeta clMeta = clearBtn.getItemMeta();
            clMeta.setDisplayName(ChatColor.GOLD + "Очистить холст");
            clearBtn.setItemMeta(clMeta);
            canvasInv.setItem(51, clearBtn);
        }
    }

    private enum ToolMode {
        BRUSH, FILL, ERASER
    }
}
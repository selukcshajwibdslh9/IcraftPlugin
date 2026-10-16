package org.icraftplugins.icraft2plugins;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class ParachuteDelivery {

    private static class BlockOffset {
        int x, y, z;
        Material material;
        byte data;

        BlockOffset(int x, int y, int z, Material material, int data) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.material = material;
            this.data = (byte) data;
        }
    }

    public static void startDelivery(IcraftPlugins plugin, Player player, List<ItemStack> itemsToDeliver) {
        Location startLoc = player.getLocation().clone().add(0, 30, 0);
        boolean isDoubleChest = plugin.getConfig().getBoolean("ibuy.chest_double", false);

        player.sendMessage("§a[iBuy] §eЗаказ принят! Грузовой парашют с посылкой вылетел...");

        List<BlockOffset> structure = new ArrayList<>();

        // 1. Сундук в центре
        structure.add(new BlockOffset(0, 0, 0, Material.CHEST, 0));
        if (isDoubleChest) {
            structure.add(new BlockOffset(1, 0, 0, Material.CHEST, 0));
        }

        // 2. Корзина и стропы
        structure.add(new BlockOffset(0, 0, 1, Material.FENCE, 0));
        structure.add(new BlockOffset(0, 0, -1, Material.FENCE, 0));
        structure.add(new BlockOffset(-1, 0, 0, Material.FENCE, 0));
        structure.add(new BlockOffset(isDoubleChest ? 2 : 1, 0, 0, Material.FENCE, 0));

        // Паутина (стропы)
        structure.add(new BlockOffset(0, 1, 1, Material.WEB, 0));
        structure.add(new BlockOffset(0, 1, -1, Material.WEB, 0));
        structure.add(new BlockOffset(-1, 1, 0, Material.WEB, 0));
        structure.add(new BlockOffset(isDoubleChest ? 2 : 1, 1, 0, Material.WEB, 0));

        // 3. Купол парашюта из шерсти
        for (int x = -2; x <= (isDoubleChest ? 3 : 2); x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) >= 2 && Math.abs(z) >= 2) continue;
                structure.add(new BlockOffset(x, 2, z, Material.WOOL, 0));
            }
        }

        for (int x = -1; x <= (isDoubleChest ? 2 : 1); x++) {
            for (int z = -1; z <= 1; z++) {
                structure.add(new BlockOffset(x, 3, z, Material.WOOL, 0));
            }
        }

        structure.add(new BlockOffset(0, 4, 0, Material.WOOL, 0));

        new BukkitRunnable() {
            Location currentLoc = startLoc.clone();
            List<FallingBlock> activeEntities = new ArrayList<>();

            @Override
            public void run() {
                Block blockUnder = currentLoc.clone().subtract(0, 1, 0).getBlock();

                // Проверка приземления (если под нами твердый блок или долетели до игрока)
                if (blockUnder.getType().isSolid() || currentLoc.getY() <= player.getLocation().getY()) {
                    cleanupEntities(activeEntities);

                    Block targetBlock = currentLoc.getBlock();
                    // Безопасная проверка воздуха для 1.12.2 через getType() != AIR
                    if (targetBlock.getType() != Material.AIR && targetBlock.getType() != Material.WATER && targetBlock.getType() != Material.STATIONARY_WATER) {
                        currentLoc.add(0, 1, 0);
                    }

                    Block block1 = currentLoc.getBlock();
                    block1.setType(Material.CHEST);

                    if (isDoubleChest) {
                        Location loc2 = currentLoc.clone().add(1, 0, 0);
                        Block block2 = loc2.getBlock();
                        block2.setType(Material.CHEST);
                    }

                    if (block1.getState() instanceof Chest) {
                        Chest chest = (Chest) block1.getState();
                        for (ItemStack item : itemsToDeliver) {
                            chest.getInventory().addItem(item);
                        }
                    }

                    player.sendMessage("§a[iBuy] §2Посылка успешно приземлилась! Забирай свой набор из сундука.");
                    cancel();
                    return;
                }

                cleanupEntities(activeEntities);

                for (BlockOffset offset : structure) {
                    Location spawnLoc = currentLoc.clone().add(offset.x, offset.y, offset.z);
                    @SuppressWarnings("deprecation")
                    FallingBlock fb = spawnLoc.getWorld().spawnFallingBlock(spawnLoc, offset.material.getId(), offset.data);
                    fb.setDropItem(false);
                    activeEntities.add(fb);
                }

                currentLoc.subtract(0, 1, 0);
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    private static void cleanupEntities(List<FallingBlock> entities) {
        for (FallingBlock fb : entities) {
            if (fb != null && !fb.isDead()) {
                fb.remove();
            }
        }
        entities.clear();
    }
}
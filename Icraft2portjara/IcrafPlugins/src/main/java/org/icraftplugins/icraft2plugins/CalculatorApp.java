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

public class CalculatorApp implements PhoneApp {

    private final IcraftPlugins plugin;
    private final String guiTitle = ChatColor.DARK_PURPLE + "Калькулятор";
    private final Map<UUID, String> calcInputs = new HashMap<>();

    public CalculatorApp(IcraftPlugins plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getTitle() {
        return guiTitle;
    }

    @Override
    public void open(Player player) {
        Inventory calcGui = Bukkit.createInventory(null, 54, guiTitle);
        calcInputs.putIfAbsent(player.getUniqueId(), "0");
        updateView(calcGui, player.getUniqueId());
        player.openInventory(calcGui);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();
        String currentInput = calcInputs.getOrDefault(uuid, "0");
        int slot = event.getRawSlot();

        String pressed = "";

        switch (slot) {
            case 1:  // Кнопка выхода в меню телефона
                plugin.openPhoneMenu(player);
                return;
            case 0:  // Сброс (C)
                calcInputs.put(uuid, "0");
                updateView(event.getInventory(), uuid);
                return;
            case 3:  pressed = "1"; break;
            case 4:  pressed = "2"; break;
            case 5:  pressed = "3"; break;
            case 8:  pressed = "+"; break;
            case 12: pressed = "4"; break;
            case 13: pressed = "5"; break;
            case 14: pressed = "6"; break;
            case 17: pressed = "-"; break;
            case 18: // Равно (=)
                try {
                    String answer = evaluateExpression(currentInput);
                    calcInputs.put(uuid, answer);
                    player.sendMessage(ChatColor.GREEN + "[Калькулятор] " + currentInput + " = " + answer);
                } catch (Exception e) {
                    calcInputs.put(uuid, "Ошибка");
                    player.sendMessage(ChatColor.RED + "[Калькулятор] Ошибка расчета!");
                }
                updateView(event.getInventory(), uuid);
                return;
            case 21: pressed = "7"; break;
            case 22: pressed = "8"; break;
            case 23: pressed = "9"; break;
            case 26: pressed = "*"; break;
            case 31: pressed = "0"; break;
            case 32: pressed = "."; break;
            case 35: pressed = "/"; break;
            default:
                return;
        }

        if (!pressed.isEmpty()) {
            if (currentInput.equals("0") || currentInput.equals("Ошибка")) {
                calcInputs.put(uuid, pressed);
            } else {
                calcInputs.put(uuid, currentInput + pressed);
            }
            updateView(event.getInventory(), uuid);
        }
    }

    private void updateView(Inventory gui, UUID playerId) {
        String currentInput = calcInputs.getOrDefault(playerId, "0");

        for (int i = 0; i < 54; i++) {
            gui.setItem(i, null);
        }

        String displayInput = currentInput;
        if (displayInput.length() > 9) {
            displayInput = displayInput.substring(displayInput.length() - 9);
        }

        for (int i = 0; i < displayInput.length(); i++) {
            char c = displayInput.charAt(i);
            ItemStack charItem = new ItemStack(Material.PAPER, 1);
            ItemMeta meta = charItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.YELLOW + String.valueOf(c));
                charItem.setItemMeta(meta);
            }
            gui.setItem(45 + i, charItem);
        }

        setButton(gui, 1, Material.BARRIER, ChatColor.AQUA + "Выход в меню");
        setButton(gui, 0, Material.REDSTONE_BLOCK, ChatColor.DARK_RED + "Сброс (C)");
        setButton(gui, 3, Material.NAME_TAG, ChatColor.WHITE + "1");
        setButton(gui, 4, Material.NAME_TAG, ChatColor.WHITE + "2");
        setButton(gui, 5, Material.NAME_TAG, ChatColor.WHITE + "3");
        setButton(gui, 8, Material.GOLD_INGOT, ChatColor.RED + "+");
        setButton(gui, 12, Material.NAME_TAG, ChatColor.WHITE + "4");
        setButton(gui, 13, Material.NAME_TAG, ChatColor.WHITE + "5");
        setButton(gui, 14, Material.NAME_TAG, ChatColor.WHITE + "6");
        setButton(gui, 17, Material.IRON_INGOT, ChatColor.RED + "-");
        setButton(gui, 18, Material.SLIME_BALL, ChatColor.GREEN + "Равно (=)");
        setButton(gui, 21, Material.NAME_TAG, ChatColor.WHITE + "7");
        setButton(gui, 22, Material.NAME_TAG, ChatColor.WHITE + "8");
        setButton(gui, 23, Material.NAME_TAG, ChatColor.WHITE + "9");
        setButton(gui, 26, Material.DIAMOND, ChatColor.RED + "*");
        setButton(gui, 31, Material.NAME_TAG, ChatColor.WHITE + "0");
        setButton(gui, 32, Material.CLAY_BALL, ChatColor.YELLOW + ".");
        setButton(gui, 35, Material.EMERALD, ChatColor.RED + "/");
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

    private String evaluateExpression(String expr) {
        List<Double> numbers = new ArrayList<>();
        List<Character> operators = new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '+' || c == '-' || c == '*' || c == '/') {
                if (sb.length() == 0 && c == '-') {
                    sb.append(c);
                } else {
                    numbers.add(Double.parseDouble(sb.toString()));
                    sb.setLength(0);
                    operators.add(c);
                }
            } else {
                sb.append(c);
            }
        }
        if (sb.length() > 0) {
            numbers.add(Double.parseDouble(sb.toString()));
        }

        if (numbers.isEmpty()) return "0";

        for (int i = 0; i < operators.size(); i++) {
            char op = operators.get(i);
            if (op == '*' || op == '/') {
                double n1 = numbers.get(i);
                double n2 = numbers.get(i + 1);
                double res;
                if (op == '*') {
                    res = n1 * n2;
                } else {
                    if (n2 == 0) throw new ArithmeticException("Division by zero");
                    res = n1 / n2;
                }
                numbers.set(i, res);
                numbers.remove(i + 1);
                operators.remove(i);
                i--;
            }
        }

        double result = numbers.get(0);
        for (int i = 0; i < operators.size(); i++) {
            char op = operators.get(i);
            double n2 = numbers.get(i + 1);
            if (op == '+') {
                result += n2;
            } else if (op == '-') {
                result -= n2;
            }
        }

        if (result == (long) result) {
            return String.valueOf((long) result);
        } else {
            return String.valueOf(result);
        }
    }
}
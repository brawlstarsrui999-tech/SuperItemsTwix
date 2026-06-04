package ru.spawnerpickaxe;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class SpawnerPickaxe extends JavaPlugin {

    private static NamespacedKey SPAWNER_PICK_KEY;

    @Override
    public void onEnable() {
        // Регистрируем ключ для PersistentDataContainer
        SPAWNER_PICK_KEY = new NamespacedKey(this, "spawner_pickaxe");

        // Сохраняем дефолтный конфиг
        saveDefaultConfig();

        // Регистрируем слушатель событий
        getServer().getPluginManager().registerEvents(
                new SpawnerPickaxeListener(this), this
        );

        getLogger().info("SpawnerPickaxe включён!");
    }

    @Override
    public void onDisable() {
        getLogger().info("SpawnerPickaxe выключен.");
    }

    /**
     * Возвращает NamespacedKey для идентификации кирки
     */
    public static NamespacedKey getSpawnerPickKey() {
        return SPAWNER_PICK_KEY;
    }

    /**
     * Проверяет, является ли предмет киркой для спавнеров
     */
    public static boolean isSpawnerPickaxe(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_PICKAXE) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(SPAWNER_PICK_KEY, PersistentDataType.BYTE);
    }

    /**
     * Создаёт кирку для спавнеров
     */
    public ItemStack createSpawnerPickaxe() {
        ItemStack item = new ItemStack(Material.NETHERITE_PICKAXE);
        ItemMeta meta = item.getItemMeta();

        // Название из конфига
        String displayName = colorize(getConfig().getString("pickaxe.display-name",
                "&#a855f7⛏ &#d4af37Кирка Спавнера &#a855f7⛏"));
        meta.setItemName(displayName);

        // Описание из конфига
        List<String> lore = getConfig().getStringList("pickaxe.lore");
        if (lore.isEmpty()) {
            lore = List.of(
                    "c7c7cМожет сломать только спавнер!",
                    "&#ff5555⚠ Одноразовая — исчезнет после использования"
            );
        }
        meta.setLore(lore.stream().map(this::colorize).toList());

        // Делаем кирку «повреждённой» визуально (необязательно, но круто)
        // Устанавливаем PersistentData тег
        meta.getPersistentDataContainer()
                .set(SPAWNER_PICK_KEY, PersistentDataType.BYTE, (byte) 1);

        // Запрещаем починку через свойство unbreakable=false
        meta.setEnchantmentGlintOverride(false);

        item.setItemMeta(meta);
        return item;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("givespawnerpick")) return false;

        if (!sender.hasPermission("spawnerpickaxe.give")) {
            sender.sendMessage(colorize(getConfig().getString("messages.no-permission",
                    "&#ff5555У вас нет прав!")));
            return true;
        }

        Player target;
        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(colorize(getConfig().getString("messages.player-not-found",
                        "&#ff5555Игрок не найден!")));
                return true;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage("_usage: /givespawnerpick [player]");
            return true;
        }

        ItemStack pickaxe = createSpawnerPickaxe();
        target.getInventory().addItem(pickaxe);

        String msg = colorize(getConfig().getString("messages.pickaxe-received",
                "7ff55Вы получили Кирку Спавнера!"));
        target.sendMessage(msg);

        if (!target.equals(sender)) {
            String givenMsg = colorize(getConfig().getString("messages.pickaxe-given",
                    "7ff55Кирка выдана игроку &f%player%7ff55!"));
            sender.sendMessage(givenMsg.replace("%player%", target.getName()));
        }

        return true;
    }

    /**
     * Утилита для цветовых кодов (MiniMessage-подобные hex + стандартные &)
     */
    public String colorize(String text) {
        if (text == null) return "";
        // Обработка HEX: &#rrggbb
        text = text.replaceAll("&#([0-9a-fA-F]{6})", "§x§$1");
        // Разбиваем HEX на §x§r§r§g§g§b§b формат
        StringBuilder sb = new StringBuilder();
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '§' && i + 1 < chars.length && chars[i + 1] == 'x') {
                sb.append("§x");
                i += 2;
                // Добавляем § перед каждой цифрой hex
                for (int j = 0; j < 6 && i < chars.length; j++) {
                    sb.append('§').append(chars[i]);
                    i++;
                }
                i--; // Компенсация инкремента цикла
            } else {
                sb.append(chars[i]);
            }
        }
        // Стандартные & цветовые коды
        return sb.toString().replace("&", "§");
    }
}
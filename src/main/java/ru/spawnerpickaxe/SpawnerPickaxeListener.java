package ru.spawnerpickaxe;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.GrindstoneInventory;
import org.bukkit.inventory.meta.BlockStateMeta;

public class SpawnerPickaxeListener implements Listener {

    private final SpawnerPickaxe plugin;

    public SpawnerPickaxeListener(SpawnerPickaxe plugin) {
        this.plugin = plugin;
    }

    // ==========================================
    //   РАЗРУШЕНИЕ БЛОКОВ
    // ==========================================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        Block block = event.getBlock();

        // Проверяем, держит ли игрок кирку для спавнеров
        if (!SpawnerPickaxe.isSpawnerPickaxe(tool)) return;

        if (block.getType() == Material.SPAWNER) {
            // ——— Разрушение спавнера ———
            event.setExpToDrop(0); // Не дропаем опыт

            // Получаем тип моба из спавнера
            CreatureSpawner spawner = (CreatureSpawner) block.getState();
            String mobType = spawner.getSpawnedType().name();

            // Отменяем стандартный дроп
            event.setDropItems(false);

            // Создаём предмет спавнера с типом моба
            Location loc = block.getLocation().add(0.5, 0.5, 0.5);
            ItemStack spawnerItem = new ItemStack(Material.SPAWNER);
            if (spawnerItem.getItemMeta() instanceof BlockStateMeta bsm) {
                CreatureSpawner spawnerState = (CreatureSpawner) bsm.getBlockState();
                spawnerState.setSpawnedType(spawner.getSpawnedType());
                bsm.setBlockState(spawnerState);
                spawnerItem.setItemMeta(bsm);
            }

            // Дропаем спавнер
            block.getWorld().dropItemNaturally(loc, spawnerItem);

            // Уничтожаем кирку
            if (player.getGameMode() != GameMode.CREATIVE) {
                tool.setAmount(tool.getAmount() - 1);
            }

            // Сообщение
            String msg = plugin.colorize(
                    plugin.getConfig().getString("messages.spawner-broken",
                            "7ff55Спавнер (%type%) добыт! Кирка разрушена...")
            );
            player.sendMessage(msg.replace("%type%", mobType));

        } else {
            // ——— Попытка разрушить не-спавнер ———
            event.setCancelled(true);
            String msg = plugin.colorize(
                    plugin.getConfig().getString("messages.cannot-break",
                            "&#ff5555Эта кирка может сломать только спавнер!")
            );
            player.sendMessage(msg);
        }
    }

    // ==========================================
    //   ЗАПРЕТ ЗАЧАРОВАНИЯ (стол зачарований)
    // ==========================================

    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        if (SpawnerPickaxe.isSpawnerPickaxe(event.getItem())) {
            event.setCancelled(true);
            event.getEnchanter().sendMessage(
                    plugin.colorize(
                            plugin.getConfig().getString("messages.cannot-enchant",
                                    "&#ff5555Эту кирку нельзя зачаровать!")
                    )
            );
        }
    }

    // ==========================================
    //   ЗАПРЕТ ЗАЧАРОВАНИЯ/ПОЧИНКИ (наковальня)
    // ==========================================

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory anvil = event.getInventory();
        ItemStack first = anvil.getFirstItem();
        ItemStack second = anvil.getSecondItem();

        // Если первый слот — кирка для спавнеров
        if (SpawnerPickaxe.isSpawnerPickaxe(first)) {
            event.setResult(null); // Запрещаем результат
        }
        // Если второй слот — кирка (например, пытаются использовать как материал)
        if (SpawnerPickaxe.isSpawnerPickaxe(second)) {
            event.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnvilClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof AnvilInventory anvil)) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;

        ItemStack first = anvil.getFirstItem();
        ItemStack second = anvil.getSecondItem();

        if (SpawnerPickaxe.isSpawnerPickaxe(first)
                || SpawnerPickaxe.isSpawnerPickaxe(second)) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                player.sendMessage(
                        plugin.colorize(
                                plugin.getConfig().getString("messages.cannot-repair",
                                        "&#ff5555Эту кирку нельзя починить или зачаровать!")
                        )
                );
            }
        }
    }

    // ==========================================
    //   ЗАПРЕТ ПОЧИНКИ (точило / grindstone)
    // ==========================================

    @EventHandler
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        GrindstoneInventory grindstone = event.getInventory();
        ItemStack first = grindstone.getFirstItem();
        ItemStack second = grindstone.getSecondItem();

        if (SpawnerPickaxe.isSpawnerPickaxe(first)
                || SpawnerPickaxe.isSpawnerPickaxe(second)) {
            event.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGrindstoneClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof GrindstoneInventory grindstone)) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;

        ItemStack first = grindstone.getFirstItem();
        ItemStack second = grindstone.getSecondItem();

        if (SpawnerPickaxe.isSpawnerPickaxe(first)
                || SpawnerPickaxe.isSpawnerPickaxe(second)) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                player.sendMessage(
                        plugin.colorize(
                                plugin.getConfig().getString("messages.cannot-repair",
                                        "&#ff5555Эту кирку нельзя починить!")
                        )
                );
            }
        }
    }

    // ==========================================
    //   ЗАПРЕТ ВЕРСТАКА (на всякий случай)
    // ==========================================

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (SpawnerPickaxe.isSpawnerPickaxe(item)) {
                event.setCancelled(true);
                break;
            }
        }
    }
}
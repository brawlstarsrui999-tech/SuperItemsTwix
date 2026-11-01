package ru.spawnerpickaxe;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.EntityType;
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        Block block = event.getBlock();

        if (!SpawnerPickaxe.isSpawnerPickaxe(tool)) return;

        if (block.getType() == Material.SPAWNER) {
            event.setExpToDrop(0);
            event.setDropItems(false);

            // Получаем тип моба ДО того, как блок будет сломан
            CreatureSpawner spawner = (CreatureSpawner) block.getState();
            EntityType entityType = spawner.getSpawnedType();
            String mobType = (entityType != null) ? entityType.name() : "UNKNOWN";

            Location loc = block.getLocation().add(0.5, 0.5, 0.5);

            // ✅ FIX: Создаём предмет спавнера с типом моба в ОБОИХ форматах
            // (старый EntityId + новый SpawnData.entity), иначе в 1.21+
            // при установке блока моб пропадает.
            ItemStack spawnerItem = new ItemStack(Material.SPAWNER);
            if (spawnerItem.getItemMeta() instanceof BlockStateMeta bsm) {
                CreatureSpawner spawnerState = (CreatureSpawner) bsm.getBlockState();
                if (entityType != null) {
                    spawnerState.setSpawnedType(entityType);
                    // Главный фикс — копируем EntitySnapshot в новый NBT-формат
                    EntitySnapshot snapshot = spawner.getSpawnedEntity();
                    if (snapshot != null) {
                        spawnerState.setSpawnedEntity(snapshot);
                    }
                }
                bsm.setBlockState(spawnerState);
                spawnerItem.setItemMeta(bsm);
            }

            block.getWorld().dropItemNaturally(loc, spawnerItem);

            if (player.getGameMode() != GameMode.CREATIVE) {
                tool.setAmount(tool.getAmount() - 1);
            }

            String msg = plugin.colorize(
                    plugin.getConfig().getString(
                            "messages.spawner-broken",
                            "&#55ff55Спавнер (&#55ffff%type%&#55ff55) добыт! Кирка разрушена..."
                    )
            );
            player.sendMessage(msg.replace("%type%", mobType));

        } else {
            event.setCancelled(true);
            String msg = plugin.colorize(
                    plugin.getConfig().getString(
                            "messages.cannot-break",
                            "&#ff5555Эта кирка может сломать только спавнер!"
                    )
            );
            player.sendMessage(msg);
        }
    }

    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        if (SpawnerPickaxe.isSpawnerPickaxe(event.getItem())) {
            event.setCancelled(true);
            event.getEnchanter().sendMessage(
                    plugin.colorize(
                            plugin.getConfig().getString(
                                    "messages.cannot-enchant",
                                    "&#ff5555Эту кирку нельзя зачаровать!"
                            )
                    )
            );
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory anvil = event.getInventory();
        ItemStack first = anvil.getItem(0);
        ItemStack second = anvil.getItem(1);
        if (SpawnerPickaxe.isSpawnerPickaxe(first)) event.setResult(null);
        if (SpawnerPickaxe.isSpawnerPickaxe(second)) event.setResult(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnvilClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof AnvilInventory anvil)) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;
        ItemStack first = anvil.getItem(0);
        ItemStack second = anvil.getItem(1);
        if (SpawnerPickaxe.isSpawnerPickaxe(first) || SpawnerPickaxe.isSpawnerPickaxe(second)) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                player.sendMessage(plugin.colorize(
                        plugin.getConfig().getString(
                                "messages.cannot-repair",
                                "&#ff5555Эту кирку нельзя починить или зачаровать!"
                        )
                ));
            }
        }
    }

    @EventHandler
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        GrindstoneInventory grindstone = event.getInventory();
        ItemStack first = grindstone.getItem(0);
        ItemStack second = grindstone.getItem(1);
        if (SpawnerPickaxe.isSpawnerPickaxe(first) || SpawnerPickaxe.isSpawnerPickaxe(second)) {
            event.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGrindstoneClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof GrindstoneInventory grindstone)) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;
        ItemStack first = grindstone.getItem(0);
        ItemStack second = grindstone.getItem(1);
        if (SpawnerPickaxe.isSpawnerPickaxe(first) || SpawnerPickaxe.isSpawnerPickaxe(second)) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                player.sendMessage(plugin.colorize(
                        plugin.getConfig().getString(
                                "messages.cannot-repair",
                                "&#ff5555Эту кирку нельзя починить!"
                        )
                ));
            }
        }
    }

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
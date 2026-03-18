package ru.endlesscode.rpginventory.event.listener;

import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;

import ru.endlesscode.rpginventory.inventory.InventoryManager;
import ru.endlesscode.rpginventory.inventory.slot.Slot;
import ru.endlesscode.rpginventory.inventory.slot.SlotManager;
import ru.endlesscode.rpginventory.utils.ItemUtils;
import ru.endlesscode.rpginventory.utils.PlayerUtils;

/**
 * MMOItems QoL: shift + right click to auto-place an MMOItem into the first suitable quick slot.
 */
public class MmoItemsListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onShiftQuickPlace(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.isSneaking() || !InventoryManager.playerIsLoaded(player)) {
            return;
        }

        ItemStack item = event.getItem();
        if (ItemUtils.isEmpty(item)) {
            return;
        }

        Slot target = findFreeQuickSlot(player, item);
        if (target == null) {
            return;
        }

        int quickIndex = target.getQuickSlot();
        ItemStack existing = player.getInventory().getItem(quickIndex);
        if (ItemUtils.isNotEmpty(existing) && !target.isCup(existing)) {
            return;
        }

        player.getInventory().setItem(quickIndex, item);
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(null);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        InventoryManager.syncQuickSlots(InventoryManager.get(player));
        PlayerUtils.updateInventory(player);
        event.setUseItemInHand(Event.Result.DENY);
    }

    private Slot findFreeQuickSlot(Player player, ItemStack item) {
        for (Slot quickSlot : SlotManager.instance().getQuickSlots()) {
            if (!quickSlot.isValidItem(item)) {
                continue;
            }

            ItemStack existing = player.getInventory().getItem(quickSlot.getQuickSlot());
            if (ItemUtils.isEmpty(existing) || quickSlot.isCup(existing)) {
                return quickSlot;
            }
        }

        return null;
    }
}

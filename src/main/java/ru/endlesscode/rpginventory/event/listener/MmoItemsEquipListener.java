package ru.endlesscode.rpginventory.event.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;

import ru.endlesscode.rpginventory.inventory.InventoryManager;
import ru.endlesscode.rpginventory.item.ItemManager;
import ru.endlesscode.rpginventory.utils.ItemUtils;
import io.lumine.mythic.lib.api.item.NBTItem;

public class MmoItemsEquipListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMmoItemsEquip(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (!InventoryManager.playerIsLoaded(player)) {
            return;
        }

        if (!(event.getInventory().getHolder() instanceof ru.endlesscode.rpginventory.inventory.PlayerWrapper)) {
            return;
        }

        InventoryAction action = event.getAction();
        if (action != InventoryAction.PLACE_ALL && action != InventoryAction.PLACE_ONE &&
            action != InventoryAction.PLACE_SOME && action != InventoryAction.SWAP_WITH_CURSOR &&
            action != InventoryAction.MOVE_TO_OTHER_INVENTORY && action != InventoryAction.HOTBAR_SWAP &&
            action != InventoryAction.HOTBAR_MOVE_AND_READD) {
            return;
        }

        boolean hasMmoItem = false;

        ItemStack slotItem = event.getCurrentItem();
        if (ItemUtils.isNotEmpty(slotItem)) {
            NBTItem nbt = NBTItem.get(slotItem);
            if (nbt.hasType()) {
                hasMmoItem = true;
            }
        }

        ItemStack cursorItem = event.getCursor();
        if (!hasMmoItem && ItemUtils.isNotEmpty(cursorItem)) {
            NBTItem nbt = NBTItem.get(cursorItem);
            if (nbt.hasType()) {
                hasMmoItem = true;
            }
        }

        if (hasMmoItem) {
            ItemManager.updateStats(player);
        }
    }
}

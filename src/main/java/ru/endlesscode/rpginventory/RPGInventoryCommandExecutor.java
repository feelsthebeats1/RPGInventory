/*
 * This file is part of RPGInventory.
 * Copyright (C) 2015-2017 Osip Fatkullin
 *
 * RPGInventory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RPGInventory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RPGInventory.  If not, see <http://www.gnu.org/licenses/>.
 */

package ru.endlesscode.rpginventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import net.milkbowl.vault.permission.Permission;
import ru.endlesscode.rpginventory.api.InventoryAPI;
import ru.endlesscode.rpginventory.event.ItemCommandEvent;
import ru.endlesscode.rpginventory.inventory.InventoryManager;
import ru.endlesscode.rpginventory.inventory.backpack.Backpack;
import ru.endlesscode.rpginventory.inventory.backpack.BackpackManager;
import ru.endlesscode.rpginventory.item.ItemManager;
import ru.endlesscode.rpginventory.pet.PetManager;
import ru.endlesscode.rpginventory.utils.ItemUtils;
import ru.endlesscode.rpginventory.utils.StringUtils;

/**
 * Created by OsipXD on 28.08.2015
 * It is part of the RpgInventory.
 * All rights reserved 2014 - 2016 © «EndlessCode Group»
 */
final class RPGInventoryCommandExecutor implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            this.printHelp(sender);
            return true;
        }

        Permission perms = RPGInventory.getPermissions();
        String subCommand = args[0].toLowerCase();

        if (perms.has(sender, "rpginventory.admin")) {
            if ("openbp".equalsIgnoreCase(subCommand)) {
                this.tryToOpenBackpackAdmin(sender, args);
                return true;
            }
            switch (subCommand.charAt(0)) {
                case 'p': // pets
                    this.tryToGivePet(sender, args);
                    return true;
                case 'f': // food
                    this.tryToGiveFood(sender, args);
                    return true;
                case 'i': // items
                    this.tryToGiveItem(sender, args);
                    return true;
                case 'b': // backpacks
                    this.tryToGiveBackpack(sender, args);
                    return true;
                case 'l': // list
                    this.onCommandList(sender);
                    return true;
                case 'r': // reload
                    this.reloadPlugin(sender);
                    return true;
            }
        }

        if (subCommand.charAt(0) == 'o') { // open
            if (args.length == 1 && perms.has(sender, "rpginventory.open")) {
                this.openInventory(sender);
            } else if (args.length > 1 && perms.has(sender, "rpginventory.open.others")) {
                this.openInventory(sender, args[1]);
            } else {
                this.missingRights(sender);
            }
        } else {
            this.printHelp(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        Permission perms = RPGInventory.getPermissions();
        boolean admin = perms.has(sender, "rpginventory.admin");
        boolean canOpenOthers = perms.has(sender, "rpginventory.open.others");
        boolean canOpen = canOpenOthers || perms.has(sender, "rpginventory.open");

        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            if (canOpen) suggestions.add("open");
            if (admin) {
                Collections.addAll(suggestions, "reload", "pet", "food", "item", "bp", "backpacks", "items", "pets", "food", "list", "openbp");
            }
            return suggestions.stream()
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            if (sub.startsWith("open") && canOpenOthers) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toList());
            }
            if (admin && (sub.startsWith("pet") || sub.startsWith("food") || sub.startsWith("item") || sub.startsWith("bp") || sub.startsWith("backpack") || sub.startsWith("openbp"))) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 3 && admin) {
            switch (sub.charAt(0)) {
                case 'p': // pet
                    return PetManager.getPetList().stream()
                            .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)))
                            .collect(Collectors.toList());
                case 'f': // food
                    return PetManager.getFoodList().stream()
                            .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)))
                            .collect(Collectors.toList());
                case 'i': // item
                    return ItemManager.getItemList().stream()
                            .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)))
                            .collect(Collectors.toList());
                case 'b': // backpack
                    return BackpackManager.getBackpackList().stream()
                            .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)))
                            .collect(Collectors.toList());
                case 'o': // openbp <player> <uuid>
                    if (sub.startsWith("openbp")) {
                        return Collections.emptyList();
                    }
                default:
                    break;
            }
        }

        return Collections.emptyList();
    }

    private void tryToGivePet(CommandSender sender, String[] args) {
        if (args.length == 1) {
            this.printPetsList(sender);
        } else if (args.length >= 3 && validatePlayer(sender, args[1])) {
            this.givePet(sender, args[1], args[2]);
            return;
        }

        sender.sendMessage(StringUtils.coloredLine("&3Usage: &6/rpginv pet [&eplayer&6] [&epetId&6]"));
    }

    private void tryToGiveFood(CommandSender sender, String[] args) {
        if (args.length == 1) {
            this.printFoodList(sender);
        } else if (args.length >= 3 && validatePlayer(sender, args[1])) {
            this.giveFood(sender, args[1], args[2], args.length > 3 ? args[3] : "1");
            return;
        }

        sender.sendMessage(StringUtils.coloredLine("&3Usage: &6/rpginv food [&eplayer&6] [&efoodId&6] (&eamount&6)"));
    }

    private void tryToGiveItem(CommandSender sender, String[] args) {
        if (args.length == 1) {
            this.printItemsList(sender);
        } else if (args.length >= 3 && validatePlayer(sender, args[1])) {
            this.giveItem(sender, args[1], args[2]);
            return;
        }

        sender.sendMessage(StringUtils.coloredLine("&3Usage: &6/rpginv item [&eplayer&6] [&eitemId&6]"));
    }

    private void tryToGiveBackpack(CommandSender sender, String[] args) {
        if (args.length == 1) {
            this.printBackpacksList(sender);
        } else if (args.length >= 3 && validatePlayer(sender, args[1])) {
            this.giveBackpack(sender, args[1], args[2]);
            return;
        }

        sender.sendMessage(StringUtils.coloredLine("&3Usage: &6/rpginv bp [&eplayer&6] [&eitemId&6]"));
    }

    private void tryToOpenBackpackAdmin(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(RPGInventory.getLanguage().getMessage("backpack.admin.usage"));
            return;
        }

        if (!validatePlayer(sender, args[1])) {
            return;
        }

        Player target = RPGInventory.getInstance().getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(StringUtils.coloredLine("&cPlayer '" + args[1] + "' not found!"));
            return;
        }

        try {
            UUID uuid = UUID.fromString(args[2]);
            Backpack backpack = BackpackManager.getBackpack(uuid);
            if (backpack == null) {
                sender.sendMessage(RPGInventory.getLanguage().getMessage("backpack.not-found"));
                return;
            }
            backpack.open(target);
            sender.sendMessage(RPGInventory.getLanguage().getMessage("backpack.admin.opened", uuid, target.getName()));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(RPGInventory.getLanguage().getMessage("backpack.invalid-uuid"));
        }
    }

    private void givePet(@NotNull CommandSender sender, String playerName, String petId) {
        final Player player = RPGInventory.getInstance().getServer().getPlayer(playerName);
        final ItemStack petItem = PetManager.getPetItem(petId);
        final String prefix = "Pet '" + petId + "'";

        if (ItemUtils.isEmpty(petItem)) {
            sender.sendMessage(StringUtils.coloredLine("&c" + prefix + " not found!"));
            this.printPetsList(sender);
        } else {
            this.giveItemToPlayer(sender, player, petItem, prefix);
        }
    }

    private void giveFood(@NotNull CommandSender sender, String playerName, String foodId, @NotNull String stringAmount) {
        final Player player = RPGInventory.getInstance().getServer().getPlayer(playerName);
        final ItemStack foodItem = PetManager.getFoodItem(foodId);
        final String prefix = "Food '" + foodId + "'";

        if (ItemUtils.isEmpty(foodItem)) {
            sender.sendMessage(StringUtils.coloredLine("&c" + prefix + " not found!"));
            this.printFoodList(sender);
        } else {
            try {
                int amount = Integer.parseInt(stringAmount);
                foodItem.setAmount(amount);
                this.giveItemToPlayer(sender, player, foodItem, prefix);
            } catch (NumberFormatException e) {
                sender.sendMessage(StringUtils.coloredLine("&cThe amount must be a number!"));
            }
        }
    }

    private void giveItem(@NotNull CommandSender sender, String playerName, String itemId) {
        final Player player = RPGInventory.getInstance().getServer().getPlayer(playerName);
        final ItemStack item = ItemManager.getItem(itemId);
        final String prefix = "Item '" + itemId + "'";

        if (ItemUtils.isEmpty(item)) {
            sender.sendMessage(StringUtils.coloredLine("&c" + prefix + " not found!"));
            this.printItemsList(sender);
        } else {
            this.giveItemToPlayer(sender, player, item, prefix);
        }
    }

    private void giveBackpack(@NotNull CommandSender sender, String playerName, String id) {
        final Player player = RPGInventory.getInstance().getServer().getPlayer(playerName);
        final ItemStack bpItem = BackpackManager.getItem(id);
        final String prefix = "Backpack '" + id + "'";

        if (ItemUtils.isEmpty(bpItem)) {
            sender.sendMessage(StringUtils.coloredLine("&c" + prefix + " not found!"));
            this.printBackpacksList(sender);
        } else {
            this.giveItemToPlayer(sender, player, bpItem, prefix);
        }
    }

    private void printPetsList(@NotNull CommandSender sender) {
        printList(sender, PetManager.getPetList(), "Pets");
    }

    private void giveItemToPlayer(@NotNull CommandSender sender, Player player, ItemStack item, String prefix) {
        String message;

        ItemCommandEvent event = new ItemCommandEvent(player, item);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            message = "&cItem command was cancelled";
        } else if (player.getInventory().addItem(event.getItem()).isEmpty()) {
            message = "&3" + prefix + " has been given to " + player.getName();
        } else {
            message = "&c" + player.getName() + " has no empty slots in the inventory.";
        }
        sender.sendMessage(StringUtils.coloredLine(message));
    }

    private void printFoodList(@NotNull CommandSender sender) {
        this.printList(sender, PetManager.getFoodList(), "Food");
    }

    private void printItemsList(@NotNull CommandSender sender) {
        this.printList(sender, ItemManager.getItemList(), "Items");
    }

    private void printBackpacksList(@NotNull CommandSender sender) {
        this.printList(sender, BackpackManager.getBackpackList(), "Backpacks");
    }

    private void printList(@NotNull CommandSender sender, List<String> list, String prefix) {
        String message = String.format(list.isEmpty() ? "&c%s not found..." : "&3%s list: &6" + list, prefix);
        sender.sendMessage(StringUtils.coloredLine(message));
    }

    private void onCommandList(@NotNull CommandSender sender) {
        sender.sendMessage(StringUtils.coloredLine("&cCommand &6/rpginv list [&etype&6]&c was removed."));
        sender.sendMessage(StringUtils.coloredLine("&3Use &6/rpginv [&epets&6|&efood&6|&eitems&6|&ebackpacks&6]&3 instead."));
    }

    private void reloadPlugin(CommandSender sender) {
        sender.sendMessage(StringUtils.coloredLine("&e[RPGInventory] Reloading..."));
        RPGInventory.getInstance().reload();
        sender.sendMessage(StringUtils.coloredLine("&e[RPGInventory] Plugin successfully reloaded!"));
    }

    private void printHelp(CommandSender sender) {
        sender.sendMessage(StringUtils.coloredLine("&3===================&b[&eRPGInventory&b]&3====================="));
        sender.sendMessage(StringUtils.coloredLine("&8[] &7Required, &8() &7Optional"));

        if (RPGInventory.getPermissions().has(sender, "rpginventory.open.others")) {
            sender.sendMessage(StringUtils.coloredLine("&6rpginv open (&eplayer&6) &7- open inventory"));
        } else if (RPGInventory.getPermissions().has(sender, "rpginventory.open")) {
            sender.sendMessage(StringUtils.coloredLine("&6rpginv open &7- open inventory"));
        }

        if (RPGInventory.getPermissions().has(sender, "rpginventory.admin")) {
            sender.sendMessage(StringUtils.coloredLine("&6rpginv reload &7- reload config"));
            sender.sendMessage(StringUtils.coloredLine("&6rpginv [&epets&6|&efood&6|&eitems&6|&ebackpacks&6] &7- show list of pets, items etc."));
            sender.sendMessage(StringUtils.coloredLine("&6rpginv food [&eplayer&6] [&efoodId&6] (&eamount&6) &7- gives food to player"));
            sender.sendMessage(StringUtils.coloredLine("&6rpginv pet [&eplayer&6] [&epetId&6] &7- gives pet to player"));
            sender.sendMessage(StringUtils.coloredLine("&6rpginv item [&eplayer&6] [&eitemId&6] &7- gives item to player"));
            sender.sendMessage(StringUtils.coloredLine("&6rpginv bp [&eplayer&6] [&ebackpackId&6] &7- gives backpack to player"));
        }

        sender.sendMessage(StringUtils.coloredLine("&3====================================================="));
    }

    private void openInventory(@NotNull CommandSender sender) {
        if (!validatePlayer(sender)) {
            return;
        }

        final Player player = (Player) sender;
        if (InventoryAPI.isRPGInventory(player.getOpenInventory().getTopInventory())) {
            return;
        }

        InventoryManager.get(player).openInventory();
    }

    private void openInventory(@NotNull CommandSender sender, String playerName) {
        if (!validatePlayer(sender) || !validatePlayer(sender, playerName)) {
            return;
        }

        final Player player = RPGInventory.getInstance().getServer().getPlayer(playerName);
        assert player != null;
        ((Player) sender).openInventory(InventoryManager.get(player).getInventory());
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean validatePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(StringUtils.coloredLine("&cThis command not allowed from console."));
            return false;
        }

        return validatePlayer(sender, (Player) sender);
    }

    private boolean validatePlayer(@NotNull CommandSender sender, String playerName) {
        final Player player = RPGInventory.getInstance().getServer().getPlayer(playerName);
        if (player == null) {
            sender.sendMessage(StringUtils.coloredLine("&cPlayer '" + playerName + "' not found!"));
        }

        return validatePlayer(sender, player);
    }

    private boolean validatePlayer(@NotNull CommandSender sender, Player player) {
        if (!InventoryManager.playerIsLoaded(player)) {
            sender.sendMessage(StringUtils.coloredLine("&cThis command not allowed here."));
            return false;
        }
        return true;
    }

    private void missingRights(CommandSender sender) {
        sender.sendMessage(RPGInventory.getLanguage().getMessage("message.perms"));
        this.printHelp(sender);
    }
}

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

package ru.endlesscode.rpginventory.inventory.backpack;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import ru.endlesscode.rpginventory.RPGInventory;
import ru.endlesscode.rpginventory.item.Texture;
import ru.endlesscode.rpginventory.item.TexturedItem;
import ru.endlesscode.rpginventory.misc.FileLanguage;
import ru.endlesscode.rpginventory.utils.ItemUtils;
import ru.endlesscode.rpginventory.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Created by OsipXD on 05.10.2015
 * It is part of the RpgInventory.
 * All rights reserved 2014 - 2016 © «EndlessCode Group»
 */
public class BackpackType extends TexturedItem {
    private final String id;
    @NotNull
    private final String name;
    @NotNull
    private final List<String> lore;
    private final int size;
    private final boolean uniqueOwner;
    private final boolean whitelistOnly;
    private final List<String> whitelist;
    private final List<String> blacklist;

    private ItemStack item;

    BackpackType(Texture texture, ConfigurationSection config) {
        super(texture);

        this.id = config.getName();
        this.name = StringUtils.coloredLine(config.getString("name", id));
        this.lore = StringUtils.coloredLines(config.getStringList("lore"));
        this.size = config.getInt("size", 56) < 56 ? config.getInt("size") : 56;
        this.uniqueOwner = config.getBoolean("unique-owner", false);
        this.whitelistOnly = config.getBoolean("whitelist-only", false);
        this.whitelist = config.getStringList("whitelist");
        this.blacklist = config.getStringList("blacklist");

        this.createItem();
    }

    private void createItem() {
        ItemStack spawnItem = this.texture.getItemStack();

        ItemMeta meta = spawnItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(this.name);

            FileLanguage lang = RPGInventory.getLanguage();
            List<String> lore = new ArrayList<>();
            lore.addAll(Arrays.asList(lang.getMessage("backpack.desc").split("\n")));
            lore.addAll(this.lore);
            lore.add(lang.getMessage("backpack.size", this.size));

            meta.setLore(lore);
            spawnItem.setItemMeta(meta);
        }

        this.item = ItemUtils.setTag(spawnItem, ItemUtils.BACKPACK_TAG, this.id);
    }

    @NotNull Backpack createBackpack(UUID uuid) {
        return new Backpack(this, uuid);
    }

    @NotNull Backpack createBackpack() {
        return new Backpack(this);
    }

    public int getSize() {
        return this.size;
    }

    @NotNull String getTitle() {
        return this.name;
    }

    public ItemStack getItem() {
        return this.item;
    }

    public String getId() {
        return this.id;
    }

    public boolean isUniqueOwner() {
        return uniqueOwner;
    }

    public boolean isAllowed(ItemStack item) {
        boolean matchesWhitelist = matchesList(this.whitelist, item);

        if (!this.whitelistOnly && matchesList(this.blacklist, item)) {
            return false;
        }

        if (this.whitelistOnly) {
            return matchesWhitelist;
        }

        if (!this.whitelist.isEmpty() && !matchesWhitelist) {
            return false;
        }
        return true;
    }

    private boolean matchesList(List<String> list, ItemStack item) {
        if (list == null || list.isEmpty()) return false;
        String materialName = item.getType().name();

        for (String entry : list) {
            String e = entry.trim();
            if (e.isEmpty()) continue;

            if (e.toLowerCase().startsWith("mmoitems:")) {
                String[] parts = e.split(":", 3);
                String type = parts.length >= 2 ? parts[1].toUpperCase() : "";
                String id = parts.length == 3 ? parts[2].toUpperCase() : null;

                io.lumine.mythic.lib.api.item.NBTItem nbt = io.lumine.mythic.lib.api.item.NBTItem.get(item);
                if (!nbt.hasType()) continue;
                String nbtType = nbt.getType() != null ? nbt.getType().toUpperCase() : "";
                String nbtId = nbt.getString("MMOITEMS_ITEM_ID") != null ? nbt.getString("MMOITEMS_ITEM_ID").toUpperCase() : "";
                if (!nbtType.equals(type)) continue;
                if (id != null && !id.equals(nbtId)) continue;
                return true;
            }

            if (materialName.equalsIgnoreCase(e)) {
                return true;
            }
        }
        return false;
    }
}

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

package ru.endlesscode.rpginventory.event.updater;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.api.stat.SharedStat;
import io.lumine.mythic.lib.api.stat.StatInstance;
import io.lumine.mythic.lib.api.stat.StatMap;
import io.lumine.mythic.lib.api.stat.modifier.StatModifier;
import io.lumine.mythic.lib.player.modifier.ModifierType;
import ru.endlesscode.inspector.bukkit.scheduler.TrackedBukkitRunnable;
import ru.endlesscode.rpginventory.api.InventoryAPI;
import ru.endlesscode.rpginventory.inventory.InventoryManager;
import ru.endlesscode.rpginventory.inventory.PlayerWrapper;
import ru.endlesscode.rpginventory.item.ItemManager;
import ru.endlesscode.rpginventory.item.ItemStat;
import ru.endlesscode.rpginventory.item.Modifier;
import ru.endlesscode.rpginventory.pet.Attributes;
import ru.endlesscode.rpginventory.utils.ItemUtils;
import io.lumine.mythic.lib.api.item.NBTItem;

import java.util.*;

/**
 * Created by OsipXD on 21.09.2015
 * It is part of the RpgInventory.
 * All rights reserved 2014 - 2016 © «EndlessCode Group»
 */
public class StatsUpdater extends TrackedBukkitRunnable {
    private final Player player;

    public StatsUpdater() {
        this.player = null;
    }

    public StatsUpdater(Player player) {
        this.player = player;
    }

    @Override
    public void run() {
        if (!InventoryManager.playerIsLoaded(this.player)) {
            return;
        }

        PlayerWrapper playerWrapper = InventoryManager.get(this.player);
        playerWrapper.updatePermissions();

        // Update speed
        AttributeInstance speedAttribute = this.player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        assert speedAttribute != null;
        AttributeModifier rpgInvModifier = null;
        for (AttributeModifier modifier : speedAttribute.getModifiers()) {
            if (modifier.getUniqueId().compareTo(Attributes.SPEED_MODIFIER_ID) == 0) {
                rpgInvModifier = modifier;
            }
        }

        if (rpgInvModifier != null) {
            speedAttribute.removeModifier(rpgInvModifier);
        }

        rpgInvModifier = new AttributeModifier(
                Attributes.SPEED_MODIFIER_ID, Attributes.SPEED_MODIFIER,
                ItemManager.getModifier(this.player, ItemStat.StatType.SPEED).getMultiplier() - 1,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1
        );

        speedAttribute.addModifier(rpgInvModifier);

        // Update info slots
        if (playerWrapper.isOpened()) {
            InventoryManager.syncInfoSlots(playerWrapper);
        }

        MythicIntergrate(this.player);

    }

    private void MythicIntergrate(Player player) {
        MMOPlayerData playerData = MMOPlayerData.get(player);
        StatMap statMap = playerData.getStatMap();
        if (statMap == null) {
            return;
        }

        final String sourceKey = "rpginventory";

        for (StatInstance instance : statMap.getInstances()) {
            instance.removeIf(key -> key.startsWith(sourceKey));
        }

        Modifier damage = ItemManager.getModifier(player, ItemStat.StatType.DAMAGE);
        Modifier handDamage = ItemManager.getModifier(player, ItemStat.StatType.HAND_DAMAGE);
        double attackFlat = damage.getBonus() + handDamage.getBonus();
        double attackRel = (damage.getMultiplier() - 1) + (handDamage.getMultiplier() - 1);
        applyStat(statMap, SharedStat.ATTACK_DAMAGE, sourceKey + ":attack", attackFlat, attackRel);

        Modifier armor = ItemManager.getModifier(player, ItemStat.StatType.ARMOR);
        applyStat(statMap, SharedStat.ARMOR, sourceKey + ":armor", armor.getBonus(), armor.getMultiplier() - 1);

        Modifier speed = ItemManager.getModifier(player, ItemStat.StatType.SPEED);
        applyStat(statMap, SharedStat.MOVEMENT_SPEED, sourceKey + ":speed", 0, speed.getMultiplier() - 1);

        Modifier jump = ItemManager.getModifier(player, ItemStat.StatType.JUMP);
        applyStat(statMap, SharedStat.JUMP_STRENGTH, sourceKey + ":jump", jump.getBonus(), jump.getMultiplier() - 1);

        Modifier atkSpeed = ItemManager.getModifier(player, ItemStat.StatType.ATTACK_SPEED);
        applyStat(statMap, SharedStat.ATTACK_SPEED, sourceKey + ":attack-speed", 0, atkSpeed.getMultiplier() - 1);

        Modifier armorToughness = ItemManager.getModifier(player, ItemStat.StatType.ARMOR_TOUGHNESS);
        applyStat(statMap, SharedStat.ARMOR_TOUGHNESS, sourceKey + ":armor-toughness", armorToughness.getBonus(),
                armorToughness.getMultiplier() - 1);

        Modifier maxHealth = ItemManager.getModifier(player, ItemStat.StatType.MAX_HEALTH);
        applyStat(statMap, SharedStat.MAX_HEALTH, sourceKey + ":max-health", maxHealth.getBonus(),
                maxHealth.getMultiplier() - 1);

        Modifier damageReduction = ItemManager.getModifier(player, ItemStat.StatType.DAMAGE_REDUCTION);
        applyStat(statMap, SharedStat.DAMAGE_REDUCTION, sourceKey + ":dmg-red", 0,
                damageReduction.getMultiplier() - 1);

        Modifier pvpDmg = ItemManager.getModifier(player, ItemStat.StatType.PVP_DAMAGE);
        applyStat(statMap, SharedStat.PVP_DAMAGE, sourceKey + ":pvp-dmg", pvpDmg.getBonus(),
                pvpDmg.getMultiplier() - 1);

        Modifier pveDmg = ItemManager.getModifier(player, ItemStat.StatType.PVE_DAMAGE);
        applyStat(statMap, SharedStat.PVE_DAMAGE, sourceKey + ":pve-dmg", pveDmg.getBonus(),
                pveDmg.getMultiplier() - 1);

        Modifier skillDmg = ItemManager.getModifier(player, ItemStat.StatType.SKILL_DAMAGE);
        applyStat(statMap, SharedStat.SKILL_DAMAGE, sourceKey + ":skill-dmg", skillDmg.getBonus(),
                skillDmg.getMultiplier() - 1);

        Modifier magicDmg = ItemManager.getModifier(player, ItemStat.StatType.MAGIC_DAMAGE);
        applyStat(statMap, SharedStat.MAGICAL_DAMAGE, sourceKey + ":magic-dmg", magicDmg.getBonus(),
                magicDmg.getMultiplier() - 1);

        Modifier magicCritChance = ItemManager.getModifier(player, ItemStat.StatType.SKILL_CRITICAL_CHANCE);
        double spellCritChance = (magicCritChance.getMultiplier() - 1) * 100;
        applyStat(statMap, SharedStat.SPELL_CRITICAL_STRIKE_CHANCE, sourceKey + ":spell-crit-chance",
                spellCritChance, 0);

        Modifier magicCritPower = ItemManager.getModifier(player, ItemStat.StatType.SKILL_CRITICAL_POWER);
        double spellCritPower = (magicCritPower.getMultiplier() - 1) * 100;
        applyStat(statMap, SharedStat.SPELL_CRITICAL_STRIKE_POWER, sourceKey + ":spell-crit-power",
                spellCritPower, 0);

        // MythicLib projectile damage uses dedicated stat; bow_damage from RPGInventory stays in vanilla handler only. yeah
        Modifier projDmg = ItemManager.getModifier(player, ItemStat.StatType.PROJECTILE_DAMAGE);
        applyStat(statMap, SharedStat.PROJECTILE_DAMAGE, sourceKey + ":proj-dmg", projDmg.getBonus(),
                projDmg.getMultiplier() - 1);

        Modifier weaponDmg = ItemManager.getModifier(player, ItemStat.StatType.WEAPON_DAMAGE);
        applyStat(statMap, SharedStat.WEAPON_DAMAGE, sourceKey + ":weapon-dmg", weaponDmg.getBonus(),
                weaponDmg.getMultiplier() - 1);

        Modifier physDmg = ItemManager.getModifier(player, ItemStat.StatType.PHYSICAL_DAMAGE);
        applyStat(statMap, SharedStat.PHYSICAL_DAMAGE, sourceKey + ":physical-dmg", physDmg.getBonus(),
                physDmg.getMultiplier() - 1);

        Modifier cdr = ItemManager.getModifier(player, ItemStat.StatType.COOLDOWN_REDUCTION);
        applyStat(statMap, SharedStat.COOLDOWN_REDUCTION, sourceKey + ":cdr", 0, cdr.getMultiplier() - 1);

        double critChance = (ItemManager.getModifier(player, ItemStat.StatType.CRIT_CHANCE).getMultiplier() - 1) * 100;
        double critPower = (ItemManager.getModifier(player, ItemStat.StatType.CRIT_DAMAGE).getMultiplier() - 1) * 100;
        applyStat(statMap, SharedStat.CRITICAL_STRIKE_CHANCE, sourceKey + ":crit-chance", critChance, 0);
        applyStat(statMap, SharedStat.CRITICAL_STRIKE_POWER, sourceKey + ":crit-power", critPower, 0);

        injectMmoItemStats(player, statMap, sourceKey);

        statMap.update(SharedStat.ATTACK_DAMAGE);
        statMap.update(SharedStat.WEAPON_DAMAGE);
        statMap.update(SharedStat.PHYSICAL_DAMAGE);
        statMap.update(SharedStat.PROJECTILE_DAMAGE);
        statMap.update(SharedStat.ARMOR);
        statMap.update(SharedStat.ARMOR_TOUGHNESS);
        statMap.update(SharedStat.MOVEMENT_SPEED);
        statMap.update(SharedStat.JUMP_STRENGTH);
        statMap.update(SharedStat.ATTACK_SPEED);
        statMap.update(SharedStat.MAX_HEALTH);
        statMap.update(SharedStat.DAMAGE_REDUCTION);
        statMap.update(SharedStat.PVP_DAMAGE);
        statMap.update(SharedStat.PVE_DAMAGE);
        statMap.update(SharedStat.SKILL_DAMAGE);
        statMap.update(SharedStat.MAGICAL_DAMAGE);
        statMap.update(SharedStat.SPELL_CRITICAL_STRIKE_CHANCE);
        statMap.update(SharedStat.SPELL_CRITICAL_STRIKE_POWER);
        statMap.update(SharedStat.ADDITIONAL_EXPERIENCE);
        statMap.update(SharedStat.COOLDOWN_REDUCTION);
        statMap.update(SharedStat.CRITICAL_STRIKE_CHANCE);
        statMap.update(SharedStat.CRITICAL_STRIKE_POWER);
    }

    private void applyStat(StatMap statMap, String stat, String key, double flatBonus, double relativeBonus) {
        StatInstance instance = statMap.getInstance(stat);
        if (instance == null) {
            return;
        }
        
        instance.removeIf(existingKey -> existingKey.startsWith(key));

        if (flatBonus != 0) {
            instance.addModifier(new StatModifier(key + ":flat", stat, flatBonus, ModifierType.FLAT));
        }
        if (relativeBonus != 0) {
            instance.addModifier(new StatModifier(key + ":rel", stat, relativeBonus, ModifierType.RELATIVE));
        }
    }

    private void injectMmoItemStats(Player player, StatMap statMap, String sourceKey) {
        Map<String, Double> bonuses = new HashMap<>();
        Set<String> availableStats = new HashSet<>();
        for (StatInstance instance : statMap.getInstances()) {
            availableStats.add(instance.getStat());
        }

        // Use identity-based tracking to avoid double-counting synced items
        Set<IdentityWrapper> seenItems = new HashSet<>();

        // First, add vanilla equipment items (these are the "real" equipped items)
        List<ItemStack> vanillaItems = new ArrayList<>();
        Collections.addAll(vanillaItems, player.getInventory().getArmorContents());
        vanillaItems.add(player.getInventory().getItemInMainHand());
        vanillaItems.add(player.getInventory().getItemInOffHand());

        for (ItemStack item : vanillaItems) {
            if (ItemUtils.isEmpty(item)) {
                continue;
            }
            seenItems.add(new IdentityWrapper(item));
            processMmoItemStats(item, bonuses, availableStats);
        }

        // Then, add custom slot items that are NOT synced to vanilla (not the same object)
        List<ItemStack> customItems = new ArrayList<>();
        customItems.addAll(InventoryAPI.getPassiveItems(player));
        customItems.addAll(InventoryAPI.getActiveItems(player));

        for (ItemStack item : customItems) {
            if (ItemUtils.isEmpty(item)) {
                continue;
            }
            // Skip if this item is already counted from vanilla equipment
            if (seenItems.contains(new IdentityWrapper(item))) {
                continue;
            }
            processMmoItemStats(item, bonuses, availableStats);
        }

        for (Map.Entry<String, Double> entry : bonuses.entrySet()) {
            StatInstance instance = statMap.getInstance(entry.getKey());
            if (instance == null) {
                continue;
            }

            String key = sourceKey + ":mmoitems:" + entry.getKey().toLowerCase();
            instance.remove(key);
            instance.addModifier(new StatModifier(key, entry.getKey(), entry.getValue(), ModifierType.FLAT));
            statMap.update(entry.getKey());
        }
    }

    private void processMmoItemStats(ItemStack item, Map<String, Double> bonuses, Set<String> availableStats) {
        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasType()) {
            return;
        }

        for (String stat : availableStats) {
            double value = nbt.getStat(stat);
            if (value != 0) {
                bonuses.merge(stat, value, Double::sum);
            }
        }
    }

    /**
     * Wrapper class for identity-based comparison of ItemStacks.
     * This ensures we don't double-count items that are synced between
     * custom slots and vanilla equipment slots.
     */
    private static class IdentityWrapper {
        private final ItemStack item;

        IdentityWrapper(ItemStack item) {
            this.item = item;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            IdentityWrapper that = (IdentityWrapper) obj;
            return this.item == that.item;  // Identity comparison, not equality
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(item);
        }
    }
}

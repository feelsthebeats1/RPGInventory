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
import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.api.stat.SharedStat;
import io.lumine.mythic.lib.api.stat.StatInstance;
import io.lumine.mythic.lib.api.stat.StatMap;
import io.lumine.mythic.lib.api.stat.modifier.StatModifier;
import io.lumine.mythic.lib.player.modifier.ModifierType;
import ru.endlesscode.inspector.bukkit.scheduler.TrackedBukkitRunnable;
import ru.endlesscode.rpginventory.api.InventoryAPI;
import ru.endlesscode.rpginventory.inventory.InventoryManager;
import ru.endlesscode.rpginventory.inventory.PlayerWrapper;
import ru.endlesscode.rpginventory.item.CustomItem;
import ru.endlesscode.rpginventory.item.ItemManager;
import ru.endlesscode.rpginventory.item.ItemStat;
import ru.endlesscode.rpginventory.pet.Attributes;
import ru.endlesscode.rpginventory.utils.InventoryUtils;
import ru.endlesscode.rpginventory.utils.ItemUtils;
import ru.endlesscode.rpginventory.utils.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by OsipXD on 21.09.2015
 * It is part of the RpgInventory.
 * All rights reserved 2014 - 2016 © «EndlessCode Group»
 */
public class StatsUpdater extends TrackedBukkitRunnable {
    private static final Map<String, String> STAT_ALIASES = new HashMap<>();

    static {
        STAT_ALIASES.put("DAMAGE", SharedStat.ATTACK_DAMAGE);
        STAT_ALIASES.put("HAND_DAMAGE", SharedStat.ATTACK_DAMAGE);
        STAT_ALIASES.put("ARMOR", SharedStat.ARMOR);
        STAT_ALIASES.put("ARMOR_TOUGHNESS", SharedStat.ARMOR_TOUGHNESS);
        STAT_ALIASES.put("JUMP", SharedStat.JUMP_STRENGTH);
        STAT_ALIASES.put("SPEED", SharedStat.MOVEMENT_SPEED);
        STAT_ALIASES.put("ATTACK_SPEED", SharedStat.ATTACK_SPEED);
        STAT_ALIASES.put("MAX_HEALTH", SharedStat.MAX_HEALTH);
        STAT_ALIASES.put("DAMAGE_REDUCTION", SharedStat.DAMAGE_REDUCTION);
        STAT_ALIASES.put("PVP_DAMAGE", SharedStat.PVP_DAMAGE);
        STAT_ALIASES.put("PVE_DAMAGE", SharedStat.PVE_DAMAGE);
        STAT_ALIASES.put("SKILL_DAMAGE", SharedStat.SKILL_DAMAGE);
        STAT_ALIASES.put("MAGIC_DAMAGE", SharedStat.MAGICAL_DAMAGE);
        STAT_ALIASES.put("PROJECTILE_DAMAGE", SharedStat.PROJECTILE_DAMAGE);
        STAT_ALIASES.put("WEAPON_DAMAGE", SharedStat.WEAPON_DAMAGE);
        STAT_ALIASES.put("PHYSICAL_DAMAGE", SharedStat.PHYSICAL_DAMAGE);
        STAT_ALIASES.put("ADDITIONAL_EXPERIENCE", SharedStat.ADDITIONAL_EXPERIENCE);
        STAT_ALIASES.put("COOLDOWN_REDUCTION", SharedStat.COOLDOWN_REDUCTION);
        STAT_ALIASES.put("CRIT_CHANCE", SharedStat.CRITICAL_STRIKE_CHANCE);
        STAT_ALIASES.put("CRIT_DAMAGE", SharedStat.CRITICAL_STRIKE_POWER);
        STAT_ALIASES.put("SKILL_CRITICAL_CHANCE", SharedStat.SPELL_CRITICAL_STRIKE_CHANCE);
        STAT_ALIASES.put("SKILL_CRITICAL_POWER", SharedStat.SPELL_CRITICAL_STRIKE_POWER);
    }

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
        Set<String> availableStats = collectAvailableStats(statMap);

        Set<String> touchedStats = new HashSet<>();
        int detectedStats = applyDetectedItemStats(player, statMap, sourceKey, availableStats, touchedStats);
        int detectedMmoSlotStats = applyRpgSlotsMmoItemStats(player, statMap, sourceKey, availableStats, touchedStats);

        for (String stat : touchedStats) {
            statMap.update(stat);
        }

        if (touchedStats.isEmpty() && (detectedStats > 0 || detectedMmoSlotStats > 0)) {
            Log.w("StatsUpdater detected stats but applied none for {0}. Detected custom: {1}, detected MMO-slot: {2}, available stat count: {3}",
                    player.getName(), detectedStats, detectedMmoSlotStats, availableStats.size());
        }
    }

    private boolean applyStat(StatMap statMap, String stat, String key, double flatBonus, double relativeBonus) {
        StatInstance instance = statMap.getInstance(stat);
        if (instance == null) {
            return false;
        }

        instance.removeIf(existingKey -> existingKey.startsWith(key));

        if (flatBonus != 0) {
            instance.addModifier(new StatModifier(key + ":flat", stat, flatBonus, ModifierType.FLAT));
        }
        if (relativeBonus != 0) {
            instance.addModifier(new StatModifier(key + ":rel", stat, relativeBonus, ModifierType.RELATIVE));
        }
        return flatBonus != 0 || relativeBonus != 0;
    }

    private Set<String> collectAvailableStats(StatMap statMap) {
        Set<String> availableStats = new HashSet<>();
        for (StatInstance instance : statMap.getInstances()) {
            availableStats.add(instance.getStat());
        }
        return availableStats;
    }

    private int applyDetectedItemStats(
            Player player,
            StatMap statMap,
            String sourceKey,
            Set<String> availableStats,
            Set<String> touchedStats
    ) {
        int detectedStats = 0;
        Map<String, Double> flatBonuses = new HashMap<>();
        Map<String, Double> relativeBonuses = new HashMap<>();
        List<ItemStack> effectiveItems = InventoryUtils.collectEffectiveItems(player, false);

        for (ItemStack item : effectiveItems) {
            CustomItem customItem = ItemManager.getCustomItem(item);
            if (customItem == null) {
                continue;
            }

            for (ItemStat stat : customItem.getStats()) {
                String statName = resolveStatName(stat.getTypeKey(), availableStats);
                if (statName == null) {
                    continue;
                }
                detectedStats++;

                double value = stat.getValue();
                if (stat.getOperationType() == ItemStat.OperationType.MINUS) {
                    value = -value;
                }
                
                if (isPercentAsFlatStatName(statName)) {
                    flatBonuses.merge(statName, value, Double::sum);
                } else if (isPureRelativeStatName(statName) || stat.isPercentage()) {
                    relativeBonuses.merge(statName, value / 100, Double::sum);
                } else {
                    flatBonuses.merge(statName, value, Double::sum);
                }
            }
        }

        Set<String> allTouched = new HashSet<>();
        allTouched.addAll(flatBonuses.keySet());
        allTouched.addAll(relativeBonuses.keySet());
        for (String statName : allTouched) {
            String key = sourceKey + ":detected:" + statName.toLowerCase();
            boolean applied = applyStat(
                    statMap,
                    statName,
                    key,
                    flatBonuses.getOrDefault(statName, 0D),
                    relativeBonuses.getOrDefault(statName, 0D)
            );
            if (applied) {
                touchedStats.add(statName);
            }
        }
        return detectedStats;
    }

    private int applyRpgSlotsMmoItemStats(
            Player player,
            StatMap statMap,
            String sourceKey,
            Set<String> availableStats,
            Set<String> touchedStats
    ) {
        int detectedStats = 0;
        Map<String, Double> bonuses = new HashMap<>();
        List<ItemStack> slotItems = InventoryAPI.getPassiveItems(player);
        slotItems.addAll(InventoryAPI.getActiveItems(player));

        for (ItemStack item : slotItems) {
            if (ItemUtils.isEmpty(item)) {
                continue;
            }

            NBTItem nbtItem = NBTItem.get(item);
            if (!nbtItem.hasType()) {
                continue;
            }

            for (String stat : availableStats) {
                double value = nbtItem.getStat(stat);
                if (value != 0) {
                    bonuses.merge(stat, value, Double::sum);
                    detectedStats++;
                }
            }
        }

        for (Map.Entry<String, Double> entry : bonuses.entrySet()) {
            String statName = entry.getKey();
            String key = sourceKey + ":mmo-slot:" + statName.toLowerCase();
            boolean applied = applyStat(statMap, statName, key, entry.getValue(), 0);
            if (applied) {
                touchedStats.add(statName);
            }
        }
        return detectedStats;
    }

    private boolean isPercentAsFlatStatName(String statName) {
        return statName.endsWith("_CHANCE")
                || statName.endsWith("_POWER")
                || statName.equals(SharedStat.ADDITIONAL_EXPERIENCE);
    }

    private boolean isPureRelativeStatName(String statName) {
        return statName.equals(SharedStat.MOVEMENT_SPEED)
                || statName.equals(SharedStat.ATTACK_SPEED)
                || statName.equals(SharedStat.DAMAGE_REDUCTION)
                || statName.equals(SharedStat.COOLDOWN_REDUCTION);
    }

    private String findCompatibleStatName(String statName, Set<String> availableStats) {
        if (availableStats.contains(statName)) {
            return statName;
        }

        String normalized = statName.toUpperCase().replace('-', '_').replace(' ', '_');
        if (availableStats.contains(normalized)) {
            return normalized;
        }

        String swapped = normalized.replace("MAGIC_", "MAGICAL_");
        if (availableStats.contains(swapped)) {
            return swapped;
        }

        String canonicalTarget = canonicalizeStatName(swapped);
        for (String availableStat : availableStats) {
            if (canonicalizeStatName(availableStat).equals(canonicalTarget)) {
                return availableStat;
            }
        }

        return findByContainsMatch(normalized, availableStats);
    }

    private String findByContainsMatch(String normalized, Set<String> availableStats) {
        String[] tokens = normalized.split("_");
        for (String availableStat : availableStats) {
            String canon = canonicalizeStatName(availableStat);
            int matchedTokens = 0;
            for (String token : tokens) {
                if (token.isEmpty()) {
                    continue;
                }
                if (canon.contains(token)) {
                    matchedTokens++;
                }
            }
            if (matchedTokens >= Math.max(1, tokens.length - 1)) {
                return availableStat;
            }
        }
        return null;
    }

    private String canonicalizeStatName(String statName) {
        return statName.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    private String resolveStatName(String statName, Set<String> availableStats) {
        String normalized = statName.toUpperCase().replace('-', '_').replace(' ', '_');
        String aliased = STAT_ALIASES.getOrDefault(normalized, normalized);

        String resolved = findCompatibleStatName(aliased, availableStats);
        if (resolved != null) {
            return resolved;
        }

        resolved = findCompatibleStatName(normalized, availableStats);
        if (resolved != null) {
            return resolved;
        }

        return aliased;
    }
}

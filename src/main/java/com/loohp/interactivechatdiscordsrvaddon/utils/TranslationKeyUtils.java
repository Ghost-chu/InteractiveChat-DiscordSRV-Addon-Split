/*
 * This file is part of InteractiveChatDiscordSrvAddon.
 *
 * Copyright (C) 2022. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2022. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.loohp.interactivechatdiscordsrvaddon.utils;

import com.loohp.interactivechat.InteractiveChat;
import com.loohp.interactivechat.utils.MCVersion;
import com.loohp.interactivechat.utils.NMSUtils;
import com.loohp.interactivechatdiscordsrvaddon.resources.ResourcePackType;
import com.loohp.interactivechatdiscordsrvaddon.wrappers.PatternTypeWrapper;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TropicalFish;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta.Generation;
import org.bukkit.inventory.meta.TropicalFishBucketMeta;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class TranslationKeyUtils {

    private static Method bukkitEnchantmentGetIdMethod;
    private static Class<?> nmsEnchantmentClass;
    private static Method getEnchantmentByIdMethod;
    private static Method getEnchantmentKeyMethod;
    private static Class<?> nmsMobEffectListClass;
    private static Field nmsMobEffectByIdField;
    private static Method getEffectFromIdMethod;
    private static Method getEffectKeyMethod;
    private static Class<?> craftItemStackClass;
    private static Class<?> nmsItemStackClass;
    private static Method asNMSCopyMethod;
    private static Method nmsGetItemMethod;
    private static Class<?> nmsItemRecordClass;
    private static Field nmsItemRecordTranslationKeyField;
    private static Class<?> nmsEntityTypesClass;
    private static Method getEntityKeyMethod;

    static {
        if (InteractiveChat.version.isLegacy()) {
            try {
                //noinspection JavaReflectionMemberAccess
                bukkitEnchantmentGetIdMethod = Enchantment.class.getMethod("getId");
                nmsEnchantmentClass = NMSUtils.getNMSClass("net.minecraft.server.%s.Enchantment", "net.minecraft.world.item.enchantment.Enchantment");
                if (InteractiveChat.version.isOld()) {
                    getEnchantmentByIdMethod = nmsEnchantmentClass.getMethod("getById", int.class);
                } else {
                    getEnchantmentByIdMethod = nmsEnchantmentClass.getMethod("c", int.class);
                }
                getEnchantmentKeyMethod = nmsEnchantmentClass.getMethod("a");
                nmsMobEffectListClass = NMSUtils.getNMSClass("net.minecraft.server.%s.MobEffectList", "net.minecraft.world.effect.MobEffectList");
                if (InteractiveChat.version.isOld()) {
                    nmsMobEffectByIdField = nmsMobEffectListClass.getField("byId");
                } else {
                    getEffectFromIdMethod = nmsMobEffectListClass.getMethod("fromId", int.class);
                }
                getEffectKeyMethod = nmsMobEffectListClass.getMethod("a");

                nmsItemRecordClass = NMSUtils.getNMSClass("net.minecraft.server.%s.ItemRecord", "net.minecraft.world.item.ItemRecord");
                nmsItemRecordTranslationKeyField = NMSUtils.reflectiveLookup(Field.class, () -> nmsItemRecordClass.getDeclaredField("c"), () -> nmsItemRecordClass.getDeclaredField("a"));

                nmsEntityTypesClass = NMSUtils.getNMSClass("net.minecraft.server.%s.EntityTypes");
                getEntityKeyMethod = nmsEntityTypesClass.getMethod("b", int.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                if (InteractiveChat.version.isOlderOrEqualTo(MCVersion.V1_17)) {
                    nmsMobEffectListClass = NMSUtils.getNMSClass("net.minecraft.server.%s.MobEffectList", "net.minecraft.world.effect.MobEffectList");
                    getEffectFromIdMethod = NMSUtils.reflectiveLookup(Method.class, () -> nmsMobEffectListClass.getMethod("fromId", int.class), () -> nmsMobEffectListClass.getMethod("byId", int.class));
                    if (InteractiveChat.version.isNewerOrEqualTo(MCVersion.V1_19)) {
                        getEffectKeyMethod = nmsMobEffectListClass.getMethod("d");
                    } else {
                        getEffectKeyMethod = nmsMobEffectListClass.getMethod("c");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            craftItemStackClass = NMSUtils.getNMSClass("org.bukkit.craftbukkit.%s.inventory.CraftItemStack");
            nmsItemStackClass = NMSUtils.getNMSClass("net.minecraft.server.%s.ItemStack", "net.minecraft.world.item.ItemStack");
            asNMSCopyMethod = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);
            nmsGetItemMethod = NMSUtils.reflectiveLookup(Method.class, () -> nmsItemStackClass.getMethod("getItem"), () -> nmsItemStackClass.getMethod("c"));
        } catch (SecurityException | ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    public static String getSpawnerDescription1() {
        return "block.minecraft.spawner.desc1";
    }

    public static String getSpawnerDescription2() {
        return "block.minecraft.spawner.desc2";
    }

    public static String getEntityTypeName(EntityType type) {
        return "entity." + type.getKey().getNamespace() + "." + type.getKey().getKey();
    }

    public static String getResourcePackVanillaName() {
        return "resourcePack.vanilla.name";
    }

    public static String getResourcePackVanillaDescription() {
        return "resourcePack.vanilla.description";
    }

    public static String getOldIncompatiblePack() {
        return "pack.incompatible.old";
    }

    public static String getNewIncompatiblePack() {
        return "pack.incompatible.new";
    }

    public static String getServerResourcePack() {
        return "addServer.resourcePack";
    }

    public static String getServerResourcePackType(ResourcePackType type) {
        return switch (type) {
            case BUILT_IN -> "pack.source.builtin";
            case WORLD -> "pack.source.world";
            case LOCAL -> "pack.source.local";
            case SERVER -> "pack.source.server";
        };
    }

    public static String getWorldSpecificResources() {
        return "resourcePack.server.name";
    }

    public static String getFilledMapId() {
        return "filled_map.id";
    }

    public static String getFilledMapScale() {
        return "filled_map.scale";
    }

    public static String getFilledMapLevel() {
        return "filled_map.level";
    }

    public static String getNoEffect() {
        return "effect.none";
    }

    public static String getEffect(PotionEffectType type) {
        NamespacedKey namespacedKey = type.getKey();
        return "effect." + namespacedKey.getNamespace() + "." + namespacedKey.getKey();
    }

    public static String getEffectLevel(int level) {
        return "potion.potency." + level;
    }

    public static String getPotionWhenDrunk() {
        return "potion.whenDrank";
    }

    public static String getPotionWithAmplifier() {
        return "potion.withAmplifier";
    }

    public static String getPotionWithDuration() {
        return "potion.withDuration";
    }

    public static String getEnchantment(Enchantment enchantment) {
        NamespacedKey namespacedKey = enchantment.getKey();
        return "enchantment." + namespacedKey.getNamespace() + "." + namespacedKey.getKey();
    }

    public static String getEnchantmentLevel(int level) {
        return "enchantment.level." + level;
    }

    public static String getDyeColor() {
        return "item.color";
    }

    public static String getUnbreakable() {
        return "item.unbreakable";
    }

    public static String getDurability() {
        return "item.durability";
    }

    public static String getCrossbowProjectile() {
        return "item.minecraft.crossbow.projectile";
    }

    public static String getCopyToClipboard() {
        return "chat.copy";
    }

    public static String getOpenUrl() {
        return "chat.link.open";
    }

    public static String getRocketFlightDuration() {
        return "item.minecraft.firework_rocket.flight";
    }

    public static String getLevelTranslation(int level) {
        if (level == 1) {
            return "container.enchant.level.one";
        } else {
            return "container.enchant.level.many";
        }
    }

    public static String getMusicDiscName(ItemStack disc) {
        NamespacedKey namespacedKey = disc.getType().getKey();
        return "item." + namespacedKey.getNamespace() + "." + namespacedKey.getKey() + ".desc";
    }

    public static String getDiscFragmentName(ItemStack fragment) {
        NamespacedKey namespacedKey = fragment.getType().getKey();
        return "item." + namespacedKey.getNamespace() + "." + namespacedKey.getKey() + ".desc";
    }

    public static String getBannerPatternItemName(Material material) {
        return "item.minecraft." + material.name().toLowerCase() + ".desc";
    }

    public static List<String> getTropicalFishBucketName(ItemStack bucket) {
        List<String> list = new ArrayList<>();
        if (!(bucket instanceof TropicalFishBucketMeta fishBucketMeta)) {
            return list;
        }
        if (!fishBucketMeta.hasVariant()) {
            return list;
        }
        TropicalFish.Pattern pattern = fishBucketMeta.getPattern();
        DyeColor bodyColor = fishBucketMeta.getBodyColor();
        DyeColor patternColor = fishBucketMeta.getPatternColor();
        int variance = FishUtils.calculateTropicalFishVariant(pattern, bodyColor, patternColor);
        int predefinedType = FishUtils.getPredefinedType(variance);
        if (predefinedType >= 0) {
            list.add("entity.minecraft.tropical_fish.predefined." + predefinedType);
        } else {
            DyeColor baseColor = FishUtils.getTropicalFishBaseColor(variance);
            list.add("entity.minecraft.tropical_fish.type." + FishUtils.getTropicalFishTypeName(variance));
            list.add("color.minecraft." + baseColor.toString().toLowerCase());
            if (!baseColor.equals(patternColor)) {
                list.add("color.minecraft." + patternColor.toString().toLowerCase());
            }
        }
        return list;
    }

    public static String getBannerPatternName(PatternTypeWrapper type, DyeColor color) {
        return "block.minecraft.banner." + type.getAssetName() + "." + color.name().toLowerCase();
    }

    public static String getAttributeKey(String attributeName) {
        return "attribute.name." + attributeName;
    }

    public static String getAttributeModifierKey(double amount, int operation) {
        if (amount > 0) {
            return "attribute.modifier.plus." + operation;
        } else if (amount < 0) {
            return "attribute.modifier.take." + operation;
        } else {
            return "attribute.modifier.equals." + operation;
        }
    }

    public static String getModifierSlotKey(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> "item.modifiers.head";
            case CHEST -> "item.modifiers.chest";
            case LEGS -> "item.modifiers.legs";
            case FEET -> "item.modifiers.feet";
            case HAND -> "item.modifiers.mainhand";
            case OFF_HAND -> "item.modifiers.offhand";
            default -> "item.modifiers." + slot.toString().toLowerCase();
        };
    }

    public static String getCanDestroy() {
        return "item.canBreak";
    }

    public static String getCanPlace() {
        return "item.canPlace";
    }

    public static String getBookAuthor() {
        return "book.byAuthor";
    }

    public static String getBookGeneration(Generation generation) {
        return switch (generation) {
            case COPY_OF_ORIGINAL -> "book.generation.1";
            case COPY_OF_COPY -> "book.generation.2";
            case TATTERED -> "book.generation.3";
            case ORIGINAL, default -> "book.generation.0";
        };
    }

    public static String getBookPageIndicator() {
        return "book.pageIndicator";
    }

    public static String getDefaultContainerTitle() {
        return "container.inventory";
    }

    public static String getEnderChestContainerTitle() {
        return "container.enderchest";
    }

    public static String getBundleFullness() {
        return "item.minecraft.bundle.fullness";
    }

    public static String getFireworkType(FireworkEffect.Type type) {
        return switch (type) {
            case BALL -> "item.minecraft.firework_star.shape.small_ball";
            case BALL_LARGE -> "item.minecraft.firework_star.shape.large_ball";
            case STAR -> "item.minecraft.firework_star.shape.star";
            case CREEPER -> "item.minecraft.firework_star.shape.creeper";
            case BURST -> "item.minecraft.firework_star.shape.burst";
            default -> "item.minecraft.firework_star.shape";
        };
    }

    public static String getFireworkTrail() {
        return "item.minecraft.firework_star.trail";
    }

    public static String getFireworkFlicker() {
        return "item.minecraft.firework_star.flicker";
    }

    public static String getFireworkFade() {
        return "item.minecraft.firework_star.fade_to";
    }

    public static String getFireworkColor(Color color) {
        DyeColor dyeColor = DyeColor.getByFireworkColor(color);
        if (dyeColor == null) {
            return "item.minecraft.firework_star.custom_color";
        } else {
            return "item.minecraft.firework_star." + dyeColor.name().toLowerCase();
        }
    }

    public static String getGoatHornInstrument(NamespacedKey instrument) {
        return "instrument." + instrument.getNamespace() + "." + instrument.getKey();
    }

}

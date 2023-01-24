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
import io.github.bananapuncher714.nbteditor.NBTEditor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;

public class PotionUtils {

    public static final int WATER_COLOR = 3694022;
    public static final int UNCRAFTABLE_COLOR = 16253176;

    private static Class<?> craftItemStackClass;
    private static Class<?> nmsItemStackClass;
    private static Class<?> nmsNbtTagCompoundClass;
    private static Class<?> craftPotionBrewerClass;
    private static Object craftPotionBrewerInstance;
    private static Method craftPotionBrewerGetEffectsFromDamageMethod;
    private static Class<?> nmsPotionRegistryClass;
    private static Method nmsPotionRegistryGetPotionRegistryFromStringMethod;
    private static Method nmsPotionRegistryGetMobEffectListMethod;
    private static Class<?> craftPotionUtilClass;
    private static Class<?> nmsMobEffectClass;
    private static Method craftPotionUtilToBukkitMethod;
    private static Method craftPotionUtilFromBukkitMethod;
    private static Class<?> nmsMobEffectListClass;
    private static Field nmsMobEffectListByIdArrayField;
    private static Method nmsMobEffectListByIdMethod;
    private static Field nmsMobEffectListTypeField;
    private static Field nmsMobEffectInfoChatFormatField;
    private static Method nmsMobEffectGetMobEffectListMethod;
    private static Field nmsMobEffectListAttributeMapField;
    private static Method nmsMobEffectGetAttributeModifierValueMethod;
    private static Class<?> nmsAttributeBaseClass;
    private static Method nmsAttributeBaseGetNameMethod;
    private static Class<?> nmsAttributeModifierClass;
    private static Class<?> craftAttributeInstanceClass;
    private static Method craftAttributeInstanceConvertMethod;

    static {
        try {
            craftItemStackClass = NMSUtils.getNMSClass("org.bukkit.craftbukkit.%s.inventory.CraftItemStack");
            nmsItemStackClass = NMSUtils.getNMSClass("net.minecraft.server.%s.ItemStack", "net.minecraft.world.item.ItemStack");
            nmsNbtTagCompoundClass = NMSUtils.getNMSClass("net.minecraft.server.%s.NBTTagCompound", "net.minecraft.nbt.NBTTagCompound");
            nmsMobEffectListClass = NMSUtils.getNMSClass("net.minecraft.server.%s.MobEffectList", "net.minecraft.world.effect.MobEffectList");
            nmsPotionRegistryClass = NMSUtils.getNMSClass("net.minecraft.server.%s.PotionRegistry", "net.minecraft.world.item.alchemy.PotionRegistry");
            nmsPotionRegistryGetPotionRegistryFromStringMethod = nmsPotionRegistryClass.getMethod("a", String.class);
            nmsPotionRegistryGetMobEffectListMethod = nmsPotionRegistryClass.getMethod("a");
            craftPotionUtilClass = NMSUtils.getNMSClass("org.bukkit.craftbukkit.%s.potion.CraftPotionUtil");
            nmsMobEffectClass = NMSUtils.getNMSClass("net.minecraft.server.%s.MobEffect", "net.minecraft.world.effect.MobEffect");
            craftPotionUtilToBukkitMethod = craftPotionUtilClass.getMethod("toBukkit", nmsMobEffectClass);
            craftPotionUtilFromBukkitMethod = craftPotionUtilClass.getMethod("fromBukkit", PotionEffect.class);
            if (InteractiveChat.version.isOlderOrEqualTo(MCVersion.V1_17)) {
                nmsMobEffectListByIdMethod = nmsMobEffectListClass.getMethod("fromId", int.class);
            } else {
                nmsMobEffectListByIdMethod = nmsMobEffectListClass.getMethod("a", int.class);
            }
            nmsMobEffectListTypeField = nmsMobEffectListClass.getDeclaredField("b");
            nmsMobEffectInfoChatFormatField = nmsMobEffectListTypeField.getType().getDeclaredField("d");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getPotionBaseColor(PotionType type) {
        PotionEffectType effect = type.getEffectType();
        if (effect == null) {
            if (type.name().equalsIgnoreCase("UNCRAFTABLE")) {
                return UNCRAFTABLE_COLOR;
            } else {
                return WATER_COLOR;
            }
        } else {
            return effect.getColor().asRGB();
        }
    }

    @SuppressWarnings("deprecation")
    public static List<PotionEffect> getBasePotionEffect(ItemStack potion) throws Exception {
        List<PotionEffect> effects = new ArrayList<>();
        if (!NBTEditor.contains(potion, "Potion")) {
            return null;
        }
        String pName = NBTEditor.getString(potion, "Potion");
        if (pName.contains(":")) {
            pName = pName.substring(pName.indexOf(":") + 1);
        }
        Object reg = nmsPotionRegistryGetPotionRegistryFromStringMethod.invoke(null, pName);
        if (reg == null) {
            return null;
        }
        List<PotionEffect> effects = new ArrayList<>();
        for (Object me : (List<?>) nmsPotionRegistryGetMobEffectListMethod.invoke(reg)) {
            effects.add((PotionEffect) craftPotionUtilToBukkitMethod.invoke(null, me));
        }
        return effects;
    }

    public static ChatColor getPotionEffectChatColor(PotionEffectType type) throws Exception {
        return ChatColor.of("#" + String.format("%08x", type.getColor().asRGB()).substring(2));
    }

    public static Map<String, AttributeModifier> getPotionAttributes(PotionEffect effect) {
        try {
            Map<String, AttributeModifier> attributes = new HashMap<>();
            Object nmsMobEffect = craftPotionUtilFromBukkitMethod.invoke(null, effect);
            Object nmsMobEffectList = nmsMobEffectGetMobEffectListMethod.invoke(nmsMobEffect);
            nmsMobEffectListAttributeMapField.setAccessible(true);
            for (Entry<?, ?> entry : ((Map<?, ?>) nmsMobEffectListAttributeMapField.get(nmsMobEffectList)).entrySet()) {
                String name = (String) nmsAttributeBaseGetNameMethod.invoke(entry.getKey());
                craftAttributeInstanceConvertMethod.setAccessible(true);
                AttributeModifier attributeModifier = (AttributeModifier) craftAttributeInstanceConvertMethod.invoke(null, entry.getValue());
                double leveledAmount = (double) nmsMobEffectGetAttributeModifierValueMethod.invoke(nmsMobEffectList, effect.getAmplifier(), entry.getValue());
                attributes.put(name, new AttributeModifier(attributeModifier.getUniqueId(), attributeModifier.getName(), leveledAmount, attributeModifier.getOperation(), attributeModifier.getSlot()));
            }
            return attributes;
        } catch (Throwable e) {
            return Collections.emptyMap();
        }
    }

}

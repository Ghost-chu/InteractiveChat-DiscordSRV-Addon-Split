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
import com.loohp.interactivechatdiscordsrvaddon.objectholders.BiomePrecipitation;
import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class WorldUtils {

    private static Class<?> craftWorldClass;
    private static Method getHandleMethod;
    private static Class<?> worldServerClass;
    private static Method getWorldTypeKeyMethod;
    private static Method getMinecraftKeyMethod;
    private static Method getBiomeAtMethod;
    private static Method holderGetMethod;
    private static Method getPrecipitationMethod;

    static {
        try {
            craftWorldClass = NMSUtils.getNMSClass("org.bukkit.craftbukkit.%s.CraftWorld");
            getHandleMethod = craftWorldClass.getMethod("getHandle");
            worldServerClass = getHandleMethod.getReturnType();
            getWorldTypeKeyMethod = worldServerClass.getMethod("getTypeKey");
            getMinecraftKeyMethod = getWorldTypeKeyMethod.getReturnType().getMethod("a");
            getBiomeAtMethod = worldServerClass.getMethod("a", int.class, int.class, int.class);
            Class<?> biomeBaseClass;
            if (InteractiveChat.version.isNewerOrEqualTo(MCVersion.V1_18)) {
                holderGetMethod = getBiomeAtMethod.getReturnType().getMethod("a");
                biomeBaseClass = Class.forName("net.minecraft.world.level.biome.BiomeBase");
            } else {
                biomeBaseClass = getBiomeAtMethod.getReturnType();
            }
            if (InteractiveChat.version.isOlderThan(MCVersion.V1_17)) {
                getPrecipitationMethod = biomeBaseClass.getMethod("d");
            } else {
                getPrecipitationMethod = biomeBaseClass.getMethod("c");
            }
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    public static String getNamespacedKey(World world) {
        return world.getKey().toString();
    }

    public static boolean isNatural(World world) {
        return world.isNatural();
    }

    @SuppressWarnings("deprecation")
    public static BiomePrecipitation getPrecipitation(Location location) {
        if (InteractiveChat.version.isNewerOrEqualTo(MCVersion.V1_16)) {
            try {
                Object craftWorldObject = craftWorldClass.cast(location.getWorld());
                Object nmsWorldServerObject = getHandleMethod.invoke(craftWorldObject);
                Object biomeBaseObject = getBiomeAtMethod.invoke(nmsWorldServerObject, location.getBlockX(), location.getBlockY(), location.getBlockZ());
                if (InteractiveChat.version.isNewerOrEqualTo(MCVersion.V1_18)) {
                    biomeBaseObject = holderGetMethod.invoke(biomeBaseObject);
                }
                return BiomePrecipitation.fromName(((Enum<?>) getPrecipitationMethod.invoke(biomeBaseObject)).name());
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        } else {
            double temperature = location.getWorld().getTemperature(location.getBlockX(), location.getBlockZ());
            if (temperature > 0.95) {
                return BiomePrecipitation.NONE;
            } else if (temperature < 0.15) {
                return BiomePrecipitation.SNOW;
            } else {
                return BiomePrecipitation.RAIN;
            }
        }
        return null;
    }

}


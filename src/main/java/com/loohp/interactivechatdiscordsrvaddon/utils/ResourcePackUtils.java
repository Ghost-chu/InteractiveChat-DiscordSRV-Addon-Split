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
import org.bukkit.Bukkit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class ResourcePackUtils {

    private static Class<?> craftServerClass;
    private static Method craftServerGetServerMethod;
    private static Method nmsGetResourcePackMethod;
    private static Method nmsGetResourcePackHashMethod;
    private static Class<?> nmsMinecraftVersionClass;
    private static Constructor<?> nmsMinecraftVersionConstructor;
    private static Method nmsMinecraftVersionGetPackVersionMethod;
    private static Object mojangPackTypeResourceEnumObject;

    private static Object nmsMinecraftVersionObject;

    static {
        try {
            if (InteractiveChat.version.isOlderThan(MCVersion.V1_19)) {
                craftServerClass = NMSUtils.getNMSClass("org.bukkit.craftbukkit.%s.CraftServer");
                craftServerGetServerMethod = craftServerClass.getMethod("getServer");
                nmsGetResourcePackMethod = NMSUtils.reflectiveLookup(Method.class, () -> craftServerGetServerMethod.getReturnType().getMethod("getResourcePack"), () -> craftServerGetServerMethod.getReturnType().getMethod("S"));
                nmsGetResourcePackHashMethod = NMSUtils.reflectiveLookup(Method.class, () -> craftServerGetServerMethod.getReturnType().getMethod("getResourcePackHash"), () -> craftServerGetServerMethod.getReturnType().getMethod("T"));
            }
            try {
                nmsMinecraftVersionClass = NMSUtils.getNMSClass("net.minecraft.server.%s.MinecraftVersion", "net.minecraft.MinecraftVersion");
                nmsMinecraftVersionConstructor = nmsMinecraftVersionClass.getDeclaredConstructor();
                nmsMinecraftVersionConstructor.setAccessible(true);
                nmsMinecraftVersionObject = nmsMinecraftVersionConstructor.newInstance();
                nmsMinecraftVersionGetPackVersionMethod = NMSUtils.reflectiveLookup(Method.class, () -> nmsMinecraftVersionClass.getMethod("getPackVersion"), () -> {
                    mojangPackTypeResourceEnumObject = com.mojang.bridge.game.PackType.RESOURCE;
                    return nmsMinecraftVersionClass.getMethod("getPackVersion", com.mojang.bridge.game.PackType.class);
                });
            } catch (Exception ignore) {
            }
        } catch (SecurityException | ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    public static String getServerResourcePack() {
        return Bukkit.getResourcePack();
    }

    public static String getServerResourcePackHash() {
        return Bukkit.getResourcePackHash();
    }

    public static int getServerResourcePackVersion() {
        try {
            if (InteractiveChat.version.isNewerOrEqualTo(MCVersion.V1_15)) {
                if (mojangPackTypeResourceEnumObject == null) {
                    return (int) nmsMinecraftVersionGetPackVersionMethod.invoke(nmsMinecraftVersionObject);
                } else {
                    return (int) nmsMinecraftVersionGetPackVersionMethod.invoke(nmsMinecraftVersionObject, mojangPackTypeResourceEnumObject);
                }
            } else if (InteractiveChat.version.isNewerOrEqualTo(MCVersion.V1_13)) {
                return 4;
            } else if (InteractiveChat.version.isNewerOrEqualTo(MCVersion.V1_11)) {
                return 3;
            } else if (InteractiveChat.version.isNewerOrEqualTo(MCVersion.V1_9)) {
                return 2;
            } else {
                return 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

}

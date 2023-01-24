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

package com.loohp.interactivechatdiscordsrvaddon.listeners;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.loohp.interactivechat.InteractiveChat;
import com.loohp.interactivechat.api.events.ICPlayerJoinEvent;
import com.loohp.interactivechat.api.events.OfflineICPlayerCreationEvent;
import com.loohp.interactivechat.api.events.OfflineICPlayerUpdateEvent;
import com.loohp.interactivechatdiscordsrvaddon.InteractiveChatDiscordSrvAddon;
import com.loohp.interactivechatdiscordsrvaddon.graphics.ImageUtils;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

public class ICPlayerEvents implements Listener {

    public static final String PROFILE_URL = "https://api.loohpjames.com/spigot/plugins/interactivechatdiscordsrvaddon/profile/%s";

    private static final Cache<UUID, Map<String, Object>> CACHED_PROPERTIES = CacheBuilder
            .newBuilder()
            .maximumSize(300000)
            .build();

    static {
        Bukkit.getScheduler().runTaskTimerAsynchronously(InteractiveChat.plugin, CACHED_PROPERTIES::cleanUp, 12000, 12000);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(ICPlayerJoinEvent event) {
        populate(event.getPlayer(), true);
    }

    @EventHandler
    public void onCreation(OfflineICPlayerCreationEvent event) {
        populate(event.getPlayer(), false);
    }

    @EventHandler
    public void onUpdate(OfflineICPlayerUpdateEvent event) {
        populate(event.getPlayer(), false);
    }

    private void populate(OfflinePlayer player, boolean scheduleAsync) {
        if (scheduleAsync) {
            Bukkit.getScheduler().runTaskAsynchronously(InteractiveChatDiscordSrvAddon.plugin, () -> populate(player, false));
            return;
        }
        Map<String, Object> cachedProperties = CACHED_PROPERTIES.getIfPresent(player.getUniqueId());
        if (cachedProperties == null) {
            cachedProperties = new HashMap<>();
            HttpResponse<JsonNode> json = Unirest.get(PROFILE_URL.replace("%s", player.getName())).asJson();
            if (json.isSuccess() && json.getBody().getObject().has("properties")) {
                JSONObject properties = json.getBody().getObject().getJSONObject("properties");
                for (Object obj : properties.keySet()) {
                    try {
                        String key = (String) obj;
                        String value = (String) properties.get(key);
                        if (value.endsWith(".png")) {
                            BufferedImage image = ImageUtils.downloadImage(value);
                            player.addProperties(key, image);
                            cachedProperties.put(key, image);
                        } else if (value.endsWith(".bin")) {
                            byte[] data = Unirest.get(value).asBytes().getBody();
                            player.addProperties(key, data);
                            cachedProperties.put(key, data);
                        } else {
                            player.addProperties(key, value);
                            cachedProperties.put(key, value);
                        }
                    } catch (Exception ignore) {
                    }
                }
            }
            CACHED_PROPERTIES.put(player.getUniqueId(), cachedProperties);
        } else {
            for (Entry<String, Object> entry : cachedProperties.entrySet()) {
                player.addProperties(entry.getKey(), entry.getValue());
            }
        }
    }

}

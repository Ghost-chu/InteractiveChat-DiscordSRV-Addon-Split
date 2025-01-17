/*
 * This file is part of InteractiveChatDiscordSrvAddon2.
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

import com.loohp.interactivechatdiscordsrvaddon.resources.ResourcePackInfo;
import com.loohp.interactivechatdiscordsrvaddon.resources.ResourcePackType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class ResourcePackInfoUtils {

    public static Component resolveName(ResourcePackInfo info) {
        return resolveName(info.getName(), info.getType());
    }

    public static Component resolveName(Component name, ResourcePackType type) {
        return switch (type) {
            case BUILT_IN, LOCAL -> name;
            case WORLD, SERVER -> Component.translatable(TranslationKeyUtils.getWorldSpecificResources());
        };
    }

    public static Component resolveDescription(ResourcePackInfo info) {
        return resolveDescription(info.getDescription(), info.getType());
    }

    public static Component resolveDescription(Component component, ResourcePackType type) {
        String space = PlainTextComponentSerializer.plainText().serialize(component).isEmpty() ? "" : " ";
        switch (type) {
            case BUILT_IN, WORLD, SERVER ->
                    component = component.append(Component.empty().append(Component.text(space + "(").append(Component.translatable(TranslationKeyUtils.getServerResourcePackType(type))).append(Component.text(")"))).color(NamedTextColor.GRAY));
        }
        return component;
    }

}

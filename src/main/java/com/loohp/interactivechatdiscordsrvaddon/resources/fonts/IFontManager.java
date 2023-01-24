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

package com.loohp.interactivechatdiscordsrvaddon.resources.fonts;

import com.loohp.interactivechatdiscordsrvaddon.resources.IAbstractManager;
import com.loohp.interactivechatdiscordsrvaddon.resources.textures.TextureResource;
import net.kyori.adventure.key.Key;

public interface IFontManager extends IAbstractManager {

    TextureResource getFontResource(String resourceLocation);

    Key getDefaultFontKey();

    FontProvider getFontProviders(Key resourceKey);

    FontProvider getFontProviders(String resourceLocation);

}

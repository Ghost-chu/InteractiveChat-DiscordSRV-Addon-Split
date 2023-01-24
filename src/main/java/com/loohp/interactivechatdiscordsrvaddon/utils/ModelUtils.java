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

import com.loohp.blockmodelrenderer.render.Face;
import com.loohp.blockmodelrenderer.render.Point3D;
import com.loohp.blockmodelrenderer.utils.MathUtils;
import com.loohp.interactivechatdiscordsrvaddon.graphics.ImageUtils;
import com.loohp.interactivechatdiscordsrvaddon.resources.models.ModelFace.ModelFaceSide;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public class ModelUtils {
    public static String getItemModelKey(Material icMaterial) {
        return icMaterial.name().toLowerCase();
    }

    public static BufferedImage convertToModernSkinTexture(BufferedImage skin) {
        if (skin.getWidth() == skin.getHeight()) {
            return skin;
        }
        int scale = skin.getWidth() / 64;
        BufferedImage modernSkin = new BufferedImage(skin.getWidth(), skin.getWidth(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = modernSkin.createGraphics();
        g.drawImage(skin, 0, 0, null);

        BufferedImage arm1 = ImageUtils.flipHorizontal(ImageUtils.copyAndGetSubImage(skin, 0, 20 * scale, 4 * scale, 12 * scale));
        BufferedImage arm2 = ImageUtils.flipHorizontal(ImageUtils.copyAndGetSubImage(skin, 4 * scale, 20 * scale, 4 * scale, 12 * scale));
        BufferedImage arm3 = ImageUtils.flipHorizontal(ImageUtils.copyAndGetSubImage(skin, 8 * scale, 20 * scale, 4 * scale, 12 * scale));
        BufferedImage arm4 = ImageUtils.flipHorizontal(ImageUtils.copyAndGetSubImage(skin, 12 * scale, 20 * scale, 4 * scale, 12 * scale));
        BufferedImage arm5 = ImageUtils.flipHorizontal(ImageUtils.copyAndGetSubImage(skin, 4 * scale, 16 * scale, 4 * scale, 4 * scale));
        BufferedImage arm6 = ImageUtils.flipHorizontal(ImageUtils.copyAndGetSubImage(skin, 8 * scale, 16 * scale, 4 * scale, 4 * scale));

        g.drawImage(arm1, 16 * scale, 52 * scale, null);
        g.drawImage(arm2, 20 * scale, 52 * scale, null);
        g.drawImage(arm3, 24 * scale, 52 * scale, null);
        g.drawImage(arm4, 28 * scale, 52 * scale, null);
        g.drawImage(arm5, 20 * scale, 48 * scale, null);
        g.drawImage(arm6, 24 * scale, 48 * scale, null);

        BufferedImage leg1 = ImageUtils.flipHorizontal(ImageUtils.copyAndGetSubImage(skin, 40 * scale, 20 * scale, 4 * scale, 12 * scale));
        BufferedImage leg2 = ImageUtils.flipHorizontal(ImageUtils.copyAndGetSubImage(skin, 44 * scale, 20 * scale, 4 * scale, 12 * scale));
        BufferedImage leg3 = ImageUtils.flipHorizontal(ImageUtils.copyAndGetSubImage(skin, 48 * scale, 20 * scale, 4 * scale, 12 * scale));
        BufferedImage leg4 = ImageUtils.flipHorizontal(ImageUtils.copyAndGetSubImage(skin, 52 * scale, 20 * scale, 4 * scale, 12 * scale));
        BufferedImage leg5 = ImageUtils.flipHorizontal(ImageUtils.copyAndGetSubImage(skin, 44 * scale, 16 * scale, 4 * scale, 4 * scale));
        BufferedImage leg6 = ImageUtils.flipHorizontal(ImageUtils.copyAndGetSubImage(skin, 48 * scale, 16 * scale, 4 * scale, 4 * scale));

        g.drawImage(leg1, 32 * scale, 52 * scale, null);
        g.drawImage(leg2, 36 * scale, 52 * scale, null);
        g.drawImage(leg3, 40 * scale, 52 * scale, null);
        g.drawImage(leg4, 44 * scale, 52 * scale, null);
        g.drawImage(leg5, 36 * scale, 48 * scale, null);
        g.drawImage(leg6, 40 * scale, 48 * scale, null);

        g.dispose();
        return modernSkin;
    }

    public static boolean isRenderedUpsideDown(Component component) {
        return isRenderedUpsideDown(PlainTextComponentSerializer.plainText().serialize(component));
    }

    public static boolean isRenderedUpsideDown(Component component, boolean hasCape) {
        return isRenderedUpsideDown(PlainTextComponentSerializer.plainText().serialize(component), hasCape);
    }

    public static boolean isRenderedUpsideDown(String name) {
        return isRenderedUpsideDown(name, true);
    }

    public static boolean isRenderedUpsideDown(String name, boolean hasCape) {
        return ("Dinnerbone".equals(name) || "Grumm".equals(name)) && hasCape;
    }

    public static boolean shouldTriggerCullface(Face face, ModelFaceSide side) {
        Point3D[] points = face.getPoints();
        return switch (side) {
            case UP -> Arrays.stream(points).allMatch(p -> MathUtils.equals(p.y, 16.0));
            case DOWN -> Arrays.stream(points).allMatch(p -> MathUtils.equals(p.y, 0.0));
            case NORTH -> Arrays.stream(points).allMatch(p -> MathUtils.equals(p.z, 0.0));
            case EAST -> Arrays.stream(points).allMatch(p -> MathUtils.equals(p.x, 16.0));
            case SOUTH -> Arrays.stream(points).allMatch(p -> MathUtils.equals(p.z, 16.0));
            case WEST -> Arrays.stream(points).allMatch(p -> MathUtils.equals(p.x, 0.0));
        };
    }

}

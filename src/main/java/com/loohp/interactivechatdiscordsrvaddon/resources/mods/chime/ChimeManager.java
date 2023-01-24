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

package com.loohp.interactivechatdiscordsrvaddon.resources.mods.chime;

import com.loohp.interactivechat.objectholders.OfflineICPlayer;
import com.loohp.interactivechatdiscordsrvaddon.registry.ResourceRegistry;
import com.loohp.interactivechatdiscordsrvaddon.resources.CustomItemTextureRegistry;
import com.loohp.interactivechatdiscordsrvaddon.resources.ResourceLoadingException;
import com.loohp.interactivechatdiscordsrvaddon.resources.ResourceManager;
import com.loohp.interactivechatdiscordsrvaddon.resources.ResourcePackFile;
import com.loohp.interactivechatdiscordsrvaddon.resources.models.BlockModel;
import com.loohp.interactivechatdiscordsrvaddon.resources.models.IModelManager;
import com.loohp.interactivechatdiscordsrvaddon.resources.models.ModelManager;
import com.loohp.interactivechatdiscordsrvaddon.resources.models.ModelOverride;
import com.loohp.interactivechatdiscordsrvaddon.resources.models.ModelOverride.ModelOverrideType;
import com.loohp.interactivechatdiscordsrvaddon.resources.mods.ModManager;
import com.loohp.interactivechatdiscordsrvaddon.resources.textures.TextureResource;
import com.loohp.interactivechatdiscordsrvaddon.utils.TriFunction;
import org.apache.commons.io.input.BOMInputStream;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class ChimeManager extends ModManager implements IChimeManager {

    public static final String MOD_NAME = "Chime";
    public static final List<String> ASSETS_FOLDERS = Collections.singletonList("overrides");
    public static final TriFunction<IModelManager, String, JSONObject, ChimeBlockModel> CHIME_MODEL_PARSING_FUNCTION = ChimeBlockModel::fromJson;

    private final List<String> overrideLocations;
    private final Map<String, TextureResource> textures;
    private final Map<String, ChimeBlockModel> models;

    public ChimeManager(ResourceManager manager) {
        super(manager, MOD_NAME, ASSETS_FOLDERS);
        this.overrideLocations = new ArrayList<>();
        this.textures = new HashMap<>();
        this.models = new HashMap<>();
        manager.getModelManager().setModelParsingFunction(CHIME_MODEL_PARSING_FUNCTION);
        if (manager.hasResourceRegistry(CustomItemTextureRegistry.IDENTIFIER)) {
            manager.getResourceRegistry(CustomItemTextureRegistry.IDENTIFIER, CustomItemTextureRegistry.class).appendResolver(new ChimeItemTextureResolver(this));
        }
    }

    @Override
    protected void loadDirectory(String namespace, ResourcePackFile root, Object... meta) {
        if (!root.exists() || !root.isDirectory()) {
            throw new IllegalArgumentException(root.getAbsolutePath() + " is not a directory.");
        }
        overrideLocations.add(root.getName() + "/");
        JSONParser parser = new JSONParser();
        Map<String, ChimeBlockModel> models = new HashMap<>();
        Map<String, TextureResource> textures = new HashMap<>();
        Collection<ResourcePackFile> files = root.listFilesRecursively(new String[]{"json", "png"});
        for (ResourcePackFile file : files) {
            try {
                String name = file.getName();
                String relativePath = file.getRelativePathFrom(root);
                String key = namespace + ":" + root.getName() + "/" + relativePath;
                key = key.substring(0, key.lastIndexOf("."));
                if (name.endsWith(".json")) {
                    InputStreamReader reader = new InputStreamReader(new BOMInputStream(file.getInputStream()), StandardCharsets.UTF_8);
                    JSONObject rootJson = (JSONObject) parser.parse(reader);
                    reader.close();
                    rootJson.remove("textures");
                    String parent = namespace + ":" + relativePath;
                    if (parent.contains(".")) {
                        parent = parent.substring(0, parent.lastIndexOf("."));
                    }
                    rootJson.put("parent", parent);
                    ChimeBlockModel model = CHIME_MODEL_PARSING_FUNCTION.apply(this, key, rootJson);
                    models.put(key, model);
                } else if (name.endsWith(".png")) {
                    textures.put(key, new TextureResource(this, key, file, true, null));
                }
            } catch (Exception e) {
                new ResourceLoadingException("Unable to load block model " + file.getAbsolutePath(), e).printStackTrace();
            }
        }
        this.models.putAll(models);
        this.textures.putAll(textures);
    }

    @Override
    protected void filterResources(Pattern namespace, Pattern path) {
        Iterator<String> itr = textures.keySet().iterator();
        while (itr.hasNext()) {
            String namespacedKey = itr.next();
            String assetNamespace = namespacedKey.substring(0, namespacedKey.indexOf(":"));
            String assetKey = namespacedKey.substring(namespacedKey.indexOf(":") + 1);
            if (!assetKey.contains(".")) {
                assetKey = assetKey + ".png";
            }
            if (namespace.matcher(assetNamespace).matches() && path.matcher(assetKey).matches()) {
                itr.remove();
            }
        }

        Iterator<String> itr2 = models.keySet().iterator();
        while (itr2.hasNext()) {
            String namespacedKey = itr2.next();
            String assetNamespace = namespacedKey.substring(0, namespacedKey.indexOf(":"));
            String assetKey = namespacedKey.substring(namespacedKey.indexOf(":") + 1);
            if (!assetKey.contains(".")) {
                assetKey = assetKey + ".json";
            }
            if (namespace.matcher(assetNamespace).matches() && path.matcher(assetKey).matches()) {
                itr2.remove();
            }
        }
    }

    @Override
    protected void reload() {

    }

    public BlockModel getRawBlockModel(String resourceLocation, boolean checkOverride) {
        if (checkOverride) {
            for (String overrideLocation : overrideLocations) {
                String overrideResourceLocation = resourceLocation.substring(0, resourceLocation.indexOf(":") + 1) + overrideLocation + resourceLocation.substring(resourceLocation.indexOf(":") + 1);
                BlockModel model = models.get(overrideResourceLocation);
                if (model != null) {
                    return model;
                }
            }
        }
        return manager.getModelManager().getRawBlockModel(resourceLocation);
    }

    @Override
    public BlockModel getRawBlockModel(String resourceLocation) {
        return getRawBlockModel(resourceLocation, false);
    }

    public BlockModel resolveBlockModel(String resourceLocation, boolean is1_8, Map<ModelOverrideType, Float> predicates, OfflineICPlayer player, World world, LivingEntity entity, ItemStack itemStack, UnaryOperator<String> translateFunction) {
        BlockModel model = getRawBlockModel(resourceLocation, true);
        if (model == null) {
            return null;
        }
        for (ModelOverride override : model.getOverrides()) {
            if (override instanceof ChimeModelOverride) {
                if (((ChimeModelOverride) override).test(predicates, player, world, entity, itemStack, translateFunction)) {
                    return resolveBlockModel(override.getModel(), is1_8, null);
                }
            } else {
                if (override.test(predicates)) {
                    return resolveBlockModel(override.getModel(), is1_8, null);
                }
            }
        }
        while (model.getParent() != null) {
            if (model.getRawParent().equals(ModelManager.ITEM_BASE)) {
                break;
            }
            if (model.getRawParent().equals(ModelManager.BLOCK_ENTITY_BASE)) {
                BlockModel builtinModel = resolveBlockModel(ResourceRegistry.BUILTIN_ENTITY_MODEL_LOCATION + resourceLocation.substring(resourceLocation.lastIndexOf("/") + 1), is1_8, predicates);
                if (builtinModel != null) {
                    return builtinModel;
                }
                break;
            }
            BlockModel parent = getRawBlockModel(model.getParent());
            if (parent == null) {
                break;
            }
            for (ModelOverride override : model.getOverrides()) {
                if (override instanceof ChimeModelOverride) {
                    if (((ChimeModelOverride) override).test(predicates, player, world, entity, itemStack, translateFunction)) {
                        return resolveBlockModel(override.getModel(), is1_8, null);
                    }
                } else {
                    if (override.test(predicates)) {
                        return resolveBlockModel(override.getModel(), is1_8, null);
                    }
                }
            }
            model = model.resolve(parent, is1_8);
        }
        return model.resolve(is1_8);
    }

    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    @Override
    public BlockModel resolveBlockModel(String resourceLocation, boolean is1_8, Map<ModelOverrideType, Float> predicates) {
        BlockModel model = getRawBlockModel(resourceLocation);
        if (model == null) {
            return null;
        }
        for (ModelOverride override : model.getOverrides()) {
            if (override.test(predicates)) {
                return resolveBlockModel(override.getModel(), is1_8, null);
            }
        }
        while (model.getParent() != null) {
            if (model.getRawParent().equals(ModelManager.ITEM_BASE)) {
                break;
            }
            if (model.getRawParent().equals(ModelManager.BLOCK_ENTITY_BASE)) {
                BlockModel builtinModel = resolveBlockModel(ResourceRegistry.BUILTIN_ENTITY_MODEL_LOCATION + resourceLocation.substring(resourceLocation.lastIndexOf("/") + 1), is1_8, predicates);
                if (builtinModel != null) {
                    return builtinModel;
                }
                break;
            }
            BlockModel parent = getRawBlockModel(model.getParent());
            if (parent == null) {
                break;
            }
            for (ModelOverride override : model.getOverrides()) {
                if (override.test(predicates)) {
                    return resolveBlockModel(override.getModel(), is1_8, null);
                }
            }
            model = model.resolve(parent, is1_8);
        }
        return model.resolve(is1_8);
    }

    @Override
    public TextureResource getTexture(String resourceLocation, boolean returnMissingTexture) {
        if (!resourceLocation.contains(":")) {
            resourceLocation = ResourceRegistry.DEFAULT_NAMESPACE + ":" + resourceLocation;
        }
        if (resourceLocation.endsWith(".png")) {
            resourceLocation = resourceLocation.substring(0, resourceLocation.length() - 4);
        }
        TextureResource textureResource = textures.get(resourceLocation);
        if (textureResource != null) {
            return textureResource;
        }
        String key = resourceLocation.substring(resourceLocation.indexOf(":") + 1);
        if (key.startsWith("textures/")) {
            resourceLocation = resourceLocation.substring(0, resourceLocation.indexOf(":") + 1) + key.substring(9);
        }
        return manager.getTextureManager().getTexture(resourceLocation, returnMissingTexture);
    }

    @Override
    public TextureResource getArmorOverrideTextures(String layer, ItemStack itemStack, OfflineICPlayer player, World world, LivingEntity entity, UnaryOperator<String> translateFunction) {
        for (String overrideLocation : overrideLocations) {
            String resourceKey = "minecraft:" + overrideLocation + "armor/" + layer;
            BlockModel model = getRawBlockModel(resourceKey, true);
            if (!(model instanceof ChimeBlockModel)) {
                return null;
            }
            for (ChimeModelOverride override : ((ChimeBlockModel) model).getChimeOverrides()) {
                if (override.hasArmorTexture() && override.test(null, player, world, entity, itemStack, translateFunction)) {
                    return getTexture(override.getArmorTexture());
                }
            }
        }
        return null;
    }

}

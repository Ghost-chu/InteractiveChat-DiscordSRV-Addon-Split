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

package com.loohp.interactivechatdiscordsrvaddon.resources;

import com.google.gson.GsonBuilder;
import com.loohp.interactivechatdiscordsrvaddon.registry.ResourceRegistry;
import com.loohp.interactivechatdiscordsrvaddon.resources.fonts.FontManager;
import com.loohp.interactivechatdiscordsrvaddon.resources.languages.LanguageManager;
import com.loohp.interactivechatdiscordsrvaddon.resources.languages.LanguageMeta;
import com.loohp.interactivechatdiscordsrvaddon.resources.models.ModelManager;
import com.loohp.interactivechatdiscordsrvaddon.resources.mods.ModManager;
import com.loohp.interactivechatdiscordsrvaddon.resources.textures.TextureManager;
import me.clip.placeholderapi.libs.kyori.adventure.text.format.NamedTextColor;
import me.clip.placeholderapi.libs.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.Component;
import org.apache.commons.io.input.BOMInputStream;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

public class ResourceManager implements AutoCloseable {

    private final List<ResourcePackInfo> resourcePackInfo;

    private final Map<String, IResourceRegistry> resourceRegistries;

    private final ModelManager modelManager;
    private final TextureManager textureManager;
    private final FontManager fontManager;
    private final LanguageManager languageManager;

    private final Map<String, ModManager> modManagers;

    private final boolean flattenLegacy;
    private final boolean fontLegacy;

    private final BiFunction<File, ResourcePackType, DefaultResourcePackInfo> defaultResourcePackInfoFunction;
    private final AtomicBoolean isValid;
    private final UUID uuid;

    public ResourceManager(boolean flattenLegacy, boolean fontLegacy, Collection<ModManagerSupplier<?>> modManagerProviders, Collection<ResourceRegistrySupplier<?>> resourceManagerUtilsProviders, BiFunction<File, ResourcePackType, DefaultResourcePackInfo> defaultResourcePackInfoFunction) {
        this.resourcePackInfo = new ArrayList<>();
        this.defaultResourcePackInfoFunction = defaultResourcePackInfoFunction;

        this.flattenLegacy = flattenLegacy;
        this.fontLegacy = fontLegacy;

        this.isValid = new AtomicBoolean(true);
        this.uuid = UUID.randomUUID();

        this.resourceRegistries = new HashMap<>();
        for (ResourceRegistrySupplier<?> resourceRegistrySupplier : resourceManagerUtilsProviders) {
            IResourceRegistry resourceRegistry = resourceRegistrySupplier.init(this);
            this.resourceRegistries.put(resourceRegistry.getRegistryIdentifier(), resourceRegistry);
        }

        this.modelManager = new ModelManager(this);
        this.textureManager = new TextureManager(this);
        this.fontManager = new FontManager(this);
        this.languageManager = new LanguageManager(this);

        this.modManagers = new HashMap<>();
        for (ModManagerSupplier<?> modManagerProvider : modManagerProviders) {
            ModManager modManager = modManagerProvider.init(this);
            this.modManagers.put(modManager.getModName(), modManager);
        }
    }

    public ResourceManager(boolean flattenLegacy, boolean fontLegacy, Collection<ModManagerSupplier<?>> modManagerProviders, Collection<ResourceRegistrySupplier<?>> resourceManagerUtilsProviders, int defaultResourcePackVersion) {
        this(flattenLegacy, fontLegacy, modManagerProviders, resourceManagerUtilsProviders, (resourcePackFile, type) -> new DefaultResourcePackInfo(Component.text(resourcePackFile.getName()), defaultResourcePackVersion, Component.text("The default look and feel of Minecraft (Modified by LOOHP)")));
    }

    public ResourcePackInfo loadResources(File resourcePackFile, ResourcePackType type) {
        return loadResources(resourcePackFile, type, false);
    }

    public synchronized ResourcePackInfo loadResources(File resourcePackFile, ResourcePackType type, boolean defaultResource) {
        if (!isValid()) {
            throw new IllegalStateException("ResourceManager already closed!");
        }
        DefaultResourcePackInfo defaultResourcePackInfo = defaultResource ? defaultResourcePackInfoFunction.apply(resourcePackFile, type) : null;

        String resourcePackNameStr = resourcePackFile.getName();
        Component resourcePackName = Component.text(resourcePackNameStr);
        if (!resourcePackFile.exists()) {
            new IllegalArgumentException(resourcePackFile.getAbsolutePath() + " is not a directory nor is a zip file.").printStackTrace();
            ResourcePackInfo info = new ResourcePackInfo(this, null, type, resourcePackName, "Resource Pack is not a directory nor a zip file.");
            resourcePackInfo.add(0, info);
            return info;
        }
        ResourcePackFile resourcePack;
        if (resourcePackFile.isDirectory()) {
            resourcePack = new ResourcePackSystemFile(resourcePackFile);
        } else {
            try {
                resourcePack = new ResourcePackZipEntryFile(resourcePackFile);
            } catch (IOException e) {
                new IllegalArgumentException(resourcePackFile.getAbsolutePath() + " is an invalid zip file.", e).printStackTrace();
                ResourcePackInfo info = new ResourcePackInfo(this, null, type, resourcePackName, "Resource Pack is an invalid zip file.");
                resourcePackInfo.add(0, info);
                return info;
            }
        }
        ResourcePackFile packMcmeta = resourcePack.getChild("pack.mcmeta");
        if (!packMcmeta.exists()) {
            new ResourceLoadingException(resourcePackNameStr + " does not have a pack.mcmeta").printStackTrace();
            ResourcePackInfo info = new ResourcePackInfo(this, resourcePack, type, resourcePackName, "pack.mcmeta not found");
            resourcePackInfo.add(0, info);
            return info;
        }

        JSONObject json;
        try (InputStreamReader reader = new InputStreamReader(new BOMInputStream(packMcmeta.getInputStream()), StandardCharsets.UTF_8)) {
            json = (JSONObject) new JSONParser().parse(reader);
        } catch (Throwable e) {
            new ResourceLoadingException("Unable to read pack.mcmeta for " + resourcePackNameStr, e).printStackTrace();
            ResourcePackInfo info = new ResourcePackInfo(this, resourcePack, type, resourcePackName, "Unable to read pack.mcmeta");
            resourcePackInfo.add(0, info);
            return info;
        }

        int format;
        Component description = null;
        Map<String, LanguageMeta> languageMeta = new HashMap<>();
        List<ResourceFilterBlock> resourceFilterBlocks;
        try {
            JSONObject packJson = (JSONObject) json.get("pack");
            if (packJson == null && defaultResource) {
                resourcePackName = defaultResourcePackInfo.getName();
                format = defaultResourcePackInfo.getVersion();
                description = defaultResourcePackInfo.getDescription();
            } else {
                format = ((Number) packJson.get("pack_format")).intValue();
                Object descriptionObj = packJson.get("description");
                if (descriptionObj instanceof JSONObject) {
                    String descriptionJson = new GsonBuilder().create().toJson(descriptionObj);
                    try {
                        description = InteractiveChatComponentSerializer.gson().deserialize(descriptionJson);
                    } catch (Exception e) {
                        description = null;
                    }
                }
                if (description == null) {
                    String rawDescription = packJson.get("description").toString();
                    try {
                        description = InteractiveChatComponentSerializer.gson().deserialize(rawDescription);
                    } catch (Exception e) {
                        description = null;
                    }
                    if (description == null) {
                        description = LegacyComponentSerializer.legacySection().deserialize(rawDescription);
                    }
                }
            }
            description = description.applyFallbackStyle(NamedTextColor.GRAY);

            JSONObject languageJson = (JSONObject) json.get("language");
            if (languageJson != null) {
                for (Object obj : languageJson.keySet()) {
                    String language = (String) obj;
                    JSONObject meta = (JSONObject) languageJson.get(language);
                    String region = (String) meta.get("region");
                    String name = (String) meta.get("name");
                    boolean bidirectional = (boolean) meta.get("bidirectional");
                    languageMeta.put(language, new LanguageMeta(language, region, name, bidirectional));
                }
            }

            JSONObject filterJson = (JSONObject) json.get("filter");
            JSONArray filterBlockArray;
            if (filterJson != null && (filterBlockArray = (JSONArray) filterJson.get("block")) != null) {
                resourceFilterBlocks = ResourceFilterBlock.fromJson(filterBlockArray);
            } else {
                resourceFilterBlocks = Collections.emptyList();
            }
        } catch (Exception e) {
            new ResourceLoadingException("Invalid pack.mcmeta for " + resourcePackNameStr, e).printStackTrace();
            ResourcePackInfo info = new ResourcePackInfo(this, resourcePack, type, resourcePackName, "Invalid pack.mcmeta");
            resourcePackInfo.add(0, info);
            return info;
        }

        BufferedImage icon = null;
        ResourcePackFile packIcon = resourcePack.getChild("pack.png");
        if (packIcon.exists()) {
            try (InputStream inputStream = packIcon.getInputStream()) {
                icon = ImageIO.read(inputStream);
            } catch (Exception ignore) {
            }
        }

        ResourcePackFile assetsFolder = resourcePack.getChild("assets");
        Map<String, TextureAtlases> textureAtlases = loadAtlases(assetsFolder);
        try {
            filterResources(resourceFilterBlocks);
            loadAssets(assetsFolder, languageMeta, textureAtlases);
        } catch (Exception e) {
            new ResourceLoadingException("Unable to load assets for " + resourcePackNameStr, e).printStackTrace();
            ResourcePackInfo info = new ResourcePackInfo(this, resourcePack, type, resourcePackName, false, "Unable to load assets", format, description, languageMeta, icon, resourceFilterBlocks, textureAtlases);
            resourcePackInfo.add(0, info);
            return info;
        }

        ResourcePackInfo info = new ResourcePackInfo(this, resourcePack, type, resourcePackName, true, null, format, description, languageMeta, icon, resourceFilterBlocks, textureAtlases);
        resourcePackInfo.add(0, info);
        return info;
    }

    private void filterResources(List<ResourceFilterBlock> resourceFilterBlocks) {
        for (ResourceFilterBlock resourceFilterBlock : resourceFilterBlocks) {
            Pattern namespace = resourceFilterBlock.getNamespace();
            Pattern path = resourceFilterBlock.getPath();

            ((AbstractManager) modelManager).filterResources(namespace, path);
            ((AbstractManager) textureManager).filterResources(namespace, path);
            ((AbstractManager) fontManager).filterResources(namespace, path);
            ((AbstractManager) languageManager).filterResources(namespace, path);
            for (ModManager modManager : modManagers.values()) {
                modManager.filterResources(namespace, path);
            }
        }

        ((AbstractManager) modelManager).reload();
        ((AbstractManager) textureManager).reload();
        ((AbstractManager) fontManager).reload();
        ((AbstractManager) languageManager).reload();
        for (ModManager modManager : modManagers.values()) {
            modManager.reload();
        }
    }

    private Map<String, TextureAtlases> loadAtlases(ResourcePackFile assetsFolder) {
        if (!assetsFolder.exists() || !assetsFolder.isDirectory()) {
            throw new IllegalArgumentException(assetsFolder.getAbsolutePath() + " is not a directory.");
        }
        Collection<ResourcePackFile> folders = assetsFolder.listFilesAndFolders();
        Map<String, TextureAtlases> atlasesByNamespace = new HashMap<>();
        for (ResourcePackFile folder : folders) {
            if (folder.isDirectory()) {
                String namespace = folder.getName();
                ResourcePackFile atlases = folder.getChild("atlases");
                if (atlases.exists() && atlases.isDirectory()) {
                    atlasesByNamespace.put(namespace, TextureAtlases.fromAtlasesFolder(atlases));
                }
            }
        }
        return Collections.unmodifiableMap(atlasesByNamespace);
    }

    private void loadAssets(ResourcePackFile assetsFolder, Map<String, LanguageMeta> languageMeta, Map<String, TextureAtlases> textureAtlases) {
        if (!assetsFolder.exists() || !assetsFolder.isDirectory()) {
            throw new IllegalArgumentException(assetsFolder.getAbsolutePath() + " is not a directory.");
        }
        Collection<ResourcePackFile> folders = assetsFolder.listFilesAndFolders();
        for (ResourcePackFile folder : folders) {
            if (folder.isDirectory()) {
                String namespace = folder.getName();
                ResourcePackFile models = folder.getChild("models");
                if (models.exists() && models.isDirectory()) {
                    ((AbstractManager) modelManager).loadDirectory(namespace, models);
                }
            }
        }
        for (ResourcePackFile folder : folders) {
            if (folder.isDirectory()) {
                String namespace = folder.getName();
                ResourcePackFile textures = folder.getChild("textures");
                if (textures.exists() && textures.isDirectory()) {
                    if (ResourceRegistry.RESOURCE_PACK_VERSION <= 9) {
                        ((AbstractManager) textureManager).loadDirectory(namespace, textures);
                    } else {
                        ((AbstractManager) textureManager).loadDirectory(namespace, textures, textureAtlases.getOrDefault(namespace, TextureAtlases.EMPTY_ATLAS));
                    }
                }
            }
        }
        for (ResourcePackFile folder : folders) {
            if (folder.isDirectory()) {
                String namespace = folder.getName();
                ResourcePackFile font = folder.getChild("font");
                if (font.exists() && font.isDirectory()) {
                    ((AbstractManager) fontManager).loadDirectory(namespace, font);
                }
            }
        }
        for (ResourcePackFile folder : folders) {
            if (folder.isDirectory()) {
                String namespace = folder.getName();
                ResourcePackFile lang = folder.getChild("lang");
                if (lang.exists() && lang.isDirectory()) {
                    ((AbstractManager) languageManager).loadDirectory(namespace, lang, languageMeta);
                }
            }
        }
        for (ModManager modManager : modManagers.values()) {
            for (String folderName : modManager.getModAssetsFolderNames()) {
                for (ResourcePackFile folder : folders) {
                    if (folder.isDirectory()) {
                        String namespace = folder.getName();
                        ResourcePackFile modFolder = folder.getChild(folderName);
                        if (modFolder.exists() && modFolder.isDirectory()) {
                            modManager.loadDirectory(namespace, modFolder);
                        }
                    }
                }
            }
        }

        ((AbstractManager) modelManager).reload();
        ((AbstractManager) textureManager).reload();
        ((AbstractManager) fontManager).reload();
        ((AbstractManager) languageManager).reload();
        for (ModManager modManager : modManagers.values()) {
            modManager.reload();
        }
    }

    public List<ResourcePackInfo> getResourcePackInfo() {
        return Collections.unmodifiableList(resourcePackInfo);
    }

    public ModelManager getModelManager() {
        return modelManager;
    }

    public TextureManager getTextureManager() {
        return textureManager;
    }

    public FontManager getFontManager() {
        return fontManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public ModManager getModManager(String modName) {
        return modManagers.get(modName);
    }

    public boolean hasModManager(String modName) {
        return modManagers.containsKey(modName);
    }

    public <T extends ModManager> T getModManager(String modName, Class<T> managerClass) {
        return (T) getModManager(modName);
    }

    public <T extends ModManager> boolean hasModManager(String modName, Class<T> managerClass) {
        return managerClass.isInstance(getModManager(modName));
    }

    public Map<String, ModManager> getModManagers() {
        return Collections.unmodifiableMap(modManagers);
    }

    public IResourceRegistry getResourceRegistry(String identifier) {
        return resourceRegistries.get(identifier);
    }

    public boolean hasResourceRegistry(String identifier) {
        return resourceRegistries.containsKey(identifier);
    }

    public <T extends IResourceRegistry> T getResourceRegistry(String identifier, Class<T> registryClass) {
        return (T) getResourceRegistry(identifier);
    }

    public <T extends IResourceRegistry> boolean hasResourceRegistry(String identifier, Class<T> registryClass) {
        return registryClass.isInstance(getResourceRegistry(identifier));
    }

    public Map<String, IResourceRegistry> getResourceRegistries() {
        return Collections.unmodifiableMap(resourceRegistries);
    }

    public boolean isFlattenLegacy() {
        return flattenLegacy;
    }

    public boolean isFontLegacy() {
        return fontLegacy;
    }

    public boolean isValid() {
        return isValid.get();
    }

    protected UUID getUuid() {
        return uuid;
    }

    @Override
    public synchronized void close() {
        if (isValid.getAndSet(false)) {
            for (ResourcePackInfo info : resourcePackInfo) {
                if (info.getResourcePackFile() != null) {
                    info.getResourcePackFile().close();
                }
            }
            for (IResourceRegistry resourceRegistry : resourceRegistries.values()) {
                resourceRegistry.close();
            }

            modelManager.close();
            textureManager.close();
            fontManager.close();
            languageManager.close();
            for (ModManager modManager : modManagers.values()) {
                modManager.close();
            }
        }
    }

    @FunctionalInterface
    public interface ModManagerSupplier<T extends ModManager> extends Function<ResourceManager, T> {

        T init(ResourceManager resourceManager);

        @Override
        default T apply(ResourceManager resourceManager) {
            return init(resourceManager);
        }

    }

    @FunctionalInterface
    public interface ResourceRegistrySupplier<T extends IResourceRegistry> extends Function<ResourceManager, T> {

        T init(ResourceManager resourceManager);

        @Override
        default T apply(ResourceManager resourceManager) {
            return init(resourceManager);
        }

    }

    public static class DefaultResourcePackInfo {

        private final Component name;
        private final int version;
        private final Component description;

        public DefaultResourcePackInfo(Component name, int version, Component description) {
            this.name = name;
            this.version = version;
            this.description = description;
        }

        public Component getName() {
            return name;
        }

        public int getVersion() {
            return version;
        }

        public Component getDescription() {
            return description;
        }

    }

}

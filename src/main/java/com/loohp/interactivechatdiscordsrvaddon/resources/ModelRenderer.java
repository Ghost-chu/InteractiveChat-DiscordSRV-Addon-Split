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

import com.loohp.blockmodelrenderer.blending.BlendingModes;
import com.loohp.blockmodelrenderer.render.Face;
import com.loohp.blockmodelrenderer.render.Hexahedron;
import com.loohp.blockmodelrenderer.render.Model;
import com.loohp.blockmodelrenderer.render.Point3D;
import com.loohp.blockmodelrenderer.utils.ColorUtils;
import com.loohp.interactivechat.InteractiveChat;
import com.loohp.interactivechat.utils.CustomArrayUtils;
import com.loohp.interactivechatdiscordsrvaddon.InteractiveChatDiscordSrvAddon;
import com.loohp.interactivechatdiscordsrvaddon.graphics.BlendingUtils;
import com.loohp.interactivechatdiscordsrvaddon.graphics.ImageUtils;
import com.loohp.interactivechatdiscordsrvaddon.registry.ResourceRegistry;
import com.loohp.interactivechatdiscordsrvaddon.resources.models.*;
import com.loohp.interactivechatdiscordsrvaddon.resources.models.ModelDisplay.ModelDisplayPosition;
import com.loohp.interactivechatdiscordsrvaddon.resources.models.ModelElement.ModelElementRotation;
import com.loohp.interactivechatdiscordsrvaddon.resources.models.ModelFace.ModelFaceSide;
import com.loohp.interactivechatdiscordsrvaddon.resources.models.ModelOverride.ModelOverrideType;
import com.loohp.interactivechatdiscordsrvaddon.resources.mods.optifine.cit.EnchantmentProperties.OpenGLBlending;
import com.loohp.interactivechatdiscordsrvaddon.resources.textures.*;
import com.loohp.interactivechatdiscordsrvaddon.utils.ModelUtils;
import com.loohp.interactivechatdiscordsrvaddon.utils.TintUtils.TintIndexData;
import com.loohp.interactivechatdiscordsrvaddon.utils.ValuePairs;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.Queue;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class ModelRenderer implements AutoCloseable {

    public static final Function<BlockModel, ValuePairs<BlockModel, Map<String, TextureResource>>> DEFAULT_POST_RESOLVE_FUNCTION = blockModel -> new ValuePairs<>(blockModel, Collections.emptyMap());

    public static final int INTERNAL_W = 64;
    public static final int INTERNAL_H = 64;

    public static final float RESCALE_22_5 = 1.0F / (float) Math.cos(((float) Math.PI / 8F)) - 1.0F;
    public static final float RESCALE_45 = 1.0F / (float) Math.cos(((float) Math.PI / 4F)) - 1.0F;

    public static final int SKIN_RESOLUTION = 1600;
    public static final int TEXTURE_RESOLUTION = 800;

    public static final String CACHE_KEY = "ModelRender";
    public static final String MODEL_NOT_FOUND = "notfound";

    private static final BufferedImage[] EMPTY_IMAGE_ARRAY = new BufferedImage[0];
    private static final double[] OVERLAY_ADDITION_FACTORS = new double[6];

    private static final String PLAYER_MODEL_RESOURCELOCATION = ResourceRegistry.BUILTIN_ENTITY_MODEL_LOCATION + "player_model";
    private static final String PLAYER_MODEL_SLIM_RESOURCELOCATION = ResourceRegistry.BUILTIN_ENTITY_MODEL_LOCATION + "player_model_slim";

    static {
        Arrays.fill(OVERLAY_ADDITION_FACTORS, ResourceRegistry.ENCHANTMENT_GLINT_FACTOR);
    }

    private final Function<String, ThreadFactory> threadFactoryBuilder;
    private final LongSupplier cacheTimeoutSupplier;
    private final IntSupplier renderThreads;
    private final ThreadPoolExecutor renderingService;
    private final ScheduledExecutorService controlService;
    private final AtomicBoolean isValid;

    public ModelRenderer(Function<String, ThreadFactory> threadFactoryBuilder, LongSupplier cacheTimeoutSupplier, IntSupplier renderThreads) {
        this.isValid = new AtomicBoolean(true);
        this.threadFactoryBuilder = threadFactoryBuilder;
        this.cacheTimeoutSupplier = cacheTimeoutSupplier;
        this.renderThreads = renderThreads;

        int renderThreadSize = renderThreads.getAsInt();

        ThreadFactory factory1 = threadFactoryBuilder.apply("InteractiveChatDiscordSRVAddon Async Model Renderer Thread #%d");
        this.renderingService = new ThreadPoolExecutor(renderThreadSize, renderThreadSize, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), factory1);
        ThreadFactory factory2 = threadFactoryBuilder.apply("InteractiveChatDiscordSRVAddon Async Model Renderer Control Thread");
        this.controlService = Executors.newSingleThreadScheduledExecutor(factory2);

        this.controlService.scheduleAtFixedRate(this::reloadPoolSize, 30, 30, TimeUnit.SECONDS);
    }

    public ModelRenderer(LongSupplier cacheTimeoutSupplier, IntSupplier renderThreads) {
        this(str -> Executors.defaultThreadFactory(), cacheTimeoutSupplier, renderThreads);
    }

    @Override
    public synchronized void close() {
        isValid.set(false);
        controlService.shutdown();
        renderingService.shutdown();
    }

    public synchronized void reloadPoolSize() {
        int renderThreadSize = renderThreads.getAsInt();
        this.renderingService.setMaximumPoolSize(renderThreadSize);
        this.renderingService.setCorePoolSize(renderThreadSize);
    }

    public RenderResult renderPlayer(int width, int height, ResourceManager manager, boolean post1_8, boolean slim, Map<String, TextureResource> providedTextures, TintIndexData tintIndexData, Map<PlayerModelItemPosition, PlayerModelItem> modelItems) {
        BlockModel playerModel = manager.getModelManager().resolveBlockModel(slim ? PLAYER_MODEL_SLIM_RESOURCELOCATION : PLAYER_MODEL_RESOURCELOCATION, InteractiveChat.version.isOld(), Collections.emptyMap());
        if (playerModel == null) {
            return new RenderResult(MODEL_NOT_FOUND);
        }
        Model playerRenderModel = generateStandardRenderModel(playerModel, manager, providedTextures, Collections.emptyMap(), tintIndexData, false, true, null);

        Map<PlayerModelItem, ValuePairs<BlockModel, Map<String, TextureResource>>> resolvedItems = new HashMap<>();
        for (PlayerModelItem playerModelItem : modelItems.values()) {
            BlockModel itemBlockModel = playerModelItem.getModelKey() == null ? null : manager.getModelManager().resolveBlockModel(playerModelItem.getModelKey(), InteractiveChat.version.isOld(), playerModelItem.getPredicate());
            ValuePairs<BlockModel, Map<String, TextureResource>> resolveFunctionResult = playerModelItem.getPostResolveFunction().apply(itemBlockModel);
            itemBlockModel = resolveFunctionResult.getFirst();
            Map<String, TextureResource> overrideTextures = resolveFunctionResult.getSecond();
            resolvedItems.put(playerModelItem, new ValuePairs<>(itemBlockModel, overrideTextures));
        }

        String cacheKey = cacheKey(width, height, manager.getUuid(), slim, cacheKeyResolvedItems(resolvedItems), cacheKeyProvidedTextures(providedTextures));
        if (manager.hasResourceRegistry(ICacheManager.IDENTIFIER, ICacheManager.class)) {
            CacheObject<?> cachedRender = manager.getResourceRegistry(ICacheManager.IDENTIFIER, ICacheManager.class).getCache(cacheKey);
            if (cachedRender != null) {
                RenderResult cachedResult = (RenderResult) cachedRender.getObject();
                if (cachedResult.isSuccessful()) {
                    return cachedResult;
                }
            }
        }

        for (Entry<PlayerModelItem, ValuePairs<BlockModel, Map<String, TextureResource>>> entry : resolvedItems.entrySet()) {
            PlayerModelItem playerModelItem = entry.getKey();
            BlockModel itemBlockModel = entry.getValue().getFirst();
            Map<String, TextureResource> overrideTextures = entry.getValue().getSecond();
            Model itemRenderModel = null;
            if (itemBlockModel != null) {
                if (itemBlockModel.getRawParent() == null || !itemBlockModel.getRawParent().contains("/")) {
                    itemRenderModel = generateStandardRenderModel(itemBlockModel, manager, playerModelItem.getProvidedTextures(), overrideTextures, playerModelItem.getTintIndexData(), playerModelItem.isEnchanted(), false, playerModelItem.getRawEnchantmentGlintProvider());
                } else if (itemBlockModel.getRawParent().equals(ModelManager.ITEM_BASE)) {
                    BufferedImage image = new BufferedImage(INTERNAL_W, INTERNAL_H, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = image.createGraphics();
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    for (int i = 0; itemBlockModel.getTextures().containsKey(ModelManager.ITEM_BASE_LAYER + i); i++) {
                        TextureResource resource = overrideTextures.getOrDefault("", overrideTextures.get(ModelManager.ITEM_BASE_LAYER + i));
                        String resourceLocation = itemBlockModel.getTextures().get(ModelManager.ITEM_BASE_LAYER + i);
                        if (!resourceLocation.contains(":")) {
                            resourceLocation = ResourceRegistry.DEFAULT_NAMESPACE + ":" + resourceLocation;
                        }
                        if (resource == null) {
                            resource = providedTextures.get(resourceLocation);
                        }
                        if (resource == null) {
                            resource = manager.getTextureManager().getTexture(resourceLocation);
                        }
                        BufferedImage texture = resource.getTexture();
                        texture = tintIndexData.applyTint(texture, 1);
                        if (resource.hasTextureMeta()) {
                            TextureMeta meta = resource.getTextureMeta();
                            if (meta.hasProperties()) {
                                TextureProperties properties = meta.getProperties();
                                if (properties.isBlur()) {
                                    texture = ImageUtils.applyGaussianBlur(texture);
                                }
                            }
                            if (meta.hasAnimation()) {
                                TextureAnimation animation = meta.getAnimation();
                                if (animation.hasWidth() && animation.hasHeight()) {
                                    texture = ImageUtils.copyAndGetSubImage(texture, 0, 0, animation.getWidth(), animation.getHeight());
                                } else {
                                    texture = ImageUtils.copyAndGetSubImage(texture, 0, 0, texture.getWidth(), texture.getWidth());
                                }
                            }
                        }
                        g.drawImage(texture, 0, 0, image.getWidth(), image.getHeight(), null);
                    }
                    g.dispose();
                    image = tintIndexData.applyTint(image, 0);
                    if (playerModelItem.isEnchanted()) {
                        image = playerModelItem.getEnchantmentGlintProvider().apply(image);
                    }
                    itemRenderModel = generateItemRenderModel(16, 16, 16, image);
                }
            }

            if (itemRenderModel != null) {
                itemRenderModel.translate(-16 / 2.0, -16 / 2.0, -16 / 2.0);
                ModelDisplay displayData = itemBlockModel.getRawDisplay().get(playerModelItem.getPosition().getModelDisplayPosition());
                boolean flipX = playerModelItem.getPosition().isLiteralFlipped();
                boolean isMirror = false;
                if (displayData == null && playerModelItem.getPosition().getModelDisplayPosition().hasFallback()) {
                    displayData = itemBlockModel.getRawDisplay().get(playerModelItem.getPosition().getModelDisplayPosition().getFallback());
                    isMirror = true;
                }
                if (isMirror || !flipX) {
                    if (displayData != null) {
                        Coordinates3D scale = displayData.getScale();
                        itemRenderModel.scale(scale.getX(), scale.getY(), scale.getZ());
                        Coordinates3D rotation = displayData.getRotation();
                        itemRenderModel.rotate(rotation.getX(), rotation.getY(), rotation.getZ() + (post1_8 ? 10 : 0), false);
                        Coordinates3D transform = displayData.getTranslation();
                        itemRenderModel.translate(transform.getX(), transform.getY() + (post1_8 ? -10 : 0), transform.getZ() + (post1_8 ? -2.75 : 0));
                    }
                    if (flipX) {
                        itemRenderModel.flipAboutPlane(false, true, true);
                    }
                } else {
                    if (displayData != null) {
                        Coordinates3D scale = displayData.getScale();
                        itemRenderModel.scale(scale.getX(), scale.getY(), scale.getZ());
                        Coordinates3D rotation = displayData.getRotation();
                        itemRenderModel.rotate(rotation.getX(), -rotation.getY(), -rotation.getZ(), false);
                        Coordinates3D transform = displayData.getTranslation();
                        itemRenderModel.translate(-transform.getX(), transform.getY(), transform.getZ());
                    }
                }
                if (playerModelItem.getPosition().yIsZAxis()) {
                    if (post1_8) {
                        itemRenderModel.rotate(180, 180, 0, false);
                    } else {
                        itemRenderModel.rotate(90, 0, 0, true);
                    }
                }
                double scale = playerModelItem.getPosition().getScale();
                itemRenderModel.scale(scale, scale, scale);
                itemRenderModel.translate((16 * scale) / 2, (16 * scale) / 2, (16 * scale) / 2);
                Coordinates3D defaultTranslation = playerModelItem.getPosition().getDefaultTranslate();
                itemRenderModel.translate(defaultTranslation.getX(), defaultTranslation.getY(), defaultTranslation.getZ());
                playerRenderModel.append(itemRenderModel);
            }
        }

        playerRenderModel.translate(-16 / 2.0, -16 / 2.0, -16 / 2.0);
        playerRenderModel.rotate(0, 180, 0, false);
        playerRenderModel.translate(16 / 2.0, 16 / 2.0, 16 / 2.0);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        renderPlayerModel(playerRenderModel, image, playerModel.getGUILight());
        RenderResult result = new RenderResult(image);
        manager.getResourceRegistry(ICacheManager.IDENTIFIER, ICacheManager.class).putCache(cacheKey, result);
        return result;
    }

    public RenderResult render(int width, int height, ResourceManager manager, Function<BlockModel, ValuePairs<BlockModel, Map<String, TextureResource>>> postResolveFunction, boolean post1_8, String modelKey, ModelDisplayPosition displayPosition, boolean enchanted, UnaryOperator<BufferedImage> enchantmentGlintProvider, Function<BufferedImage, RawEnchantmentGlintData> rawEnchantmentGlintProvider) {
        return render(width, height, manager, postResolveFunction, post1_8, modelKey, displayPosition, Collections.emptyMap(), Collections.emptyMap(), TintIndexData.EMPTY_INSTANCE, enchanted, enchantmentGlintProvider, rawEnchantmentGlintProvider);
    }

    public RenderResult render(int width, int height, ResourceManager manager, Function<BlockModel, ValuePairs<BlockModel, Map<String, TextureResource>>> postResolveFunction, boolean post1_8, String modelKey, ModelDisplayPosition displayPosition, Map<ModelOverrideType, Float> predicate, Map<String, TextureResource> providedTextures, TintIndexData tintIndexData, boolean enchanted, UnaryOperator<BufferedImage> enchantmentGlintProvider, Function<BufferedImage, RawEnchantmentGlintData> rawEnchantmentGlintProvider) {
        return render(width, height, INTERNAL_W, INTERNAL_H, manager, postResolveFunction, post1_8, modelKey, displayPosition, predicate, providedTextures, tintIndexData, enchanted, enchantmentGlintProvider, rawEnchantmentGlintProvider);
    }

    public RenderResult render(int width, int height, int internalWidth, int internalHeight, ResourceManager manager, Function<BlockModel, ValuePairs<BlockModel, Map<String, TextureResource>>> postResolveFunction, boolean post1_8, String modelKey, ModelDisplayPosition displayPosition, Map<ModelOverrideType, Float> predicate, Map<String, TextureResource> providedTextures, TintIndexData tintIndexData, boolean enchanted, UnaryOperator<BufferedImage> enchantmentGlintProvider, Function<BufferedImage, RawEnchantmentGlintData> rawEnchantmentGlintProvider) {
        return render(width, height, internalWidth, internalHeight, manager, postResolveFunction, post1_8, modelKey, displayPosition, predicate, providedTextures, tintIndexData, enchanted, false, enchantmentGlintProvider, rawEnchantmentGlintProvider);
    }

    public RenderResult render(int width, int height, int internalWidth, int internalHeight, ResourceManager manager, Function<BlockModel, ValuePairs<BlockModel, Map<String, TextureResource>>> postResolveFunction, boolean post1_8, String modelKey, ModelDisplayPosition displayPosition, Map<ModelOverrideType, Float> predicate, Map<String, TextureResource> providedTextures, TintIndexData tintIndexData, boolean enchanted, boolean usePlayerModelPosition, UnaryOperator<BufferedImage> enchantmentGlintProvider, Function<BufferedImage, RawEnchantmentGlintData> rawEnchantmentGlintProvider) {
        if (postResolveFunction == null) {
            postResolveFunction = DEFAULT_POST_RESOLVE_FUNCTION;
        }

        String rejectedReason = null;
        BlockModel blockModel = manager.getModelManager().resolveBlockModel(modelKey, post1_8, predicate);
        ValuePairs<BlockModel, Map<String, TextureResource>> resolveFunctionResult = postResolveFunction.apply(blockModel);
        blockModel = resolveFunctionResult.getFirst();
        Map<String, TextureResource> overrideTextures = resolveFunctionResult.getSecond();
        if (blockModel == null) {
            return new RenderResult(MODEL_NOT_FOUND);
        }

        String cacheKey = cacheKey(width, height, manager.getUuid(), postResolveFunction.hashCode(), modelKey, displayPosition, predicate, cacheKeyProvidedTextures(providedTextures), cacheKeyProvidedTextures(overrideTextures), enchanted);
        CacheObject<?> cachedRender = manager.getResourceRegistry(ICacheManager.IDENTIFIER, ICacheManager.class).getCache(cacheKey);
        if (cachedRender != null) {
            RenderResult cachedResult = (RenderResult) cachedRender.getObject();
            if (cachedResult.isSuccessful()) {
                return cachedResult;
            }
        }

        BufferedImage image = new BufferedImage(internalWidth, internalHeight, BufferedImage.TYPE_INT_ARGB);
        if (blockModel.getRawParent() == null || !blockModel.getRawParent().contains("/")) {
            renderBlockModel(generateStandardRenderModel(blockModel, manager, providedTextures, overrideTextures, tintIndexData, enchanted, false, rawEnchantmentGlintProvider), image, blockModel.getDisplay(displayPosition), blockModel.getGUILight(), usePlayerModelPosition);
        } else if (blockModel.getRawParent().equals(ModelManager.ITEM_BASE)) {
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            for (int i = 0; blockModel.getTextures().containsKey(ModelManager.ITEM_BASE_LAYER + i); i++) {
                TextureResource resource = overrideTextures.getOrDefault("", overrideTextures.get(ModelManager.ITEM_BASE_LAYER + i));
                String resourceLocation = blockModel.getTextures().get(ModelManager.ITEM_BASE_LAYER + i);
                if (!resourceLocation.contains(":")) {
                    resourceLocation = ResourceRegistry.DEFAULT_NAMESPACE + ":" + resourceLocation;
                }
                if (resource == null) {
                    resource = providedTextures.get(resourceLocation);
                }
                if (resource == null) {
                    resource = manager.getTextureManager().getTexture(resourceLocation);
                }
                BufferedImage texture = resource.getTexture();
                if (resource.hasTextureMeta()) {
                    TextureMeta meta = resource.getTextureMeta();
                    if (meta.hasProperties()) {
                        TextureProperties properties = meta.getProperties();
                        if (properties.isBlur()) {
                            texture = ImageUtils.applyGaussianBlur(texture);
                        }
                    }
                    if (meta.hasAnimation()) {
                        TextureAnimation animation = meta.getAnimation();
                        if (animation.hasWidth() && animation.hasHeight()) {
                            texture = ImageUtils.copyAndGetSubImage(texture, 0, 0, animation.getWidth(), animation.getHeight());
                        } else {
                            texture = ImageUtils.copyAndGetSubImage(texture, 0, 0, texture.getWidth(), texture.getWidth());
                        }
                    }
                }
                g.drawImage(texture, 0, 0, image.getWidth(), image.getHeight(), null);
            }
            g.dispose();
            image = tintIndexData.applyTint(image, 0);
            if (enchanted) {
                image = enchantmentGlintProvider.apply(image);
            }
        } else {
            rejectedReason = blockModel.getRawParent();
        }
        RenderResult result;
        if (rejectedReason == null) {
            result = new RenderResult(ImageUtils.resizeImageQuality(image, width, height));
        } else {
            result = new RenderResult(rejectedReason);
        }
        manager.getResourceRegistry(ICacheManager.IDENTIFIER, ICacheManager.class).putCache(cacheKey, result);
        return result;
    }

    private Model generateItemRenderModel(double width, double height, double depth, BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        double intervalX = (1.0 / (double) w) * width;
        double intervalY = (1.0 / (double) h) * height;
        double z = depth / 2 - 0.5;
        List<Hexahedron> hexahedrons = new ArrayList<>();
        int[] colors = image.getRGB(0, 0, w, h, null, 0, w);
        hexahedrons.add(Hexahedron.fromCorners(new Point3D(0, 0, z), new Point3D(width, height, z + 1), new BufferedImage[]{null, null, ImageUtils.flipHorizontal(image), null, ImageUtils.copyImage(image), null}));
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int color = colors[y * w + x];
                if (ColorUtils.getAlpha(color) > 0) {
                    BufferedImage pixel = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
                    pixel.setRGB(0, 0, color);
                    BufferedImage[] imageArray = new BufferedImage[6];
                    imageArray[0] = ColorUtils.getAlpha(ImageUtils.getRGB(colors, x, y + 1, w, h)) <= 0 ? ImageUtils.copyImage(pixel) : null; //u
                    imageArray[1] = ColorUtils.getAlpha(ImageUtils.getRGB(colors, x, y - 1, w, h)) <= 0 ? ImageUtils.copyImage(pixel) : null; //d
                    imageArray[2] = null; //n
                    imageArray[3] = ColorUtils.getAlpha(ImageUtils.getRGB(colors, x + 1, y, w, h)) <= 0 ? ImageUtils.copyImage(pixel) : null; //e
                    imageArray[4] = null; //s
                    imageArray[5] = ColorUtils.getAlpha(ImageUtils.getRGB(colors, x - 1, y, w, h)) <= 0 ? ImageUtils.copyImage(pixel) : null; //w
                    if (!CustomArrayUtils.allNull(imageArray)) {
                        double scaledX = (double) x * intervalX;
                        double scaledY = height - (double) y * intervalY;
                        hexahedrons.add(Hexahedron.fromCorners(new Point3D(scaledX, scaledY, z), new Point3D(scaledX + intervalX, scaledY - intervalY, z + 1), imageArray));
                    }
                }
            }
        }
        return new Model(hexahedrons);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private Model generateStandardRenderModel(BlockModel blockModel, ResourceManager manager, Map<String, TextureResource> providedTextures, Map<String, TextureResource> overrideTextures, TintIndexData tintIndexData, boolean enchanted, boolean skin, Function<BufferedImage, RawEnchantmentGlintData> rawEnchantmentGlintProvider) {
        Map<String, BufferedImage> cachedResize = new ConcurrentHashMap<>();
        Map<String, RawEnchantmentGlintData> cachedEnchantmentGlint = new ConcurrentHashMap<>();
        List<ModelElement> elements = new ArrayList<>(blockModel.getElements());
        List<Hexahedron> hexahedrons = new ArrayList<>(elements.size());
        Queue<Future<Hexahedron>> tasks = new ConcurrentLinkedQueue<>();
        Iterator<ModelElement> itr = elements.iterator();
        while (itr.hasNext()) {
            ModelElement element = itr.next();
            tasks.add(renderingService.submit(() -> {
                ModelElementRotation rotation = element.getRotation();
                BufferedImage[] images = new BufferedImage[6];
                Hexahedron hexahedron = Hexahedron.fromCorners(new Point3D(element.getFrom().getX(), element.getFrom().getY(), element.getFrom().getZ()), new Point3D(element.getTo().getX(), element.getTo().getY(), element.getTo().getZ()), images);
                BufferedImage[][] overlayImages = new BufferedImage[6][];
                BlendingModes[][] overlayBlendMode = new BlendingModes[6][];
                int i = 0;
                for (ModelFaceSide side : ModelFaceSide.values()) {
                    ModelFace faceData = element.getFace(side);
                    if (faceData == null) {
                        images[i] = null;
                    } else {
                        ModelFaceSide cullface = faceData.getCullface();
                        if (cullface != null) {
                            Face face = hexahedron.getByDirectionOrder().get(i);
                            if (ModelUtils.shouldTriggerCullface(face, cullface)) {
                                switch (cullface) {
                                    case UP -> face.setCullface(hexahedron.getDownFace());
                                    case DOWN -> face.setCullface(hexahedron.getUpFace());
                                    case NORTH -> face.setCullface(hexahedron.getSouthFace());
                                    case EAST -> face.setCullface(hexahedron.getWestFace());
                                    case SOUTH -> face.setCullface(hexahedron.getNorthFace());
                                    case WEST -> face.setCullface(hexahedron.getEastFace());
                                }
                            }
                        }
                        TextureUV uv = faceData.getUV();
                        TextureResource resource = findKey(blockModel.getTextures(), faceData.getRawTexture()).stream().findFirst().map(overrideTextures::get).orElse(null);
                        String texture = faceData.getTexture();
                        if (resource == null) {
                            resource = providedTextures.get(texture);
                        }
                        if (resource == null) {
                            resource = manager.getTextureManager().getTexture(texture, false);
                        }
                        if (resource == null || !resource.isTexture()) {
                            images[i] = null;
                        } else if (uv != null && (uv.getXDiff() == 0 || uv.getYDiff() == 0)) {
                            images[i] = null;
                        } else {
                            BufferedImage cached = cachedResize.get(texture);
                            if (cached == null) {
                                cached = resource.getTexture();
                                if (resource.hasTextureMeta()) {
                                    TextureMeta meta = resource.getTextureMeta();
                                    if (meta.hasProperties()) {
                                        TextureProperties properties = meta.getProperties();
                                        if (properties.isBlur()) {
                                            cached = ImageUtils.applyGaussianBlur(cached);
                                        }
                                    }
                                    if (meta.hasAnimation()) {
                                        TextureAnimation animation = meta.getAnimation();
                                        if (animation.hasWidth() && animation.hasHeight()) {
                                            cached = ImageUtils.copyAndGetSubImage(cached, 0, 0, animation.getWidth(), animation.getHeight());
                                        } else {
                                            cached = ImageUtils.copyAndGetSubImage(cached, 0, 0, cached.getWidth(), cached.getWidth());
                                        }
                                    }
                                }
                                if (cached.getWidth() > cached.getHeight()) {
                                    cached = ImageUtils.resizeImageFillWidth(cached, skin ? SKIN_RESOLUTION : TEXTURE_RESOLUTION);
                                } else {
                                    cached = ImageUtils.resizeImageFillHeight(cached, skin ? SKIN_RESOLUTION : TEXTURE_RESOLUTION);
                                }
                                cachedResize.put(texture, cached);
                            }
                            BufferedImage image = cached;

                            if (uv == null) {
                                Point3D[] points;
                                double x1;
                                double y1;
                                double x2;
                                double y2;
                                switch (side) {
                                    case DOWN -> {
                                        points = hexahedron.getDownFace().getPoints();
                                        x1 = points[2].z;
                                        y1 = points[2].x;
                                        x2 = points[0].z;
                                        y2 = points[0].x;
                                    }
                                    case EAST -> {
                                        points = hexahedron.getEastFace().getPoints();
                                        x1 = points[2].z;
                                        y1 = points[2].y;
                                        x2 = points[0].z;
                                        y2 = points[0].y;
                                    }
                                    case NORTH -> {
                                        points = hexahedron.getNorthFace().getPoints();
                                        x1 = points[2].x;
                                        y1 = points[2].y;
                                        x2 = points[0].x;
                                        y2 = points[0].y;
                                    }
                                    case SOUTH -> {
                                        points = hexahedron.getSouthFace().getPoints();
                                        x1 = points[0].x;
                                        y1 = points[2].y;
                                        x2 = points[2].x;
                                        y2 = points[0].y;
                                    }
                                    case UP -> {
                                        points = hexahedron.getUpFace().getPoints();
                                        x1 = points[0].x;
                                        y1 = points[0].z;
                                        x2 = points[2].x;
                                        y2 = points[2].z;
                                    }
                                    case WEST, default -> {
                                        points = hexahedron.getWestFace().getPoints();
                                        x1 = points[0].z;
                                        y1 = points[2].y;
                                        x2 = points[2].z;
                                        y2 = points[0].y;
                                    }
                                }
                                uv = new TextureUV(x1, y1, x2, y2);
                            }
                            int width = image.getWidth();
                            int height = image.getHeight();
                            double scale = (double) width / 16.0;
                            uv = uv.getScaled(scale, ((double) height / (double) width) * scale);
                            int x1;
                            int y1;
                            int dX;
                            int dY;
                            if (uv.isVerticallyFlipped()) {
                                y1 = (int) Math.ceil(height - uv.getY1());
                                dY = Math.abs((int) Math.floor(height - uv.getY2()) - y1);
                            } else {
                                y1 = (int) Math.ceil(uv.getY1());
                                dY = Math.abs((int) Math.floor(uv.getY2()) - y1);
                            }
                            if (uv.isHorizontallyFlipped()) {
                                x1 = (int) Math.ceil(width - uv.getX1());
                                dX = Math.abs((int) Math.floor(width - uv.getX2()) - x1);
                            } else {
                                x1 = (int) Math.ceil(uv.getX1());
                                dX = Math.abs((int) Math.floor(uv.getX2()) - x1);
                            }
                            image = ImageUtils.copyAndGetSubImage(image, x1, y1, Math.max(1, dX), Math.max(1, dY), uv.isHorizontallyFlipped(), uv.isVerticallyFlipped());
                            int rotationAngle = faceData.getRotation();
                            if (rotationAngle % 360 != 0) {
                                image = ImageUtils.rotateImageByDegrees(image, rotationAngle);
                            }
                            image = tintIndexData.applyTint(image, faceData.getTintindex());
                            if (enchanted) {
                                String key = image.getWidth() + "x" + image.getHeight();
                                RawEnchantmentGlintData overlayResult = cachedEnchantmentGlint.get(key);
                                if (overlayResult == null) {
                                    cachedEnchantmentGlint.put(key, overlayResult = rawEnchantmentGlintProvider.apply(image));
                                }
                                overlayImages[i] = overlayResult.getOverlay().toArray(EMPTY_IMAGE_ARRAY);
                                overlayBlendMode[i] = overlayResult.getBlending().stream().map(BlendingUtils::convert).toArray(BlendingModes[]::new);
                            }

                            images[i] = image;
                        }
                    }
                    i++;
                }
                hexahedron.setImage(images);
                hexahedron.setOverlay(overlayImages);
                hexahedron.setOverlayBlendingMode(overlayBlendMode);
                hexahedron.setOverlayAdditionFactor(OVERLAY_ADDITION_FACTORS);
                if (rotation != null) {
                    hexahedron.translate(-rotation.getOrigin().getX(), -rotation.getOrigin().getY(), -rotation.getOrigin().getZ());
                    if (rotation.isRescale()) {
                        double absAngle = Math.abs(rotation.getAngle());
                        if (absAngle != 0F) {
                            if (absAngle == 22.5F) {
                                hexahedron.scale(RESCALE_22_5, RESCALE_22_5, RESCALE_22_5);
                            } else if (absAngle == 45F) {
                                hexahedron.scale(RESCALE_45, RESCALE_45, RESCALE_45);
                            } else {
                                throw new IllegalArgumentException("Element rotation can only be between angles 45 and -45 with 22.5 degrees increments");
                            }
                        }
                    }
                    switch (rotation.getAxis()) {
                        case X -> hexahedron.rotate(rotation.getAngle(), 0, 0, false);
                        case Y -> hexahedron.rotate(0, rotation.getAngle(), 0, false);
                        case Z, default -> hexahedron.rotate(0, 0, rotation.getAngle(), false);
                    }
                    hexahedron.translate(rotation.getOrigin().getX(), rotation.getOrigin().getY(), rotation.getOrigin().getZ());
                }
                return hexahedron;
            }));
            itr.remove();
        }
        while (!tasks.isEmpty() || !elements.isEmpty()) {
            Future<Hexahedron> task = tasks.poll();
            if (task == null) {
                try {
                    TimeUnit.MILLISECONDS.sleep(1);
                } catch (InterruptedException ignored) {
                }
            } else {
                try {
                    hexahedrons.add(task.get());
                } catch (Throwable e) {
                    new RuntimeException("Unable to generate model: " + blockModel.getResourceLocation(), e).printStackTrace();
                    for (Future<Hexahedron> t : tasks) {
                        t.cancel(true);
                    }
                    if (e instanceof OutOfMemoryError) {
                        System.gc();
                    }
                    BufferedImage[] missingTextures = new BufferedImage[6];
                    Arrays.fill(missingTextures, TextureManager.getMissingImage(16, 16));
                    return new Model(Hexahedron.fromCorners(new Point3D(0, 0, 0), new Point3D(16, 16, 16), missingTextures));
                }
            }
        }
        return new Model(hexahedrons);
    }

    private void renderPlayerModel(Model renderModel, BufferedImage image, ModelGUILight lightData) {
        AffineTransform baseTransform = AffineTransform.getTranslateInstance(image.getWidth() / 2.0, (double) image.getHeight() / 7 * 5);
        baseTransform.concatenate(AffineTransform.getScaleInstance(image.getWidth() / 39.09375, image.getWidth() / 39.09375));
        renderModel.translate(-16 / 2.0, -16 / 2.0, -16 / 2.0);
        renderModel.updateLighting(lightData.getLightVector(), lightData.getAmbientLevel(), lightData.getMaxLevel());
        long start = System.currentTimeMillis();
        renderModel.render(image, true, baseTransform, BlendingModes.NORMAL, renderingService).join();
        InteractiveChatDiscordSrvAddon.plugin.playerModelRenderingTimes.add((int) Math.min(System.currentTimeMillis() - start, Integer.MAX_VALUE));
    }

    private void renderBlockModel(Model renderModel, BufferedImage image, ModelDisplay displayData, ModelGUILight lightData, boolean usePlayerPosition) {
        AffineTransform baseTransform;
        if (usePlayerPosition) {
            renderModel.translate(-16 / 2.0, -16 / 2.0, -16 / 2.0);
            renderModel.rotate(0, 180, 0, false);
            renderModel.translate(16 / 2.0, 16 / 2.0, 16 / 2.0);
            baseTransform = AffineTransform.getTranslateInstance(image.getWidth() / 2.0, (double) image.getHeight() / 7 * 5);
            baseTransform.concatenate(AffineTransform.getScaleInstance(image.getWidth() / 39.09375, image.getWidth() / 39.09375));
        } else {
            baseTransform = AffineTransform.getTranslateInstance(image.getWidth() / 2.0, image.getHeight() / 2.0);
            baseTransform.concatenate(AffineTransform.getScaleInstance(image.getWidth() / 16.0, image.getHeight() / 16.0));
        }
        renderModel.translate(-16 / 2.0, -16 / 2.0, -16 / 2.0);
        if (displayData != null) {
            Coordinates3D scale = displayData.getScale();
            renderModel.scale(scale.getX(), scale.getY(), scale.getZ());
            Coordinates3D rotation = displayData.getRotation();
            renderModel.rotate(rotation.getX(), rotation.getY(), rotation.getZ(), false);
            Coordinates3D transform = displayData.getTranslation();
            renderModel.translate(transform.getX(), transform.getY(), transform.getZ());
        }
        renderModel.updateLighting(lightData.getLightVector(), lightData.getAmbientLevel(), lightData.getMaxLevel());
        renderModel.render(image, true, baseTransform, BlendingModes.NORMAL, renderingService).join();
    }

    private String cacheKey(Object... obj) {
        return Arrays.stream(obj).map(each -> {
            if (each == null) {
                return "null";
            } else if (each instanceof Map) {
                return cacheKeyMap((Map<?, ?>) each);
            }
            return each.toString();
        }).collect(Collectors.joining("/", CACHE_KEY + "/", ""));
    }

    private String cacheKeyMap(Map<?, ?> map) {
        Comparator<Entry<?, ?>> c = Comparator.comparing(entry -> entry.getKey() == null ? "null" : entry.getKey().toString());
        c = c.thenComparing(entry -> entry.getValue() == null ? "null" : entry.getValue().toString());
        return map.entrySet().stream().sorted(c).map(entry -> (entry.getKey() == null ? "null" : entry.getKey().toString()) + ":" + (entry.getValue() == null ? "null" : entry.getValue().toString())).collect(Collectors.joining(", ", "{", "}"));
    }

    private String cacheKeyProvidedTextures(Map<String, TextureResource> providedTextures) {
        return providedTextures.entrySet().stream().map(entry -> {
            TextureResource resource = entry.getValue();
            if (resource.isTexture()) {
                return entry.getKey() + ":" + ImageUtils.hash(resource.getTexture());
            } else if (resource.hasFile()) {
                return entry.getKey() + ":" + resource.getFile().getAbsolutePath();
            }
            return entry.getKey() + ":" + resource;
        }).collect(Collectors.joining(", ", "{", "}"));
    }

    private String cacheKeyResolvedItems(Map<PlayerModelItem, ValuePairs<BlockModel, Map<String, TextureResource>>> modelItems) {
        return modelItems.entrySet().stream().map(entry -> {
            PlayerModelItem resource = entry.getKey();
            return entry.getKey() + ":[" + resource.getPosition() + ", " + resource.getModelKey() + ", " + cacheKeyMap(resource.getPredicate()) + ", " + resource.isEnchanted() + ", " + cacheKeyProvidedTextures(resource.getProvidedTextures()) + ", " + resource.getPostResolveFunction().hashCode() + ", " + cacheKeyProvidedTextures(entry.getValue().getSecond()) + "]";
        }).collect(Collectors.joining(", ", "{", "}"));
    }

    private <K, V> Set<K> findKey(Map<K, V> map, V value) {
        Set<K> result = new HashSet<>();
        for (Entry<K, V> entry : map.entrySet()) {
            if (entry.getValue().equals(value)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public enum PlayerModelItemPosition {

        HELMET(new Coordinates3D(3.05, 22.05, 3.05), 0.62, false, false, ModelDisplayPosition.HEAD),
        RIGHT_HAND(new Coordinates3D(7.3, -2.5, 4.05), 0.9, false, true, ModelDisplayPosition.THIRDPERSON_RIGHTHAND),
        LEFT_HAND(new Coordinates3D(-5.6, -2.5, 4.05), 0.9, true, true, ModelDisplayPosition.THIRDPERSON_LEFTHAND);

        private final Coordinates3D defaultTranslate;
        private final double scale;
        private final boolean literalFlipped;
        private final boolean yIsZAxis;
        private final ModelDisplayPosition modelDisplayPosition;

        PlayerModelItemPosition(Coordinates3D defaultTranslate, double scale, boolean literalFlipped, boolean yIsZAxis, ModelDisplayPosition modelDisplayPosition) {
            this.defaultTranslate = defaultTranslate;
            this.scale = scale;
            this.literalFlipped = literalFlipped;
            this.yIsZAxis = yIsZAxis;
            this.modelDisplayPosition = modelDisplayPosition;
        }

        public ModelDisplayPosition getModelDisplayPosition() {
            return modelDisplayPosition;
        }

        public Coordinates3D getDefaultTranslate() {
            return defaultTranslate;
        }

        public double getScale() {
            return scale;
        }

        public boolean isLiteralFlipped() {
            return literalFlipped;
        }

        public boolean yIsZAxis() {
            return yIsZAxis;
        }

    }

    public static class PlayerModelItem {

        private final PlayerModelItemPosition position;
        private final String modelKey;
        private final Function<BlockModel, ValuePairs<BlockModel, Map<String, TextureResource>>> postResolveFunction;
        private final Map<ModelOverrideType, Float> predicate;
        private final boolean enchanted;
        private final Map<String, TextureResource> providedTextures;
        private final TintIndexData tintIndexData;
        private final UnaryOperator<BufferedImage> enchantmentGlintProvider;
        private final Function<BufferedImage, RawEnchantmentGlintData> rawEnchantmentGlintProvider;

        public PlayerModelItem(PlayerModelItemPosition position, String modelKey, Function<BlockModel, ValuePairs<BlockModel, Map<String, TextureResource>>> postResolveFunction, Map<ModelOverrideType, Float> predicate, boolean enchanted, Map<String, TextureResource> providedTextures, TintIndexData tintIndexData, UnaryOperator<BufferedImage> enchantmentGlintProvider, Function<BufferedImage, RawEnchantmentGlintData> rawEnchantmentGlintProvider) {
            this.position = position;
            this.modelKey = modelKey;
            if (postResolveFunction == null) {
                this.postResolveFunction = DEFAULT_POST_RESOLVE_FUNCTION;
            } else {
                this.postResolveFunction = postResolveFunction;
            }
            this.predicate = predicate;
            this.enchanted = enchanted;
            this.providedTextures = providedTextures;
            this.tintIndexData = tintIndexData;
            this.enchantmentGlintProvider = enchantmentGlintProvider;
            this.rawEnchantmentGlintProvider = rawEnchantmentGlintProvider;
        }

        public PlayerModelItemPosition getPosition() {
            return position;
        }

        public String getModelKey() {
            return modelKey;
        }

        public Function<BlockModel, ValuePairs<BlockModel, Map<String, TextureResource>>> getPostResolveFunction() {
            return postResolveFunction;
        }

        public Map<ModelOverrideType, Float> getPredicate() {
            return predicate;
        }

        public boolean isEnchanted() {
            return enchanted;
        }

        public Map<String, TextureResource> getProvidedTextures() {
            return providedTextures;
        }

        public TintIndexData getTintIndexData() {
            return tintIndexData;
        }

        public UnaryOperator<BufferedImage> getEnchantmentGlintProvider() {
            return enchantmentGlintProvider;
        }

        public Function<BufferedImage, RawEnchantmentGlintData> getRawEnchantmentGlintProvider() {
            return rawEnchantmentGlintProvider;
        }

    }

    public static class RawEnchantmentGlintData {

        private final List<BufferedImage> overlay;
        private final List<OpenGLBlending> blending;

        public RawEnchantmentGlintData(List<BufferedImage> overlay, List<OpenGLBlending> blending) {
            this.overlay = overlay;
            this.blending = blending;
        }

        public List<BufferedImage> getOverlay() {
            return overlay;
        }

        public List<OpenGLBlending> getBlending() {
            return blending;
        }

    }

    public static class RenderResult {

        private final BufferedImage image;
        private final String rejectedReason;

        public RenderResult(BufferedImage image) {
            this.image = image;
            this.rejectedReason = null;
        }

        public RenderResult(String rejectedReason) {
            this.image = null;
            this.rejectedReason = rejectedReason;
        }

        public boolean isSuccessful() {
            return image != null;
        }

        public BufferedImage getImage() {
            return ImageUtils.copyImage(image);
        }

        public String getRejectedReason() {
            return rejectedReason;
        }

    }

}

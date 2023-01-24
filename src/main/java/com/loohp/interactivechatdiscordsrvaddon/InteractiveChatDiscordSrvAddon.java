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

package com.loohp.interactivechatdiscordsrvaddon;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.loohp.interactivechatdiscordsrvaddon.AssetsDownloader.ServerResourcePackDownloadResult;
import com.loohp.interactivechatdiscordsrvaddon.debug.Debug;
import com.loohp.interactivechatdiscordsrvaddon.graphics.ImageGeneration;
import com.loohp.interactivechatdiscordsrvaddon.graphics.ImageUtils;
import com.loohp.interactivechatdiscordsrvaddon.listeners.ICPlayerEvents;
import com.loohp.interactivechatdiscordsrvaddon.registry.InteractiveChatRegistry;
import com.loohp.interactivechatdiscordsrvaddon.registry.ResourceRegistry;
import com.loohp.interactivechatdiscordsrvaddon.resources.*;
import com.loohp.interactivechatdiscordsrvaddon.resources.ResourceManager.ModManagerSupplier;
import com.loohp.interactivechatdiscordsrvaddon.resources.fonts.FontManager;
import com.loohp.interactivechatdiscordsrvaddon.resources.mods.ModManager;
import com.loohp.interactivechatdiscordsrvaddon.resources.mods.chime.ChimeManager;
import com.loohp.interactivechatdiscordsrvaddon.resources.mods.optifine.OptifineManager;
import com.loohp.interactivechatdiscordsrvaddon.utils.ResourcePackUtils;
import com.loohp.interactivechatdiscordsrvaddon.utils.TranslationKeyUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class InteractiveChatDiscordSrvAddon extends JavaPlugin implements Listener {
    public static final String CONFIG_ID = "interactivechatdiscordsrvaddon_config";
    public static InteractiveChatDiscordSrvAddon plugin;
    public static InteractiveChat interactivechat;
    public static boolean isReady = false;
    public static boolean debug = false;
    public final AtomicLong imageCounter = new AtomicLong(0);
    public final AtomicLong inventoryImageCounter = new AtomicLong(0);
    public final List<String> resourceOrder = new ArrayList<>();
    protected final ReentrantLock resourceReloadLock = new ReentrantLock(true);
    protected final Map<String, byte[]> extras = new ConcurrentHashMap<>();
    public boolean renderHandHeldItems = true;
    public String itemDisplaySingle = "";
    public String itemDisplayMultiple = "";
    public String reloadConfigMessage;
    public String reloadTextureMessage;
    public String defaultResourceHashLang;
    public String loadedResourcesLang;
    public boolean imageWhitelistEnabled = false;
    public List<String> whitelistedImageUrls = new ArrayList<>();
    public int cacheTimeout = 1200;
    public boolean reducedAssetsDownloadInfo = false;
    public String language = "en_us";
    public PlaceholderCooldownManager placeholderCooldownManager;
    public String defaultResourceHash = "N/A";
    public boolean forceUnicode = false;
    public boolean includeServerResourcePack = true;
    public String alternateResourcePackURL = "";
    public String alternateResourcePackHash = "";
    public boolean optifineCustomTextures = true;
    public boolean chimeOverrideModels = true;
    public int embedDeleteAfter = 0;
    public boolean showDurability = true;
    public boolean showArmorColor = true;
    public boolean showMapScale = true;
    public boolean showFireworkRocketDetailsInCrossbow = true;
    public boolean allowSlotSelection = true;
    public boolean showMaps = true;
    public boolean showBooks = true;
    public boolean showContainers = true;
    public int rendererThreads = -1;
    public ModelRenderer modelRenderer;
    public ExecutorService mediaReadingService;
    private ResourceManager resourceManager;

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> cachePlayerSkin(ICPlayerFactory.getICPlayer(event.getPlayer())), 40);
    }

    public ResourceManager getResourceManager() {
        if (resourceManager == null) {
            throw new ResourceLoadingException("Resources are still being loaded, please wait!");
        }
        return resourceManager;
    }

    @EventHandler
    public void onInteractiveChatReload(InteractiveChatConfigReloadEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> placeholderCooldownManager.reloadPlaceholders(), 5);
    }

    public boolean isResourceManagerReady() {
        return resourceManager != null;
    }

    public byte[] getExtras(String str) {
        return extras.get(str);
    }

    @Override
    public void onLoad() {

    }

    @Override
    public void onEnable() {
        plugin = this;
        interactivechat = InteractiveChat.plugin;


        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        AssetsDownloader.loadLibraries(getDataFolder());

        try {
            Config.loadConfig(CONFIG_ID, new File(getDataFolder(), "config.yml"), getClass().getClassLoader().getResourceAsStream("config.yml"), getClass().getClassLoader().getResourceAsStream("config.yml"), true);
        } catch (IOException e) {
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        reloadConfig();


        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new ICPlayerEvents(), this);
        getServer().getPluginManager().registerEvents(new Debug(), this);
        getCommand("interactivechatdiscordsrv").setExecutor(new Commands());

        File resourcepacks = new File(getDataFolder(), "resourcepacks");
        if (!resourcepacks.exists()) {
            File resources = new File(getDataFolder(), "resources");
            if (resources.exists() && resources.isDirectory()) {
                try {
                    Files.move(resources.toPath(), resourcepacks.toPath(), StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException e) {
                    getServer().getConsoleSender().sendMessage(ChatColor.RED + "[ICDiscordSrvAddon] Unable to move folder, are any files opened?");
                    e.printStackTrace();
                    getServer().getPluginManager().disablePlugin(this);
                    return;
                }
            } else {
                resourcepacks.mkdirs();
            }
        }
        File serverResourcePack = new File(getDataFolder(), "server-resource-packs");
        if (!serverResourcePack.exists()) {
            serverResourcePack.mkdirs();
        }

        if (!compatible()) {
            for (int i = 0; i < 10; i++) {
                getServer().getConsoleSender().sendMessage(ChatColor.RED + "[ICDiscordSrvAddon] VERSION NOT COMPATIBLE WITH INSTALLED INTERACTIVECHAT VERSION, PLEASE UPDATE BOTH TO LATEST!!!!");
            }
            getServer().getPluginManager().disablePlugin(this);
            return;
        } else {
            getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[ICDiscordSrvAddon] InteractiveChat DiscordSRV Addon has been Enabled!");
        }

        reloadTextures(false, false);
        modelRenderer = new ModelRenderer(str -> new ThreadFactoryBuilder().setNameFormat(str).build(), () -> InteractiveChatDiscordSrvAddon.plugin.cacheTimeout, () -> {
            if (rendererThreads > 0) {
                return rendererThreads;
            }
            return Runtime.getRuntime().availableProcessors() + rendererThreads;
        });

        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("InteractiveChatDiscordSRVAddon Async Media Reading Thread #%d").build();
        mediaReadingService = Executors.newFixedThreadPool(4, factory);

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            for (ICPlayer player : ICPlayerFactory.getOnlineICPlayers()) {
                cachePlayerSkin(player);
            }
            AssetsDownloader.loadExtras();
        }, 600, 6000);

        Bukkit.getScheduler().runTask(this, () -> placeholderCooldownManager = new PlaceholderCooldownManager());
    }


    private void cachePlayerSkin(ICPlayer player) {
        Debug.debug("Caching skin for player " + player.getName() + " (" + player.getUniqueId() + ")");
        if (player.isLocal()) {
            try {
                UUID uuid = player.getUniqueId();
                JSONObject json = (JSONObject) new JSONParser().parse(SkinUtils.getSkinJsonFromProfile(player.getLocalPlayer()));
                String value = (String) ((JSONObject) ((JSONObject) json.get("textures")).get("SKIN")).get("url");
                BufferedImage skin = ImageUtils.downloadImage(value);
                resourceManager.getResourceRegistry(ICacheManager.IDENTIFIER, ICacheManager.class).putCache(uuid + value + ImageGeneration.PLAYER_SKIN_CACHE_KEY, skin);
            } catch (Exception e) {
            }
        } else {
            try {
                UUID uuid = player.getUniqueId();
                String value = SkinUtils.getSkinURLFromUUID(uuid);
                BufferedImage skin = ImageUtils.downloadImage(value);
                resourceManager.getResourceRegistry(ICacheManager.IDENTIFIER, ICacheManager.class).putCache(uuid + "null" + ImageGeneration.PLAYER_SKIN_CACHE_KEY, skin);
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void onDisable() {
        modelRenderer.close();
        mediaReadingService.shutdown();
        if (resourceManager != null) {
            resourceManager.close();
        }
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[ICDiscordSrvAddon] InteractiveChat DiscordSRV Addon has been Disabled!");
    }

    public boolean compatible() {
        try {
            return Registry.class.getField("INTERACTIVE_CHAT_DISCORD_SRV_ADDON_COMPATIBLE_VERSION").getInt(null) == InteractiveChatRegistry.class.getField("INTERACTIVE_CHAT_DISCORD_SRV_ADDON_COMPATIBLE_VERSION").getInt(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void reloadConfig() {
        Config config = Config.getConfig(CONFIG_ID);
        config.reload();

        reloadConfigMessage = ChatColor.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.ReloadConfig"));
        reloadTextureMessage = ChatColor.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.ReloadTexture"));
        defaultResourceHashLang = ChatColor.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.StatusCommand.DefaultResourceHash"));
        loadedResourcesLang = ChatColor.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.StatusCommand.LoadedResources"));

        debug = config.getConfiguration().getBoolean("Debug.PrintInfoToConsole");

        resourceOrder.clear();
        List<String> order = config.getConfiguration().getStringList("Resources.Order");
        ListIterator<String> itr = order.listIterator(order.size());
        while (itr.hasPrevious()) {
            String pack = itr.previous();
            resourceOrder.add(pack);
        }

        includeServerResourcePack = config.getConfiguration().getBoolean("Resources.IncludeServerResourcePack");
        alternateResourcePackURL = config.getConfiguration().getString("Resources.AlternateServerResourcePack.URL");
        alternateResourcePackHash = config.getConfiguration().getString("Resources.AlternateServerResourcePack.Hash");
        optifineCustomTextures = config.getConfiguration().getBoolean("Resources.OptifineCustomTextures");
        chimeOverrideModels = config.getConfiguration().getBoolean("Resources.ChimeOverrideModels") && InteractiveChat.version.isNewerOrEqualTo(MCVersion.V1_16);

        renderHandHeldItems = config.getConfiguration().getBoolean("InventoryImage.Inventory.RenderHandHeldItems");

        imageWhitelistEnabled = config.getConfiguration().getBoolean("DiscordAttachments.RestrictImageUrl.Enabled");
        whitelistedImageUrls = config.getConfiguration().getStringList("DiscordAttachments.RestrictImageUrl.Whitelist");


        cacheTimeout = config.getConfiguration().getInt("Settings.CacheTimeout") * 20;

        reducedAssetsDownloadInfo = config.getConfiguration().getBoolean("Settings.ReducedAssetsDownloadInfo");

        embedDeleteAfter = config.getConfiguration().getInt("Settings.EmbedDeleteAfter");

        itemDisplaySingle = config.getConfiguration().getString("InventoryImage.Item.EmbedDisplay.Single");
        itemDisplayMultiple = config.getConfiguration().getString("InventoryImage.Item.EmbedDisplay.Multiple");
        showDurability = config.getConfiguration().getBoolean("ToolTipSettings.ShowDurability");
        showArmorColor = config.getConfiguration().getBoolean("ToolTipSettings.ShowArmorColor");
        showMapScale = config.getConfiguration().getBoolean("ToolTipSettings.ShowMapScale");
        showFireworkRocketDetailsInCrossbow = config.getConfiguration().getBoolean("ToolTipSettings.ShowFireworkRocketDetailsInCrossbow");

        allowSlotSelection = config.getConfiguration().getBoolean("DiscordItemDetailsAndInteractions.AllowInventorySelection");
        showMaps = config.getConfiguration().getBoolean("DiscordItemDetailsAndInteractions.ShowMaps");
        showBooks = config.getConfiguration().getBoolean("DiscordItemDetailsAndInteractions.ShowBooks");
        showContainers = config.getConfiguration().getBoolean("DiscordItemDetailsAndInteractions.ShowContainers");
        rendererThreads = config.getConfiguration().getInt("Settings.RendererSettings.RendererThreads");
        language = config.getConfiguration().getString("Resources.Language");
        LanguageUtils.loadTranslations(language);
        forceUnicode = config.getConfiguration().getBoolean("Resources.ForceUnicodeFont");
    }


    public void reloadTextures(boolean redownload, boolean clean, CommandSender... receivers) {
        CommandSender[] senders;
        if (Arrays.stream(receivers).noneMatch(each -> each.equals(Bukkit.getConsoleSender()))) {
            senders = Arrays.copyOf(receivers, receivers.length + 1);
            senders[senders.length - 1] = Bukkit.getConsoleSender();
        } else {
            senders = receivers;
        }

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                if (!resourceReloadLock.tryLock(0, TimeUnit.MILLISECONDS)) {
                    sendMessage(ChatColor.YELLOW + "Resource reloading already in progress!", senders);
                    return;
                }
                isReady = false;
                if (InteractiveChatDiscordSrvAddon.plugin.isResourceManagerReady()) {
                    Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                        InteractiveChatDiscordSrvAddon.plugin.getResourceManager().close();
                        return null;
                    }).get();
                }
                try {
                    AssetsDownloader.loadAssets(getDataFolder(), redownload, clean, receivers);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                List<String> resourceList = new ArrayList<>();
                resourceList.add("Default");
                resourceList.addAll(resourceOrder);

                File serverResourcePackFolder = new File(getDataFolder(), "server-resource-packs");
                File serverResourcePack = null;
                if (includeServerResourcePack) {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.GRAY + "[ICDiscordSrvAddon] Checking for server resource pack...");
                    ServerResourcePackDownloadResult result = AssetsDownloader.downloadServerResourcePack(serverResourcePackFolder);
                    serverResourcePack = result.getResourcePackFile();
                    if (result.getError() != null) {
                        result.getError().printStackTrace();
                    }
                    switch (result.getType()) {
                        case SUCCESS_NO_CHANGES:
                            sendMessage(ChatColor.GREEN + "[ICDiscordSrvAddon] Server resource pack found with verification hash: No changes", senders);
                            resourceList.add(serverResourcePack.getName());
                            break;
                        case SUCCESS_WITH_HASH:
                            sendMessage(ChatColor.GREEN + "[ICDiscordSrvAddon] Server resource pack found with verification hash: Hash changed, downloaded", senders);
                            resourceList.add(serverResourcePack.getName());
                            break;
                        case SUCCESS_NO_HASH:
                            sendMessage(ChatColor.GREEN + "[ICDiscordSrvAddon] Server resource pack found without verification hash: Downloaded", senders);
                            resourceList.add(serverResourcePack.getName());
                            break;
                        case FAILURE_WRONG_HASH:
                            sendMessage(ChatColor.RED + "[ICDiscordSrvAddon] Server resource pack had wrong hash (expected " + result.getExpectedHash() + ", found " + result.getPackHash() + ")", senders);
                            sendMessage(ChatColor.RED + "[ICDiscordSrvAddon] Server resource pack will not be applied: Hash check failure", senders);
                            break;
                        case FAILURE_DOWNLOAD:
                            sendMessage(ChatColor.RED + "[ICDiscordSrvAddon] Failed to download server resource pack", senders);
                            break;
                        case NO_PACK:
                            Bukkit.getConsoleSender().sendMessage(ChatColor.GRAY + "[ICDiscordSrvAddon] No server resource pack found");
                            break;
                    }
                }

                sendMessage(ChatColor.AQUA + "[ICDiscordSrvAddon] Reloading ResourceManager: " + ChatColor.YELLOW + String.join(", ", resourceList), senders);

                List<ModManagerSupplier<?>> mods = new ArrayList<>();
                if (chimeOverrideModels) {
                    mods.add(ChimeManager::new);
                }
                if (optifineCustomTextures) {
                    mods.add(OptifineManager::new);
                }

                @SuppressWarnings("resource")
                ResourceManager resourceManager = new ResourceManager(
                        InteractiveChat.version.isLegacy(),
                        InteractiveChat.version.isOlderOrEqualTo(MCVersion.V1_18_2),
                        mods,
                        Arrays.asList(CustomItemTextureRegistry.getDefaultSupplier(), ICacheManager.getDefaultSupplier(new File(getDataFolder(), "cache"))),
                        (resourcePackFile, type) -> new ResourceManager.DefaultResourcePackInfo(
                                Component.translatable(TranslationKeyUtils.getResourcePackVanillaName()),
                                ResourcePackUtils.getServerResourcePackVersion(),
                                Component.translatable(TranslationKeyUtils.getResourcePackVanillaDescription()).append(Component.text(" (Modified by LOOHP)"))
                        )
                );

                for (Entry<String, ModManager> entry : resourceManager.getModManagers().entrySet()) {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.GRAY + "[ICDiscordSrvAddon] Registered ModManager \"" + entry.getKey() + "\" of class \"" + entry.getValue().getClass().getName() + "\"");
                }

                resourceManager.getFontManager().setDefaultKey(forceUnicode ? FontManager.UNIFORM_FONT : FontManager.DEFAULT_FONT);
                resourceManager.getLanguageManager().setTranslateFunction(LanguageUtils::getTranslation);
                resourceManager.getLanguageManager().setAvailableLanguagesSupplier(LanguageUtils::getLoadedLanguages);
                resourceManager.getLanguageManager().registerReloadListener(e -> {
                    LanguageUtils.clearPluginTranslations(InteractiveChatDiscordSrvAddon.plugin);
                    for (Entry<String, Map<String, String>> entry : e.getTranslations().entrySet()) {
                        LanguageUtils.loadPluginTranslations(InteractiveChatDiscordSrvAddon.plugin, entry.getKey(), entry.getValue());
                    }
                });

                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "[ICDiscordSrvAddon] Loading \"Default\" resources...");
                resourceManager.loadResources(new File(getDataFolder() + "/built-in", "Default"), ResourcePackType.BUILT_IN, true);
                for (String resourceName : resourceOrder) {
                    try {
                        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "[ICDiscordSrvAddon] Loading \"" + resourceName + "\" resources...");
                        File resourcePackFile = new File(getDataFolder(), "resourcepacks/" + resourceName);
                        ResourcePackInfo info = resourceManager.loadResources(resourcePackFile, ResourcePackType.LOCAL);
                        if (info.getStatus()) {
                            if (info.compareServerPackFormat(ResourceRegistry.RESOURCE_PACK_VERSION) > 0) {
                                sendMessage(ChatColor.YELLOW + "[ICDiscordSrvAddon] Warning: \"" + resourceName + "\" was made for a newer version of Minecraft!", senders);
                            } else if (info.compareServerPackFormat(ResourceRegistry.RESOURCE_PACK_VERSION) < 0) {
                                sendMessage(ChatColor.YELLOW + "[ICDiscordSrvAddon] Warning: \"" + resourceName + "\" was made for an older version of Minecraft!", senders);
                            }
                        } else {
                            if (info.getRejectedReason() == null) {
                                sendMessage(ChatColor.RED + "[ICDiscordSrvAddon] Unable to load \"" + resourceName + "\"", senders);
                            } else {
                                sendMessage(ChatColor.RED + "[ICDiscordSrvAddon] Unable to load \"" + resourceName + "\", Reason: " + info.getRejectedReason(), senders);
                            }
                        }
                    } catch (Exception e) {
                        sendMessage(ChatColor.RED + "[ICDiscordSrvAddon] Unable to load \"" + resourceName + "\"", senders);
                        e.printStackTrace();
                    }
                }
                if (includeServerResourcePack && serverResourcePack != null && serverResourcePack.exists()) {
                    String resourceName = serverResourcePack.getName();
                    try {
                        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "[ICDiscordSrvAddon] Loading \"" + resourceName + "\" resources...");
                        ResourcePackInfo info = resourceManager.loadResources(serverResourcePack, ResourcePackType.SERVER);
                        if (info.getStatus()) {
                            if (info.compareServerPackFormat(ResourceRegistry.RESOURCE_PACK_VERSION) > 0) {
                                sendMessage(ChatColor.YELLOW + "[ICDiscordSrvAddon] Warning: \"" + resourceName + "\" was made for a newer version of Minecraft!", senders);
                            } else if (info.compareServerPackFormat(ResourceRegistry.RESOURCE_PACK_VERSION) < 0) {
                                sendMessage(ChatColor.YELLOW + "[ICDiscordSrvAddon] Warning: \"" + resourceName + "\" was made for an older version of Minecraft!", senders);
                            }
                        } else {
                            if (info.getRejectedReason() == null) {
                                sendMessage(ChatColor.RED + "[ICDiscordSrvAddon] Unable to load \"" + resourceName + "\"", senders);
                            } else {
                                sendMessage(ChatColor.RED + "[ICDiscordSrvAddon] Unable to load \"" + resourceName + "\", Reason: " + info.getRejectedReason(), senders);
                            }
                        }
                    } catch (Exception e) {
                        sendMessage(ChatColor.RED + "[ICDiscordSrvAddon] Unable to load \"" + resourceName + "\"", senders);
                        e.printStackTrace();
                    }
                }

                Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                    InteractiveChatDiscordSrvAddon.plugin.resourceManager = resourceManager;

                    if (resourceManager.getResourcePackInfo().stream().allMatch(ResourcePackInfo::getStatus)) {
                        sendMessage(ChatColor.AQUA + "[ICDiscordSrvAddon] Loaded all resources!", senders);
                        isReady = true;
                    } else {
                        sendMessage(ChatColor.RED + "[ICDiscordSrvAddon] There is a problem while loading resources.", senders);
                    }
                    return null;
                }).get();

                resourceReloadLock.unlock();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }

    public void sendMessage(String message, CommandSender... senders) {
        for (CommandSender sender : senders) {
            sender.sendMessage(message);
        }
    }

}

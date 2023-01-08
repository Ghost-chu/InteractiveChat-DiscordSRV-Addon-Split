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
import com.loohp.interactivechat.InteractiveChat;
import com.loohp.interactivechat.api.events.InteractiveChatConfigReloadEvent;
import com.loohp.interactivechat.config.Config;
import com.loohp.interactivechat.libs.net.kyori.adventure.text.Component;
import com.loohp.interactivechat.libs.org.json.simple.JSONObject;
import com.loohp.interactivechat.libs.org.json.simple.parser.JSONParser;
import com.loohp.interactivechat.objectholders.ICPlayer;
import com.loohp.interactivechat.objectholders.ICPlayerFactory;
import com.loohp.interactivechat.objectholders.PlaceholderCooldownManager;
import com.loohp.interactivechat.registry.Registry;
import com.loohp.interactivechat.utils.*;
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
import com.loohp.interactivechatdiscordsrvaddon.resources.fonts.FontTextureResource;
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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.*;
import java.util.Queue;
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

    protected final ReentrantLock resourceReloadLock = new ReentrantLock(true);
    public final AtomicLong messagesCounter = new AtomicLong(0);
    public final AtomicLong imageCounter = new AtomicLong(0);
    public final AtomicLong inventoryImageCounter = new AtomicLong(0);
    public final AtomicLong attachmentCounter = new AtomicLong(0);
    public final AtomicLong attachmentImageCounter = new AtomicLong(0);
    public final AtomicLong imagesViewedCounter = new AtomicLong(0);
    public final Queue<Integer> playerModelRenderingTimes = new ConcurrentLinkedQueue<>();
    public boolean itemImage = true;
    public boolean invImage = true;
    public boolean enderImage = true;
    public boolean usePlayerInvView = true;
    public boolean renderHandHeldItems = true;
    public String itemDisplaySingle = "";
    public String itemDisplayMultiple = "";
    public Color invColor = Color.black;
    public Color enderColor = Color.black;
    public boolean itemUseTooltipImageOnBaseItem = false;
    public boolean itemAltAir = true;
    public boolean invShowLevel = true;
    public boolean hoverEnabled = true;
    public boolean hoverImage = true;
    public Set<String> hoverIgnore = new HashSet<>();
    public boolean hoverUseTooltipImage = true;
    public String reloadConfigMessage;
    public String reloadTextureMessage;
    public String linkExpired;
    public String interactionExpire;
    public String previewLoading;
    public String accountNotLinked;
    public String unableToRetrieveData;
    public String invalidDiscordChannel;
    public String trueLabel;
    public String falseLabel;
    public String defaultResourceHashLang;
    public String fontsActiveLang;
    public String loadedResourcesLang;
    public boolean convertDiscordAttachments = true;
    public String discordAttachmentsFormattingText;
    public boolean discordAttachmentsFormattingHoverEnabled = true;
    public String discordAttachmentsFormattingHoverText;
    public boolean discordAttachmentsImagesUseMaps = true;
    public long discordAttachmentsPreviewLimit = 0;
    public int discordAttachmentTimeout = 0;
    public String discordAttachmentsFormattingImageAppend;
    public String discordAttachmentsFormattingImageAppendHover;
    public Color discordAttachmentsMapBackgroundColor = null;
    public boolean imageWhitelistEnabled = false;
    public List<String> whitelistedImageUrls = new ArrayList<>();
    public boolean translateMentions = true;
    public String mentionHighlight = "";
    public boolean deathMessageItem = true;
    public boolean deathMessageTranslated = true;
    public boolean advancementName = true;
    public boolean advancementItem = true;
    public boolean advancementDescription = true;
    public boolean updaterEnabled = true;
    public int cacheTimeout = 1200;
    public boolean escapePlaceholdersFromDiscord = true;
    public boolean escapeDiscordMarkdownInItems = true;
    public boolean reducedAssetsDownloadInfo = false;
    public boolean playbackBarEnabled = true;
    public Color playbackBarFilledColor;
    public Color playbackBarEmptyColor;
    public String language = "en_us";
    public boolean respondToCommandsInInvalidChannels = true;
    public String discordMemberLabel = "";
    public String discordMemberDescription = "";
    public String discordSlotLabel = "";
    public String discordSlotDescription = "";
    public boolean resourcepackCommandEnabled = true;
    public String resourcepackCommandDescription = "";
    public boolean resourcepackCommandIsMainServer = true;
    public boolean playerinfoCommandEnabled = true;
    public String playerinfoCommandDescription = "";
    public boolean playerinfoCommandIsMainServer = true;
    public String playerinfoCommandFormatTitle = "";
    public String playerinfoCommandFormatSubTitle = "";
    public List<String> playerinfoCommandFormatOnline = new ArrayList<>();
    public List<String> playerinfoCommandFormatOffline = new ArrayList<>();
    public boolean playerlistCommandEnabled = true;
    public String playerlistCommandDescription = "";
    public boolean playerlistCommandIsMainServer = true;
    public boolean playerlistCommandBungeecord = true;
    public boolean playerlistCommandOnlyInteractiveChatServers = true;
    public int playerlistCommandDeleteAfter = 10;
    public String playerlistCommandPlayerFormat = "";
    public boolean playerlistCommandAvatar = true;
    public boolean playerlistCommandPing = true;
    public String playerlistCommandHeader = "";
    public String playerlistCommandFooter = "";
    public boolean playerlistCommandParsePlayerNamesWithMiniMessage = false;
    public String playerlistCommandEmptyServer = "";
    public Color playerlistCommandColor = new Color(153, 153, 153);
    public int playerlistCommandMinWidth = 0;
    public int playerlistMaxPlayers = 80;
    public List<String> playerlistOrderingTypes = new ArrayList<>();
    public boolean shareItemCommandEnabled = true;
    public boolean shareItemCommandAsOthers = true;
    public boolean shareItemCommandIsMainServer = true;
    public String shareItemCommandInGameMessageText = "";
    public String shareItemCommandTitle = "";
    public boolean shareInvCommandEnabled = true;
    public boolean shareInvCommandAsOthers = true;
    public boolean shareInvCommandIsMainServer = true;
    public String shareInvCommandInGameMessageText = "";
    public String shareInvCommandInGameMessageHover = "";
    public String shareInvCommandTitle = "";
    public String shareInvCommandSkullName = "";
    public boolean shareEnderCommandEnabled = true;
    public boolean shareEnderCommandAsOthers = true;
    public boolean shareEnderCommandIsMainServer = true;
    public String shareEnderCommandInGameMessageText = "";
    public String shareEnderCommandInGameMessageHover = "";
    public String shareEnderCommandTitle = "";
    public PlaceholderCooldownManager placeholderCooldownManager;
    public String defaultResourceHash = "N/A";
    public final List<String> resourceOrder = new ArrayList<>();
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

    private ResourceManager resourceManager;
    public ModelRenderer modelRenderer;
    public ExecutorService mediaReadingService;

    protected final Map<String, byte[]> extras = new ConcurrentHashMap<>();

    public ResourceManager getResourceManager() {
        if (resourceManager == null) {
            throw new ResourceLoadingException("Resources are still being loaded, please wait!");
        }
        return resourceManager;
    }

    public boolean isResourceManagerReady() {
        return resourceManager != null;
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

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> cachePlayerSkin(ICPlayerFactory.getICPlayer(event.getPlayer())), 40);
    }

    @EventHandler
    public void onInteractiveChatReload(InteractiveChatConfigReloadEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> placeholderCooldownManager.reloadPlaceholders(), 5);
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

        reloadConfigMessage = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.ReloadConfig"));
        reloadTextureMessage = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.ReloadTexture"));
        linkExpired = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.LinkExpired"));
        previewLoading = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.PreviewLoading"));
        accountNotLinked = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.AccountNotLinked"));
        unableToRetrieveData = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.UnableToRetrieveData"));
        invalidDiscordChannel = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.InvalidDiscordChannel"));
        interactionExpire = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.InteractionExpired"));
        trueLabel = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.TrueLabel"));
        falseLabel = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.FalseLabel"));

        defaultResourceHashLang = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.StatusCommand.DefaultResourceHash"));
        fontsActiveLang = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.StatusCommand.FontsActive"));
        loadedResourcesLang = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.StatusCommand.LoadedResources"));

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

        itemImage = config.getConfiguration().getBoolean("InventoryImage.Item.Enabled");
        invImage = config.getConfiguration().getBoolean("InventoryImage.Inventory.Enabled");
        enderImage = config.getConfiguration().getBoolean("InventoryImage.EnderChest.Enabled");

        usePlayerInvView = config.getConfiguration().getBoolean("InventoryImage.Inventory.UsePlayerInventoryView");
        renderHandHeldItems = config.getConfiguration().getBoolean("InventoryImage.Inventory.RenderHandHeldItems");

        itemUseTooltipImageOnBaseItem = config.getConfiguration().getBoolean("InventoryImage.Item.UseTooltipImageOnBaseItem");
        itemAltAir = config.getConfiguration().getBoolean("InventoryImage.Item.AlternateAirTexture");

        invShowLevel = config.getConfiguration().getBoolean("InventoryImage.Inventory.ShowExperienceLevel");

        hoverEnabled = config.getConfiguration().getBoolean("HoverEventDisplay.Enabled");
        hoverImage = config.getConfiguration().getBoolean("HoverEventDisplay.ShowCursorImage");
        hoverIgnore.clear();
        hoverIgnore = new HashSet<>(config.getConfiguration().getStringList("HoverEventDisplay.IgnoredPlaceholderKeys"));

        hoverUseTooltipImage = config.getConfiguration().getBoolean("HoverEventDisplay.UseTooltipImage");

        convertDiscordAttachments = config.getConfiguration().getBoolean("DiscordAttachments.Convert");
        discordAttachmentsFormattingText = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("DiscordAttachments.Formatting.Text"));
        discordAttachmentsFormattingHoverEnabled = config.getConfiguration().getBoolean("DiscordAttachments.Formatting.Hover.Enabled");
        discordAttachmentsFormattingHoverText = ChatColorUtils.translateAlternateColorCodes('&', String.join("\n", config.getConfiguration().getStringList("DiscordAttachments.Formatting.Hover.HoverText")));
        discordAttachmentsImagesUseMaps = config.getConfiguration().getBoolean("DiscordAttachments.ShowImageUsingMaps");
        discordAttachmentsPreviewLimit = config.getConfiguration().getLong("DiscordAttachments.FileSizeLimit");
        discordAttachmentTimeout = config.getConfiguration().getInt("DiscordAttachments.Timeout") * 20;
        discordAttachmentsFormattingImageAppend = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("DiscordAttachments.Formatting.ImageOriginal"));
        discordAttachmentsFormattingImageAppendHover = ChatColorUtils.translateAlternateColorCodes('&', String.join("\n", config.getConfiguration().getStringList("DiscordAttachments.Formatting.Hover.ImageOriginalHover")));

        boolean transparent = config.getConfiguration().getBoolean("DiscordAttachments.ImageMapBackground.Transparent");
        if (transparent) {
            discordAttachmentsMapBackgroundColor = null;
        } else {
            discordAttachmentsMapBackgroundColor = ColorUtils.hex2Rgb(config.getConfiguration().getString("DiscordAttachments.ImageMapBackground.Color"));
        }

        imageWhitelistEnabled = config.getConfiguration().getBoolean("DiscordAttachments.RestrictImageUrl.Enabled");
        whitelistedImageUrls = config.getConfiguration().getStringList("DiscordAttachments.RestrictImageUrl.Whitelist");

        updaterEnabled = config.getConfiguration().getBoolean("Options.UpdaterEnabled");

        cacheTimeout = config.getConfiguration().getInt("Settings.CacheTimeout") * 20;

        escapePlaceholdersFromDiscord = config.getConfiguration().getBoolean("Settings.EscapePlaceholdersSentFromDiscord");
        escapeDiscordMarkdownInItems = config.getConfiguration().getBoolean("Settings.EscapeDiscordMarkdownFormattingInItems");
        reducedAssetsDownloadInfo = config.getConfiguration().getBoolean("Settings.ReducedAssetsDownloadInfo");

        embedDeleteAfter = config.getConfiguration().getInt("Settings.EmbedDeleteAfter");

        itemDisplaySingle = config.getConfiguration().getString("InventoryImage.Item.EmbedDisplay.Single");
        itemDisplayMultiple = config.getConfiguration().getString("InventoryImage.Item.EmbedDisplay.Multiple");
        invColor = ColorUtils.hex2Rgb(config.getConfiguration().getString("InventoryImage.Inventory.EmbedColor"));
        enderColor = ColorUtils.hex2Rgb(config.getConfiguration().getString("InventoryImage.EnderChest.EmbedColor"));

        deathMessageItem = config.getConfiguration().getBoolean("DeathMessage.ShowItems");
        deathMessageTranslated = config.getConfiguration().getBoolean("DeathMessage.TranslatedDeathMessage");

        advancementName = config.getConfiguration().getBoolean("Advancements.CorrectAdvancementName");
        advancementItem = config.getConfiguration().getBoolean("Advancements.ChangeToItemIcon");
        advancementDescription = config.getConfiguration().getBoolean("Advancements.ShowDescription");

        translateMentions = config.getConfiguration().getBoolean("DiscordMention.TranslateMentions");
        mentionHighlight = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("DiscordMention.MentionHighlight"));

        playbackBarEnabled = config.getConfiguration().getBoolean("DiscordAttachments.PlaybackBar.Enabled");
        playbackBarFilledColor = ColorUtils.hex2Rgb(config.getConfiguration().getString("DiscordAttachments.PlaybackBar.FilledColor"));
        playbackBarEmptyColor = ColorUtils.hex2Rgb(config.getConfiguration().getString("DiscordAttachments.PlaybackBar.EmptyColor"));

        respondToCommandsInInvalidChannels = config.getConfiguration().getBoolean("DiscordCommands.GlobalSettings.RespondToCommandsInInvalidChannels");

        discordMemberLabel = config.getConfiguration().getString("DiscordCommands.GlobalSettings.Messages.MemberLabel").toLowerCase();
        discordMemberDescription = config.getConfiguration().getString("DiscordCommands.GlobalSettings.Messages.MemberDescription");
        discordSlotLabel = config.getConfiguration().getString("DiscordCommands.GlobalSettings.Messages.SlotLabel").toLowerCase();
        discordSlotDescription = config.getConfiguration().getString("DiscordCommands.GlobalSettings.Messages.SlotDescription");

        resourcepackCommandEnabled = config.getConfiguration().getBoolean("DiscordCommands.ResourcePack.Enabled");
        resourcepackCommandDescription = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("DiscordCommands.ResourcePack.Description"));
        resourcepackCommandIsMainServer = config.getConfiguration().getBoolean("DiscordCommands.ResourcePack.IsMainServer");

        playerinfoCommandEnabled = config.getConfiguration().getBoolean("DiscordCommands.PlayerInfo.Enabled");
        playerinfoCommandDescription = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("DiscordCommands.PlayerInfo.Description"));
        playerinfoCommandIsMainServer = config.getConfiguration().getBoolean("DiscordCommands.PlayerInfo.IsMainServer");
        playerinfoCommandFormatTitle = config.getConfiguration().getString("DiscordCommands.PlayerInfo.InfoFormatting.Title");
        playerinfoCommandFormatSubTitle = config.getConfiguration().getString("DiscordCommands.PlayerInfo.InfoFormatting.SubTitle");
        playerinfoCommandFormatOnline = config.getConfiguration().getStringList("DiscordCommands.PlayerInfo.InfoFormatting.WhenOnline");
        playerinfoCommandFormatOffline = config.getConfiguration().getStringList("DiscordCommands.PlayerInfo.InfoFormatting.WhenOffline");

        playerlistCommandEnabled = config.getConfiguration().getBoolean("DiscordCommands.PlayerList.Enabled");
        playerlistCommandDescription = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("DiscordCommands.PlayerList.Description"));
        playerlistCommandIsMainServer = config.getConfiguration().getBoolean("DiscordCommands.PlayerList.IsMainServer");
        playerlistCommandBungeecord = config.getConfiguration().getBoolean("DiscordCommands.PlayerList.ListBungeecordPlayers");
        playerlistCommandOnlyInteractiveChatServers = config.getConfiguration().getBoolean("DiscordCommands.PlayerList.OnlyInteractiveChatServers");
        playerlistCommandDeleteAfter = config.getConfiguration().getInt("DiscordCommands.PlayerList.DeleteAfter");
        playerlistCommandPlayerFormat = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("DiscordCommands.PlayerList.TablistOptions.PlayerFormat"));
        playerlistCommandAvatar = config.getConfiguration().getBoolean("DiscordCommands.PlayerList.TablistOptions.ShowPlayerAvatar");
        playerlistCommandPing = config.getConfiguration().getBoolean("DiscordCommands.PlayerList.TablistOptions.ShowPlayerPing");
        playerlistCommandHeader = ChatColorUtils.translateAlternateColorCodes('&', String.join("\n", config.getConfiguration().getStringList("DiscordCommands.PlayerList.TablistOptions.HeaderText")));
        playerlistCommandFooter = ChatColorUtils.translateAlternateColorCodes('&', String.join("\n", config.getConfiguration().getStringList("DiscordCommands.PlayerList.TablistOptions.FooterText")));
        playerlistCommandParsePlayerNamesWithMiniMessage = config.getConfiguration().getBoolean("DiscordCommands.PlayerList.TablistOptions.ParsePlayerNamesWithMiniMessage");
        playerlistCommandEmptyServer = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("DiscordCommands.PlayerList.EmptyServer"));
        playerlistCommandColor = ColorUtils.hex2Rgb(config.getConfiguration().getString("DiscordCommands.PlayerList.TablistOptions.SidebarColor"));
        playerlistCommandMinWidth = config.getConfiguration().getInt("DiscordCommands.PlayerList.TablistOptions.PlayerMinWidth");
        playerlistMaxPlayers = config.getConfiguration().getInt("DiscordCommands.PlayerList.TablistOptions.MaxPlayersDisplayable");
        playerlistOrderingTypes = config.getConfiguration().getStringList("DiscordCommands.PlayerList.TablistOptions.PlayerOrder.OrderBy");

        shareItemCommandEnabled = config.getConfiguration().getBoolean("DiscordCommands.ShareItem.Enabled");
        shareItemCommandAsOthers = config.getConfiguration().getBoolean("DiscordCommands.ShareItem.AllowAsOthers");
        shareItemCommandIsMainServer = config.getConfiguration().getBoolean("DiscordCommands.ShareItem.IsMainServer");
        shareItemCommandInGameMessageText = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("DiscordCommands.ShareItem.InGameMessage.Text"));
        shareItemCommandTitle = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("DiscordCommands.ShareItem.InventoryTitle"));

        shareInvCommandEnabled = config.getConfiguration().getBoolean("DiscordCommands.ShareInventory.Enabled");
        shareInvCommandAsOthers = config.getConfiguration().getBoolean("DiscordCommands.ShareInventory.AllowAsOthers");
        shareInvCommandIsMainServer = config.getConfiguration().getBoolean("DiscordCommands.ShareInventory.IsMainServer");
        shareInvCommandInGameMessageText = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("DiscordCommands.ShareInventory.InGameMessage.Text"));
        shareInvCommandInGameMessageHover = ChatColorUtils.translateAlternateColorCodes('&', String.join("\n", config.getConfiguration().getStringList("DiscordCommands.ShareInventory.InGameMessage.Hover")));
        shareInvCommandTitle = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("DiscordCommands.ShareInventory.InventoryTitle"));
        shareInvCommandSkullName = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("DiscordCommands.ShareInventory.SkullDisplayName"));

        shareEnderCommandEnabled = config.getConfiguration().getBoolean("DiscordCommands.ShareEnderChest.Enabled");
        shareEnderCommandAsOthers = config.getConfiguration().getBoolean("DiscordCommands.ShareEnderChest.AllowAsOthers");
        shareEnderCommandIsMainServer = config.getConfiguration().getBoolean("DiscordCommands.ShareEnderChest.IsMainServer");
        shareEnderCommandInGameMessageText = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("DiscordCommands.ShareEnderChest.InGameMessage.Text"));
        shareEnderCommandInGameMessageHover = ChatColorUtils.translateAlternateColorCodes('&', String.join("\n", config.getConfiguration().getStringList("DiscordCommands.ShareEnderChest.InGameMessage.Hover")));
        shareEnderCommandTitle = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("DiscordCommands.ShareEnderChest.InventoryTitle"));

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

        FontTextureResource.setCacheTime(cacheTimeout);
    }

    public byte[] getExtras(String str) {
        return extras.get(str);
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

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
import com.loohp.interactivechat.libs.com.cryptomorin.xseries.XMaterial;
import com.loohp.interactivechat.libs.net.kyori.adventure.text.Component;
import com.loohp.interactivechat.objectholders.ICPlayer;
import com.loohp.interactivechat.objectholders.OfflineICPlayer;
import com.loohp.interactivechat.objectholders.ValuePairs;
import com.loohp.interactivechat.utils.BookUtils;
import com.loohp.interactivechat.utils.FilledMapUtils;
import com.loohp.interactivechat.utils.InteractiveChatComponentSerializer;
import com.loohp.interactivechat.utils.LanguageUtils;
import com.loohp.interactivechatdiscordsrvaddon.InteractiveChatDiscordSrvAddon;
import com.loohp.interactivechatdiscordsrvaddon.debug.Debug;
import com.loohp.interactivechatdiscordsrvaddon.graphics.ImageGeneration;
import com.loohp.interactivechatdiscordsrvaddon.graphics.ImageUtils;
import com.loohp.interactivechatdiscordsrvaddon.listeners.DiscordInteractionEvents;
import com.loohp.interactivechatdiscordsrvaddon.objectholders.DiscordDisplayData;
import com.loohp.interactivechatdiscordsrvaddon.objectholders.DiscordMessageContent;
import com.loohp.interactivechatdiscordsrvaddon.objectholders.HoverClickDisplayData;
import com.loohp.interactivechatdiscordsrvaddon.objectholders.ImageDisplayData;
import com.loohp.interactivechatdiscordsrvaddon.objectholders.ImageDisplayType;
import com.loohp.interactivechatdiscordsrvaddon.objectholders.InteractionHandler;
import com.loohp.interactivechatdiscordsrvaddon.objectholders.ToolTipComponent;
import com.loohp.interactivechatdiscordsrvaddon.registry.ResourceRegistry;
import com.loohp.interactivechatdiscordsrvaddon.resources.CustomItemTextureRegistry;
import com.loohp.interactivechatdiscordsrvaddon.resources.models.ModelDisplay.ModelDisplayPosition;
import com.loohp.interactivechatdiscordsrvaddon.utils.DiscordItemStackUtils.DiscordToolTip;
import com.loohp.interactivechatdiscordsrvaddon.wrappers.TitledInventoryWrapper;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.GenericComponentInteractionCreateEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.selections.SelectOption;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.selections.SelectionMenu;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.selections.SelectionMenuInteraction;
import github.scarsz.discordsrv.dependencies.jda.api.requests.restaction.WebhookMessageUpdateAction;
import github.scarsz.discordsrv.dependencies.jda.api.requests.restaction.interactions.ReplyAction;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.map.MapView;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DiscordContentUtils {

    public static final Color OFFSET_WHITE = new Color(0xFFFFFE);

    public static final String BOOK_EMOJI = "\uD83D\uDCD6";
    public static final String LEFT_EMOJI = "\u2B05\uFE0F";
    public static final String RIGHT_EMOJI = "\u27A1\uFE0F";

    public static ValuePairs<List<DiscordMessageContent>, InteractionHandler> createContents(List<DiscordDisplayData> dataList, OfflineICPlayer player) {
        List<DiscordMessageContent> contents = new ArrayList<>();
        List<ActionRow> interactionsToRegister = new ArrayList<>();
        List<String> interactions = new ArrayList<>();
        BiConsumer<GenericComponentInteractionCreateEvent, List<DiscordMessageContent>> interactionConsumer = (event, discordMessageContents) -> {};
        int i = -1;
        for (DiscordDisplayData data : dataList) {
            i++;
            if (data instanceof ImageDisplayData) {
                ImageDisplayData iData = (ImageDisplayData) data;
                ImageDisplayType type = iData.getType();
                String title = iData.getTitle();
                if (iData.getItemStack().isPresent()) {
                    Debug.debug("createContents creating item discord content");
                    ItemStack item = iData.getItemStack().get();
                    Color color = DiscordItemStackUtils.getDiscordColor(item);
                    if (color == null || color.equals(Color.WHITE)) {
                        color = OFFSET_WHITE;
                    }
                    try {
                        BufferedImage image = ImageGeneration.getItemStackImage(item, data.getPlayer(), InteractiveChatDiscordSrvAddon.plugin.itemAltAir, 48);
                        byte[] imageData = ImageUtils.toArray(image);

                        DiscordMessageContent content = new DiscordMessageContent(title, null, color);
                        content.setTitle(DiscordItemStackUtils.getItemNameForDiscord(item, player));
                        content.setThumbnail("attachment://Item_" + i + ".png");

                        content.addAttachment("Item_" + i + ".png", imageData);
                        contents.add(content);

                        DiscordToolTip discordToolTip = DiscordItemStackUtils.getToolTip(item, player);
                        List<ToolTipComponent<?>> toolTipComponents = discordToolTip.getComponents();

                        boolean forceShow = false;
                        if (type.equals(ImageDisplayType.ITEM_CONTAINER)) {
                            TitledInventoryWrapper inv = iData.getInventory().get();
                            BufferedImage container = ImageGeneration.getInventoryImage(inv.getInventory(), inv.getTitle(), data.getPlayer());
                            toolTipComponents.add(ToolTipComponent.image(container));
                            forceShow = true;
                        } else if (iData.isFilledMap()) {
                            forceShow = true;
                        }

                        if (forceShow || !discordToolTip.isBaseItem() || InteractiveChatDiscordSrvAddon.plugin.itemUseTooltipImageOnBaseItem) {
                            BufferedImage tooltip = ImageGeneration.getToolTipImage(toolTipComponents);

                            if (iData.isFilledMap()) {
                                MapView mapView = FilledMapUtils.getMapView(item);
                                boolean isContextual = mapView == null || FilledMapUtils.isContextual(mapView);
                                ICPlayer icPlayer = iData.getPlayer().getPlayer();
                                boolean isPlayerLocal = icPlayer != null && icPlayer.isLocal();
                                if (!isContextual || isPlayerLocal) {
                                    BufferedImage map = ImageGeneration.getMapImage(item, isPlayerLocal ? icPlayer.getLocalPlayer() : null);
                                    tooltip = ImageUtils.resizeImage(tooltip, 5);
                                    tooltip = ImageUtils.appendImageBottom(tooltip, map, 10, 0);
                                }
                            }

                            byte[] tooltipData = ImageUtils.toArray(tooltip);
                            content.addAttachment("ToolTip_" + i + ".png", tooltipData);
                            content.addImageUrl("attachment://ToolTip_" + i + ".png");
                        }

                        if (iData.isBook()) {
                            List<Component> pages = BookUtils.getPages((BookMeta) item.getItemMeta());
                            List<Supplier<BufferedImage>> images = ImageGeneration.getBookInterfaceSuppliers(pages);
                            byte[][] cachedImages = new byte[images.size()][];
                            cachedImages[0] = ImageUtils.toArray(images.get(0).get());
                            if (!images.isEmpty()) {
                                UUID interactionUuid = UUID.randomUUID();
                                interactionsToRegister.add(ActionRow.of(Button.secondary("open_book_" + interactionUuid, BOOK_EMOJI)));
                                interactions.add("open_book_" + interactionUuid);
                                interactions.add("left_book_" + interactionUuid);
                                interactions.add("right_book_" + interactionUuid);
                                interactions.add("selection_book_" + interactionUuid);
                                interactionConsumer = interactionConsumer.andThen(getBookHandler(interactionUuid, color, images, cachedImages));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (iData.getInventory().isPresent()) {
                    Debug.debug("createContents creating inventory discord content");
                    TitledInventoryWrapper inv = iData.getInventory().get();
                    try {
                        BufferedImage image;
                        if (iData.isPlayerInventory()) {
                            if (InteractiveChatDiscordSrvAddon.plugin.usePlayerInvView) {
                                image = ImageGeneration.getPlayerInventoryImage(inv.getInventory(), iData.getPlayer());
                            } else {
                                image = ImageGeneration.getInventoryImage(inv.getInventory(), inv.getTitle(), data.getPlayer());
                            }
                        } else {
                            image = ImageGeneration.getInventoryImage(inv.getInventory(), inv.getTitle(), data.getPlayer());
                        }
                        Color color;
                        switch (type) {
                            case ENDERCHEST:
                                color = InteractiveChatDiscordSrvAddon.plugin.enderColor;
                                break;
                            case INVENTORY:
                                color = InteractiveChatDiscordSrvAddon.plugin.invColor;
                                break;
                            default:
                                color = Color.black;
                                break;
                        }
                        byte[] imageData = ImageUtils.toArray(image);
                        DiscordMessageContent content = new DiscordMessageContent(title, null, null, "attachment://Inventory_" + i + ".png", color);
                        content.addAttachment("Inventory_" + i + ".png", imageData);
                        if (type.equals(ImageDisplayType.INVENTORY) && InteractiveChatDiscordSrvAddon.plugin.invShowLevel) {
                            int level = iData.getPlayer().getExperienceLevel();
                            byte[] bottleData = ImageUtils.toArray(InteractiveChatDiscordSrvAddon.plugin.modelRenderer.render(32, 32, InteractiveChatDiscordSrvAddon.plugin.resourceManager, InteractiveChatDiscordSrvAddon.plugin.resourceManager.getResourceRegistry(CustomItemTextureRegistry.IDENTIFIER, CustomItemTextureRegistry.class).getItemPostResolveFunction("minecraft:item/experience_bottle", null, XMaterial.EXPERIENCE_BOTTLE.parseItem(), InteractiveChat.version.isOld(), null, null, null, null, InteractiveChatDiscordSrvAddon.plugin.resourceManager.getLanguageManager().getTranslateFunction().ofLanguage(InteractiveChatDiscordSrvAddon.plugin.language)).orElse(null), InteractiveChat.version.isOld(), "minecraft:item/experience_bottle", ModelDisplayPosition.GUI, false, null, null).getImage());
                            content.addAttachment("Level_" + i + ".png", bottleData);
                            content.setFooter(ComponentStringUtils.convertFormattedString(LanguageUtils.getTranslation(TranslationKeyUtils.getLevelTranslation(level), InteractiveChatDiscordSrvAddon.plugin.language), level));
                            content.setFooterImageUrl("attachment://Level_" + i + ".png");
                        }
                        contents.add(content);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else if (data instanceof HoverClickDisplayData) {
                Debug.debug("createContents creating hover event discord content");
                try {
                    HoverClickDisplayData hData = (HoverClickDisplayData) data;
                    String title = hData.getDisplayText();
                    Color color = hData.getColor();
                    DiscordMessageContent content = new DiscordMessageContent(title, null, color);
                    String body = "";
                    String preview = null;
                    if (hData.hasHover()) {
                        if (InteractiveChatDiscordSrvAddon.plugin.hoverUseTooltipImage) {
                            Component print = hData.getHoverText();
                            BufferedImage tooltip = ImageGeneration.getToolTipImage(print, true);
                            byte[] tooltipData = ImageUtils.toArray(tooltip);
                            content.addAttachment("ToolTip_" + i + ".png", tooltipData);
                            content.addImageUrl("attachment://ToolTip_" + i + ".png");
                            content.addDescription(null);
                        } else {
                            body += ComponentStringUtils.stripColorAndConvertMagic(InteractiveChatComponentSerializer.bungeecordApiLegacy().serialize(hData.getHoverText()));
                        }
                    }
                    if (hData.hasClick()) {
                        switch (hData.getClickAction()) {
                            case COPY_TO_CLIPBOARD:
                                if (body.length() > 0) {
                                    body += "\n\n";
                                }
                                body += LanguageUtils.getTranslation(TranslationKeyUtils.getCopyToClipboard(), InteractiveChatDiscordSrvAddon.plugin.language) + ": __" + hData.getClickValue() + "__";
                                break;
                            case OPEN_URL:
                                if (body.length() > 0) {
                                    body += "\n\n";
                                }
                                String url = hData.getClickValue();
                                body += LanguageUtils.getTranslation(TranslationKeyUtils.getOpenUrl(), InteractiveChatDiscordSrvAddon.plugin.language) + ": __" + url + "__";
                                if (URLRequestUtils.IMAGE_URL_PATTERN.matcher(url).matches() && URLRequestUtils.isAllowed(url)) {
                                    preview = url;
                                }
                                break;
                            default:
                                break;
                        }
                    }
                    if (!body.isEmpty()) {
                        content.addDescription(body);
                    }
                    if (InteractiveChatDiscordSrvAddon.plugin.hoverImage) {
                        BufferedImage image = InteractiveChatDiscordSrvAddon.plugin.resourceManager.getTextureManager().getTexture(ResourceRegistry.IC_MISC_TEXTURE_LOCATION + "hover_cursor").getTexture();
                        byte[] imageData = ImageUtils.toArray(image);
                        content.setAuthorIconUrl("attachment://Hover_" + i + ".png");
                        content.addAttachment("Hover_" + i + ".png", imageData);
                    }
                    if (preview != null) {
                        content.addImageUrl(preview);
                    }
                    contents.add(content);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return new ValuePairs<>(contents, new InteractionHandler(interactionsToRegister, interactions, InteractiveChat.itemDisplayTimeout, interactionConsumer));
    }

    private static BiConsumer<GenericComponentInteractionCreateEvent, List<DiscordMessageContent>> getBookHandler(UUID interactionUuid, Color color, List<Supplier<BufferedImage>> imageSuppliers, byte[][] cachedImages) {
        Map<String, AtomicInteger> currentPages = new ConcurrentHashMap<>();
        List<SelectOption> selectOptions = IntStream.range(1, cachedImages.length + 1).mapToObj(i -> {
            String asText = String.valueOf(i);
            return SelectOption.of(ComponentStringUtils.convertFormattedString(LanguageUtils.getTranslation(TranslationKeyUtils.getBookPageIndicator(), InteractiveChatDiscordSrvAddon.plugin.language), i, cachedImages.length), asText);
        }).collect(Collectors.toList());
        return (event, discordMessageContents) -> {
            User self = DiscordSRV.getPlugin().getJda().getSelfUser();
            User user = event.getUser();
            if (self.equals(user)) {
                return;
            }
            String id = event.getComponent().getId();
            Message message = event.getMessage();

            if (id.equals("open_book_" + interactionUuid)) {
                AtomicInteger currentPage = new AtomicInteger(0);
                currentPages.put(user.getId(), currentPage);
                DiscordMessageContent bookContent = new DiscordMessageContent(null, null, null, "attachment://Page.png", color);
                bookContent.addAttachment("Page.png", cachedImages[0]);
                ValuePairs<List<MessageEmbed>, Set<String>> pair = bookContent.toJDAMessageEmbeds();
                ReplyAction action = event.replyEmbeds(pair.getFirst()).setEphemeral(true);
                for (String name : pair.getSecond()) {
                    action = action.addFile(bookContent.getAttachments().get(name), name);
                }
                Button leftButton = Button.danger("left_book_" + interactionUuid, LEFT_EMOJI).asDisabled();
                Button rightButton = Button.success("right_book_" + interactionUuid, RIGHT_EMOJI);
                if (cachedImages.length <= 1) {
                    rightButton = rightButton.asDisabled();
                }
                SelectionMenu selectionMenu = SelectionMenu.create("selection_book_" + interactionUuid).setRequiredRange(1, 1).addOptions(selectOptions).setDefaultValues(Arrays.asList("1")).build();
                action.addActionRows(ActionRow.of(leftButton, rightButton), ActionRow.of(selectionMenu)).queue(h -> h.retrieveOriginal().queue(m -> DiscordInteractionEvents.getInteractionData(id).getMessageIds().add(m.getTextChannel().getId() + "/" + m.getId())));
                return;
            }
            AtomicInteger currentPage = currentPages.get(user.getId());
            if (currentPage == null) {
                currentPages.put(user.getId(), currentPage = new AtomicInteger(0));
            }
            event.deferEdit().queue();
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (currentPage) {
                if (id.equals("selection_book_" + interactionUuid) && event.getInteraction() instanceof SelectionMenuInteraction) {
                    int pageNumber = currentPage.updateAndGet(i -> Integer.parseInt(((SelectionMenuInteraction) event.getInteraction()).getValues().get(0)) - 1);
                    byte[] pageFile = cachedImages[pageNumber];
                    if (pageFile == null) {
                        try {
                            cachedImages[pageNumber] = pageFile = ImageUtils.toArray(imageSuppliers.get(pageNumber).get());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    WebhookMessageUpdateAction<Message> action = event.getHook().editOriginal(message.getContentRaw()).retainFiles(Collections.emptyList()).addFile(pageFile, "Page.png");
                    Button leftButton = Button.danger("left_book_" + interactionUuid, LEFT_EMOJI);
                    if (currentPage.get() <= 0) {
                        leftButton = leftButton.asDisabled();
                    }
                    Button rightButton = Button.success("right_book_" + interactionUuid, RIGHT_EMOJI);
                    if (currentPage.get() >= cachedImages.length - 1) {
                        rightButton = rightButton.asDisabled();
                    }
                    SelectionMenu selectionMenu = SelectionMenu.create("selection_book_" + interactionUuid).setRequiredRange(1, 1).addOptions(selectOptions).setDefaultValues(Arrays.asList(String.valueOf(currentPage.get() + 1))).build();
                    action.setActionRows(ActionRow.of(leftButton, rightButton), ActionRow.of(selectionMenu)).queue();
                } else if (id.equals("left_book_" + interactionUuid)) {
                    if (currentPage.get() > 0) {
                        int pageNumber = currentPage.decrementAndGet();
                        byte[] pageFile = cachedImages[pageNumber];
                        if (pageFile == null) {
                            try {
                                cachedImages[pageNumber] = pageFile = ImageUtils.toArray(imageSuppliers.get(pageNumber).get());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        WebhookMessageUpdateAction<Message> action = event.getHook().editOriginal(message.getContentRaw()).retainFiles(Collections.emptyList()).addFile(pageFile, "Page.png");
                        Button leftButton = Button.danger("left_book_" + interactionUuid, LEFT_EMOJI);
                        if (currentPage.get() <= 0) {
                            leftButton = leftButton.asDisabled();
                        }
                        Button rightButton = Button.success("right_book_" + interactionUuid, RIGHT_EMOJI);
                        if (currentPage.get() >= cachedImages.length - 1) {
                            rightButton = rightButton.asDisabled();
                        }
                        SelectionMenu selectionMenu = SelectionMenu.create("selection_book_" + interactionUuid).setRequiredRange(1, 1).addOptions(selectOptions).setDefaultValues(Arrays.asList(String.valueOf(currentPage.get() + 1))).build();
                        action.setActionRows(ActionRow.of(leftButton, rightButton), ActionRow.of(selectionMenu)).queue();
                    }
                } else if (id.equals("right_book_" + interactionUuid)) {
                    if (currentPage.get() < cachedImages.length - 1) {
                        int pageNumber = currentPage.incrementAndGet();
                        byte[] pageFile = cachedImages[pageNumber];
                        if (pageFile == null) {
                            try {
                                cachedImages[pageNumber] = pageFile = ImageUtils.toArray(imageSuppliers.get(pageNumber).get());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        WebhookMessageUpdateAction<Message> action = event.getHook().editOriginal(message.getContentRaw()).retainFiles(Collections.emptyList()).addFile(pageFile, "Page.png");
                        Button leftButton = Button.danger("left_book_" + interactionUuid, LEFT_EMOJI);
                        if (currentPage.get() <= 0) {
                            leftButton = leftButton.asDisabled();
                        }
                        Button rightButton = Button.success("right_book_" + interactionUuid, RIGHT_EMOJI);
                        if (currentPage.get() >= cachedImages.length - 1) {
                            rightButton = rightButton.asDisabled();
                        }
                        SelectionMenu selectionMenu = SelectionMenu.create("selection_book_" + interactionUuid).setRequiredRange(1, 1).addOptions(selectOptions).setDefaultValues(Arrays.asList(String.valueOf(currentPage.get() + 1))).build();
                        action.setActionRows(ActionRow.of(leftButton, rightButton), ActionRow.of(selectionMenu)).queue();
                    }
                }
            }
        };
    }

}
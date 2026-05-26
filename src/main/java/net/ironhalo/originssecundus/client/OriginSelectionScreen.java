package net.ironhalo.originssecundus.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.ironhalo.originssecundus.data.CustomizationOption;
import net.ironhalo.originssecundus.data.OriginDataManager;
import net.ironhalo.originssecundus.data.OriginDefinition;
import net.ironhalo.originssecundus.data.PowerDefinition;
import net.ironhalo.originssecundus.network.OriginSelectPayload;
import net.ironhalo.originssecundus.origin.PlayerOrigin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class OriginSelectionScreen extends Screen {
    private static final int PANEL_LIGHT = 0xFFE7E7E7;
    private static final int PANEL_MID = 0xFF888888;
    private static final int PANEL_DARK = 0xFF242424;
    private static final int INSET = 0xFF555555;
    private static final int GOLD = 0xFFB98A13;
    private static final int PANEL_TEXTURE_WIDTH = 176;
    private static final int PANEL_TEXTURE_HEIGHT = 182;
    private static final int BUTTON_TEXTURE_WIDTH = 119;
    private static final int BUTTON_TEXTURE_HEIGHT = 20;
    private static final int TAB_TEXTURE_WIDTH = 32;
    private static final int TAB_TEXTURE_HEIGHT = 28;
    private static final int SMALL_BUTTON_SOURCE_SIZE = 20;
    private static final int SMALL_BUTTON_SIZE = 14;
    private static final int ORIGIN_ARROW_SIZE = 18;
    private static final int NAME_PLATE_WIDTH = 150;
    private static final int NAME_PLATE_HEIGHT = 26;
    private static final float RIGHT_TEXT_SCALE = 0.92F;
    private static final int RIGHT_TEXT_LINE_HEIGHT = 10;
    private static final int RIGHT_TEXT_SMALL_LINE_HEIGHT = 9;
    private static final int RIGHT_SCROLL_WHEEL_STEP = 8;
    // Temporary helper for pixel-level layout tuning. Set to false before release.
    private static final boolean UI_TUNING_ENABLED = false;
    private static final int UI_TUNING_LIMIT = 24;

    private final boolean initialSelection;
    private final List<OriginDefinition> origins;
    private final Map<String, String> customization = new LinkedHashMap<>();
    private OriginDefinition selected;
    private Step step = Step.DETAILS;
    private boolean rotationLocked;
    private boolean draggingPreview;
    private boolean draggingRightScroll;
    private boolean uiTuningMode;
    private UiTuningTarget uiTuningTarget = UiTuningTarget.NONE;
    private PressedControl pressedControl = PressedControl.NONE;
    private int rightScrollDragOffset;
    private int selectedTabTuneX;
    private int normalTabTuneX;
    private int tabListTuneY;
    private int tuningStartSelectedTabX;
    private int tuningStartNormalTabX;
    private int tuningStartTabListY;
    private double dragStartX;
    private double dragStartY;
    private float previewRotation;
    private float previewPitch;
    private String draggingSliderKey;
    private String draggingHumanSliderKey;
    private String draggingHumanPaletteKey;
    private boolean skinPaletteOpen;
    private boolean eyePaletteOpen;
    private boolean leftEyePaletteOpen;
    private boolean rightEyePaletteOpen;
    private boolean hairPaletteOpen;
    private int originScrollOffset;
    private int rightScrollOffset;
    private EditorPreviewPlayer editorPreviewPlayer;

    private int tabsX;
    private int previewX;
    private int rightX;
    private int panelY;
    private int panelH;
    private int tabsW;
    private int previewW;
    private int rightW;
    private int gap;
    private int footerButtonW;
    private int footerButtonH;
    private int footerGap;

    private static final ResourceLocation BACKGROUND_TEXTURE = guiTexture("background");
    private static final ResourceLocation PANEL_BACKGROUND_TEXTURE = guiTexture("background_panel");
    private static final ResourceLocation PANEL_BORDER_TEXTURE = guiTexture("border");
    private static final ResourceLocation BUTTON_TEXTURE = guiTexture("button");
    private static final ResourceLocation BUTTON_DISABLED_TEXTURE = guiTexture("button_disabled");
    private static final ResourceLocation TAB_TEXTURE = guiTexture("tabs/tab");
    private static final ResourceLocation TAB_SELECTED_TEXTURE = guiTexture("tabs/tab_lselected");
    private static final ResourceLocation CHOOSE_ORIGIN_TEXTURE = guiTexture("choose_origin");
    private static final ResourceLocation NAME_PLATE_TEXTURE = guiTexture("name_plate");
    private static final ResourceLocation SCROLL_BAR_TEXTURE = guiTexture("scroll_bar/pressed");
    private static final ResourceLocation SCROLL_BAR_SLOT_TEXTURE = guiTexture("scroll_bar/slot");
    private static final ResourceLocation IMPACT_NONE_TEXTURE = guiTexture("impact/none");
    private static final ResourceLocation IMPACT_LOW_TEXTURE = guiTexture("impact/low");
    private static final ResourceLocation IMPACT_MEDIUM_TEXTURE = guiTexture("impact/medium");
    private static final ResourceLocation IMPACT_HIGH_TEXTURE = guiTexture("impact/high");
    private static final ResourceLocation ARROW_UP_TEXTURE = guiTexture("buttons/button_up");
    private static final ResourceLocation ARROW_UP_DISABLED_TEXTURE = guiTexture("buttons/button_up_disabled");
    private static final ResourceLocation ARROW_DOWN_TEXTURE = guiTexture("buttons/button_down");
    private static final ResourceLocation ARROW_DOWN_DISABLED_TEXTURE = guiTexture("buttons/button_down_disabled");
    private static final ResourceLocation LOCKED_BUTTON_TEXTURE = guiTexture("buttons/locked_button");
    private static final ResourceLocation LOCKED_BUTTON_DISABLED_TEXTURE = guiTexture("buttons/locked_button_disabled");
    private static final ResourceLocation UNLOCKED_BUTTON_TEXTURE = guiTexture("buttons/unlocked_button");
    private static final ResourceLocation UNLOCKED_BUTTON_DISABLED_TEXTURE = guiTexture("buttons/unlocked_button_disabled");
    private static final ResourceLocation DEFAULT_POSE_BUTTON_TEXTURE = guiTexture("buttons/default_pose_button");
    private static final ResourceLocation DEFAULT_POSE_BUTTON_DISABLED_TEXTURE = guiTexture("buttons/default_pose_button_disabled");
    private static final ResourceLocation BADGE_ACTIVE_TEXTURE = guiTexture("badge/active");
    private static final ResourceLocation BADGE_TOGGLE_TEXTURE = guiTexture("badge/toggle");
    private static final ResourceLocation BADGE_INFO_TEXTURE = guiTexture("badge/info");

    public OriginSelectionScreen(boolean initialSelection) {
        super(Component.translatable("screen.originssecundus.select_origin"));
        this.initialSelection = initialSelection;
        this.origins = OriginDataManager.origins();
        Optional<ResourceLocation> current = ClientOriginState.originId();
        this.selected = current.flatMap(OriginDataManager::origin)
                .or(() -> OriginDataManager.origin(ResourceLocation.fromNamespaceAndPath("originssecundus", "human")))
                .orElse(this.origins.getFirst());
        this.customization.putAll(this.selected.defaultCustomizationValues());
        this.customization.putAll(ClientOriginState.customization());
        if (HumanEditorSkin.isEditable(this.selected)) {
            HumanEditorSkin.ensureDefaults(this.customization, this.selected);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !initialSelection;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        layout();
        if (pressedControl != PressedControl.NONE && !isLeftMouseButtonDown()) {
            pressedControl = PressedControl.NONE;
        }
        renderDirtBackdrop(graphics);
        Component title = step == Step.DETAILS
                ? Component.translatable("screen.originssecundus.select_origin")
                : Component.translatable("screen.originssecundus.edit_origin");
        graphics.drawCenteredString(this.font, title, this.width / 2, Math.max(8, panelY - 24), 0xFFFFFFFF);
        renderPreviewPanel(graphics, mouseX, mouseY);
        renderRightPanel(graphics, mouseX, mouseY);
        renderOriginTabs(graphics, mouseX, mouseY);
        renderFooterButtons(graphics, mouseX, mouseY);
        renderUiTuningOverlay(graphics);
        renderOriginTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (UI_TUNING_ENABLED && keyCode == GLFW.GLFW_KEY_F8) {
            uiTuningMode = !uiTuningMode;
            uiTuningTarget = UiTuningTarget.NONE;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        layout();
        if (button == 0) {
            if (handleUiTuningClick(mouseX, mouseY)) {
                return true;
            }
            if (handleOriginArrowClick(mouseX, mouseY)) {
                return true;
            }
            if (handleOriginTabClick(mouseX, mouseY)) {
                return true;
            }
            if (handleRightScrollClick(mouseX, mouseY)) {
                return true;
            }
            if (isInside(mouseX, mouseY, resetButtonX(), panelY + 12, SMALL_BUTTON_SIZE, SMALL_BUTTON_SIZE)) {
                if (Math.abs(previewRotation) < 0.001F && Math.abs(previewPitch) < 0.001F) {
                    return true;
                }
                pressedControl = PressedControl.RESET_POSE;
                previewRotation = 0.0F;
                previewPitch = 0.0F;
                return true;
            }
            if (isInside(mouseX, mouseY, lockButtonX(), panelY + 12, SMALL_BUTTON_SIZE, SMALL_BUTTON_SIZE)) {
                pressedControl = PressedControl.LOCK_TOGGLE;
                rotationLocked = !rotationLocked;
                return true;
            }
            if (isInside(mouseX, mouseY, previewX, panelY, previewW, panelH)) {
                draggingPreview = true;
                dragStartX = mouseX;
                dragStartY = mouseY;
                return true;
            }
            if (handleFooterClick(mouseX, mouseY)) {
                return true;
            }
            if (step == Step.CUSTOMIZATION && handleCustomizationClick(mouseX, mouseY)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        layout();
        if (isInside(mouseX, mouseY, tabsX - 2, panelY, tabsW + 4, panelH)) {
            selectAdjacentOrigin(scrollY > 0 ? -1 : 1);
            return true;
        }
        if (isInside(mouseX, mouseY, rightX, panelY, rightW, panelH)) {
            rightScrollOffset = clamp(rightScrollOffset + (scrollY > 0 ? -RIGHT_SCROLL_WHEEL_STEP : RIGHT_SCROLL_WHEEL_STEP), 0, maxRightScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (uiTuningTarget != UiTuningTarget.NONE) {
            updateUiTuningDrag(mouseX, mouseY);
            return true;
        }
        if (draggingRightScroll) {
            updateRightScrollFromMouse(mouseY);
            return true;
        }
        if (draggingHumanPaletteKey != null) {
            updateHumanPalette(draggingHumanPaletteKey, mouseX, mouseY);
            return true;
        }
        if (draggingHumanSliderKey != null) {
            updateHumanSlider(draggingHumanSliderKey, mouseX);
            return true;
        }
        if (draggingPreview) {
            previewRotation += (float) (mouseX - dragStartX) * 1.2F;
            previewPitch = clamp(previewPitch - (float) (mouseY - dragStartY) * 0.8F, -45.0F, 45.0F);
            dragStartX = mouseX;
            dragStartY = mouseY;
            return true;
        }
        if (draggingSliderKey != null) {
            updateSlider(draggingSliderKey, mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = draggingPreview || draggingRightScroll || draggingSliderKey != null || draggingHumanSliderKey != null || draggingHumanPaletteKey != null || pressedControl != PressedControl.NONE || uiTuningTarget != UiTuningTarget.NONE;
        draggingPreview = false;
        draggingRightScroll = false;
        draggingSliderKey = null;
        draggingHumanSliderKey = null;
        draggingHumanPaletteKey = null;
        uiTuningTarget = UiTuningTarget.NONE;
        pressedControl = PressedControl.NONE;
        return handled || super.mouseReleased(mouseX, mouseY, button);
    }

    private void layout() {
        boolean compact = this.width < 760 || this.height < 420;
        gap = -1;
        tabsW = compact ? 30 : TAB_TEXTURE_WIDTH;
        footerButtonH = compact ? 18 : 18;
        footerGap = compact ? 8 : 14;

        int top = compact ? 24 : Math.max(34, this.height / 10);
        int maxPanelHeight = Math.max(160, this.height - top - footerGap - footerButtonH - 12);
        panelH = Math.min(compact ? 260 : Math.max(220, this.height * 66 / 100), maxPanelHeight);
        panelY = compact ? top : Math.max(34, (this.height - panelH - footerGap - footerButtonH) / 2 - 1);

        int sideMargin = compact ? 8 : Math.max(70, this.width / 8);
        int panelW = Math.max(compact ? 140 : 220, (this.width - sideMargin * 2 - tabsW - gap * 2) / 2);
        int maxPanelW = compact ? 230 : Math.max(240, panelH * 95 / 100);
        panelW = Math.min(panelW, maxPanelW);
        previewW = panelW;
        rightW = panelW;

        int total = tabsW + gap + previewW + gap + rightW;
        tabsX = Math.max(4, (this.width - total) / 2);
        previewX = tabsX + tabsW + gap;
        rightX = previewX + previewW + gap;
        footerButtonW = Math.min(112, Math.max(96, Math.min(previewW, rightW) / 3));
        rightScrollOffset = clamp(rightScrollOffset, 0, maxRightScroll());
    }

    private void renderDirtBackdrop(GuiGraphics graphics) {
        if (hasTexture(BACKGROUND_TEXTURE)) {
            tileTexture(graphics, BACKGROUND_TEXTURE, 0, 0, width, height, 16);
            return;
        }
        graphics.fill(0, 0, width, height, 0xFF24180F);
        for (int y = 0; y < height; y += 12) {
            for (int x = 0; x < width; x += 12) {
                int color = ((x + y) / 12) % 2 == 0 ? 0x332F2116 : 0x33201810;
                graphics.fill(x, y, Math.min(width, x + 12), Math.min(height, y + 12), color);
            }
        }
    }

    private void tileTexture(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height, int tileSize) {
        for (int yy = y; yy < y + height; yy += tileSize) {
            int drawH = Math.min(tileSize, y + height - yy);
            for (int xx = x; xx < x + width; xx += tileSize) {
                int drawW = Math.min(tileSize, x + width - xx);
                graphics.blit(texture, xx, yy, drawW, drawH, 0.0F, 0.0F, drawW, drawH, tileSize, tileSize);
            }
        }
    }

    private void tileTextureAligned(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height, int tileSize) {
        int bottom = y + height;
        int right = x + width;
        for (int yy = y; yy < bottom; ) {
            int v = Math.floorMod(yy, tileSize);
            int drawH = Math.min(tileSize - v, bottom - yy);
            for (int xx = x; xx < right; ) {
                int u = Math.floorMod(xx, tileSize);
                int drawW = Math.min(tileSize - u, right - xx);
                graphics.blit(texture, xx, yy, drawW, drawH, (float) u, (float) v, drawW, drawH, tileSize, tileSize);
                xx += drawW;
            }
            yy += drawH;
        }
    }

    private void renderOriginTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        int tabH = originTabHeight();
        int pitch = originTabPitch();
        int visible = visibleOriginTabs();
        int selectedIndex = selectedOriginIndex();
        ensureSelectedOriginVisible(selectedIndex);
        originScrollOffset = clamp(originScrollOffset, 0, Math.max(0, origins.size() - visible));
        int arrowX = tabsX + (tabsW - ORIGIN_ARROW_SIZE) / 2;
        renderArrowButton(graphics, arrowX, originUpButtonY(), false, selectedIndex <= 0 || pressedControl == PressedControl.ORIGIN_UP);
        for (int i = 0; i < visible; i++) {
            OriginDefinition origin = origins.get(i + originScrollOffset);
            int y = originTabListTop() + i * pitch;
            boolean active = origin.id().equals(selected.id());
            int fill = active ? 0xFFE2E2E2 : 0xFFBDBDBD;
            int drawX = originTabDrawX(active);
            int drawW = originTabDrawWidth();
            drawTabPanel(graphics, drawX, y, drawW, tabH, fill, active);
            int iconSize = Math.min(20, tabH - 8);
            renderOriginIcon(graphics, origin, drawX + (drawW - iconSize) / 2, y + (tabH - iconSize) / 2, iconSize);
        }
        renderArrowButton(graphics, arrowX, originDownButtonY(), true, selectedIndex >= origins.size() - 1 || pressedControl == PressedControl.ORIGIN_DOWN);
    }

    private void renderArrowButton(GuiGraphics graphics, int x, int y, boolean down, boolean disabled) {
        int width = ORIGIN_ARROW_SIZE;
        int height = ORIGIN_ARROW_SIZE;
        ResourceLocation arrowTexture = disabled
                ? (down ? ARROW_DOWN_DISABLED_TEXTURE : ARROW_UP_DISABLED_TEXTURE)
                : (down ? ARROW_DOWN_TEXTURE : ARROW_UP_TEXTURE);
        if (hasTexture(arrowTexture)) {
            graphics.blit(arrowTexture, x, y, width, height, 0.0F, 0.0F, SMALL_BUTTON_SOURCE_SIZE, SMALL_BUTTON_SOURCE_SIZE, SMALL_BUTTON_SOURCE_SIZE, SMALL_BUTTON_SOURCE_SIZE);
            return;
        }
        drawButtonPanel(graphics, x, y, width, height, 0xFF9A9A9A, disabled);
        if (down) {
            graphics.fill(x + 7, y + 8, x + width - 7, y + 11, 0xFFFFFFFF);
            graphics.fill(x + 10, y + 11, x + width - 10, y + 15, 0xFFFFFFFF);
            graphics.fill(x + width / 2 - 1, y + 15, x + width / 2 + 1, y + 19, 0xFFFFFFFF);
        } else {
            graphics.fill(x + width / 2 - 1, y + 7, x + width / 2 + 1, y + 11, 0xFFFFFFFF);
            graphics.fill(x + 10, y + 11, x + width - 10, y + 15, 0xFFFFFFFF);
            graphics.fill(x + 7, y + 15, x + width - 7, y + 18, 0xFFFFFFFF);
        }
    }

    private void renderOriginTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int visible = visibleOriginTabs();
        int pitch = originTabPitch();
        int tabH = originTabHeight();
        for (int i = 0; i < visible; i++) {
            int index = i + originScrollOffset;
            int y = originTabListTop() + i * pitch;
            if (index < origins.size()) {
                boolean active = origins.get(index).id().equals(selected.id());
                int hitX = originTabDrawX(active);
                int hitW = originTabDrawWidth();
                if (!isInside(mouseX, mouseY, hitX, y, hitW, tabH)) {
                    continue;
                }
                graphics.renderTooltip(font, Component.literal(origins.get(index).name()), mouseX, mouseY);
                return;
            }
        }
    }

    private void renderUiTuningOverlay(GuiGraphics graphics) {
        if (!UI_TUNING_ENABLED || !uiTuningMode) {
            return;
        }
        int x = previewX + 12;
        int y = Math.max(8, panelY - 18);
        graphics.fill(x - 4, y - 4, x + 214, y + 44, 0xCC101010);
        graphics.drawString(font, "UI tuning ON (F8)", x, y, 0xFFFFE080, false);
        graphics.drawString(font, "selectedTabX: " + selectedTabTuneX + "  normalTabX: " + normalTabTuneX, x, y + 12, 0xFFFFFFFF, false);
        graphics.drawString(font, "tabListY: " + tabListTuneY + "  drag tabs with mouse", x, y + 24, 0xFFD0D0D0, false);
    }

    private void renderOriginIcon(GuiGraphics graphics, OriginDefinition origin, int x, int y, int size) {
        ResourceLocation texture = originGuiTexture(origin, "origins");
        if (hasTexture(texture)) {
            graphics.blit(texture, x, y, size, size, 0.0F, 0.0F, 32, 32, 32, 32);
            return;
        }
        int accent = 0xFF000000 | origin.accentColor();
        graphics.fill(x, y, x + size, y + size, accent);
        graphics.fill(x + size / 4, y + size / 4, x + size - size / 4, y + size - size / 5, 0xFF303030);
        graphics.drawCenteredString(font, origin.name().substring(0, 1), x + size / 2, y + size / 2 - 4, 0xFFFFFFFF);
    }

    private void renderPreviewPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        drawRaisedPanel(graphics, previewX, panelY, previewW, panelH, PANEL_LIGHT);
        int previewFrame = 10;
        int previewInnerX = previewX + previewFrame;
        int previewInnerY = panelY + previewFrame;
        int previewInnerW = previewW - previewFrame * 2;
        int previewInnerH = panelH - previewFrame * 2;
        if (hasTexture(BACKGROUND_TEXTURE)) {
            tileTextureAligned(graphics, BACKGROUND_TEXTURE, previewInnerX, previewInnerY, previewInnerW, previewInnerH, 16);
        } else {
            graphics.fill(previewInnerX, previewInnerY, previewInnerX + previewInnerW, previewInnerY + previewInnerH, 0xFF242424);
        }
        graphics.fill(previewInnerX, previewInnerY, previewInnerX + previewInnerW, previewInnerY + previewInnerH, 0x88000000);

        int lockY = panelY + 12;
        int lockSize = SMALL_BUTTON_SIZE;
        int resetX = resetButtonX();
        boolean defaultPose = Math.abs(previewRotation) < 0.001F && Math.abs(previewPitch) < 0.001F;
        ResourceLocation resetTexture = pressedControl == PressedControl.RESET_POSE ? DEFAULT_POSE_BUTTON_DISABLED_TEXTURE : DEFAULT_POSE_BUTTON_TEXTURE;
        if (hasTexture(resetTexture)) {
            graphics.blit(resetTexture, resetX, lockY, lockSize, lockSize, 0.0F, 0.0F, SMALL_BUTTON_SOURCE_SIZE, SMALL_BUTTON_SOURCE_SIZE, SMALL_BUTTON_SOURCE_SIZE, SMALL_BUTTON_SOURCE_SIZE);
        } else {
            drawButtonPanel(graphics, resetX, lockY, lockSize, lockSize, 0xFF8E8E8E, pressedControl == PressedControl.RESET_POSE);
            graphics.drawCenteredString(font, "R", resetX + lockSize / 2, lockY + 7, 0xFFFFFFFF);
        }

        int lockX = lockButtonX();
        ResourceLocation lockTexture = rotationLocked ? LOCKED_BUTTON_TEXTURE : UNLOCKED_BUTTON_DISABLED_TEXTURE;
        if (hasTexture(lockTexture)) {
            graphics.blit(lockTexture, lockX, lockY, lockSize, lockSize, 0.0F, 0.0F, SMALL_BUTTON_SOURCE_SIZE, SMALL_BUTTON_SOURCE_SIZE, SMALL_BUTTON_SOURCE_SIZE, SMALL_BUTTON_SOURCE_SIZE);
        } else {
            drawButtonPanel(graphics, lockX, lockY, lockSize, lockSize, rotationLocked ? 0xFFB0B0B0 : 0xFF8E8E8E);
            graphics.drawCenteredString(font, rotationLocked ? "L" : "U", lockX + lockSize / 2, lockY + 7, 0xFFFFFFFF);
        }

        int centerX = previewX + previewW / 2;
        boolean humanPreview = HumanEditorSkin.isEditable(selected);
        int entityScale = (int) (clamp(Math.min(previewW, panelH) / 3.55F, 36.0F, 84.0F)
                * (humanPreview ? 1.06D : previewHeightScale()));
        int footY = panelY + panelH - Math.max(24, panelH / 10);
        renderFeatureBackdrop(graphics, centerX, footY, entityScale);
        LivingEntity previewEntity = previewEntity();
        if (previewEntity != null) {
            int innerLeft = previewX + previewFrame + 8;
            int innerRight = previewX + previewW - previewFrame - 8;
            int innerTop = panelY + previewFrame + 8;
            int innerBottom = panelY + panelH - previewFrame - 8;
            if (rotationLocked || draggingPreview) {
                renderStaticPreviewEntity(
                        graphics,
                        innerLeft,
                        innerTop,
                        innerRight,
                        innerBottom,
                        entityScale,
                        0.05F,
                        previewEntity
                );
            } else {
                InventoryScreen.renderEntityInInventoryFollowsMouse(
                        graphics,
                        innerLeft,
                        innerTop,
                        innerRight,
                        innerBottom,
                        entityScale,
                        0.05F,
                        mouseX,
                        mouseY,
                        previewEntity
                );
            }
        } else {
            renderPixelBody(graphics, centerX, footY - entityScale * 2);
        }
        renderFeatureOverlay(graphics, centerX, footY - entityScale * 2, entityScale);
    }

    private LivingEntity previewEntity() {
        if (minecraft == null || minecraft.player == null) {
            return null;
        }
        if (!HumanEditorSkin.isEditable(selected) || minecraft.level == null) {
            return minecraft.player;
        }
        HumanEditorSkin.ensureDefaults(customization, selected);
        if (editorPreviewPlayer == null || editorPreviewPlayer.clientLevel != minecraft.level) {
            editorPreviewPlayer = new EditorPreviewPlayer(
                    minecraft.level,
                    new GameProfile(UUID.nameUUIDFromBytes("originssecundus-human-editor".getBytes(java.nio.charset.StandardCharsets.UTF_8)), "OriginsSecundus")
            );
        }
        editorPreviewPlayer.setEditorSkin(HumanEditorSkin.skin(minecraft, selected, customization));
        editorPreviewPlayer.setEditorWingsTexture(HumanEditorSkin.isAvian(selected) ? HumanEditorSkin.wingsTexture(customization) : null);
        editorPreviewPlayer.setEditorScale(
                humanScale(HumanEditorSkin.WIDTH),
                humanScale(HumanEditorSkin.HEIGHT),
                humanScale(HumanEditorSkin.DEPTH)
        );
        editorPreviewPlayer.setEditorLimbScale(
                HumanEditorSkin.armMuscleScale(customization),
                HumanEditorSkin.legMuscleScale(customization),
                HumanEditorSkin.armLengthScale(customization),
                HumanEditorSkin.legLengthScale(customization)
        );
        editorPreviewPlayer.tickCount = minecraft.player.tickCount;
        editorPreviewPlayer.setPose(minecraft.player.getPose());
        editorPreviewPlayer.setOnGround(true);
        return editorPreviewPlayer;
    }

    private void renderStaticPreviewEntity(
            GuiGraphics graphics,
            int left,
            int top,
            int right,
            int bottom,
            int scale,
            float yOffset,
            LivingEntity entity
    ) {
        renderStaticPreviewEntity(graphics, left, top, right, bottom, scale, yOffset, entity, previewRotation, previewPitch, 1.0F, 1.0F, 1.0F);
    }

    private void renderStaticPreviewEntity(
            GuiGraphics graphics,
            int left,
            int top,
            int right,
            int bottom,
            int scale,
            float yOffset,
            LivingEntity entity,
            float renderRotation,
            float renderPitch,
            float modelScaleX,
            float modelScaleY,
            float modelScaleZ
    ) {
        float oldBodyRot = entity.yBodyRot;
        float oldBodyRotO = entity.yBodyRotO;
        float oldYaw = entity.getYRot();
        float oldYawO = entity.yRotO;
        float oldPitch = entity.getXRot();
        float oldPitchO = entity.xRotO;
        float oldHeadRot = entity.yHeadRot;
        float oldHeadRotO = entity.yHeadRotO;
        float oldWalkSpeed = entity.walkAnimation.speed();

        entity.yBodyRot = 180.0F;
        entity.yBodyRotO = 180.0F;
        entity.setYRot(180.0F);
        entity.yRotO = 180.0F;
        entity.setXRot(0.0F);
        entity.xRotO = 0.0F;
        entity.yHeadRot = 180.0F;
        entity.yHeadRotO = 180.0F;
        entity.walkAnimation.setSpeed(0.0F);

        float centerX = (left + right) / 2.0F;
        float centerY = (top + bottom) / 2.0F;
        float entityScale = entity.getScale();
        float pivotY = entity.getBbHeight() / 2.0F;
        Quaternionf pose = new Quaternionf()
                .rotateZ((float) Math.PI)
                .rotateX(renderPitch * (float) (Math.PI / 180.0D))
                .rotateY(renderRotation * (float) (Math.PI / 180.0D));

        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 50.0F);
        graphics.pose().scale(scale / entityScale, scale / entityScale, -scale / entityScale);
        graphics.pose().mulPose(pose);
        graphics.pose().scale(modelScaleX, modelScaleY, modelScaleZ);
        graphics.pose().translate(0.0F, -pivotY - yOffset * entityScale, 0.0F);
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        RenderSystem.runAsFancy(() -> dispatcher.render(
                entity,
                0.0D,
                0.0D,
                0.0D,
                0.0F,
                1.0F,
                graphics.pose(),
                graphics.bufferSource(),
                15728880
        ));
        graphics.flush();
        dispatcher.setRenderShadow(true);
        graphics.pose().popPose();
        Lighting.setupFor3DItems();

        entity.yBodyRot = oldBodyRot;
        entity.yBodyRotO = oldBodyRotO;
        entity.setYRot(oldYaw);
        entity.yRotO = oldYawO;
        entity.setXRot(oldPitch);
        entity.xRotO = oldPitchO;
        entity.yHeadRot = oldHeadRot;
        entity.yHeadRotO = oldHeadRotO;
        entity.walkAnimation.setSpeed(oldWalkSpeed);
    }

    private void renderFeatureBackdrop(GuiGraphics graphics, int centerX, int footY, int entityScale) {
    }

    private void renderFeatureOverlay(GuiGraphics graphics, int centerX, int headY, int entityScale) {
        int feature = Math.max(8, entityScale / 4);
        if (selected.modelHint().contains("demon")) {
            graphics.fill(centerX - feature * 3, headY - feature * 5, centerX - feature * 2, headY - feature * 2, 0xFF3A1010);
            graphics.fill(centerX + feature * 2, headY - feature * 5, centerX + feature * 3, headY - feature * 2, 0xFF3A1010);
            graphics.fill(centerX - feature * 3, headY - feature * 6, centerX - feature * 2, headY - feature * 5, 0xFFAF3030);
            graphics.fill(centerX + feature * 2, headY - feature * 6, centerX + feature * 3, headY - feature * 5, 0xFFAF3030);
        }
        if (selected.modelHint().contains("feline")) {
            graphics.fill(centerX - feature * 3, headY - feature * 4, centerX - feature * 2, headY - feature * 2, 0xFFB07A42);
            graphics.fill(centerX + feature * 2, headY - feature * 4, centerX + feature * 3, headY - feature * 2, 0xFFB07A42);
        }
        if (selected.modelHint().contains("merling")) {
            graphics.fill(centerX - feature * 5, headY + entityScale, centerX - feature * 3, headY + entityScale + feature * 5, 0xFF2E8793);
            graphics.fill(centerX + feature * 3, headY + entityScale, centerX + feature * 5, headY + entityScale + feature * 5, 0xFF2E8793);
        }
    }

    private void renderPixelBody(GuiGraphics graphics, int centerX, int y) {
        graphics.fill(centerX - 20, y - 48, centerX + 20, y - 8, 0xFFD8B89A);
        graphics.fill(centerX - 28, y - 8, centerX + 28, y + 70, 0xFF5E6652);
        graphics.fill(centerX - 46, y, centerX - 28, y + 62, 0xFFB9B5AB);
        graphics.fill(centerX + 28, y, centerX + 46, y + 62, 0xFFB9B5AB);
    }

    private void renderRightPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        drawRaisedPanel(graphics, rightX, panelY, rightW, panelH, PANEL_LIGHT);
        int pad = contentPad();
        int accent = 0xFF000000 | selected.accentColor();
        int titleX = rightX + pad;
        int titleY = panelY + 7;
        int titleH = NAME_PLATE_HEIGHT;
        int titleW = rightW - pad * 2;
        drawTitleBar(graphics, titleX, titleY, titleW, titleH, accent);
        graphics.drawCenteredString(font, selected.name(), rightX + rightW / 2, titleY + (titleH - 8) / 2, 0xFFFFFFFF);
        renderImpactDots(graphics, titleX + titleW - 38, titleY + 9);

        rightScrollOffset = clamp(rightScrollOffset, 0, maxRightScroll());
        if (step == Step.DETAILS) {
            renderDetails(graphics);
        } else {
            renderCustomization(graphics, mouseX, mouseY);
        }
    }

    private void renderDetails(GuiGraphics graphics) {
        int pad = contentPad();
        int x = rightTextLeft();
        int contentTop = rightContentTop();
        int contentBottom = rightContentBottom();
        int y = contentTop - rightScrollOffset;
        int textW = rightTextLayoutWidth();
        int textCenter = x + rightTextWidth() / 2;
        for (var line : font.split(Component.literal(selected.description()), textW)) {
            drawCenteredStringIfVisible(graphics, line, textCenter, y, 0xFFFFFFFF, contentTop, contentBottom);
            y += RIGHT_TEXT_LINE_HEIGHT;
        }
        y += 12;
        drawStringIfVisible(graphics, Component.literal("Powers"), x, y, 0xFFE9E9E9, contentTop, contentBottom);
        y += 14;
        for (PowerDefinition power : OriginDataManager.powersFor(selected)) {
            ResourceLocation badgeTexture = badgeTexture(power);
            if (hasTexture(badgeTexture) && isRowVisible(y, 9, contentTop, contentBottom)) {
                graphics.blit(badgeTexture, x, y + 1, 9, 9, 0.0F, 0.0F, 9, 9, 9, 9);
            } else {
                fillIfVisible(graphics, x + 2, y + 3, x + 7, y + 8, 0xFF90C66D, contentTop, contentBottom);
            }
            drawStringIfVisible(graphics, Component.literal(power.name()), x + 14, y, 0xFFFFFFFF, contentTop, contentBottom);
            y += RIGHT_TEXT_LINE_HEIGHT;
            for (var line : font.split(Component.literal(power.description()), textW - 14)) {
                drawStringIfVisible(graphics, line, x + 14, y, 0xFFD8D8D8, contentTop, contentBottom);
                y += RIGHT_TEXT_SMALL_LINE_HEIGHT;
            }
            y += 5;
        }
        renderRightScrollHint(graphics);
    }

    private void renderCustomization(GuiGraphics graphics, int mouseX, int mouseY) {
        if (HumanEditorSkin.isEditable(selected)) {
            renderHumanCustomization(graphics);
            return;
        }
        int pad = contentPad();
        int x = rightTextLeft();
        int contentTop = rightContentTop();
        int contentBottom = rightContentBottom();
        int y = contentTop - rightScrollOffset;
        drawStringIfVisible(graphics, Component.translatable("screen.originssecundus.customize"), x, y, 0xFFFFFFFF, contentTop, contentBottom);
        y += 22;
        for (CustomizationOption option : selected.customization()) {
            drawStringIfVisible(graphics, Component.literal(option.label()), x, y, option.raceUnique() ? 0xFFFFE2A0 : 0xFFFFFFFF, contentTop, contentBottom);
            y += 12;
            if ("slider".equals(option.type())) {
                if (isRowVisible(y, 18, contentTop, contentBottom)) {
                    renderSlider(graphics, option, x, y, rightTextWidth());
                }
                y += 24;
            } else if ("toggle".equals(option.type())) {
                if (isRowVisible(y, 18, contentTop, contentBottom)) {
                    renderToggle(graphics, option, x, y);
                }
                y += 24;
            } else {
                if (isRowVisible(y, 20, contentTop, contentBottom)) {
                    renderChoice(graphics, option, x, y, rightTextWidth());
                }
                y += 24;
            }
        }
        renderRightScrollHint(graphics);
    }

    private void renderHumanCustomization(GuiGraphics graphics) {
        HumanEditorSkin.ensureDefaults(customization, selected);
        int x = rightTextLeft();
        int width = rightTextWidth();
        int y = rightContentTop() - rightScrollOffset;

        y = renderHumanChoice(graphics, x, y, width, "Model", humanGenderLabel());
        if (HumanEditorSkin.isAvian(selected)) {
            y = renderHumanChoice(graphics, x, y, width, "Wings", humanIndexedLabel(HumanEditorSkin.WINGS, "wings", 1));
        }
        y = renderHumanScaleSlider(graphics, x, y, width, "Height", HumanEditorSkin.HEIGHT);
        y = renderHumanScaleSlider(graphics, x, y, width, "Width", HumanEditorSkin.WIDTH);
        y = renderHumanScaleSlider(graphics, x, y, width, "Depth", HumanEditorSkin.DEPTH);
        y = renderHumanOffsetSlider(graphics, x, y, width, "Arm muscles", HumanEditorSkin.ARM_MUSCLES);
        y = renderHumanOffsetSlider(graphics, x, y, width, "Leg muscles", HumanEditorSkin.LEG_MUSCLES);
        y = renderHumanOffsetSlider(graphics, x, y, width, "Leg length", HumanEditorSkin.LEG_LENGTH);
        y = renderHumanOffsetSlider(graphics, x, y, width, "Arm length", HumanEditorSkin.ARM_LENGTH);
        y = renderHumanChoice(graphics, x, y, width, "Skin", humanIndexedLabel(HumanEditorSkin.SKIN, "skin", 1));
        y = renderHumanPaletteButton(graphics, x, y, width, "Skin color", HumanEditorSkin.skinColormap(), HumanEditorSkin.SKIN_COLOR_X, HumanEditorSkin.SKIN_COLOR_Y, skinPaletteOpen);
        y = renderHumanChoice(graphics, x, y, width, "Eyes", humanEyesLabel());
        y = renderHumanEyePaletteControls(graphics, x, y, width);
        y = renderHumanChoice(graphics, x, y, width, "Eyelashes", humanIndexedLabel(HumanEditorSkin.EYELASHES, "eyelashes", 0));
        y = renderHumanOffsetSlider(graphics, x, y, width, "Eyelashes shade", HumanEditorSkin.EYELASHES_BRIGHTNESS);
        y = renderHumanChoice(graphics, x, y, width, "Eyebrows", humanIndexedLabel(HumanEditorSkin.EYEBROWS, "eyebrows", 1));
        y = renderHumanOffsetSlider(graphics, x, y, width, "Eyebrows shade", HumanEditorSkin.EYEBROWS_BRIGHTNESS);
        y = renderHumanChoice(graphics, x, y, width, "Hair", humanIndexedLabel(HumanEditorSkin.HAIR, "hair", 1));
        if ("male".equals(HumanEditorSkin.gender(customization))) {
            y = renderHumanChoice(graphics, x, y, width, "Beard", humanIndexedLabel(HumanEditorSkin.BEARD, "beard", 0));
        }
        y = renderHumanPaletteButton(graphics, x, y, width, "Hair color", HumanEditorSkin.hairColormap(), HumanEditorSkin.HAIR_COLOR_X, HumanEditorSkin.HAIR_COLOR_Y, hairPaletteOpen);
        y = renderHumanChoice(graphics, x, y, width, "Scars", humanScarsLabel());
        y = renderHumanChoice(graphics, x, y, width, "Tattoo", humanIndexedLabel(HumanEditorSkin.TATTOO, "tattoo", 0));
        renderHumanChoice(graphics, x, y, width, "Clothes", humanIndexedLabel(HumanEditorSkin.CLOTHES, "clothes", 0));
        renderRightScrollHint(graphics);
    }

    private int renderHumanChoice(GuiGraphics graphics, int x, int y, int width, String label, String value) {
        int contentTop = rightContentTop();
        int contentBottom = rightContentBottom();
        drawStringIfVisible(graphics, Component.literal(label), x, y, 0xFFFFFFFF, contentTop, contentBottom);
        int controlY = y + 11;
        if (isRowVisible(controlY, 17, contentTop, contentBottom)) {
            graphics.fill(x, controlY, x + width, controlY + 17, 0xFF202020);
            graphics.fill(x + 1, controlY + 1, x + 18, controlY + 16, 0xFF606060);
            graphics.fill(x + width - 18, controlY + 1, x + width - 1, controlY + 16, 0xFF606060);
            graphics.drawString(font, "<", x + 6, controlY + 5, 0xFFFFFFFF, false);
            graphics.drawString(font, ">", x + width - 12, controlY + 5, 0xFFFFFFFF, false);
            graphics.drawCenteredString(font, value, x + width / 2, controlY + 5, 0xFFFFFFFF);
        }
        return y + 34;
    }

    private int renderHumanScaleSlider(GuiGraphics graphics, int x, int y, int width, String label, String key) {
        return renderHumanSignedSlider(graphics, x, y, width, label, HumanEditorSkin.scaleOffset(customization, key));
    }

    private int renderHumanOffsetSlider(GuiGraphics graphics, int x, int y, int width, String label, String key) {
        return renderHumanSignedSlider(graphics, x, y, width, label, HumanEditorSkin.offsetValue(customization, key));
    }

    private int renderHumanSignedSlider(GuiGraphics graphics, int x, int y, int width, String label, int offset) {
        int contentTop = rightContentTop();
        int contentBottom = rightContentBottom();
        drawStringIfVisible(graphics, Component.literal(label), x, y, 0xFFFFFFFF, contentTop, contentBottom);
        int controlY = y + 10;
        if (isRowVisible(controlY, 22, contentTop, contentBottom)) {
            int clamped = clamp(offset, -5, 5);
            double percent = (clamped + 5.0D) / 10.0D;
            int knobX = x + (int) Math.round(clamp((float) percent, 0.0F, 1.0F) * width);
            graphics.fill(x, controlY + 8, x + width, controlY + 12, 0xFF1E1E1E);
            graphics.fill(x, controlY + 8, knobX, controlY + 12, GOLD);
            graphics.fill(knobX - 3, controlY + 4, knobX + 3, controlY + 16, 0xFFE6E6E6);
            graphics.drawString(font, Integer.toString(clamped), x + width - 18, controlY + 15, 0xFFDADADA, false);
        }
        return y + 36;
    }

    private int renderHumanPaletteButton(GuiGraphics graphics, int x, int y, int width, String label, ResourceLocation texture, String xKey, String yKey, boolean open) {
        int contentTop = rightContentTop();
        int contentBottom = rightContentBottom();
        int size = humanPaletteSize();
        drawStringIfVisible(graphics, Component.literal(label), x, y, 0xFFFFFFFF, contentTop, contentBottom);
        int buttonY = y + 11;
        if (isRowVisible(buttonY, 17, contentTop, contentBottom)) {
            graphics.fill(x, buttonY, x + width, buttonY + 17, 0xFF202020);
            graphics.fill(x + 1, buttonY + 1, x + width - 1, buttonY + 16, 0xFF606060);
            graphics.drawCenteredString(font, "\u0412\u044b\u0431\u0440\u0430\u0442\u044c \u0446\u0432\u0435\u0442", x + width / 2, buttonY + 5, 0xFFFFFFFF);
        }
        int paletteY = humanPalettePopupY(y);
        int paletteX = humanPalettePopupX(x, width);
        if (open && isRowVisible(paletteY, size, contentTop, contentBottom)) {
            graphics.fill(paletteX - 1, paletteY - 1, paletteX + size + 1, paletteY + size + 1, 0xFF111111);
            if (hasTexture(texture)) {
                graphics.blit(texture, paletteX, paletteY, size, size, 0.0F, 0.0F, 32, 32, 32, 32);
            } else {
                graphics.fill(paletteX, paletteY, paletteX + size, paletteY + size, 0xFFB0A090);
            }
            int cursorX = paletteX + (int) Math.round(humanDouble(xKey, 0.2D) * (size - 1));
            int cursorY = paletteY + (int) Math.round(humanDouble(yKey, 0.16D) * (size - 1));
            graphics.fill(cursorX - 3, cursorY - 3, cursorX + 4, cursorY + 4, 0xFF000000);
            graphics.fill(cursorX - 2, cursorY - 2, cursorX + 3, cursorY + 3, 0xFFFFFFFF);
            graphics.fill(cursorX - 1, cursorY - 1, cursorX + 2, cursorY + 2, 0xFF222222);
        }
        return y + humanPaletteRowHeight(open);
    }

    private int renderHumanEyePaletteControls(GuiGraphics graphics, int x, int y, int width) {
        if (!humanHeterochromia()) {
            return renderHumanPaletteButton(graphics, x, y, width, "Eye color", HumanEditorSkin.eyesColormap(), HumanEditorSkin.EYE_COLOR_X, HumanEditorSkin.EYE_COLOR_Y, eyePaletteOpen);
        }

        int contentTop = rightContentTop();
        int contentBottom = rightContentBottom();
        int size = humanPaletteSize();
        drawStringIfVisible(graphics, Component.literal("Eye color"), x, y, 0xFFFFFFFF, contentTop, contentBottom);
        int buttonY = y + 11;
        int halfW = width / 2;
        if (isRowVisible(buttonY, 17, contentTop, contentBottom)) {
            graphics.fill(x, buttonY, x + halfW - 1, buttonY + 17, 0xFF202020);
            graphics.fill(x + halfW + 1, buttonY, x + width, buttonY + 17, 0xFF202020);
            graphics.fill(x + 1, buttonY + 1, x + halfW - 2, buttonY + 16, 0xFF606060);
            graphics.fill(x + halfW + 2, buttonY + 1, x + width - 1, buttonY + 16, 0xFF606060);
            graphics.drawCenteredString(font, "Left", x + halfW / 2, buttonY + 5, 0xFFFFFFFF);
            graphics.drawCenteredString(font, "Right", x + halfW + (width - halfW) / 2, buttonY + 5, 0xFFFFFFFF);
        }

        int paletteY = humanPalettePopupY(y);
        if ((leftEyePaletteOpen || rightEyePaletteOpen) && isRowVisible(paletteY, size, contentTop, contentBottom)) {
            boolean left = leftEyePaletteOpen;
            int paletteX = left ? humanPalettePopupX(x, halfW - 1) : humanPalettePopupX(x + halfW + 1, width - halfW - 1);
            String colorX = left ? HumanEditorSkin.LEFT_EYE_COLOR_X : HumanEditorSkin.RIGHT_EYE_COLOR_X;
            String colorY = left ? HumanEditorSkin.LEFT_EYE_COLOR_Y : HumanEditorSkin.RIGHT_EYE_COLOR_Y;
            graphics.fill(paletteX - 1, paletteY - 1, paletteX + size + 1, paletteY + size + 1, 0xFF111111);
            if (hasTexture(HumanEditorSkin.eyesColormap())) {
                graphics.blit(HumanEditorSkin.eyesColormap(), paletteX, paletteY, size, size, 0.0F, 0.0F, 32, 32, 32, 32);
            } else {
                graphics.fill(paletteX, paletteY, paletteX + size, paletteY + size, 0xFF807060);
            }
            int cursorX = paletteX + (int) Math.round(humanDouble(colorX, 0.5D) * (size - 1));
            int cursorY = paletteY + (int) Math.round(humanDouble(colorY, 0.5D) * (size - 1));
            graphics.fill(cursorX - 3, cursorY - 3, cursorX + 4, cursorY + 4, 0xFF000000);
            graphics.fill(cursorX - 2, cursorY - 2, cursorX + 3, cursorY + 3, 0xFFFFFFFF);
            graphics.fill(cursorX - 1, cursorY - 1, cursorX + 2, cursorY + 2, 0xFF222222);
        }
        return y + humanPaletteRowHeight(leftEyePaletteOpen || rightEyePaletteOpen);
    }

    private void renderSlider(GuiGraphics graphics, CustomizationOption option, int x, int y, int width) {
        double value = Double.parseDouble(customization.getOrDefault(option.key(), option.normalizedDefault()));
        double percent = (value - option.min()) / (option.max() - option.min());
        int knobX = x + (int) Math.round(percent * width);
        graphics.fill(x, y + 7, x + width, y + 11, 0xFF1E1E1E);
        graphics.fill(x, y + 7, knobX, y + 11, GOLD);
        graphics.fill(knobX - 3, y + 3, knobX + 3, y + 15, 0xFFE6E6E6);
        graphics.drawString(font, String.format(java.util.Locale.ROOT, "%.2f", value), x + width - 34, y + 14, 0xFFDADADA, false);
    }

    private void renderToggle(GuiGraphics graphics, CustomizationOption option, int x, int y) {
        boolean active = Boolean.parseBoolean(customization.getOrDefault(option.key(), option.normalizedDefault()));
        graphics.fill(x, y + 2, x + 30, y + 15, active ? 0xFF6CA96A : 0xFF2B2B2B);
        graphics.fill(active ? x + 18 : x + 2, y + 4, active ? x + 28 : x + 12, y + 13, 0xFFEDEDED);
    }

    private void renderChoice(GuiGraphics graphics, CustomizationOption option, int x, int y, int width) {
        String value = customization.getOrDefault(option.key(), option.normalizedDefault());
        graphics.fill(x, y + 1, x + width, y + 16, 0xFF303030);
        graphics.fill(x, y + 1, x + 16, y + 16, 0xFF606060);
        graphics.fill(x + width - 16, y + 1, x + width, y + 16, 0xFF606060);
        graphics.drawString(font, "<", x + 5, y + 5, 0xFFFFFFFF, false);
        graphics.drawString(font, ">", x + width - 11, y + 5, 0xFFFFFFFF, false);
        graphics.drawCenteredString(font, value, x + width / 2, y + 5, 0xFFFFFFFF);
    }

    private void renderImpactDots(GuiGraphics graphics, int x, int y) {
        ResourceLocation impactTexture = switch (selected.impact()) {
            case 1 -> IMPACT_LOW_TEXTURE;
            case 2 -> IMPACT_MEDIUM_TEXTURE;
            case 3 -> IMPACT_HIGH_TEXTURE;
            default -> IMPACT_NONE_TEXTURE;
        };
        if (hasTexture(impactTexture)) {
            graphics.blit(impactTexture, x, y, 28, 8, 0.0F, 0.0F, 28, 8, 28, 8);
            return;
        }
        for (int i = 0; i < 3; i++) {
            int color = i < selected.impact() ? 0xFF89B76B : 0xFFC8C8C8;
            graphics.fill(x + i * 12, y, x + i * 12 + 8, y + 8, 0xFF111111);
            graphics.fill(x + i * 12 + 1, y + 1, x + i * 12 + 7, y + 7, color);
        }
    }

    private void renderFooterButtons(GuiGraphics graphics, int mouseX, int mouseY) {
        int y = panelY + panelH + footerGap;
        boolean backEnabled = step == Step.CUSTOMIZATION || !initialSelection;
        drawButtonPanel(graphics, previewX, y, footerButtonW, footerButtonH, 0xFF9A9A9A, !backEnabled || pressedControl == PressedControl.BACK);
        graphics.drawCenteredString(font, Component.translatable("screen.originssecundus.back"), previewX + footerButtonW / 2, y + footerButtonH / 2 - 4, backEnabled ? 0xFFFFFFFF : 0xFFBDBDBD);
        int nextX = rightX + rightW - footerButtonW;
        drawButtonPanel(graphics, nextX, y, footerButtonW, footerButtonH, 0xFF9A9A9A, pressedControl == PressedControl.NEXT);
        Component label = step == Step.DETAILS ? Component.translatable("screen.originssecundus.next") : Component.translatable("screen.originssecundus.confirm");
        graphics.drawCenteredString(font, label, nextX + footerButtonW / 2, y + footerButtonH / 2 - 4, 0xFFFFFFFF);
    }

    private void drawRaisedPanel(GuiGraphics graphics, int x, int y, int width, int height, int fill) {
        if (hasTexture(PANEL_BACKGROUND_TEXTURE) && hasTexture(PANEL_BORDER_TEXTURE)) {
            drawOriginsPanel(graphics, x, y, width, height);
            return;
        }
        drawFallbackRaisedPanel(graphics, x, y, width, height, fill);
    }

    private void drawButtonPanel(GuiGraphics graphics, int x, int y, int width, int height, int fill) {
        drawButtonPanel(graphics, x, y, width, height, fill, false);
    }

    private void drawButtonPanel(GuiGraphics graphics, int x, int y, int width, int height, int fill, boolean disabled) {
        ResourceLocation texture = disabled ? BUTTON_DISABLED_TEXTURE : BUTTON_TEXTURE;
        if (hasTexture(texture)) {
            graphics.blit(texture, x, y, width, height, 0.0F, 0.0F, BUTTON_TEXTURE_WIDTH, BUTTON_TEXTURE_HEIGHT, BUTTON_TEXTURE_WIDTH, BUTTON_TEXTURE_HEIGHT);
            return;
        }
        drawFallbackRaisedPanel(graphics, x, y, width, height, disabled ? 0xFF777777 : fill);
    }

    private void drawTabPanel(GuiGraphics graphics, int x, int y, int width, int height, int fill, boolean selected) {
        ResourceLocation texture = selected ? TAB_SELECTED_TEXTURE : TAB_TEXTURE;
        if (hasTexture(texture)) {
            graphics.blit(texture, x, y, width, height, 0.0F, 0.0F, TAB_TEXTURE_WIDTH, TAB_TEXTURE_HEIGHT, TAB_TEXTURE_WIDTH, TAB_TEXTURE_HEIGHT);
            return;
        }
        drawFallbackRaisedPanel(graphics, x, y, width, height, fill);
    }

    private void drawFallbackRaisedPanel(GuiGraphics graphics, int x, int y, int width, int height, int fill) {
        graphics.fill(x, y, x + width, y + height, 0xFF111111);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFF3A3A3A);
        graphics.fill(x + 3, y + 3, x + width - 4, y + height - 4, 0xFFC6C6C6);
        graphics.fill(x + 5, y + 5, x + width - 5, y + height - 5, fill);
        graphics.fill(x + 5, y + 5, x + width - 5, y + 7, 0xFFFFFFFF);
        graphics.fill(x + 5, y + 5, x + 7, y + height - 5, 0xFFFFFFFF);
        graphics.fill(x + 5, y + height - 8, x + width - 5, y + height - 5, 0xFF6A6A6A);
        graphics.fill(x + width - 8, y + 5, x + width - 5, y + height - 5, 0xFF6A6A6A);
        graphics.fill(x + 3, y + height - 4, x + width - 3, y + height - 2, 0xFF222222);
        graphics.fill(x + width - 4, y + 3, x + width - 2, y + height - 2, 0xFF222222);
    }

    private void drawTitleBar(GuiGraphics graphics, int x, int y, int width, int height, int fill) {
        if (hasTexture(NAME_PLATE_TEXTURE)) {
            graphics.blit(NAME_PLATE_TEXTURE, x, y, width, height, 0.0F, 0.0F, NAME_PLATE_WIDTH, NAME_PLATE_HEIGHT, NAME_PLATE_WIDTH, NAME_PLATE_HEIGHT);
            return;
        }
        graphics.fill(x, y, x + width, y + height, 0xFF111111);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, fill);
        graphics.fill(x + 3, y + 3, x + width - 3, y + 5, 0xFFEBCB56);
        graphics.fill(x + 3, y + height - 5, x + width - 3, y + height - 3, 0xFF6E5209);
    }

    private void drawOriginsPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.blit(PANEL_BACKGROUND_TEXTURE, x, y, width, height, 0.0F, 0.0F, PANEL_TEXTURE_WIDTH, PANEL_TEXTURE_HEIGHT, PANEL_TEXTURE_WIDTH, PANEL_TEXTURE_HEIGHT);
        graphics.blit(PANEL_BORDER_TEXTURE, x, y, width, height, 0.0F, 0.0F, PANEL_TEXTURE_WIDTH, PANEL_TEXTURE_HEIGHT, PANEL_TEXTURE_WIDTH, PANEL_TEXTURE_HEIGHT);
    }

    private void drawOriginsAtlasFrame(GuiGraphics graphics, int x, int y, int width, int height, int fill) {
        int slice = Math.min(8, Math.max(2, Math.min(width, height) / 3));
        int sourceW = 174;
        int sourceH = 181;
        blitChooseOrigin(graphics, x, y, slice, slice, 0, 0, slice, slice);
        blitChooseOrigin(graphics, x + width - slice, y, slice, slice, sourceW - slice, 0, slice, slice);
        blitChooseOrigin(graphics, x, y + height - slice, slice, slice, 0, sourceH - slice, slice, slice);
        blitChooseOrigin(graphics, x + width - slice, y + height - slice, slice, slice, sourceW - slice, sourceH - slice, slice, slice);

        blitChooseOrigin(graphics, x + slice, y, width - slice * 2, slice, slice, 0, sourceW - slice * 2, slice);
        blitChooseOrigin(graphics, x + slice, y + height - slice, width - slice * 2, slice, slice, sourceH - slice, sourceW - slice * 2, slice);
        blitChooseOrigin(graphics, x, y + slice, slice, height - slice * 2, 0, slice, slice, sourceH - slice * 2);
        blitChooseOrigin(graphics, x + width - slice, y + slice, slice, height - slice * 2, sourceW - slice, slice, slice, sourceH - slice * 2);

        if (width > slice * 2 && height > slice * 2) {
            graphics.fill(x + slice, y + slice, x + width - slice, y + height - slice, fill);
        }
    }

    private void blitChooseOrigin(GuiGraphics graphics, int x, int y, int width, int height, int u, int v, int sourceWidth, int sourceHeight) {
        if (width > 0 && height > 0 && sourceWidth > 0 && sourceHeight > 0) {
            graphics.blit(CHOOSE_ORIGIN_TEXTURE, x, y, width, height, (float) u, (float) v, sourceWidth, sourceHeight, 256, 256);
        }
    }

    private boolean handleOriginTabClick(double mouseX, double mouseY) {
        int visible = visibleOriginTabs();
        int pitch = originTabPitch();
        int tabH = originTabHeight();
        for (int i = 0; i < visible; i++) {
            int y = originTabListTop() + i * pitch;
            int index = i + originScrollOffset;
            if (index < origins.size()) {
                boolean active = origins.get(index).id().equals(selected.id());
                int hitX = originTabDrawX(active);
                int hitW = originTabDrawWidth();
                if (!isInside(mouseX, mouseY, hitX, y, hitW, tabH)) {
                    continue;
                }
                selectOriginAtIndex(index);
                return true;
            }
        }
        return false;
    }

    private boolean handleUiTuningClick(double mouseX, double mouseY) {
        if (!UI_TUNING_ENABLED || !uiTuningMode) {
            return false;
        }
        int visible = visibleOriginTabs();
        int pitch = originTabPitch();
        int tabH = originTabHeight();
        for (int i = 0; i < visible; i++) {
            int index = i + originScrollOffset;
            if (index >= origins.size()) {
                continue;
            }
            boolean active = origins.get(index).id().equals(selected.id());
            int y = originTabListTop() + i * pitch;
            int hitX = originTabDrawX(active);
            int hitW = originTabDrawWidth();
            if (!isInside(mouseX, mouseY, hitX, y, hitW, tabH)) {
                continue;
            }
            uiTuningTarget = active ? UiTuningTarget.SELECTED_TAB : UiTuningTarget.NORMAL_TABS;
            tuningStartSelectedTabX = selectedTabTuneX;
            tuningStartNormalTabX = normalTabTuneX;
            tuningStartTabListY = tabListTuneY;
            dragStartX = mouseX;
            dragStartY = mouseY;
            return true;
        }
        return false;
    }

    private void updateUiTuningDrag(double mouseX, double mouseY) {
        int deltaX = (int) Math.round(mouseX - dragStartX);
        int deltaY = (int) Math.round(mouseY - dragStartY);
        if (uiTuningTarget == UiTuningTarget.SELECTED_TAB) {
            selectedTabTuneX = clamp(tuningStartSelectedTabX + deltaX, -UI_TUNING_LIMIT, UI_TUNING_LIMIT);
        } else if (uiTuningTarget == UiTuningTarget.NORMAL_TABS) {
            normalTabTuneX = clamp(tuningStartNormalTabX + deltaX, -UI_TUNING_LIMIT, UI_TUNING_LIMIT);
        }
        tabListTuneY = clamp(tuningStartTabListY + deltaY, -UI_TUNING_LIMIT, UI_TUNING_LIMIT);
    }

    private boolean handleOriginArrowClick(double mouseX, double mouseY) {
        int arrowX = tabsX + (tabsW - ORIGIN_ARROW_SIZE) / 2;
        if (isInside(mouseX, mouseY, arrowX, originUpButtonY(), ORIGIN_ARROW_SIZE, ORIGIN_ARROW_SIZE)) {
            if (selectedOriginIndex() <= 0) {
                return true;
            }
            pressedControl = PressedControl.ORIGIN_UP;
            selectAdjacentOrigin(-1);
            return true;
        }
        if (isInside(mouseX, mouseY, arrowX, originDownButtonY(), ORIGIN_ARROW_SIZE, ORIGIN_ARROW_SIZE)) {
            if (selectedOriginIndex() >= origins.size() - 1) {
                return true;
            }
            pressedControl = PressedControl.ORIGIN_DOWN;
            selectAdjacentOrigin(1);
            return true;
        }
        return false;
    }

    private boolean handleFooterClick(double mouseX, double mouseY) {
        int y = panelY + panelH + footerGap;
        if (isInside(mouseX, mouseY, previewX, y, footerButtonW, footerButtonH)) {
            if (step == Step.DETAILS && initialSelection) {
                return true;
            }
            pressedControl = PressedControl.BACK;
            if (step == Step.CUSTOMIZATION) {
                step = Step.DETAILS;
            } else if (!initialSelection) {
                onClose();
            }
            return true;
        }
        int nextX = rightX + rightW - footerButtonW;
        if (isInside(mouseX, mouseY, nextX, y, footerButtonW, footerButtonH)) {
            pressedControl = PressedControl.NEXT;
            if (step == Step.DETAILS) {
                step = Step.CUSTOMIZATION;
                rightScrollOffset = 0;
            } else {
                PacketDistributor.sendToServer(new OriginSelectPayload(selected.id().toString(), PlayerOrigin.customizationToJson(customization)));
                onClose();
            }
            return true;
        }
        return false;
    }

    private boolean handleCustomizationClick(double mouseX, double mouseY) {
        if (HumanEditorSkin.isEditable(selected)) {
            return handleHumanCustomizationClick(mouseX, mouseY);
        }
        int x = rightTextLeft();
        int y = rightContentTop() + 22 - rightScrollOffset;
        int width = rightTextWidth();
        int contentTop = rightContentTop();
        int contentBottom = rightContentBottom();
        for (CustomizationOption option : selected.customization()) {
            y += 12;
            if ("slider".equals(option.type())) {
                if (isRowVisible(y, 18, contentTop, contentBottom) && isInside(mouseX, mouseY, x, y, width, 18)) {
                    draggingSliderKey = option.key();
                    updateSlider(option.key(), mouseX);
                    return true;
                }
                y += 24;
            } else if ("toggle".equals(option.type())) {
                if (isRowVisible(y, 18, contentTop, contentBottom) && isInside(mouseX, mouseY, x, y, 36, 18)) {
                    boolean value = Boolean.parseBoolean(customization.getOrDefault(option.key(), option.normalizedDefault()));
                    customization.put(option.key(), Boolean.toString(!value));
                    return true;
                }
                y += 24;
            } else {
                if (isRowVisible(y, 20, contentTop, contentBottom) && isInside(mouseX, mouseY, x, y, width, 20)) {
                    cycleChoice(option, mouseX < x + width / 2);
                    return true;
                }
                y += 24;
            }
        }
        return false;
    }

    private boolean handleHumanCustomizationClick(double mouseX, double mouseY) {
        HumanEditorSkin.ensureDefaults(customization, selected);
        int x = rightTextLeft();
        int width = rightTextWidth();
        int y = rightContentTop() - rightScrollOffset;

        if (humanChoiceHit(mouseX, mouseY, x, y, width)) {
            customization.put(HumanEditorSkin.GENDER, "male".equals(HumanEditorSkin.gender(customization)) ? "female" : "male");
            clampHumanIndexesAfterGenderChange();
            return true;
        }
        y += 34;

        if (HumanEditorSkin.isAvian(selected)) {
            if (humanChoiceHit(mouseX, mouseY, x, y, width)) {
                cycleHumanIndex(HumanEditorSkin.WINGS, "wings", mouseX < x + width / 2 ? -1 : 1);
                return true;
            }
            y += 34;
        }

        String[] sliders = {
                HumanEditorSkin.HEIGHT,
                HumanEditorSkin.WIDTH,
                HumanEditorSkin.DEPTH,
                HumanEditorSkin.ARM_MUSCLES,
                HumanEditorSkin.LEG_MUSCLES,
                HumanEditorSkin.LEG_LENGTH,
                HumanEditorSkin.ARM_LENGTH
        };
        for (String slider : sliders) {
            if (humanSliderHit(mouseX, mouseY, x, y, width)) {
                draggingHumanSliderKey = slider;
                updateHumanSlider(slider, mouseX);
                return true;
            }
            y += 36;
        }

        if (humanChoiceHit(mouseX, mouseY, x, y, width)) {
            cycleHumanIndex(HumanEditorSkin.SKIN, "skin", mouseX < x + width / 2 ? -1 : 1);
            return true;
        }
        y += 34;

        if (humanPaletteButtonHit(mouseX, mouseY, x, y, width)) {
            skinPaletteOpen = !skinPaletteOpen;
            rightScrollOffset = clamp(rightScrollOffset, 0, maxRightScroll());
            return true;
        }
        if (skinPaletteOpen && humanPaletteHit(mouseX, mouseY, x, y)) {
            draggingHumanPaletteKey = "skin";
            updateHumanPalette(draggingHumanPaletteKey, mouseX, mouseY);
            return true;
        }
        y += humanPaletteRowHeight(skinPaletteOpen);

        if (humanChoiceHit(mouseX, mouseY, x, y, width)) {
            cycleHumanIndex(HumanEditorSkin.EYES, "eyes", mouseX < x + width / 2 ? -1 : 1);
            eyePaletteOpen = false;
            leftEyePaletteOpen = false;
            rightEyePaletteOpen = false;
            return true;
        }
        y += 34;

        if (handleHumanEyePaletteClick(mouseX, mouseY, x, y, width)) {
            return true;
        }
        y += humanPaletteRowHeight(eyePaletteOpen || leftEyePaletteOpen || rightEyePaletteOpen);

        if (humanChoiceHit(mouseX, mouseY, x, y, width)) {
            cycleHumanIndex(HumanEditorSkin.EYELASHES, "eyelashes", mouseX < x + width / 2 ? -1 : 1);
            return true;
        }
        y += 34;

        if (humanSliderHit(mouseX, mouseY, x, y, width)) {
            draggingHumanSliderKey = HumanEditorSkin.EYELASHES_BRIGHTNESS;
            updateHumanSlider(HumanEditorSkin.EYELASHES_BRIGHTNESS, mouseX);
            return true;
        }
        y += 36;

        if (humanChoiceHit(mouseX, mouseY, x, y, width)) {
            cycleHumanIndex(HumanEditorSkin.EYEBROWS, "eyebrows", mouseX < x + width / 2 ? -1 : 1);
            return true;
        }
        y += 34;

        if (humanSliderHit(mouseX, mouseY, x, y, width)) {
            draggingHumanSliderKey = HumanEditorSkin.EYEBROWS_BRIGHTNESS;
            updateHumanSlider(HumanEditorSkin.EYEBROWS_BRIGHTNESS, mouseX);
            return true;
        }
        y += 36;

        if (humanChoiceHit(mouseX, mouseY, x, y, width)) {
            cycleHumanIndex(HumanEditorSkin.HAIR, "hair", mouseX < x + width / 2 ? -1 : 1);
            return true;
        }
        y += 34;

        if ("male".equals(HumanEditorSkin.gender(customization))) {
            if (humanChoiceHit(mouseX, mouseY, x, y, width)) {
                cycleHumanIndex(HumanEditorSkin.BEARD, "beard", mouseX < x + width / 2 ? -1 : 1);
                return true;
            }
            y += 34;
        }

        if (humanPaletteButtonHit(mouseX, mouseY, x, y, width)) {
            hairPaletteOpen = !hairPaletteOpen;
            rightScrollOffset = clamp(rightScrollOffset, 0, maxRightScroll());
            return true;
        }
        if (hairPaletteOpen && humanPaletteHit(mouseX, mouseY, x, y)) {
            draggingHumanPaletteKey = "hair";
            updateHumanPalette(draggingHumanPaletteKey, mouseX, mouseY);
            return true;
        }
        y += humanPaletteRowHeight(hairPaletteOpen);

        if (humanChoiceHit(mouseX, mouseY, x, y, width)) {
            cycleHumanScars(mouseX < x + width / 2 ? -1 : 1);
            return true;
        }
        y += 34;

        if (humanChoiceHit(mouseX, mouseY, x, y, width)) {
            cycleHumanIndex(HumanEditorSkin.TATTOO, "tattoo", mouseX < x + width / 2 ? -1 : 1);
            return true;
        }
        y += 34;

        if (humanChoiceHit(mouseX, mouseY, x, y, width)) {
            cycleHumanIndex(HumanEditorSkin.CLOTHES, "clothes", mouseX < x + width / 2 ? -1 : 1);
            return true;
        }
        return false;
    }

    private void updateHumanSlider(String key, double mouseX) {
        int x = rightTextLeft();
        int width = rightTextWidth();
        double percent = Math.max(0.0D, Math.min(1.0D, (mouseX - x) / width));
        int offset = clamp((int) Math.round(percent * 10.0D) - 5, -5, 5);
        if (HumanEditorSkin.HEIGHT.equals(key) || HumanEditorSkin.WIDTH.equals(key) || HumanEditorSkin.DEPTH.equals(key)) {
            customization.put(key, Double.toString(HumanEditorSkin.scaleFromOffset(key, offset)));
        } else {
            customization.put(key, Integer.toString(offset));
        }
    }

    private void updateHumanPalette(String palette, double mouseX, double mouseY) {
        int size = humanPaletteSize();
        int x = findHumanPaletteX(palette);
        int paletteY = findHumanPaletteY(palette);
        if (paletteY == Integer.MIN_VALUE || x == Integer.MIN_VALUE) {
            return;
        }
        double xNorm = Math.max(0.0D, Math.min(1.0D, (mouseX - x) / Math.max(1, size - 1)));
        double yNorm = Math.max(0.0D, Math.min(1.0D, (mouseY - paletteY) / Math.max(1, size - 1)));
        switch (palette) {
            case "hair" -> {
                customization.put(HumanEditorSkin.HAIR_COLOR_X, Double.toString(xNorm));
                customization.put(HumanEditorSkin.HAIR_COLOR_Y, Double.toString(yNorm));
            }
            case "eye" -> {
                customization.put(HumanEditorSkin.EYE_COLOR_X, Double.toString(xNorm));
                customization.put(HumanEditorSkin.EYE_COLOR_Y, Double.toString(yNorm));
            }
            case "left_eye" -> {
                customization.put(HumanEditorSkin.LEFT_EYE_COLOR_X, Double.toString(xNorm));
                customization.put(HumanEditorSkin.LEFT_EYE_COLOR_Y, Double.toString(yNorm));
            }
            case "right_eye" -> {
                customization.put(HumanEditorSkin.RIGHT_EYE_COLOR_X, Double.toString(xNorm));
                customization.put(HumanEditorSkin.RIGHT_EYE_COLOR_Y, Double.toString(yNorm));
            }
            default -> {
                customization.put(HumanEditorSkin.SKIN_COLOR_X, Double.toString(xNorm));
                customization.put(HumanEditorSkin.SKIN_COLOR_Y, Double.toString(yNorm));
            }
        }
    }

    private int findHumanPaletteY(String palette) {
        int y = rightContentTop() - rightScrollOffset;
        y += 34; // model
        if (HumanEditorSkin.isAvian(selected)) {
            y += 34; // wings
        }
        y += 36 * 7; // height, width, depth, muscles, lengths
        y += 34; // skin type
        if ("skin".equals(palette)) {
            return skinPaletteOpen ? humanPalettePopupY(y) : Integer.MIN_VALUE;
        }
        y += humanPaletteRowHeight(skinPaletteOpen);
        y += 34; // eyes
        if ("eye".equals(palette)) {
            return eyePaletteOpen ? humanPalettePopupY(y) : Integer.MIN_VALUE;
        }
        if ("left_eye".equals(palette) || "right_eye".equals(palette)) {
            return (leftEyePaletteOpen || rightEyePaletteOpen) ? humanPalettePopupY(y) : Integer.MIN_VALUE;
        }
        y += humanPaletteRowHeight(eyePaletteOpen || leftEyePaletteOpen || rightEyePaletteOpen);
        y += 34; // eyelashes
        y += 36; // eyelashes shade
        y += 34; // eyebrows
        y += 36; // eyebrows shade
        y += 34; // hair
        if ("male".equals(HumanEditorSkin.gender(customization))) {
            y += 34; // beard
        }
        if ("hair".equals(palette)) {
            return hairPaletteOpen ? humanPalettePopupY(y) : Integer.MIN_VALUE;
        }
        return Integer.MIN_VALUE;
    }

    private int findHumanPaletteX(String palette) {
        int x = rightTextLeft();
        int width = rightTextWidth();
        int halfW = width / 2;
        return switch (palette) {
            case "left_eye" -> humanPalettePopupX(x, halfW - 1);
            case "right_eye" -> humanPalettePopupX(x + halfW + 1, width - halfW - 1);
            default -> humanPalettePopupX(x, width);
        };
    }

    private boolean humanChoiceHit(double mouseX, double mouseY, int x, int y, int width) {
        int controlY = y + 11;
        return isRowVisible(controlY, 17, rightContentTop(), rightContentBottom())
                && isInside(mouseX, mouseY, x, controlY, width, 17);
    }

    private boolean humanSliderHit(double mouseX, double mouseY, int x, int y, int width) {
        int controlY = y + 10;
        return isRowVisible(controlY, 22, rightContentTop(), rightContentBottom())
                && isInside(mouseX, mouseY, x, controlY, width, 22);
    }

    private boolean humanPaletteButtonHit(double mouseX, double mouseY, int x, int y, int width) {
        int buttonY = y + 11;
        return isRowVisible(buttonY, 17, rightContentTop(), rightContentBottom())
                && isInside(mouseX, mouseY, x, buttonY, width, 17);
    }

    private boolean humanPaletteHit(double mouseX, double mouseY, int x, int y) {
        int paletteY = humanPalettePopupY(y);
        int size = humanPaletteSize();
        int paletteX = humanPalettePopupX(x, rightTextWidth());
        return isRowVisible(paletteY, size, rightContentTop(), rightContentBottom())
                && isInside(mouseX, mouseY, paletteX, paletteY, size, size);
    }

    private boolean humanPaletteHit(String palette, double mouseX, double mouseY) {
        int paletteY = findHumanPaletteY(palette);
        int paletteX = findHumanPaletteX(palette);
        int size = humanPaletteSize();
        return paletteY != Integer.MIN_VALUE
                && paletteX != Integer.MIN_VALUE
                && isRowVisible(paletteY, size, rightContentTop(), rightContentBottom())
                && isInside(mouseX, mouseY, paletteX, paletteY, size, size);
    }

    private boolean handleHumanEyePaletteClick(double mouseX, double mouseY, int x, int y, int width) {
        if (!humanHeterochromia()) {
            if (humanPaletteButtonHit(mouseX, mouseY, x, y, width)) {
                eyePaletteOpen = !eyePaletteOpen;
                leftEyePaletteOpen = false;
                rightEyePaletteOpen = false;
                rightScrollOffset = clamp(rightScrollOffset, 0, maxRightScroll());
                return true;
            }
            if (eyePaletteOpen && humanPaletteHit("eye", mouseX, mouseY)) {
                draggingHumanPaletteKey = "eye";
                updateHumanPalette(draggingHumanPaletteKey, mouseX, mouseY);
                return true;
            }
            return false;
        }

        int buttonY = y + 11;
        int halfW = width / 2;
        if (isRowVisible(buttonY, 17, rightContentTop(), rightContentBottom())) {
            if (isInside(mouseX, mouseY, x, buttonY, halfW - 1, 17)) {
                leftEyePaletteOpen = !leftEyePaletteOpen;
                rightEyePaletteOpen = false;
                eyePaletteOpen = false;
                rightScrollOffset = clamp(rightScrollOffset, 0, maxRightScroll());
                return true;
            }
            if (isInside(mouseX, mouseY, x + halfW + 1, buttonY, width - halfW - 1, 17)) {
                rightEyePaletteOpen = !rightEyePaletteOpen;
                leftEyePaletteOpen = false;
                eyePaletteOpen = false;
                rightScrollOffset = clamp(rightScrollOffset, 0, maxRightScroll());
                return true;
            }
        }
        if (leftEyePaletteOpen && humanPaletteHit("left_eye", mouseX, mouseY)) {
            draggingHumanPaletteKey = "left_eye";
            updateHumanPalette(draggingHumanPaletteKey, mouseX, mouseY);
            return true;
        }
        if (rightEyePaletteOpen && humanPaletteHit("right_eye", mouseX, mouseY)) {
            draggingHumanPaletteKey = "right_eye";
            updateHumanPalette(draggingHumanPaletteKey, mouseX, mouseY);
            return true;
        }
        return false;
    }

    private void cycleHumanIndex(String key, String folder, int delta) {
        int min = HumanEditorSkin.minIndex(folder);
        int max = HumanEditorSkin.maxIndex(minecraft, customization, folder);
        int value = humanInt(key, min) + delta;
        if (value < min) {
            value = max;
        } else if (value > max) {
            value = min;
        }
        customization.put(key, Integer.toString(value));
    }

    private void cycleHumanScars(int delta) {
        int max = HumanEditorSkin.maxIndex(minecraft, customization, "scars");
        int value = humanInt(HumanEditorSkin.SCARS, 0) + delta;
        if (value < 0) {
            value = max;
        } else if (value > max) {
            value = 0;
        }
        customization.put(HumanEditorSkin.SCARS, Integer.toString(value));
    }

    private void clampHumanIndexesAfterGenderChange() {
        clampHumanIndex(HumanEditorSkin.SKIN, "skin");
        clampHumanIndex(HumanEditorSkin.EYES, "eyes");
        if (HumanEditorSkin.isAvian(selected)) {
            clampHumanIndex(HumanEditorSkin.WINGS, "wings");
        }
        clampHumanIndex(HumanEditorSkin.EYELASHES, "eyelashes");
        clampHumanIndex(HumanEditorSkin.HAIR, "hair");
        clampHumanIndex(HumanEditorSkin.EYEBROWS, "eyebrows");
        clampHumanIndex(HumanEditorSkin.TATTOO, "tattoo");
        clampHumanIndex(HumanEditorSkin.CLOTHES, "clothes");
        if (!"male".equals(HumanEditorSkin.gender(customization))) {
            customization.put(HumanEditorSkin.BEARD, "0");
        } else {
            clampHumanIndex(HumanEditorSkin.BEARD, "beard");
        }
        customization.put(HumanEditorSkin.SCARS, Integer.toString(clamp(humanInt(HumanEditorSkin.SCARS, 0), 0, HumanEditorSkin.maxIndex(minecraft, customization, "scars"))));
    }

    private void clampHumanIndex(String key, String folder) {
        int min = HumanEditorSkin.minIndex(folder);
        int max = HumanEditorSkin.maxIndex(minecraft, customization, folder);
        customization.put(key, Integer.toString(clamp(humanInt(key, min), min, max)));
    }

    private String humanGenderLabel() {
        return "male".equals(HumanEditorSkin.gender(customization)) ? "Male" : "Female";
    }

    private String humanEyesLabel() {
        return humanHeterochromia() ? "Heterochromia" : "Normal";
    }

    private boolean humanHeterochromia() {
        return humanInt(HumanEditorSkin.EYES, 0) == 1;
    }

    private String humanIndexedLabel(String key, String folder, int fallback) {
        int max = HumanEditorSkin.maxIndex(minecraft, customization, folder);
        int value = humanInt(key, fallback);
        if (HumanEditorSkin.hasNoneChoice(folder) && value == 0) {
            return "None";
        }
        return value + " / " + max;
    }

    private String humanScarsLabel() {
        int value = humanInt(HumanEditorSkin.SCARS, 0);
        return value == 0 ? "None" : value + " / " + HumanEditorSkin.maxIndex(minecraft, customization, "scars");
    }

    private int humanPaletteSize() {
        return Math.min(96, Math.max(72, rightTextWidth() / 2));
    }

    private int humanPalettePopupY(int y) {
        return y + 31;
    }

    private int humanPalettePopupX(int x, int width) {
        return x + Math.max(0, (width - humanPaletteSize()) / 2);
    }

    private int humanPaletteRowHeight(boolean open) {
        return open ? 38 + humanPaletteSize() : 34;
    }

    private int humanInt(String key, int fallback) {
        try {
            return Integer.parseInt(customization.getOrDefault(key, Integer.toString(fallback)));
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private double humanDouble(String key, double fallback) {
        try {
            return Double.parseDouble(customization.getOrDefault(key, Double.toString(fallback)));
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private void updateSlider(String key, double mouseX) {
        CustomizationOption option = selected.customization().stream()
                .filter(candidate -> candidate.key().equals(key))
                .findFirst()
                .orElse(null);
        if (option == null) {
            return;
        }
        int x = rightTextLeft();
        int width = rightTextWidth();
        double percent = Math.max(0.0D, Math.min(1.0D, (mouseX - x) / width));
        double raw = option.min() + (option.max() - option.min()) * percent;
        double stepped = Math.round(raw / option.step()) * option.step();
        customization.put(key, Double.toString(Math.max(option.min(), Math.min(option.max(), stepped))));
    }

    private void cycleChoice(CustomizationOption option, boolean backwards) {
        if (option.choices().isEmpty()) {
            return;
        }
        String value = customization.getOrDefault(option.key(), option.normalizedDefault());
        int index = Math.max(0, option.choices().indexOf(value));
        index += backwards ? -1 : 1;
        if (index < 0) {
            index = option.choices().size() - 1;
        }
        if (index >= option.choices().size()) {
            index = 0;
        }
        customization.put(option.key(), option.choices().get(index));
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private void scrollOrigins(int direction) {
        int visible = visibleOriginTabs();
        originScrollOffset = clamp(originScrollOffset + direction, 0, Math.max(0, origins.size() - visible));
    }

    private boolean handleRightScrollClick(double mouseX, double mouseY) {
        int maxScroll = maxRightScroll();
        if (maxScroll <= 0) {
            return false;
        }
        int x = rightScrollTrackX();
        int y = rightScrollTrackY();
        int trackH = rightScrollTrackHeight();
        if (!isInside(mouseX, mouseY, x - 5, y, 14, trackH)) {
            return false;
        }
        int thumbY = rightScrollThumbY();
        int thumbH = rightScrollThumbHeight();
        if (isInside(mouseX, mouseY, x - 4, thumbY, 12, thumbH)) {
            rightScrollDragOffset = (int) mouseY - thumbY;
        } else {
            rightScrollDragOffset = thumbH / 2;
            updateRightScrollFromMouse(mouseY);
        }
        draggingRightScroll = true;
        pressedControl = PressedControl.NONE;
        return true;
    }

    private void updateRightScrollFromMouse(double mouseY) {
        int maxScroll = maxRightScroll();
        if (maxScroll <= 0) {
            rightScrollOffset = 0;
            return;
        }
        int trackY = rightScrollTrackY();
        int trackH = rightScrollTrackHeight();
        int thumbH = rightScrollThumbHeight();
        int movable = Math.max(1, trackH - thumbH);
        int thumbTop = clamp((int) mouseY - rightScrollDragOffset, trackY, trackY + movable);
        rightScrollOffset = clamp((int) Math.round((thumbTop - trackY) * (maxScroll / (double) movable)), 0, maxScroll);
    }

    private void selectAdjacentOrigin(int direction) {
        if (origins.isEmpty()) {
            return;
        }
        int current = selectedOriginIndex();
        if (current < 0) {
            current = 0;
        }
        selectOriginAtIndex(clamp(current + direction, 0, origins.size() - 1));
    }

    private void selectOriginAtIndex(int index) {
        if (index < 0 || index >= origins.size()) {
            return;
        }
        selected = origins.get(index);
        customization.clear();
        customization.putAll(selected.defaultCustomizationValues());
        if (HumanEditorSkin.isEditable(selected)) {
            HumanEditorSkin.ensureDefaults(customization, selected);
        }
        step = Step.DETAILS;
        skinPaletteOpen = false;
        eyePaletteOpen = false;
        leftEyePaletteOpen = false;
        rightEyePaletteOpen = false;
        hairPaletteOpen = false;
        rightScrollOffset = 0;
        ensureSelectedOriginVisible(index);
    }

    private int selectedOriginIndex() {
        for (int i = 0; i < origins.size(); i++) {
            if (origins.get(i).id().equals(selected.id())) {
                return i;
            }
        }
        return -1;
    }

    private void ensureSelectedOriginVisible(int selectedIndex) {
        if (selectedIndex < 0) {
            return;
        }
        int visible = visibleOriginTabs();
        if (selectedIndex < originScrollOffset) {
            originScrollOffset = selectedIndex;
        } else if (selectedIndex >= originScrollOffset + visible) {
            originScrollOffset = selectedIndex - visible + 1;
        }
        originScrollOffset = clamp(originScrollOffset, 0, Math.max(0, origins.size() - visible));
    }

    private void renderRightScrollHint(GuiGraphics graphics) {
        int maxScroll = maxRightScroll();
        if (maxScroll <= 0) {
            return;
        }
        int x = rightScrollTrackX();
        int y = rightScrollTrackY();
        int trackH = rightScrollTrackHeight();
        int thumbH = rightScrollThumbHeight();
        int thumbY = rightScrollThumbY();
        if (hasTexture(SCROLL_BAR_SLOT_TEXTURE) && hasTexture(SCROLL_BAR_TEXTURE)) {
            graphics.blit(SCROLL_BAR_SLOT_TEXTURE, x - 2, y, 8, trackH, 0.0F, 0.0F, 8, 134, 8, 134);
            graphics.blit(SCROLL_BAR_TEXTURE, x - 1, thumbY, 6, thumbH, 0.0F, 0.0F, 6, 27, 6, 27);
            return;
        }
        graphics.fill(x, y, x + 4, y + trackH, 0x88303030);
        graphics.fill(x - 1, thumbY, x + 5, thumbY + thumbH, 0xCCBFBFBF);
    }

    private int contentPad() {
        return rightW < 220 ? 16 : 26;
    }

    private int rightTextLeft() {
        return rightX + (rightW < 220 ? 20 : 36);
    }

    private int rightTextRight() {
        return rightScrollTrackX() - 10;
    }

    private int rightTextWidth() {
        return Math.max(80, rightTextRight() - rightTextLeft());
    }

    private int rightTextLayoutWidth() {
        return Math.max(80, (int) (rightTextWidth() / RIGHT_TEXT_SCALE));
    }

    private int rightScrollTrackX() {
        return rightX + rightW - 18;
    }

    private int rightScrollTrackY() {
        return rightContentTop();
    }

    private int rightScrollTrackHeight() {
        return Math.max(16, rightContentBottom() - rightScrollTrackY());
    }

    private int rightScrollThumbHeight() {
        int trackH = rightScrollTrackHeight();
        int maxScroll = maxRightScroll();
        return clamp(trackH * trackH / Math.max(trackH, trackH + maxScroll), 18, Math.min(27, trackH));
    }

    private int rightScrollThumbY() {
        int maxScroll = maxRightScroll();
        if (maxScroll <= 0) {
            return rightScrollTrackY();
        }
        int trackH = rightScrollTrackHeight();
        int thumbH = rightScrollThumbHeight();
        return rightScrollTrackY() + (int) ((trackH - thumbH) * (rightScrollOffset / (double) maxScroll));
    }

    private int lockButtonX() {
        return previewX + previewW - SMALL_BUTTON_SIZE - 16;
    }

    private int resetButtonX() {
        return lockButtonX() - SMALL_BUTTON_SIZE - 2;
    }

    private int originUpButtonY() {
        return panelY + 2;
    }

    private int originTabAreaTop() {
        return originUpButtonY() + ORIGIN_ARROW_SIZE + 5;
    }

    private int originTabListTop() {
        int available = Math.max(0, originTabAreaBottom() - originTabAreaTop());
        int visible = visibleOriginTabs();
        int occupied = originTabHeight() + Math.max(0, visible - 1) * originTabPitch();
        return originTabAreaTop() + Math.max(0, (available - occupied) / 2) + tabListTuneY;
    }

    private int originDownButtonY() {
        return panelY + panelH - ORIGIN_ARROW_SIZE - 2;
    }

    private int originTabAreaBottom() {
        return originDownButtonY() - 5;
    }

    private int rightContentTop() {
        return panelY + 42;
    }

    private int rightContentBottom() {
        return panelY + panelH - 20;
    }

    private int maxRightScroll() {
        int visibleHeight = Math.max(1, rightContentBottom() - rightContentTop());
        int contentHeight = step == Step.DETAILS ? detailsContentHeight() : customizationContentHeight();
        return Math.max(0, contentHeight - visibleHeight);
    }

    private int detailsContentHeight() {
        int textW = rightTextLayoutWidth();
        int height = 0;
        height += font.split(Component.literal(selected.description()), textW).size() * RIGHT_TEXT_LINE_HEIGHT;
        height += 26;
        for (PowerDefinition power : OriginDataManager.powersFor(selected)) {
            height += RIGHT_TEXT_LINE_HEIGHT;
            height += font.split(Component.literal(power.description()), textW - 14).size() * RIGHT_TEXT_SMALL_LINE_HEIGHT;
            height += 5;
        }
        return height;
    }

    private int customizationContentHeight() {
        if (HumanEditorSkin.isEditable(selected)) {
            int height = 34; // model
            if (HumanEditorSkin.isAvian(selected)) {
                height += 34; // wings
            }
            height += 36 * 7; // height, width, depth, muscles, lengths
            height += 34; // skin
            height += humanPaletteRowHeight(skinPaletteOpen);
            height += 34; // eyes
            height += humanPaletteRowHeight(eyePaletteOpen || leftEyePaletteOpen || rightEyePaletteOpen);
            height += 34; // eyelashes
            height += 36; // eyelashes shade
            height += 34; // eyebrows
            height += 36; // eyebrows shade
            height += 34; // hair
            if ("male".equals(HumanEditorSkin.gender(customization))) {
                height += 34; // beard
            }
            height += humanPaletteRowHeight(hairPaletteOpen);
            height += 34; // scars
            height += 34; // tattoo
            height += 34; // clothes
            return height;
        }
        return 22 + selected.customization().size() * 36;
    }

    private int visibleOriginTabs() {
        int available = Math.max(0, originTabAreaBottom() - originTabAreaTop());
        int tabH = originTabHeight();
        int pitch = originTabPitch();
        int count = available < tabH ? 1 : 1 + (available - tabH) / pitch;
        return Math.min(origins.size(), Math.max(1, count));
    }

    private int originTabHeight() {
        return tabsW == TAB_TEXTURE_WIDTH ? TAB_TEXTURE_HEIGHT : tabsW * TAB_TEXTURE_HEIGHT / TAB_TEXTURE_WIDTH;
    }

    private int originTabPitch() {
        return Math.max(1, originTabHeight() - 1);
    }

    private int originTabDrawX(boolean selected) {
        if (selected) {
            return previewX + 5 - originTabDrawWidth() + selectedTabTuneX;
        }
        return previewX - Math.max(1, originTabDrawWidth() - 4) + normalTabTuneX;
    }

    private int originTabDrawWidth() {
        return tabsW;
    }

    private void drawStringIfVisible(GuiGraphics graphics, Component text, int x, int y, int color, int top, int bottom) {
        if (isRowVisible(y, 10, top, bottom)) {
            drawScaledString(graphics, text, x, y, color);
        }
    }

    private void drawStringIfVisible(GuiGraphics graphics, net.minecraft.util.FormattedCharSequence text, int x, int y, int color, int top, int bottom) {
        if (isRowVisible(y, 10, top, bottom)) {
            drawScaledString(graphics, text, x, y, color);
        }
    }

    private void drawCenteredStringIfVisible(GuiGraphics graphics, net.minecraft.util.FormattedCharSequence text, int centerX, int y, int color, int top, int bottom) {
        if (isRowVisible(y, 10, top, bottom)) {
            int x = centerX - Math.round(font.width(text) * RIGHT_TEXT_SCALE / 2.0F);
            drawScaledString(graphics, text, x, y, color);
        }
    }

    private void drawScaledString(GuiGraphics graphics, Component text, int x, int y, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(RIGHT_TEXT_SCALE, RIGHT_TEXT_SCALE, 1.0F);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private void drawScaledString(GuiGraphics graphics, net.minecraft.util.FormattedCharSequence text, int x, int y, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(RIGHT_TEXT_SCALE, RIGHT_TEXT_SCALE, 1.0F);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private void fillIfVisible(GuiGraphics graphics, int left, int topY, int right, int bottomY, int color, int top, int bottom) {
        if (bottomY >= top && topY <= bottom) {
            graphics.fill(left, Math.max(topY, top), right, Math.min(bottomY, bottom), color);
        }
    }

    private static boolean isRowVisible(int y, int height, int top, int bottom) {
        return y + height >= top && y <= bottom;
    }

    private ResourceLocation originGuiTexture(OriginDefinition origin, String folder) {
        return ResourceLocation.fromNamespaceAndPath(origin.id().getNamespace(), "textures/gui/" + folder + "/" + origin.id().getPath() + ".png");
    }

    private ResourceLocation badgeTexture(PowerDefinition power) {
        String type = power.type();
        if (type.contains("active")) {
            return BADGE_ACTIVE_TEXTURE;
        }
        if (type.contains("toggle") || power.id().getPath().contains("phantomize")) {
            return BADGE_TOGGLE_TEXTURE;
        }
        return BADGE_INFO_TEXTURE;
    }

    private static ResourceLocation guiTexture(String name) {
        return ResourceLocation.fromNamespaceAndPath("originssecundus", "textures/gui/" + name + ".png");
    }

    private static ResourceLocation guiSpriteTexture(String name) {
        return ResourceLocation.fromNamespaceAndPath("originssecundus", "textures/gui/sprites/" + name + ".png");
    }

    private boolean hasTexture(ResourceLocation texture) {
        return minecraft != null && minecraft.getResourceManager().getResource(texture).isPresent();
    }

    private boolean isLeftMouseButtonDown() {
        return minecraft != null
                && GLFW.glfwGetMouseButton(minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    private double previewHeightScale() {
        try {
            return Math.max(0.85D, Math.min(1.15D, Double.parseDouble(customization.getOrDefault("height", "1.0"))));
        } catch (NumberFormatException ignored) {
            return 1.0D;
        }
    }

    private float humanScale(String key) {
        HumanEditorSkin.ensureDefaults(customization, selected);
        if (HumanEditorSkin.WIDTH.equals(key)) {
            return (float) HumanEditorSkin.widthScale(customization);
        }
        if (HumanEditorSkin.DEPTH.equals(key)) {
            return (float) HumanEditorSkin.depthScale(customization);
        }
        return (float) HumanEditorSkin.heightScale(customization);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum PressedControl {
        NONE,
        ORIGIN_UP,
        ORIGIN_DOWN,
        RESET_POSE,
        LOCK_TOGGLE,
        BACK,
        NEXT
    }

    private enum UiTuningTarget {
        NONE,
        SELECTED_TAB,
        NORMAL_TABS
    }

    private enum Step {
        DETAILS,
        CUSTOMIZATION
    }
}

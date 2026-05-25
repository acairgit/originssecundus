package net.ironhalo.originssecundus.client;

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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class OriginSelectionScreen extends Screen {
    private static final int PANEL_LIGHT = 0xFFE7E7E7;
    private static final int PANEL_MID = 0xFF888888;
    private static final int PANEL_DARK = 0xFF242424;
    private static final int INSET = 0xFF555555;
    private static final int GOLD = 0xFFB98A13;

    private final boolean initialSelection;
    private final List<OriginDefinition> origins;
    private final Map<String, String> customization = new LinkedHashMap<>();
    private OriginDefinition selected;
    private Step step = Step.DETAILS;
    private boolean rotationLocked;
    private boolean draggingPreview;
    private double dragStartX;
    private double dragStartY;
    private float previewRotation;
    private float previewPitch;
    private String draggingSliderKey;
    private int originScrollOffset;
    private int rightScrollOffset;

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
    private static final ResourceLocation PANEL_TEXTURE = guiTexture("panel");
    private static final ResourceLocation BUTTON_TEXTURE = guiTexture("button");
    private static final ResourceLocation TAB_TEXTURE = guiTexture("tab");
    private static final ResourceLocation TAB_SELECTED_TEXTURE = guiTexture("tab_selected");
    private static final ResourceLocation ARROW_UP_TEXTURE = guiTexture("arrow_up");
    private static final ResourceLocation ARROW_DOWN_TEXTURE = guiTexture("arrow_down");
    private static final ResourceLocation LOCK_TEXTURE = guiTexture("lock");
    private static final ResourceLocation UNLOCK_TEXTURE = guiTexture("unlock");
    private static final ResourceLocation RESET_TEXTURE = guiTexture("reset");

    public OriginSelectionScreen(boolean initialSelection) {
        super(Component.translatable("screen.originssecundus.select_origin"));
        this.initialSelection = initialSelection;
        this.origins = OriginDataManager.origins();
        Optional<ResourceLocation> current = ClientOriginState.originId();
        this.selected = current.flatMap(OriginDataManager::origin)
                .or(() -> OriginDataManager.origin(ResourceLocation.fromNamespaceAndPath("originssecundus", "avian")))
                .orElse(this.origins.getFirst());
        this.customization.putAll(this.selected.defaultCustomizationValues());
        this.customization.putAll(ClientOriginState.customization());
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !initialSelection;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        layout();
        renderDirtBackdrop(graphics);
        graphics.drawCenteredString(this.font, Component.translatable("screen.originssecundus.select_origin"), this.width / 2, 16, 0xFFFFFFFF);
        renderOriginTabs(graphics, mouseX, mouseY);
        renderPreviewPanel(graphics, mouseX, mouseY);
        renderRightPanel(graphics, mouseX, mouseY);
        renderFooterButtons(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        layout();
        if (button == 0) {
            if (handleOriginArrowClick(mouseX, mouseY)) {
                return true;
            }
            if (handleOriginTabClick(mouseX, mouseY)) {
                return true;
            }
            if (isInside(mouseX, mouseY, resetButtonX(), panelY + 12, 22, 22)) {
                previewRotation = 0.0F;
                previewPitch = 0.0F;
                return true;
            }
            if (isInside(mouseX, mouseY, lockButtonX(), panelY + 12, 22, 22)) {
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
            scrollOrigins(scrollY > 0 ? -1 : 1);
            return true;
        }
        if (isInside(mouseX, mouseY, rightX, panelY, rightW, panelH)) {
            rightScrollOffset = clamp(rightScrollOffset + (scrollY > 0 ? -28 : 28), 0, maxRightScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
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
        draggingPreview = false;
        draggingSliderKey = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void layout() {
        boolean compact = this.width < 760 || this.height < 420;
        gap = compact ? 4 : 6;
        tabsW = compact ? 30 : 36;
        footerButtonH = compact ? 18 : 20;
        footerGap = compact ? 5 : 8;

        int top = compact ? 24 : 34;
        panelH = Math.min(compact ? 270 : 340, Math.max(140, this.height - top - footerGap - footerButtonH - 8));
        panelY = compact ? top : Math.max(34, (this.height - panelH) / 2 - 8);

        int sideMargin = compact ? 6 : 18;
        int available = Math.max(260, this.width - sideMargin * 2 - tabsW - gap * 2);
        int minRight = compact ? 130 : 190;
        int maxRight = compact ? 210 : 300;
        rightW = clamp(this.width * (compact ? 38 : 31) / 100, minRight, Math.min(maxRight, available - 120));
        previewW = available - rightW;
        if (!compact && previewW > 360) {
            previewW = 360;
        }
        if (previewW < 120) {
            int delta = 120 - previewW;
            previewW = 120;
            rightW = Math.max(120, rightW - delta);
        }

        int total = tabsW + gap + previewW + gap + rightW;
        tabsX = Math.max(4, (this.width - total) / 2);
        previewX = tabsX + tabsW + gap;
        rightX = previewX + previewW + gap;
        footerButtonW = Math.min(86, Math.max(58, Math.min(previewW, rightW) / 2));
        rightScrollOffset = clamp(rightScrollOffset, 0, maxRightScroll());
    }

    private void renderDirtBackdrop(GuiGraphics graphics) {
        if (hasTexture(BACKGROUND_TEXTURE)) {
            for (int y = 0; y < height; y += 32) {
                for (int x = 0; x < width; x += 32) {
                    graphics.blit(BACKGROUND_TEXTURE, x, y, 32, 32, 0.0F, 0.0F, 32, 32, 32, 32);
                }
            }
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

    private void renderOriginTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        int tabH = originTabHeight();
        int pitch = originTabPitch();
        int visible = visibleOriginTabs();
        originScrollOffset = clamp(originScrollOffset, 0, Math.max(0, origins.size() - visible));
        renderArrowButton(graphics, tabsX + 2, panelY + 8, false);
        for (int i = 0; i < visible; i++) {
            OriginDefinition origin = origins.get(i + originScrollOffset);
            int y = panelY + 50 + i * pitch;
            boolean active = origin.id().equals(selected.id());
            int fill = active ? 0xFFE2E2E2 : 0xFFBDBDBD;
            drawTabPanel(graphics, tabsX, y, tabsW, tabH, fill, active);
            renderOriginIcon(graphics, origin, tabsX + 6, y + 6, Math.max(16, tabH - 12));
        }
        renderArrowButton(graphics, tabsX + 2, panelY + panelH - 30, true);
    }

    private void renderArrowButton(GuiGraphics graphics, int x, int y, boolean down) {
        int width = Math.max(24, tabsW - 4);
        int height = Math.max(22, Math.min(28, width));
        drawButtonPanel(graphics, x, y, width, height, 0xFF9A9A9A);
        ResourceLocation arrowTexture = down ? ARROW_DOWN_TEXTURE : ARROW_UP_TEXTURE;
        if (hasTexture(arrowTexture)) {
            int icon = Math.min(16, Math.min(width, height) - 8);
            graphics.blit(arrowTexture, x + (width - icon) / 2, y + (height - icon) / 2, icon, icon, 0.0F, 0.0F, 16, 16, 16, 16);
            return;
        }
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
        graphics.fill(previewX + 18, panelY + 18, previewX + previewW - 18, panelY + panelH - 18, 0xDD141414);
        graphics.fill(previewX + 20, panelY + 20, previewX + previewW - 20, panelY + panelH - 20, 0xCC1D1A16);

        int lockY = panelY + 12;
        int lockSize = 22;
        int resetX = resetButtonX();
        drawButtonPanel(graphics, resetX, lockY, lockSize, lockSize, 0xFF8E8E8E);
        if (hasTexture(RESET_TEXTURE)) {
            graphics.blit(RESET_TEXTURE, resetX + 4, lockY + 4, 14, 14, 0.0F, 0.0F, 16, 16, 16, 16);
        } else {
            graphics.drawCenteredString(font, "R", resetX + lockSize / 2, lockY + 7, 0xFFFFFFFF);
        }

        int lockX = lockButtonX();
        drawButtonPanel(graphics, lockX, lockY, lockSize, lockSize, rotationLocked ? 0xFFB0B0B0 : 0xFF8E8E8E);
        ResourceLocation lockTexture = rotationLocked ? LOCK_TEXTURE : UNLOCK_TEXTURE;
        if (hasTexture(lockTexture)) {
            graphics.blit(lockTexture, lockX + 4, lockY + 4, 14, 14, 0.0F, 0.0F, 16, 16, 16, 16);
        } else {
            graphics.drawCenteredString(font, rotationLocked ? "L" : "U", lockX + lockSize / 2, lockY + 7, 0xFFFFFFFF);
        }

        int centerX = previewX + previewW / 2;
        int entityScale = (int) (clamp(Math.min(previewW, panelH) / 4, 34, 78) * previewHeightScale());
        int footY = panelY + panelH - Math.max(24, panelH / 10);
        renderFeatureBackdrop(graphics, centerX, footY, entityScale);
        if (minecraft != null && minecraft.player != null) {
            int innerLeft = previewX + 32;
            int innerRight = previewX + previewW - 32;
            int innerTop = panelY + 34;
            int innerBottom = panelY + panelH - 28;
            if (rotationLocked || draggingPreview) {
                renderStaticPreviewEntity(
                        graphics,
                        innerLeft,
                        innerTop,
                        innerRight,
                        innerBottom,
                        entityScale,
                        0.05F,
                        minecraft.player
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
                        minecraft.player
                );
            }
        } else {
            renderPixelBody(graphics, centerX, footY - entityScale * 2);
        }
        renderFeatureOverlay(graphics, centerX, footY - entityScale * 2, entityScale);
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
                .rotateX(previewPitch * (float) (Math.PI / 180.0D))
                .rotateY(previewRotation * (float) (Math.PI / 180.0D));

        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 50.0F);
        graphics.pose().scale(scale / entityScale, scale / entityScale, -scale / entityScale);
        graphics.pose().mulPose(pose);
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
        if (selected.modelHint().contains("angel") || selected.modelHint().contains("avian") || selected.modelHint().contains("elytrian")) {
            int color = selected.modelHint().contains("angel") ? 0xAAE9E9E9 : 0xAAA2A2A2;
            int span = Math.max(54, entityScale * 2);
            int inner = Math.max(28, entityScale);
            graphics.fill(centerX - span, footY - entityScale * 3, centerX - inner, footY - entityScale * 2, color);
            graphics.fill(centerX + inner, footY - entityScale * 3, centerX + span, footY - entityScale * 2, color);
            graphics.fill(centerX - span - entityScale / 2, footY - entityScale * 2, centerX - inner, footY - entityScale, color);
            graphics.fill(centerX + inner, footY - entityScale * 2, centerX + span + entityScale / 2, footY - entityScale, color);
        }
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
        int frame = Math.max(10, Math.min(18, rightW / 12));
        int pad = contentPad();
        graphics.fill(rightX + frame, panelY + frame, rightX + rightW - frame, panelY + panelH - frame, INSET);
        int accent = 0xFF000000 | selected.accentColor();
        graphics.fill(rightX + pad, panelY + 24, rightX + rightW - pad, panelY + 50, accent);
        graphics.drawCenteredString(font, selected.name(), rightX + rightW / 2, panelY + 33, 0xFFFFFFFF);
        if (rightW >= 220) {
            renderImpactDots(graphics, rightX + rightW - pad - 44, panelY + 33);
        }

        rightScrollOffset = clamp(rightScrollOffset, 0, maxRightScroll());
        if (step == Step.DETAILS) {
            renderDetails(graphics);
        } else {
            renderCustomization(graphics, mouseX, mouseY);
        }
    }

    private void renderDetails(GuiGraphics graphics) {
        int pad = contentPad();
        int x = rightX + pad;
        int contentTop = rightContentTop();
        int contentBottom = rightContentBottom();
        int y = contentTop - rightScrollOffset;
        int textW = Math.max(80, rightW - pad * 2);
        for (var line : font.split(Component.literal(selected.description()), textW)) {
            drawStringIfVisible(graphics, line, x, y, 0xFFFFFFFF, contentTop, contentBottom);
            y += 11;
        }
        y += 12;
        drawStringIfVisible(graphics, Component.literal("Powers"), x, y, 0xFFE9E9E9, contentTop, contentBottom);
        y += 15;
        for (PowerDefinition power : OriginDataManager.powersFor(selected)) {
            fillIfVisible(graphics, x, y + 3, x + 5, y + 8, 0xFF90C66D, contentTop, contentBottom);
            drawStringIfVisible(graphics, Component.literal(power.name()), x + 10, y, 0xFFFFFFFF, contentTop, contentBottom);
            y += 12;
            for (var line : font.split(Component.literal(power.description()), textW - 10)) {
                drawStringIfVisible(graphics, line, x + 10, y, 0xFFD8D8D8, contentTop, contentBottom);
                y += 10;
            }
            y += 5;
        }
        renderRightScrollHint(graphics);
    }

    private void renderCustomization(GuiGraphics graphics, int mouseX, int mouseY) {
        int pad = contentPad();
        int x = rightX + pad;
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
                    renderSlider(graphics, option, x, y, Math.max(80, rightW - pad * 2));
                }
                y += 24;
            } else if ("toggle".equals(option.type())) {
                if (isRowVisible(y, 18, contentTop, contentBottom)) {
                    renderToggle(graphics, option, x, y);
                }
                y += 24;
            } else {
                if (isRowVisible(y, 20, contentTop, contentBottom)) {
                    renderChoice(graphics, option, x, y, Math.max(80, rightW - pad * 2));
                }
                y += 24;
            }
        }
        renderRightScrollHint(graphics);
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
        for (int i = 0; i < 3; i++) {
            int color = i < selected.impact() ? 0xFF89B76B : 0xFFC8C8C8;
            graphics.fill(x + i * 16, y, x + i * 16 + 10, y + 10, color);
            graphics.fill(x + i * 16 + 2, y + 2, x + i * 16 + 8, y + 8, color | 0x00202020);
        }
    }

    private void renderFooterButtons(GuiGraphics graphics, int mouseX, int mouseY) {
        int y = panelY + panelH + footerGap;
        drawButtonPanel(graphics, previewX, y, footerButtonW, footerButtonH, 0xFF9A9A9A);
        graphics.drawCenteredString(font, Component.translatable("screen.originssecundus.back"), previewX + footerButtonW / 2, y + footerButtonH / 2 - 4, 0xFFFFFFFF);
        int nextX = rightX + rightW - footerButtonW;
        drawButtonPanel(graphics, nextX, y, footerButtonW, footerButtonH, 0xFF9A9A9A);
        Component label = step == Step.DETAILS ? Component.translatable("screen.originssecundus.next") : Component.translatable("screen.originssecundus.confirm");
        graphics.drawCenteredString(font, label, nextX + footerButtonW / 2, y + footerButtonH / 2 - 4, 0xFFFFFFFF);
    }

    private void drawRaisedPanel(GuiGraphics graphics, int x, int y, int width, int height, int fill) {
        drawTexturedOrRaised(graphics, x, y, width, height, fill, PANEL_TEXTURE);
    }

    private void drawButtonPanel(GuiGraphics graphics, int x, int y, int width, int height, int fill) {
        drawTexturedOrRaised(graphics, x, y, width, height, fill, BUTTON_TEXTURE);
    }

    private void drawTabPanel(GuiGraphics graphics, int x, int y, int width, int height, int fill, boolean selected) {
        drawTexturedOrRaised(graphics, x, y, width, height, fill, selected ? TAB_SELECTED_TEXTURE : TAB_TEXTURE);
    }

    private void drawTexturedOrRaised(GuiGraphics graphics, int x, int y, int width, int height, int fill, ResourceLocation texture) {
        if (hasTexture(texture)) {
            graphics.blit(texture, x, y, width, height, 0.0F, 0.0F, 64, 64, 64, 64);
            return;
        }
        graphics.fill(x, y, x + width, y + height, 0xFF111111);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, PANEL_DARK);
        graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, fill);
        graphics.fill(x + 4, y + 4, x + width - 4, y + 7, 0xFFFFFFFF);
        graphics.fill(x + 4, y + height - 7, x + width - 4, y + height - 4, 0xFF686868);
    }

    private boolean handleOriginTabClick(double mouseX, double mouseY) {
        int visible = visibleOriginTabs();
        int pitch = originTabPitch();
        int tabH = originTabHeight();
        for (int i = 0; i < visible; i++) {
            int y = panelY + 50 + i * pitch;
            int index = i + originScrollOffset;
            if (index < origins.size() && isInside(mouseX, mouseY, tabsX, y, tabsW, tabH)) {
                selected = origins.get(index);
                customization.clear();
                customization.putAll(selected.defaultCustomizationValues());
                step = Step.DETAILS;
                rightScrollOffset = 0;
                return true;
            }
        }
        return false;
    }

    private boolean handleOriginArrowClick(double mouseX, double mouseY) {
        if (isInside(mouseX, mouseY, tabsX + 2, panelY + 8, Math.max(24, tabsW - 4), Math.max(22, Math.min(28, tabsW - 4)))) {
            scrollOrigins(-1);
            return true;
        }
        if (isInside(mouseX, mouseY, tabsX + 2, panelY + panelH - 30, Math.max(24, tabsW - 4), Math.max(22, Math.min(28, tabsW - 4)))) {
            scrollOrigins(1);
            return true;
        }
        return false;
    }

    private boolean handleFooterClick(double mouseX, double mouseY) {
        int y = panelY + panelH + footerGap;
        if (isInside(mouseX, mouseY, previewX, y, footerButtonW, footerButtonH)) {
            if (step == Step.CUSTOMIZATION) {
                step = Step.DETAILS;
            } else if (!initialSelection) {
                onClose();
            }
            return true;
        }
        int nextX = rightX + rightW - footerButtonW;
        if (isInside(mouseX, mouseY, nextX, y, footerButtonW, footerButtonH)) {
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
        int pad = contentPad();
        int x = rightX + pad;
        int y = panelY + 98 - rightScrollOffset;
        int width = Math.max(80, rightW - pad * 2);
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

    private void updateSlider(String key, double mouseX) {
        CustomizationOption option = selected.customization().stream()
                .filter(candidate -> candidate.key().equals(key))
                .findFirst()
                .orElse(null);
        if (option == null) {
            return;
        }
        int x = rightX + contentPad();
        int width = Math.max(80, rightW - contentPad() * 2);
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

    private void renderRightScrollHint(GuiGraphics graphics) {
        int maxScroll = maxRightScroll();
        if (maxScroll <= 0) {
            return;
        }
        int x = rightX + rightW - 18;
        int y = rightContentTop();
        int trackH = Math.max(16, rightContentBottom() - y);
        int thumbH = clamp(trackH * trackH / Math.max(trackH, trackH + maxScroll), 18, trackH);
        int thumbY = y + (int) ((trackH - thumbH) * (rightScrollOffset / (double) maxScroll));
        graphics.fill(x, y, x + 4, y + trackH, 0x88303030);
        graphics.fill(x - 1, thumbY, x + 5, thumbY + thumbH, 0xCCBFBFBF);
    }

    private int contentPad() {
        return rightW < 220 ? 16 : 26;
    }

    private int lockButtonX() {
        return previewX + previewW - 34;
    }

    private int resetButtonX() {
        return lockButtonX() - 26;
    }

    private int rightContentTop() {
        return panelY + 64;
    }

    private int rightContentBottom() {
        return panelY + panelH - 26;
    }

    private int maxRightScroll() {
        int visibleHeight = Math.max(1, rightContentBottom() - rightContentTop());
        int contentHeight = step == Step.DETAILS ? detailsContentHeight() : customizationContentHeight();
        return Math.max(0, contentHeight - visibleHeight);
    }

    private int detailsContentHeight() {
        int pad = contentPad();
        int textW = Math.max(80, rightW - pad * 2);
        int height = 0;
        height += font.split(Component.literal(selected.description()), textW).size() * 11;
        height += 27;
        for (PowerDefinition power : OriginDataManager.powersFor(selected)) {
            height += 12;
            height += font.split(Component.literal(power.description()), textW - 10).size() * 10;
            height += 5;
        }
        return height;
    }

    private int customizationContentHeight() {
        return 22 + selected.customization().size() * 36;
    }

    private int visibleOriginTabs() {
        return Math.min(origins.size(), Math.max(1, (panelH - 58) / originTabPitch()));
    }

    private int originTabHeight() {
        return Math.min(34, Math.max(28, tabsW));
    }

    private int originTabPitch() {
        return originTabHeight() + 8;
    }

    private void drawStringIfVisible(GuiGraphics graphics, Component text, int x, int y, int color, int top, int bottom) {
        if (isRowVisible(y, 10, top, bottom)) {
            graphics.drawString(font, text, x, y, color, false);
        }
    }

    private void drawStringIfVisible(GuiGraphics graphics, net.minecraft.util.FormattedCharSequence text, int x, int y, int color, int top, int bottom) {
        if (isRowVisible(y, 10, top, bottom)) {
            graphics.drawString(font, text, x, y, color, false);
        }
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

    private static ResourceLocation guiTexture(String name) {
        return ResourceLocation.fromNamespaceAndPath("originssecundus", "textures/gui/" + name + ".png");
    }

    private boolean hasTexture(ResourceLocation texture) {
        return minecraft != null && minecraft.getResourceManager().getResource(texture).isPresent();
    }

    private double previewHeightScale() {
        try {
            return Math.max(0.85D, Math.min(1.15D, Double.parseDouble(customization.getOrDefault("height", "1.0"))));
        } catch (NumberFormatException ignored) {
            return 1.0D;
        }
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

    private enum Step {
        DETAILS,
        CUSTOMIZATION
    }
}

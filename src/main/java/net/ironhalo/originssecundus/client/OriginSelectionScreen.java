package net.ironhalo.originssecundus.client;

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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

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
    private float previewRotation;
    private String draggingSliderKey;

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

    public OriginSelectionScreen(boolean initialSelection) {
        super(Component.translatable("screen.originssecundus.select_origin"));
        this.initialSelection = initialSelection;
        this.origins = OriginDataManager.origins();
        Optional<ResourceLocation> current = ClientOriginState.originId();
        this.selected = current.flatMap(OriginDataManager::origin).orElse(this.origins.getFirst());
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
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        layout();
        if (button == 0) {
            if (handleOriginTabClick(mouseX, mouseY)) {
                return true;
            }
            if (isInside(mouseX, mouseY, previewX + previewW - 42, panelY + 12, 28, 28)) {
                rotationLocked = !rotationLocked;
                return true;
            }
            if (!rotationLocked && isInside(mouseX, mouseY, previewX, panelY, previewW, panelH)) {
                draggingPreview = true;
                dragStartX = mouseX;
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingPreview && !rotationLocked) {
            previewRotation += (float) (mouseX - dragStartX) * 0.45F;
            dragStartX = mouseX;
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
        boolean compact = this.width < 700 || this.height < 360;
        gap = compact ? 5 : 8;
        tabsW = compact ? 36 : 46;
        footerButtonH = compact ? 24 : 34;
        footerGap = compact ? 8 : 18;

        int top = compact ? 30 : 42;
        panelH = Math.min(430, Math.max(150, this.height - top - footerGap - footerButtonH - 8));
        panelY = compact ? top : Math.max(42, (this.height - panelH) / 2 - 4);

        int sideMargin = compact ? 8 : 24;
        int available = Math.max(260, this.width - sideMargin * 2 - tabsW - gap * 2);
        int minRight = compact ? 150 : 230;
        int maxRight = compact ? 230 : 360;
        rightW = clamp(this.width * 34 / 100, minRight, Math.min(maxRight, available - 130));
        previewW = available - rightW;
        if (!compact && previewW > 430) {
            previewW = 430;
        }
        if (previewW < 130) {
            int delta = 130 - previewW;
            previewW = 130;
            rightW = Math.max(120, rightW - delta);
        }

        int total = tabsW + gap + previewW + gap + rightW;
        tabsX = Math.max(4, (this.width - total) / 2);
        previewX = tabsX + tabsW + gap;
        rightX = previewX + previewW + gap;
        footerButtonW = Math.min(150, Math.max(82, Math.min(previewW, rightW)));
    }

    private void renderDirtBackdrop(GuiGraphics graphics) {
        graphics.fill(0, 0, width, height, 0xFF24180F);
        for (int y = 0; y < height; y += 12) {
            for (int x = 0; x < width; x += 12) {
                int color = ((x + y) / 12) % 2 == 0 ? 0x332F2116 : 0x33201810;
                graphics.fill(x, y, Math.min(width, x + 12), Math.min(height, y + 12), color);
            }
        }
    }

    private void renderOriginTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        int visible = Math.min(origins.size(), Math.max(1, (panelH - 64) / 54));
        renderArrowButton(graphics, tabsX + 2, panelY + 8, false);
        for (int i = 0; i < visible; i++) {
            OriginDefinition origin = origins.get(i);
            int y = panelY + 66 + i * 54;
            boolean active = origin.id().equals(selected.id());
            int fill = active ? 0xFFE2E2E2 : 0xFFBDBDBD;
            int tabH = Math.min(46, Math.max(34, tabsW));
            drawRaisedPanel(graphics, tabsX, y, tabsW, tabH, fill);
            int accent = 0xFF000000 | origin.accentColor();
            graphics.fill(tabsX + 9, y + 8, tabsX + tabsW - 9, y + tabH - 12, accent);
            graphics.fill(tabsX + 14, y + 12, tabsX + tabsW - 14, y + tabH - 7, 0xFF303030);
            graphics.drawCenteredString(font, origin.name().substring(0, 1), tabsX + tabsW / 2, y + tabH / 2 - 4, 0xFFFFFFFF);
        }
        renderArrowButton(graphics, tabsX + 2, panelY + panelH - 34, true);
    }

    private void renderArrowButton(GuiGraphics graphics, int x, int y, boolean down) {
        int width = Math.max(30, tabsW - 4);
        int height = Math.max(26, Math.min(34, width));
        drawRaisedPanel(graphics, x, y, width, height, 0xFF9A9A9A);
        if (down) {
            graphics.fill(x + 8, y + 9, x + width - 8, y + 13, 0xFFFFFFFF);
            graphics.fill(x + 12, y + 13, x + width - 12, y + 18, 0xFFFFFFFF);
            graphics.fill(x + width / 2 - 1, y + 18, x + width / 2 + 1, y + 22, 0xFFFFFFFF);
        } else {
            graphics.fill(x + width / 2 - 1, y + 8, x + width / 2 + 1, y + 12, 0xFFFFFFFF);
            graphics.fill(x + 12, y + 12, x + width - 12, y + 17, 0xFFFFFFFF);
            graphics.fill(x + 8, y + 17, x + width - 8, y + 21, 0xFFFFFFFF);
        }
    }

    private void renderPreviewPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        drawRaisedPanel(graphics, previewX, panelY, previewW, panelH, PANEL_LIGHT);
        graphics.fill(previewX + 18, panelY + 18, previewX + previewW - 18, panelY + panelH - 18, 0xDD141414);
        graphics.fill(previewX + 20, panelY + 20, previewX + previewW - 20, panelY + panelH - 20, 0xCC1D1A16);

        int lockX = previewX + previewW - 42;
        int lockY = panelY + 12;
        drawRaisedPanel(graphics, lockX, lockY, 28, 28, rotationLocked ? 0xFFB0B0B0 : 0xFF8E8E8E);
        graphics.drawCenteredString(font, rotationLocked ? "L" : "U", lockX + 14, lockY + 10, 0xFFFFFFFF);

        int centerX = previewX + previewW / 2;
        int entityScale = clamp(Math.min(previewW, panelH) / 4, 36, 92);
        int footY = panelY + panelH - Math.max(24, panelH / 10);
        renderFeatureBackdrop(graphics, centerX, footY, entityScale);
        if (minecraft != null && minecraft.player != null) {
            float virtualMouseX = centerX + previewRotation;
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics,
                    previewX + 32,
                    panelY + 34,
                    previewX + previewW - 32,
                    panelY + panelH - 28,
                    entityScale,
                    0.05F,
                    virtualMouseX,
                    footY - entityScale * 2,
                    minecraft.player
            );
        } else {
            renderPixelBody(graphics, centerX, footY - entityScale * 2);
        }
        renderFeatureOverlay(graphics, centerX, footY - entityScale * 2, entityScale);
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
        graphics.fill(rightX + pad, panelY + 28, rightX + rightW - pad, panelY + 60, accent);
        graphics.drawCenteredString(font, selected.name(), rightX + rightW / 2, panelY + 39, 0xFFFFFFFF);
        if (rightW >= 220) {
            renderImpactDots(graphics, rightX + rightW - pad - 48, panelY + 39);
        }

        if (step == Step.DETAILS) {
            renderDetails(graphics);
        } else {
            renderCustomization(graphics, mouseX, mouseY);
        }
    }

    private void renderDetails(GuiGraphics graphics) {
        int pad = contentPad();
        int x = rightX + pad;
        int y = panelY + 76;
        int textW = Math.max(80, rightW - pad * 2);
        for (var line : font.split(Component.literal(selected.description()), textW)) {
            graphics.drawString(font, line, x, y, 0xFFFFFFFF, false);
            y += 11;
        }
        y += 12;
        graphics.drawString(font, "Powers", x, y, 0xFFE9E9E9, false);
        y += 15;
        for (PowerDefinition power : OriginDataManager.powersFor(selected)) {
            if (y > panelY + panelH - 48) {
                break;
            }
            graphics.fill(x, y + 3, x + 5, y + 8, 0xFF90C66D);
            graphics.drawString(font, power.name(), x + 10, y, 0xFFFFFFFF, false);
            y += 12;
            for (var line : font.split(Component.literal(power.description()), textW - 10)) {
                if (y > panelY + panelH - 48) {
                    break;
                }
                graphics.drawString(font, line, x + 10, y, 0xFFD8D8D8, false);
                y += 10;
            }
            y += 5;
        }
    }

    private void renderCustomization(GuiGraphics graphics, int mouseX, int mouseY) {
        int pad = contentPad();
        int x = rightX + pad;
        int y = panelY + 76;
        graphics.drawString(font, Component.translatable("screen.originssecundus.customize"), x, y, 0xFFFFFFFF, false);
        y += 22;
        for (CustomizationOption option : selected.customization()) {
            if (y > panelY + panelH - 54) {
                break;
            }
            graphics.drawString(font, option.label(), x, y, option.raceUnique() ? 0xFFFFE2A0 : 0xFFFFFFFF, false);
            y += 12;
            if ("slider".equals(option.type())) {
                renderSlider(graphics, option, x, y, Math.max(80, rightW - pad * 2));
                y += 24;
            } else if ("toggle".equals(option.type())) {
                renderToggle(graphics, option, x, y);
                y += 24;
            } else {
                renderChoice(graphics, option, x, y, Math.max(80, rightW - pad * 2));
                y += 24;
            }
        }
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
        graphics.fill(x, y + 2, x + 36, y + 16, active ? 0xFF6CA96A : 0xFF2B2B2B);
        graphics.fill(active ? x + 22 : x + 2, y + 4, active ? x + 34 : x + 14, y + 14, 0xFFEDEDED);
    }

    private void renderChoice(GuiGraphics graphics, CustomizationOption option, int x, int y, int width) {
        String value = customization.getOrDefault(option.key(), option.normalizedDefault());
        graphics.fill(x, y + 1, x + width, y + 18, 0xFF303030);
        graphics.fill(x, y + 1, x + 18, y + 18, 0xFF606060);
        graphics.fill(x + width - 18, y + 1, x + width, y + 18, 0xFF606060);
        graphics.drawString(font, "<", x + 6, y + 6, 0xFFFFFFFF, false);
        graphics.drawString(font, ">", x + width - 13, y + 6, 0xFFFFFFFF, false);
        graphics.drawCenteredString(font, value, x + width / 2, y + 6, 0xFFFFFFFF);
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
        drawRaisedPanel(graphics, previewX, y, footerButtonW, footerButtonH, 0xFF9A9A9A);
        graphics.drawCenteredString(font, Component.translatable("screen.originssecundus.back"), previewX + footerButtonW / 2, y + footerButtonH / 2 - 4, 0xFFFFFFFF);
        int nextX = rightX + rightW - footerButtonW;
        drawRaisedPanel(graphics, nextX, y, footerButtonW, footerButtonH, 0xFF9A9A9A);
        Component label = step == Step.DETAILS ? Component.translatable("screen.originssecundus.next") : Component.translatable("screen.originssecundus.confirm");
        graphics.drawCenteredString(font, label, nextX + footerButtonW / 2, y + footerButtonH / 2 - 4, 0xFFFFFFFF);
    }

    private void drawRaisedPanel(GuiGraphics graphics, int x, int y, int width, int height, int fill) {
        graphics.fill(x, y, x + width, y + height, 0xFF111111);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, PANEL_DARK);
        graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, fill);
        graphics.fill(x + 4, y + 4, x + width - 4, y + 7, 0xFFFFFFFF);
        graphics.fill(x + 4, y + height - 7, x + width - 4, y + height - 4, 0xFF686868);
    }

    private boolean handleOriginTabClick(double mouseX, double mouseY) {
        int visible = Math.min(origins.size(), Math.max(1, (panelH - 64) / 54));
        for (int i = 0; i < visible; i++) {
            int y = panelY + 66 + i * 54;
            if (isInside(mouseX, mouseY, tabsX, y, tabsW, 46)) {
                selected = origins.get(i);
                customization.clear();
                customization.putAll(selected.defaultCustomizationValues());
                step = Step.DETAILS;
                return true;
            }
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
        int y = panelY + 98;
        int width = Math.max(80, rightW - pad * 2);
        for (CustomizationOption option : selected.customization()) {
            if (y > panelY + panelH - 54) {
                break;
            }
            y += 12;
            if ("slider".equals(option.type())) {
                if (isInside(mouseX, mouseY, x, y, width, 18)) {
                    draggingSliderKey = option.key();
                    updateSlider(option.key(), mouseX);
                    return true;
                }
                y += 24;
            } else if ("toggle".equals(option.type())) {
                if (isInside(mouseX, mouseY, x, y, 44, 20)) {
                    boolean value = Boolean.parseBoolean(customization.getOrDefault(option.key(), option.normalizedDefault()));
                    customization.put(option.key(), Boolean.toString(!value));
                    return true;
                }
                y += 24;
            } else {
                if (isInside(mouseX, mouseY, x, y, width, 20)) {
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

    private int contentPad() {
        return rightW < 220 ? 20 : 38;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum Step {
        DETAILS,
        CUSTOMIZATION
    }
}

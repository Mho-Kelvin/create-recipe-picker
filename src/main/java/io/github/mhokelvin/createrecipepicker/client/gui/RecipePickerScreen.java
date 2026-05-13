package io.github.mhokelvin.createrecipepicker.client.gui;

import io.github.mhokelvin.createrecipepicker.CreateRecipePicker;
import io.github.mhokelvin.createrecipepicker.client.ConflictScanner;
import io.github.mhokelvin.createrecipepicker.client.RecipePickerClientState;
import io.github.mhokelvin.createrecipepicker.network.ClearPreferencePacket;
import io.github.mhokelvin.createrecipepicker.network.RecipePickerNetworking;
import io.github.mhokelvin.createrecipepicker.network.SetPreferencePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RecipePickerScreen extends Screen {

    private static final int BG_DARK = 0xf0_1a1408;
    private static final int BORDER_TOP = 0xff_e9a14a;
    private static final int BORDER_BOT = 0xff_8a5e1e;
    private static final int CARD_BG = 0xc0_2a1f0c;
    private static final int PILL_BG = 0xa0_3a2a14;
    private static final int PILL_HOVER = 0xb0_8a5e1e;
    private static final int PILL_PREFERRED = 0xff_8a5e1e;
    private static final int PILL_PREFERRED_HOVER = 0xff_b07c30;
    private static final int TOGGLE_ACTIVE = 0xff_8a5e1e;
    private static final int TOGGLE_INACTIVE = 0x80_2a1f0c;
    private static final int TEXT_BRASS = 0xff_e9a14a;
    private static final int TEXT_DIM = 0xff_b89970;

    private static final int WINDOW_W = 256;
    private static final int WINDOW_H = 224;
    private static final int LIST_INSET = 6;
    private static final int BORDER_THICKNESS = 2;

    private static final int HEADER_TITLE_Y = 7;
    private static final int FILTER_ROW_Y = 20;
    private static final int FILTER_ROW_H = 16;
    private static final int HEADER_H = FILTER_ROW_Y + FILTER_ROW_H + 4;

    private static final int CARD_PAD_X = 6;
    private static final int CARD_PAD_Y = 4;
    private static final int HEADER_ROW_H = 22;
    private static final int PILL_H = 18;
    private static final int PILL_GAP = 2;
    private static final int CARD_GAP = 3;
    private static final int PILL_INSET_X = 4;
    private static final int PILL_PAD_X = 4;

    private static final int ICON_SIZE = 16;
    private static final int OUTPUT_GAP = 3;

    private static final int TOGGLE_W = 32;
    private static final int TOGGLE_GAP = 2;

    private record TypeFilter(String label, ResourceLocation typeId) {}

    private static final List<TypeFilter> TYPE_FILTERS = List.of(
            new TypeFilter("Crush", new ResourceLocation("create:crushing")),
            new TypeFilter("Mill", new ResourceLocation("create:milling")),
            new TypeFilter("Haunt", new ResourceLocation("create:haunting")),
            new TypeFilter("Splash", new ResourceLocation("create:splashing")));

    private final List<ConflictScanner.Conflict> allConflicts;
    private final Set<ResourceLocation> activeTypes;
    private List<ConflictScanner.Conflict> filteredConflicts;
    private EditBox searchBox;
    private int guiLeft;
    private int guiTop;
    private int scrollPx = 0;
    private int contentHeight = 0;

    public RecipePickerScreen() {
        super(Component.literal("Recipe Picker"));
        this.allConflicts = ConflictScanner.get();
        this.activeTypes = new LinkedHashSet<>();
        for (TypeFilter f : TYPE_FILTERS) activeTypes.add(f.typeId());
        this.filteredConflicts = allConflicts;
    }

    @Override
    protected void init() {
        super.init();
        guiLeft = (width - WINDOW_W) / 2;
        guiTop = (height - WINDOW_H) / 2;

        int togglesW = TYPE_FILTERS.size() * TOGGLE_W + (TYPE_FILTERS.size() - 1) * TOGGLE_GAP;
        int searchX = guiLeft + LIST_INSET + togglesW + 6;
        int searchW = guiLeft + WINDOW_W - LIST_INSET - searchX;
        searchBox = new EditBox(font, searchX, guiTop + FILTER_ROW_Y, searchW, FILTER_ROW_H,
                Component.literal("search"));
        searchBox.setMaxLength(48);
        searchBox.setBordered(true);
        searchBox.setResponder(s -> recomputeFiltered());
        searchBox.setHint(Component.literal("Search items...").withStyle(ChatFormatting.DARK_GRAY));
        addRenderableWidget(searchBox);

        recomputeFiltered();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static int cardHeight(ConflictScanner.Conflict c) {
        int pills = c.alternatives().size();
        return CARD_PAD_Y + HEADER_ROW_H
                + pills * PILL_H + Math.max(0, pills - 1) * PILL_GAP
                + CARD_PAD_Y;
    }

    private void recomputeFiltered() {
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase();
        List<ConflictScanner.Conflict> result = new ArrayList<>();
        for (ConflictScanner.Conflict c : allConflicts) {
            if (!activeTypes.contains(c.typeId())) continue;
            if (!query.isEmpty()) {
                String name = new ItemStack(c.inputItem()).getHoverName().getString().toLowerCase();
                String id = c.inputItemId().toString().toLowerCase();
                if (!name.contains(query) && !id.contains(query)) continue;
            }
            result.add(c);
        }
        filteredConflicts = result;

        contentHeight = 0;
        for (ConflictScanner.Conflict c : filteredConflicts) {
            contentHeight += cardHeight(c) + CARD_GAP;
        }
        int listH = WINDOW_H - HEADER_H - LIST_INSET;
        int maxScroll = Math.max(0, contentHeight - listH);
        if (scrollPx > maxScroll) scrollPx = maxScroll;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        renderBackground(g);
        renderPanel(g, guiLeft, guiTop, WINDOW_W, WINDOW_H);

        g.drawString(font, getTitle(), guiLeft + 8, guiTop + HEADER_TITLE_Y, TEXT_BRASS, false);
        String subtitle = filteredConflicts.size() == allConflicts.size()
                ? allConflicts.size() + " conflict" + (allConflicts.size() == 1 ? "" : "s")
                : filteredConflicts.size() + " / " + allConflicts.size();
        g.drawString(font, Component.literal(subtitle).withStyle(ChatFormatting.GRAY),
                guiLeft + WINDOW_W - 8 - font.width(subtitle), guiTop + HEADER_TITLE_Y, TEXT_DIM, false);

        renderTypeToggles(g, mouseX, mouseY);

        int listX = guiLeft + LIST_INSET;
        int listY = guiTop + HEADER_H;
        int listW = WINDOW_W - LIST_INSET * 2;
        int listH = WINDOW_H - HEADER_H - LIST_INSET;
        g.enableScissor(listX, listY, listX + listW, listY + listH);

        if (filteredConflicts.isEmpty()) {
            String hint = allConflicts.isEmpty() ? "No recipe conflicts detected" : "No matches";
            g.drawString(font, hint,
                    listX + listW / 2 - font.width(hint) / 2,
                    listY + listH / 2 - 4, TEXT_DIM, false);
        } else {
            int y = listY - scrollPx;
            for (ConflictScanner.Conflict c : filteredConflicts) {
                int h = cardHeight(c);
                if (y + h >= listY && y <= listY + listH) {
                    renderCard(g, c, listX, y, listW, mouseX, mouseY);
                }
                y += h + CARD_GAP;
            }
        }
        g.disableScissor();

        super.render(g, mouseX, mouseY, partialTicks);

        renderHoverTooltip(g, mouseX, mouseY);
    }

    private void renderPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fillGradient(x, y, x + w, y + h, BORDER_TOP, BORDER_BOT);
        g.fill(x + BORDER_THICKNESS, y + BORDER_THICKNESS,
                x + w - BORDER_THICKNESS, y + h - BORDER_THICKNESS, BG_DARK);
    }

    private void renderTypeToggles(GuiGraphics g, int mouseX, int mouseY) {
        for (int i = 0; i < TYPE_FILTERS.size(); i++) {
            TypeFilter f = TYPE_FILTERS.get(i);
            int tx = toggleX(i);
            int ty = guiTop + FILTER_ROW_Y;
            boolean active = activeTypes.contains(f.typeId());
            boolean hovered = mouseX >= tx && mouseX < tx + TOGGLE_W
                    && mouseY >= ty && mouseY < ty + FILTER_ROW_H;
            int bg = active ? TOGGLE_ACTIVE : TOGGLE_INACTIVE;
            if (hovered) bg = active ? BORDER_TOP : 0xb0_3a2a14;
            g.fill(tx, ty, tx + TOGGLE_W, ty + FILTER_ROW_H, bg);
            int textX = tx + TOGGLE_W / 2 - font.width(f.label()) / 2;
            g.drawString(font, f.label(), textX, ty + 4,
                    active ? 0xffffffff : TEXT_DIM, false);
        }
    }

    private int toggleX(int i) {
        return guiLeft + LIST_INSET + i * (TOGGLE_W + TOGGLE_GAP);
    }

    private void renderCard(GuiGraphics g, ConflictScanner.Conflict c,
                            int x, int y, int w, int mouseX, int mouseY) {
        int h = cardHeight(c);
        g.fill(x, y, x + w, y + h, CARD_BG);

        ItemStack inputStack = new ItemStack(c.inputItem());
        g.renderItem(inputStack, x + CARD_PAD_X, y + CARD_PAD_Y);
        g.drawString(font, c.typeId().getPath(),
                x + CARD_PAD_X + 22, y + CARD_PAD_Y + 1, TEXT_DIM, false);
        g.drawString(font, inputStack.getHoverName(),
                x + CARD_PAD_X + 22, y + CARD_PAD_Y + 12, 0xffffffff, false);

        ResourceLocation preferredRecipe = RecipePickerClientState.getPreference(c.typeId(), c.inputItemId());

        RegistryAccess registries = registries();
        int pillX = x + PILL_INSET_X;
        int pillW = w - PILL_INSET_X * 2;
        int pillY = y + CARD_PAD_Y + HEADER_ROW_H;
        for (int i = 0; i < c.alternatives().size(); i++) {
            Recipe<?> alt = c.alternatives().get(i);
            boolean preferred = alt.getId().equals(preferredRecipe);
            boolean hovered = mouseX >= pillX && mouseX < pillX + pillW
                    && mouseY >= pillY && mouseY < pillY + PILL_H;
            g.fill(pillX, pillY, pillX + pillW, pillY + PILL_H,
                    pillColor(preferred, hovered));
            renderOutputsInPill(g, alt, pillX, pillY, pillW, registries);
            pillY += PILL_H + PILL_GAP;
        }
    }

    private static int pillColor(boolean preferred, boolean hovered) {
        if (preferred && hovered) return PILL_PREFERRED_HOVER;
        if (preferred) return PILL_PREFERRED;
        if (hovered) return PILL_HOVER;
        return PILL_BG;
    }

    private void renderOutputsInPill(GuiGraphics g, Recipe<?> recipe,
                                     int pillX, int pillY, int pillW, RegistryAccess registries) {
        List<ConflictScanner.OutputEntry> outputs = ConflictScanner.outputsOf(recipe, registries);
        int iconY = pillY + 1;
        int iconX = pillX + PILL_PAD_X;
        for (ConflictScanner.OutputEntry e : outputs) {
            if (iconX + ICON_SIZE > pillX + pillW - PILL_PAD_X) break;
            if (e.chance() < 1f) {
                renderChanceSlot(g, iconX, iconY);
            }
            g.renderItem(e.stack(), iconX, iconY);
            g.renderItemDecorations(font, e.stack(), iconX, iconY);
            iconX += ICON_SIZE + OUTPUT_GAP;
        }
    }

    private static final int CHECKER_BRASS = 0xc0_6d4c18;
    private static final int CHECKER_CELL = 4;
    private static final int CHANCE_SLOT_SIZE = ICON_SIZE;
    private static final int CHECKER_CELLS_PER_SIDE = CHANCE_SLOT_SIZE / CHECKER_CELL;

    private static void renderChanceSlot(GuiGraphics g, int iconX, int iconY) {
        for (int row = 0; row < CHECKER_CELLS_PER_SIDE; row++) {
            for (int col = 0; col < CHECKER_CELLS_PER_SIDE; col++) {
                if ((row + col) % 2 != 0) continue;
                int x = iconX + col * CHECKER_CELL;
                int y = iconY + row * CHECKER_CELL;
                g.fill(x, y, x + CHECKER_CELL, y + CHECKER_CELL, CHECKER_BRASS);
            }
        }
    }

    private void renderHoverTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (filteredConflicts.isEmpty()) return;
        int listX = guiLeft + LIST_INSET;
        int listY = guiTop + HEADER_H;
        int listW = WINDOW_W - LIST_INSET * 2;
        int listH = WINDOW_H - HEADER_H - LIST_INSET;
        if (mouseY < listY || mouseY > listY + listH || mouseX < listX || mouseX > listX + listW) return;

        int y = listY - scrollPx;
        for (ConflictScanner.Conflict c : filteredConflicts) {
            int h = cardHeight(c);
            int pillX = listX + PILL_INSET_X;
            int pillW = listW - PILL_INSET_X * 2;
            int pillY = y + CARD_PAD_Y + HEADER_ROW_H;
            for (Recipe<?> alt : c.alternatives()) {
                if (mouseX >= pillX && mouseX < pillX + pillW
                        && mouseY >= pillY && mouseY < pillY + PILL_H) {
                    g.renderComponentTooltip(font, buildAltTooltip(alt), mouseX, mouseY);
                    return;
                }
                pillY += PILL_H + PILL_GAP;
            }
            y += h + CARD_GAP;
        }
    }

    private List<Component> buildAltTooltip(Recipe<?> alt) {
        RegistryAccess registries = registries();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(prettyNamespace(alt.getId().getNamespace()))
                .withStyle(ChatFormatting.GRAY));
        for (ConflictScanner.OutputEntry e : ConflictScanner.outputsOf(alt, registries)) {
            String count = e.stack().getCount() + "× ";
            Component name = e.stack().getHoverName().copy().withStyle(ChatFormatting.WHITE);
            Component line = Component.literal("• " + count).withStyle(ChatFormatting.GRAY).append(name);
            if (e.chance() < 1f) {
                int pct = Math.round(e.chance() * 100);
                line = line.copy().append(Component.literal(" (" + pct + "%)").withStyle(ChatFormatting.DARK_GRAY));
            }
            lines.add(line);
        }
        return lines;
    }

    private static String prettyNamespace(String namespace) {
        return ModList.get().getModContainerById(namespace)
                .map(c -> c.getModInfo().getDisplayName())
                .orElse(namespace);
    }

    private static RegistryAccess registries() {
        return Minecraft.getInstance().level == null
                ? RegistryAccess.EMPTY
                : Minecraft.getInstance().level.registryAccess();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int listH = WINDOW_H - HEADER_H - LIST_INSET;
        int maxScroll = Math.max(0, contentHeight - listH);
        scrollPx = Math.max(0, Math.min(maxScroll, scrollPx - (int) (delta * 16)));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Type toggle clicks first
        if (button == 0 && mouseY >= guiTop + FILTER_ROW_Y && mouseY < guiTop + FILTER_ROW_Y + FILTER_ROW_H) {
            for (int i = 0; i < TYPE_FILTERS.size(); i++) {
                int tx = toggleX(i);
                if (mouseX >= tx && mouseX < tx + TOGGLE_W) {
                    TypeFilter f = TYPE_FILTERS.get(i);
                    if (!activeTypes.add(f.typeId())) activeTypes.remove(f.typeId());
                    recomputeFiltered();
                    return true;
                }
            }
        }
        // Pill clicks
        if (button == 0 && !filteredConflicts.isEmpty()) {
            int listX = guiLeft + LIST_INSET;
            int listY = guiTop + HEADER_H;
            int listW = WINDOW_W - LIST_INSET * 2;
            int listH = WINDOW_H - HEADER_H - LIST_INSET;
            if (mouseY >= listY && mouseY < listY + listH) {
                int y = listY - scrollPx;
                for (ConflictScanner.Conflict c : filteredConflicts) {
                    int h = cardHeight(c);
                    int pillX = listX + PILL_INSET_X;
                    int pillW = listW - PILL_INSET_X * 2;
                    int pillY = y + CARD_PAD_Y + HEADER_ROW_H;
                    for (Recipe<?> alt : c.alternatives()) {
                        if (mouseX >= pillX && mouseX < pillX + pillW
                                && mouseY >= pillY && mouseY < pillY + PILL_H) {
                            ResourceLocation currentPreferred = RecipePickerClientState
                                    .getPreference(c.typeId(), c.inputItemId());
                            if (alt.getId().equals(currentPreferred)) {
                                RecipePickerNetworking.CHANNEL.sendToServer(
                                        new ClearPreferencePacket(c.typeId(), c.inputItemId()));
                            } else {
                                RecipePickerNetworking.CHANNEL.sendToServer(
                                        new SetPreferencePacket(c.typeId(), c.inputItemId(), alt.getId()));
                            }
                            return true;
                        }
                        pillY += PILL_H + PILL_GAP;
                    }
                    y += h + CARD_GAP;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}

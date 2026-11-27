package zone.moddev.mc.skysgrassslabs.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.language.I18n;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.config.SkysGrassSlabsConfig;

/** Save generation and recovery choices for the next Minecraft start. */
public final class GrassSlabsConfigScreen extends Screen {
    private static final String GENERATION_KEY = "config.skysgrassslabs.generateGrassSlabs";
    private static final String REPLACEMENT_KEY = "config.skysgrassslabs.forceReplaceBuildingBricksSlabs";
    private static final String GRASS_SLABS_KEY = "config.skysgrassslabs.forceReplaceGrassSlabsModContent";

    private final Screen parent;
    private boolean generateGrass;
    private boolean replaceLegacy;
    private boolean replaceGrassSlabs;
    private Button generationButton;
    private Button replacementButton;
    private Button grassSlabsButton;

    public GrassSlabsConfigScreen(Screen parent) {
        super(Component.literal(SkysGrassSlabs.NAME));
        this.parent = parent;
        generateGrass = SkysGrassSlabsConfig.configuredGrassGeneration();
        replaceLegacy = SkysGrassSlabsConfig.configuredLegacyReplacement();
        replaceGrassSlabs = SkysGrassSlabsConfig.configuredGrassSlabsReplacement();
    }

    @Override
    protected void init() {
        int left = width / 2 - 150;
        generationButton = addRenderableWidget(Button.builder(label(GENERATION_KEY, generateGrass), button -> {
            generateGrass = !generateGrass;
            button.setMessage(label(GENERATION_KEY, generateGrass));
        }).bounds(left, 65, 300, 20).build());
        replacementButton = addRenderableWidget(Button.builder(label(REPLACEMENT_KEY, replaceLegacy), button -> {
            replaceLegacy = !replaceLegacy;
            button.setMessage(label(REPLACEMENT_KEY, replaceLegacy));
        }).bounds(left, 95, 300, 20).build());
        grassSlabsButton = addRenderableWidget(Button.builder(label(GRASS_SLABS_KEY, replaceGrassSlabs), button -> {
            replaceGrassSlabs = !replaceGrassSlabs;
            button.setMessage(label(GRASS_SLABS_KEY, replaceGrassSlabs));
        }).bounds(left, 125, 300, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> {
            SkysGrassSlabsConfig.saveForNextStart(generateGrass, replaceLegacy, replaceGrassSlabs);
            minecraft.setScreen(parent);
        }).bounds(width / 2 - 100, height - 35, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics, mouseX, mouseY, partialTicks);
        graphics.drawCenteredString(font, title, width / 2, 25, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTicks);

        if (generationButton.isMouseOver(mouseX, mouseY)) {
            graphics.renderTooltip(font, font.split(Component.translatable(GENERATION_KEY + ".tooltip"), 280),
                    mouseX, mouseY);
        } else if (replacementButton.isMouseOver(mouseX, mouseY)) {
            graphics.renderTooltip(font, font.split(Component.translatable(REPLACEMENT_KEY + ".tooltip"), 280),
                    mouseX, mouseY);
        } else if (grassSlabsButton.isMouseOver(mouseX, mouseY)) {
            graphics.renderTooltip(font, font.split(Component.translatable(GRASS_SLABS_KEY + ".tooltip"), 280),
                    mouseX, mouseY);
        }
    }

    @Override
    public void onClose() {
        // Escape returns without saving the pending button values.
        minecraft.setScreen(parent);
    }

    private static Component label(String key, boolean enabled) {
        return Component.literal(I18n.get(key) + ": " + I18n.get(enabled ? "options.on" : "options.off"));
    }
}

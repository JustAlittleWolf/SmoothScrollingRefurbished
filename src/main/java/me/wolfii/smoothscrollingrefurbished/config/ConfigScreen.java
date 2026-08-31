package me.wolfii.smoothscrollingrefurbished.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class ConfigScreen extends OptionsSubScreen {
    private final List<AbstractWidget> optionButtons = new ArrayList<>();
    private Button resetButton;
    private TestList testList;

    public ConfigScreen(Screen lastScreen, Options options) {
        super(lastScreen, options, Component.translatable("smoothscrollingrefurbished.config.title"));
    }

    private static OptionInstance<Integer> doubleSliderOption(
        String translatableKey,
        double minInclusive,
        double maxInclusive,
        double initial,
        int decimals,
        Consumer<Double> onChange
    ) {
        return doubleSliderOption(translatableKey, minInclusive, maxInclusive, initial, decimals, onChange, (it, original) -> original.apply(it));
    }

    private static OptionInstance<Integer> doubleSliderOption(
        String translatableKey,
        double minInclusive,
        double maxInclusive,
        double initial,
        int decimals,
        Consumer<Double> onChange,
        BiFunction<Double, Function<Double, Component>, Component> valueToStringOverride
    ) {
        double scale = Math.pow(10, decimals);
        String valueFormat = "%." + decimals + "f";

        return new OptionInstance<>(
            translatableKey,
            OptionInstance.cachedConstantTooltip(Component.translatable(translatableKey + ".tooltip")),
            (_, value) ->
                Options.genericValueLabel(
                    Component.translatable(translatableKey),
                    valueToStringOverride.apply(value / scale, it ->
                        Component.literal(String.format(Locale.ROOT, valueFormat, it))
                    )
                ),
            new OptionInstance.IntRange((int) Math.round(minInclusive * scale), (int) Math.round(maxInclusive * scale)),
            (int) Math.round(initial * scale),
            value -> onChange.accept(value.doubleValue() / scale)
        );
    }

    private static OptionInstance<?>[] options() {
        return new OptionInstance[]{
            doubleSliderOption(
                "smoothscrollingrefurbished.option.scrollStrength",
                0.1,
                2.5,
                Config.INSTANCE.scrollStrength,
                2,
                newValue -> Config.INSTANCE.scrollStrength = newValue
            ),
            doubleSliderOption(
                "smoothscrollingrefurbished.option.scrollbarFriction",
                0.01,
                0.1,
                Config.INSTANCE.scrollbarFriction,
                3,
                newValue -> Config.INSTANCE.scrollbarFriction = newValue
            ),
            doubleSliderOption(
                "smoothscrollingrefurbished.option.pushBackStrength",
                0.1,
                Config.MAX_PUSHBACK_STRENGTH + 0.01,
                Config.INSTANCE.pushBackStrength,
                2,
                newValue -> Config.INSTANCE.pushBackStrength = newValue,
                (value, original) ->
                    value <= Config.MAX_PUSHBACK_STRENGTH
                        ? original.apply(value)
                        : Component.translatable("options.off")
            ),
        };
    }

    @Override
    public void onClose() {
        super.onClose();
        Config.save();
    }

    @Override
    protected void addContents() {

    }

    @Override
    protected void addOptions() {
    }

    @Override
    protected void init() {
        super.init();
        this.list = null;
        this.optionButtons.clear();

        for (OptionInstance<?> option : options()) {
            AbstractWidget button = option.createButton(this.options, 0, 0, 100);
            this.optionButtons.add(button);
            this.addRenderableWidget(button);
        }

        this.resetButton = Button.builder(
            Component.translatable("controls.reset"),
            _ -> {
                Config.INSTANCE.resetToDefaults();
                this.rebuildWidgets();
            }
        ).build();
        this.addRenderableWidget(this.resetButton);

        this.testList = new TestList(this.minecraft);
        this.addRenderableWidget(this.testList);

        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        super.repositionElements();

        int totalWidth = Math.min(340, this.width - 24);
        int startX = (this.width - totalWidth) / 2;
        int leftWidth = (int) ((totalWidth - 8) * 0.6);
        int rightWidth = totalWidth - 8 - leftWidth;
        int topY = this.layout.getHeaderHeight();

        int y = topY;
        for (AbstractWidget button : this.optionButtons) {
            button.setPosition(startX, y);
            button.setWidth(leftWidth);
            y += 26;
        }

        if (this.resetButton != null) {
            int bottomY = topY + this.layout.getContentHeight() - this.resetButton.getHeight();
            this.resetButton.setPosition(startX, Math.max(y, bottomY));
            this.resetButton.setWidth(leftWidth);
        }

        if (this.testList != null) {
            this.testList.updateSizeAndPosition(rightWidth, this.layout.getContentHeight(), startX + leftWidth + 8, topY);
        }
    }

    private static class TestList extends AbstractSelectionList<TestList.Entry> {
        public TestList(Minecraft minecraft) {
            super(minecraft, 0, 0, 0, 50);
            for (int i = 1; i <= 25; i++) {
                this.addEntry(new Entry(Component.translatable("smoothscrollingrefurbished.options.scrolltest")));
            }
        }

        @Override
        public int getRowWidth() {
            return this.getWidth() - 10;
        }

        @Override
        protected int scrollBarX() {
            return this.getRight() - 6;
        }

        @Override
        protected void updateWidgetNarration(@NonNull NarrationElementOutput output) {
        }

        public static class Entry extends AbstractSelectionList.Entry<Entry> {
            private final Component text;

            public Entry(Component text) {
                this.text = text;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                graphics.text(Minecraft.getInstance().font, this.text, this.getContentX() + 4, this.getContentYMiddle() - 4, 0xFFFFFFFF, true);
            }
        }
    }
}
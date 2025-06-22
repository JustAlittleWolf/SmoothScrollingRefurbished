package me.wolfii.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.gui.widget.ScrollableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntryListWidget.class)
public class EntryListWidgetMixin {
    @Inject(
        method = "renderWidget",
        at = @At("HEAD")
    )
    private void manipulateScrollAmount(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ((ScrollableWidget) (Object) this).smoothScrollingRefurbished$manipulateScrollAmount(delta);
    }
}

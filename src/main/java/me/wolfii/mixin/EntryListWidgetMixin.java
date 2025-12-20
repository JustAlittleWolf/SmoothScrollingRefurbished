package me.wolfii.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSelectionList.class)
public class EntryListWidgetMixin {
    @Inject(
        method = "renderWidget",
        at = @At("HEAD")
    )
    private void manipulateScrollAmount(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ((AbstractScrollArea) (Object) this).smoothScrollingRefurbished$manipulateScrollAmount(delta);
    }
}

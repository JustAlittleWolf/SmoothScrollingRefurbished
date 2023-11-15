package me.wolfii.mixin;

import me.wolfii.Config;
import me.wolfii.ScrollMath;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntryListWidget.class)
public abstract class EntryListWidgetMixin {
    @Shadow
    private double scrollAmount;
    @Shadow
    protected int bottom;

    @Shadow
    public abstract int getMaxScroll();

    @Unique
    private double animationTimer = 0;
    @Unique
    private double scrollStartVelocity = 0;

    @Inject(method = "render", at = @At("HEAD"))
    private void manipulateScrollAmount(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        checkOutOfBounds(delta);

        if (Math.abs(ScrollMath.scrollbarVelocity(animationTimer, scrollStartVelocity)) < 1.0) return;
        applyMotion(delta);
    }

    @Unique
    private void applyMotion(float delta) {
        scrollAmount += ScrollMath.scrollbarVelocity(animationTimer, scrollStartVelocity) * delta;
        animationTimer += delta * 10 / Config.animationDuration;
    }

    @Unique
    private void checkOutOfBounds(float delta) {
        if (scrollAmount < 0) {
            scrollAmount += ScrollMath.pushBackStrength(Math.abs(scrollAmount), delta);
            if (scrollAmount > -0.2) scrollAmount = 0;
        }
        if (scrollAmount > getMaxScroll()) {
            scrollAmount -= ScrollMath.pushBackStrength(scrollAmount - getMaxScroll(), delta);
            if (scrollAmount < getMaxScroll() + 0.2) scrollAmount = getMaxScroll();
        }
    }

    @Redirect(method = "mouseScrolled", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/EntryListWidget;setScrollAmount(D)V"))
    private void setVelocity(EntryListWidget<?> instance, double amount) {
        double diff = amount - scrollAmount;
        diff = Math.signum(diff) * Math.min(Math.abs(diff), 10);
        diff *= Config.scrollSpeed;
        if (Math.signum(diff) != Math.signum(scrollStartVelocity)) diff *= 2.5d;
        animationTimer *= 0.5;
        scrollStartVelocity = ScrollMath.scrollbarVelocity(animationTimer, scrollStartVelocity) + diff;
        animationTimer = 0;
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V"))
    private void modifyScrollbar(DrawContext instance, Identifier texture, int x, int y, int width, int height) {
        if (scrollAmount < 0) {
            height -= ScrollMath.dampenSquish(Math.abs(scrollAmount), height);
        }
        if (y + height > bottom) {
            y = bottom - height;
        }
        if (scrollAmount > getMaxScroll()) {
            int squish = ScrollMath.dampenSquish(scrollAmount - getMaxScroll(), height);
            y += squish;
            height -= squish;
        }
        instance.drawGuiTexture(texture, x, y, width, height);
    }
}
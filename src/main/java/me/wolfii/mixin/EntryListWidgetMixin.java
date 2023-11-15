package me.wolfii.mixin;

import me.wolfii.Config;
import me.wolfii.ScrollMath;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.EntryListWidget;
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
        diff *= Config.scrollSpeed;
        if (Math.signum(diff) != Math.signum(scrollStartVelocity)) diff *= 2.5d;
        animationTimer *= 0.5;
        scrollStartVelocity = ScrollMath.scrollbarVelocity(animationTimer, scrollStartVelocity) + diff;
        animationTimer = 0;
    }

    @Redirect(method = "render", at=@At(value="INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V", ordinal = 1))
    private void modifyScrollbar(DrawContext instance, int x1, int y1, int x2, int y2, int color) {
        int height = y2 - y1;
        if(scrollAmount < 0) {
            y2 -= ScrollMath.dampenSquish(Math.abs(scrollAmount), height);
        }
        if(y1 + height > bottom) {
            y2 = bottom;
            y1 = bottom - height;
        }
        if (scrollAmount > getMaxScroll()) {
            int squish = ScrollMath.dampenSquish(scrollAmount - getMaxScroll(), height);
            y1 += squish;
        }
        instance.fill(x1, y1, x2, y2, color);
        instance.fill(x1, y1, x2 - 1, y2 - 1, -4144960);
    }

    @Redirect(method = "render", at=@At(value="INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V", ordinal = 2))
    private void cancelScrollbarForeground(DrawContext instance, int x1, int y1, int x2, int y2, int color) {}
}
package me.wolfii.mixin;

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
    private double velocity = 0;
    @Unique
    private double animationTimer = 0;
    @SuppressWarnings("FieldCanBeLocal")
    @Unique
    private final double maxVelocity = 200;


    @Inject(method = "render", at = @At("HEAD"))
    private void manipulateScrollAmount(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        checkOutOfBounds(delta);

        if (velocity == 0) return;
        applyMotion(delta);
    }

    @Unique
    private void applyMotion(float delta) {
        velocity *= Math.max(0, 1 - 3 * animationTimer);
        scrollAmount += velocity * 0.1d;
        animationTimer += delta * 0.005d;
        if (Math.abs(velocity) < 0.2) velocity = 0;
    }

    @Unique
    private void checkOutOfBounds(float delta) {
        if (scrollAmount < 0) {
            scrollAmount += pushBackFactor(Math.abs(scrollAmount), delta);
            if (scrollAmount > -0.2) scrollAmount = 0;
        }
        if (scrollAmount > getMaxScroll()) {
            scrollAmount -= pushBackFactor(scrollAmount - getMaxScroll(), delta);
            if (scrollAmount < getMaxScroll() + 0.2) scrollAmount = getMaxScroll();
        }
    }

    @Unique
    private double pushBackFactor(double distance, float delta) {
        return ((distance + 4d) * delta / 0.3d) / 4.0d;
    }

    @Redirect(method = "mouseScrolled", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/EntryListWidget;setScrollAmount(D)V"))
    private void setTarget(EntryListWidget<?> instance, double amount) {
        double diff = amount - scrollAmount;
        if(Math.signum(diff) != Math.signum(velocity)) diff *= 2.5d;
        velocity += diff;
        velocity = Math.max(-maxVelocity, Math.min(maxVelocity, velocity));
        animationTimer = 0;
    }

    @Redirect(method = "render", at=@At(value="INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V"))
    private void modifyScrollbar(DrawContext instance, Identifier texture, int x, int y, int width, int height) {
        if(scrollAmount < 0) {
            height -= dampenSquish(Math.abs(scrollAmount), height);
        }
        if(y + height > bottom) {
            y = bottom - height;
        }
        if (scrollAmount > getMaxScroll()) {
            int squish = dampenSquish(scrollAmount - getMaxScroll(), height);
            y += squish;
            height -= squish;
        }
        instance.drawGuiTexture(texture, x, y, width, height);
    }

    @Unique
    private int dampenSquish(double squish, int height) {
        double proportion = Math.min(1, squish / 100);
        return (int) (Math.min(0.85, proportion) * height);
    }
}
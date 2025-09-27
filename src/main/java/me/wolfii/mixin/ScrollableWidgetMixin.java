package me.wolfii.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import me.wolfii.Config;
import me.wolfii.ScrollMath;
import me.wolfii.ScrollableWidgetManipulator;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ScrollableWidget;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScrollableWidget.class)
public abstract class ScrollableWidgetMixin implements ScrollableWidgetManipulator {
    @Shadow
    private double scrollY;

    @Shadow
    public abstract int getMaxScrollY();

    @Shadow
    public abstract void setScrollY(double scrollY);

    @Unique
    private double animationTimer = 0;
    @Unique
    private double scrollStartVelocity = 0;
    @Unique
    private boolean renderSmooth = false;

    @Unique
    public void smoothScrollingRefurbished$manipulateScrollAmount(float delta) {
        renderSmooth = true;
        checkOutOfBounds(delta);

        if (Math.abs(ScrollMath.scrollbarVelocity(animationTimer, scrollStartVelocity)) < 1.0) return;
        applyMotion(delta);
    }

    @Unique
    private void applyMotion(float delta) {
        setScrollY(scrollY + ScrollMath.scrollbarVelocity(animationTimer, scrollStartVelocity) * delta);
        animationTimer += delta * 10 / Config.animationDuration;
    }

    @Unique
    private void checkOutOfBounds(float delta) {
        if (scrollY < 0) {
            setScrollY(scrollY + ScrollMath.pushBackStrength(Math.abs(scrollY), delta));
            if (scrollY > -0.2) scrollY = 0;
        }
        if (scrollY > getMaxScrollY()) {
            setScrollY(scrollY - ScrollMath.pushBackStrength(scrollY - getMaxScrollY(), delta));
            if (scrollY < getMaxScrollY() + 0.2) scrollY = getMaxScrollY();
        }
    }

    @WrapOperation(
        method = "mouseScrolled",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/ScrollableWidget;setScrollY(D)V"
        )
    )
    private void setVelocity(ScrollableWidget instance, double scrollY, Operation<Void> original) {
        if (!renderSmooth) {
            instance.setScrollY(scrollY);
            return;
        }
        double diff = scrollY - this.scrollY;
        diff = Math.signum(diff) * Math.min(Math.abs(diff), 10);
        diff *= Config.scrollSpeed;
        if (Math.signum(diff) != Math.signum(scrollStartVelocity)) diff *= 2.5d;
        animationTimer *= 0.5;
        scrollStartVelocity = ScrollMath.scrollbarVelocity(animationTimer, scrollStartVelocity) + diff;
        animationTimer = 0;
    }

    @WrapOperation(
        method = "drawScrollbar",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V",
            ordinal = 1
        )
    )
    private void modifyScrollbar(DrawContext instance, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, Operation<Void> original) {
        if (!renderSmooth) {
            instance.drawGuiTexture(pipeline, sprite, x, y, width, height);
            return;
        }
        if (scrollY < 0) {
            height -= ScrollMath.dampenSquish(Math.abs(scrollY), height);
        }
        int bottom = ((ScrollableWidget) (Object) this).getBottom();
        if (y + height > bottom) {
            y = bottom - height;
        }
        if (scrollY > getMaxScrollY()) {
            int squish = ScrollMath.dampenSquish(scrollY - getMaxScrollY(), height);
            y += squish;
            height -= squish;
        }
        instance.drawGuiTexture(pipeline, sprite, x, y, width, height);
    }

    @WrapOperation(
        method = "mouseDragged",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/ScrollableWidget;setScrollY(D)V",
            ordinal = 2
        )
    )
    private void clampDraggedScrollY(ScrollableWidget instance, double scrollY, Operation<Void> original) {
        original.call(instance, MathHelper.clamp(scrollY, 0.0, this.getMaxScrollY()));
    }

    @Inject(
        method = "setScrollY",
        at = @At("TAIL")
    )
    private void setScrollYUnclamped(double scrollY, CallbackInfo ci) {
        this.scrollY = scrollY;
    }
}
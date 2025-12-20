package me.wolfii.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import me.wolfii.Config;
import me.wolfii.ScrollMath;
import me.wolfii.ScrollableWidgetManipulator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractScrollArea.class)
public abstract class ScrollableWidgetMixin implements ScrollableWidgetManipulator {
    @Shadow
    private double scrollAmount;

    @Shadow
    public abstract int maxScrollAmount();

    @Shadow
    public abstract void setScrollAmount(double scrollY);

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
        setScrollAmount(scrollAmount + ScrollMath.scrollbarVelocity(animationTimer, scrollStartVelocity) * delta);
        animationTimer += delta * 10 / Config.animationDuration;
    }

    @Unique
    private void checkOutOfBounds(float delta) {
        if (scrollAmount < 0) {
            setScrollAmount(scrollAmount + ScrollMath.pushBackStrength(Math.abs(scrollAmount), delta));
            if (scrollAmount > -0.2) scrollAmount = 0;
        }
        if (scrollAmount > maxScrollAmount()) {
            setScrollAmount(scrollAmount - ScrollMath.pushBackStrength(scrollAmount - maxScrollAmount(), delta));
            if (scrollAmount < maxScrollAmount() + 0.2) scrollAmount = maxScrollAmount();
        }
    }

    @WrapOperation(
        method = "mouseScrolled",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/AbstractScrollArea;setScrollAmount(D)V"
        )
    )
    private void setVelocity(AbstractScrollArea instance, double scrollY, Operation<Void> original) {
        if (!renderSmooth) {
			original.call(instance, scrollY);
            return;
        }
        double diff = scrollY - this.scrollAmount;
        diff = Math.signum(diff) * Math.min(Math.abs(diff), 10);
        diff *= Config.scrollSpeed;
        if (Math.signum(diff) != Math.signum(scrollStartVelocity)) diff *= 2.5d;
        animationTimer *= 0.5;
        scrollStartVelocity = ScrollMath.scrollbarVelocity(animationTimer, scrollStartVelocity) + diff;
        animationTimer = 0;
    }

    @WrapOperation(
        method = "renderScrollbar",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIII)V",
            ordinal = 1
        )
    )
    private void modifyScrollbar(GuiGraphics instance, RenderPipeline pipeline, ResourceLocation sprite, int x, int y, int width, int height, Operation<Void> original) {
        if (!renderSmooth) {
            instance.blitSprite(pipeline, sprite, x, y, width, height);
            return;
        }
        if (scrollAmount < 0) {
            height -= ScrollMath.dampenSquish(Math.abs(scrollAmount), height);
        }
        int bottom = ((AbstractScrollArea) (Object) this).getBottom();
        if (y + height > bottom) {
            y = bottom - height;
        }
        if (scrollAmount > maxScrollAmount()) {
            int squish = ScrollMath.dampenSquish(scrollAmount - maxScrollAmount(), height);
            y += squish;
            height -= squish;
        }
		original.call(instance, pipeline, sprite, x, y, width, height);
    }

    @WrapOperation(
        method = "mouseDragged",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/AbstractScrollArea;setScrollAmount(D)V",
            ordinal = 2
        )
    )
    private void clampDraggedScrollY(AbstractScrollArea instance, double scrollY, Operation<Void> original) {
        original.call(instance, Mth.clamp(scrollY, 0.0, this.maxScrollAmount()));
    }

    @Inject(
        method = "setScrollAmount",
        at = @At("TAIL")
    )
    private void setScrollYUnclamped(double scrollY, CallbackInfo ci) {
        this.scrollAmount = scrollY;
    }
}
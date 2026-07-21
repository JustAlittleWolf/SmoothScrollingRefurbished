package me.wolfii.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import me.wolfii.Config;
import me.wolfii.ScrollMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractScrollArea.class)
public abstract class AbstractScrollAreaMixin {
    @Shadow
    private double scrollAmount;

    @Shadow
    public abstract int maxScrollAmount();

    @Shadow
    public abstract void setScrollAmount(double scrollAmount);

    @Shadow
    protected abstract boolean scrollable();

    @Unique
    private double animationTimer = 0;
    @Unique
    private double scrollStartVelocity = 0;
    @Unique
    private boolean renderSmooth = false;

    @Unique
    private void applyMotion(float delta) {
        this.setScrollAmount(this.scrollAmount + ScrollMath.scrollbarVelocity(this.animationTimer, this.scrollStartVelocity) * delta);
        this.animationTimer += delta * 10 / Config.animationDuration;
    }

    @Unique
    private void checkOutOfBounds(float delta) {
        if (this.scrollAmount < 0) {
            this.setScrollAmount(this.scrollAmount + ScrollMath.pushBackStrength(Math.abs(this.scrollAmount), delta));
            if (this.scrollAmount > -0.2) this.scrollAmount = 0;
        }
        if (this.scrollAmount > this.maxScrollAmount()) {
            this.setScrollAmount(this.scrollAmount - ScrollMath.pushBackStrength(this.scrollAmount - this.maxScrollAmount(), delta));
            if (this.scrollAmount < this.maxScrollAmount() + 0.2) this.scrollAmount = this.maxScrollAmount();
        }
    }

    @WrapMethod(
        method = "extractScrollbar"
    )
    private void manipulateScrollAmount(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Operation<Void> original) {
        original.call(graphics, mouseX, mouseY);

        this.renderSmooth = this.scrollable();
        if (!this.renderSmooth) {
            return;
        }
        float delta = Minecraft.getInstance().getDeltaTracker().getRealtimeDeltaTicks();
        this.checkOutOfBounds(delta);

        if (Math.abs(ScrollMath.scrollbarVelocity(this.animationTimer, this.scrollStartVelocity)) < 1.0) return;
        this.applyMotion(delta);
    }

    @WrapOperation(
        method = "mouseScrolled",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/AbstractScrollArea;setScrollAmount(D)V"
        )
    )
    private void setVelocity(AbstractScrollArea instance, double scrollY, Operation<Void> original) {
        if (!this.renderSmooth) {
            original.call(instance, scrollY);
            return;
        }
        double diff = scrollY - this.scrollAmount;
        diff = Math.signum(diff) * Math.min(Math.abs(diff), 10);
        diff *= Config.scrollSpeed;
        if (Math.signum(diff) != Math.signum(this.scrollStartVelocity)) diff *= 2.5d;
        this.animationTimer *= 0.5;
        this.scrollStartVelocity = ScrollMath.scrollbarVelocity(this.animationTimer, this.scrollStartVelocity) + diff;
        this.animationTimer = 0;
    }

    @WrapOperation(
        method = "extractScrollbar",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
            ordinal = 3
        )
    )
    private void modifyScrollbar(GuiGraphicsExtractor instance, RenderPipeline renderPipeline, Identifier location, int x, int y, int width, int height, Operation<Void> original) {
        if (!this.renderSmooth) {
            original.call(instance, renderPipeline, location, x, y, width, height);
            return;
        }
        if (this.scrollAmount < 0) {
            height -= ScrollMath.dampenSquish(Math.abs(this.scrollAmount), height);
        }
        int bottom = ((AbstractScrollArea) (Object) this).getBottom();
        if (y + height > bottom) {
            y = bottom - height;
        }
        if (this.scrollAmount > this.maxScrollAmount()) {
            int squish = ScrollMath.dampenSquish(this.scrollAmount - this.maxScrollAmount(), height);
            y += squish;
            height -= squish;
        }
        original.call(instance, renderPipeline, location, x, y, width, height);
    }

    @WrapOperation(
        method = "mouseDragged",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/AbstractScrollArea;setScrollAmount(D)V",
            ordinal = 2
        )
    )
    private void clampDraggedScrollY(AbstractScrollArea instance, double scrollAmount, Operation<Void> original) {
        if (!this.renderSmooth) {
            original.call(instance, scrollAmount);
            return;
        }
        original.call(instance, Mth.clamp(scrollAmount, 0.0, this.maxScrollAmount()));
    }

    @WrapMethod(method = "setScrollAmount")
    private void setScrollYUnclamped(double scrollAmount, Operation<Void> original) {
        if (!this.renderSmooth || scrollAmount > this.maxScrollAmount() + 1e5 || scrollAmount < -1e5) {
            original.call(scrollAmount);
            return;
        }
        this.scrollAmount = scrollAmount;
    }
}
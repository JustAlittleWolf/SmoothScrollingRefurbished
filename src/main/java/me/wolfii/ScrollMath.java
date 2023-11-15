package me.wolfii;

public class ScrollMath {
    public static double scrollbarVelocity(double timer, double factor) {
        return Math.pow(1 - Config.scrollbarDrag, timer) * factor;
    }

    public static int dampenSquish(double squish, int height) {
        double proportion = Math.min(1, squish / 100);
        return (int) (Math.min(0.85, proportion) * height);
    }

    public static double pushBackStrength(double distance, float delta) {
        return ((distance + 4d) * delta / 0.3d) / (3.2d/ Config.pushBackStrength);
    }
}

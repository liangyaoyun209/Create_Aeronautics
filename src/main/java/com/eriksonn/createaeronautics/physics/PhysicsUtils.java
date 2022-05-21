package com.eriksonn.createaeronautics.physics;

import com.eriksonn.createaeronautics.index.CAConfig;
import com.eriksonn.createaeronautics.index.CATags;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.gen.feature.template.Template;

public class PhysicsUtils {

    public static double[][][] LeviCivitaTensor;
    public final static double deltaTime = 0.05f;//converts the time unit to seconds instead of ticks
    public final static double gravity = 4.00;// m/s^2

    public static void generateLeviCivitaTensor() {
        LeviCivitaTensor = new double[3][3][3];
        LeviCivitaTensor[0][1][2] = LeviCivitaTensor[2][0][1] = LeviCivitaTensor[1][2][0] = 1;
        LeviCivitaTensor[2][1][0] = LeviCivitaTensor[0][2][1] = LeviCivitaTensor[1][0][2] = -1;
    }

    public static double getBlockMass(Template.BlockInfo info) {
        if (info.state.is(CATags.LIGHT)) {
            return CAConfig.LIGHT_BLOCK_WEIGHT.get();
        }
        return CAConfig.DEFAULT_BLOCK_WEIGHT.get();
    }

    private static final double worldHeight = 256.0;// pressure at world height = 0
    private static final double referenceHeight = 64.0;// pressure at sea level = 1

    private static final double worldHeightSmoothness = 20;// exponential dropoff at worldheight
    private static final double linearScaleHeight = 300;// inverse slope of linear region

    /**
     * Gets the air pressure as a function of altitude
     * The air pressure curve is linear at low altitues and drops off smoothly to zero at worldheight
     * It is also normalized to be 1.0 at sea level
     *
     * @param pos Position in world-space
     * @return the airpressure scaling
     */
    public static double getAirPressure(Vector3d pos) {
        double height = pos.y;
        if (height > worldHeight) return 0;

        double E = Math.exp((height - worldHeight) / worldHeightSmoothness);

        return (1 - (height - referenceHeight) / linearScaleHeight) * (1 - E);

    }

    public static double getAirPressureDerivative(Vector3d pos) {
        double height = pos.y;
        if (height > worldHeight) return 0;

        double E = Math.exp((height - worldHeight) / worldHeightSmoothness);

        return -(1 - E) / linearScaleHeight
                - (1 - (height - referenceHeight) / linearScaleHeight) * E / worldHeightSmoothness;

    }
}

package com.eriksonn.createaeronautics.index;

import com.eriksonn.createaeronautics.CreateAeronautics;
import com.jozufozu.flywheel.core.PartialModel;
import net.minecraft.util.ResourceLocation;

public class CABlockPartials {

    public static final PartialModel
    CANNON_BARREL = get("stationary_potato_cannon/barrel"),
    CANNON_BELLOW = get("stationary_potato_cannon/bellow"),
    ENGINE_PISTON = get("stirling_engine/piston"),
    GYRO_BEARING_PISTON_HEAD = get("gyroscopic_propeller_bearing/piston_head"),
    GYRO_BEARING_PISTON_POLE = get("gyroscopic_propeller_bearing/piston_pole"),
    MINI_COMPASS = get("compass_table/mini_compass"),
    MINI_COMPASS_ARROW = get("compass_table/mini_compass_arrow"),
    COMPASS_CONTACT_OFF = get("compass_table/contact_off"),
    COMPASS_CONTACT_ON = get("compass_table/contact_on");

    private static PartialModel get(String path) {
        ResourceLocation L = new ResourceLocation(CreateAeronautics.MODID, "block/" + path);
        return new PartialModel(L);
    }
    public static void clientInit() {
        // init static fields
    }
}

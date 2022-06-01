package com.eriksonn.createaeronautics.contraptions;

import com.eriksonn.createaeronautics.world.FakeAirshipClientWorld;
import com.simibubi.create.content.contraptions.components.structureMovement.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.render.ContraptionRenderDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;

public class ClientAirshipEntityManager {
    public boolean invalid = false;

    Profiler profiler;
    public AirshipContraptionEntity airship;
    public FakeAirshipClientWorld fakeClientWorld;

    public ClientAirshipEntityManager(AirshipContraptionEntity airshipContraption) {
        this.airship = airshipContraption;
    }

    public void initFakeClientWorld() {
        AirshipContraption airshipContraption = (AirshipContraption) airship.getContraption();
        profiler = new Profiler(() -> 0, () -> 0, false);
        RegistryKey<World> dimension = airship.level.dimension();
        DimensionType dimensionType = airship.level.dimensionType();
        ClientWorld.ClientWorldInfo clientWorldInfo = new ClientWorld.ClientWorldInfo(Difficulty.PEACEFUL, false, true);
        fakeClientWorld = new FakeAirshipClientWorld(
                airship,
                Minecraft.getInstance().getConnection(),
                clientWorldInfo,
                dimension,
                dimensionType,
                0, () -> profiler,
                null, false, 0
        );
//        AirshipManager.INSTANCE.AllClientAirships.put(airship.plotId, airship);

        airshipContraption.maybeInstancedTileEntities.clear();
        airshipContraption.specialRenderedTileEntities.clear();
        airshipContraption.presentTileEntities.clear();
    }

    public void tick() {
        AirshipContraption airshipContraption = (AirshipContraption) airship.getContraption();
        profiler.startTick();
        fakeClientWorld.tick(() -> true);

        for (ControlledContraptionEntity contraptionEntity : airship.subContraptions.values()) {
            contraptionEntity.tick();
        }

        fakeClientWorld.tickBlockEntities();
        profiler.endTick();

        if (invalid) {
            ContraptionRenderDispatcher.invalidate(airshipContraption);
            invalid = false;
        }
    }

    public static AirshipContraptionEntity airshipFromWorld(World world) {
        if(world instanceof FakeAirshipClientWorld) {
            FakeAirshipClientWorld fakeWorld = (FakeAirshipClientWorld) world;
            AirshipContraptionEntity airship = fakeWorld.airship;
            return airship;
        }
        return null;
    }

    public World getWorld() {
        return fakeClientWorld;
    }
}

package com.eriksonn.createaeronautics.blocks;

import com.eriksonn.createaeronautics.blocks.propeller_bearing.MecanicalBearingTileEntityExtension;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.MechanicalBearingTileEntity;
import net.minecraft.tileentity.TileEntityType;

public class AbstractPropellerBearingTileEntity extends MechanicalBearingTileEntity implements MecanicalBearingTileEntityExtension {
    public AbstractPropellerBearingTileEntity(TileEntityType<? extends MechanicalBearingTileEntity> type) {
        super(type);
    }

    @Override
    public boolean isPropeller() {
        return false;
    }
}

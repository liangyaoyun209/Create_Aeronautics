package com.eriksonn.createaeronautics.blocks.analog_clutch;

import com.eriksonn.createaeronautics.index.CAShapes;
import com.eriksonn.createaeronautics.index.CATileEntities;
import com.simibubi.create.content.contraptions.relays.encased.ClutchBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class AnalogClutchBlock extends ClutchBlock {
    public AnalogClutchBlock(Properties properties) {
        super(properties);
    }

    int previousPower = 0;

    @Override
    public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
                                boolean isMoving) {
        if (worldIn.isClientSide)
            return;

        int power = worldIn.getBestNeighborSignal(pos);
        withTileEntityDo(worldIn, pos, (tile) -> {
            ((AnalogClutchTileEntity) tile).neighborChanged();
        });

        if (previousPower != power) {
//            detachKinetics(worldIn, pos, true);
        }
        if (power > 0 != previousPower > 0) {
            worldIn.setBlock(pos, state.cycle(POWERED), 2);
        }
        previousPower = power;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return CATileEntities.ANALOG_CLUTCH.create();
    }

    @Override
    public VoxelShape getShape(BlockState pState, IBlockReader pLevel, BlockPos pPos, ISelectionContext pContext) {
        return CAShapes.ANALOG_CLUTCH.get(pState.getValue(AXIS));
    }
}

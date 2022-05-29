package com.eriksonn.createaeronautics.blocks.optical_sensor;

import com.eriksonn.createaeronautics.index.CAShapes;
import com.eriksonn.createaeronautics.index.CATileEntities;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.filtering.FilteringBehaviour;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DirectionalBlock;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class OpticalSensorBlock extends WrenchableDirectionalBlock {

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public OpticalSensorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
        super.createBlockStateDefinition(builder);
    }


    @Override
    public VoxelShape getShape(BlockState state, IBlockReader pLevel, BlockPos pPos,
                               ISelectionContext pContext) {
        return CAShapes.OPTICAL_SENSOR.get(state.getValue(DirectionalBlock.FACING));
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state.setValue(FACING, state.getValue(FACING).getOpposite()).setValue(POWERED, false);
    }


    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return CATileEntities.OPTICAL_SENSOR.create();
    }



    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, IBlockReader world, BlockPos pos, Direction side) {
        return side != state.getValue(FACING)
                .getOpposite();
    }

    @Override
    public void onRemove(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.hasTileEntity() && state.getBlock() != newState.getBlock()) {
            TileEntityBehaviour.destroy(worldIn, pos, FilteringBehaviour.TYPE);
            worldIn.removeBlockEntity(pos);
        }
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return state.getValue(POWERED);
    }

    @Override
    public int getSignal(BlockState blockState, IBlockReader blockAccess, BlockPos pos, Direction side) {
        OpticalSensorTileEntity te = (OpticalSensorTileEntity) blockAccess.getBlockEntity(pos);

        return isSignalSource(blockState) && (side == null || side != blockState.getValue(FACING).getOpposite()) ? Math.round((te.beamLength - (te.detectionLength - 1)) * (15f / te.beamLength)) : 0;
    }

}

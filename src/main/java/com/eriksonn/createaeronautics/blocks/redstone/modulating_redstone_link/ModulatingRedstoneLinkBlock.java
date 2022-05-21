package com.eriksonn.createaeronautics.blocks.redstone.modulating_redstone_link;

import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;

public class ModulatingRedstoneLinkBlock extends WrenchableDirectionalBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public ModulatingRedstoneLinkBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POWERED, false));
    }
    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
        super.createBlockStateDefinition(builder);
    }
    @Override
    public boolean canSurvive(BlockState state, IWorldReader worldIn, BlockPos pos) {
        BlockPos neighbourPos = pos.relative(state.getValue(FACING)
                .getOpposite());
        BlockState neighbour = worldIn.getBlockState(neighbourPos);
        return !neighbour.getMaterial()
                .isReplaceable();
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        BlockState state = defaultBlockState();
        state = state.setValue(FACING, context.getClickedFace());
        return state;
    }
}

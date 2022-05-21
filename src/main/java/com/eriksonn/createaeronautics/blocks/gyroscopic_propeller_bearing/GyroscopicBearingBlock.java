package com.eriksonn.createaeronautics.blocks.gyroscopic_propeller_bearing;

import com.eriksonn.createaeronautics.blocks.propeller_bearing.PropellerBearingBlock;
import com.eriksonn.createaeronautics.blocks.propeller_bearing.PropellerBearingTileEntity;
import com.eriksonn.createaeronautics.blocks.stationary_potato_cannon.StationaryPotatoCannonTileEntity;
import com.eriksonn.createaeronautics.index.CAShapes;
import com.eriksonn.createaeronautics.index.CATileEntities;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingBlock;
import com.simibubi.create.foundation.block.ITE;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class GyroscopicBearingBlock extends BearingBlock implements ITE<GyroscopicPropellerBearingTileEntity>  {
    public GyroscopicBearingBlock(Properties properties) {
        super(properties);
    }
    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
    }
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return CATileEntities.GYROSCOPIC_PROPELLER_BEARING.create();
    }
    @Override
    public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
                                boolean isMoving) {
        if (worldIn.isClientSide)
            return;
        this.withTileEntityDo(worldIn, pos, GyroscopicPropellerBearingTileEntity::updateSignal);

    }
    @Override
    public ActionResultType use(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn,
                                BlockRayTraceResult hit) {
        if (!player.mayBuild())
            return ActionResultType.FAIL;
        if (player.isShiftKeyDown())
            return ActionResultType.FAIL;
        if (player.getItemInHand(handIn)
                .isEmpty()) {
            if (worldIn.isClientSide) {
                withTileEntityDo(worldIn, pos, te -> {if (te.isRunning()) te.startDisassemblySlowdown();});
                return ActionResultType.SUCCESS;
            }
            withTileEntityDo(worldIn, pos, te -> {
                if (te.isRunning()) {
                    //te.disassemble();
                    te.startDisassemblySlowdown();
                    return;
                }
                te.setAssembleNextTick(true);
                //te.assembleNextTick
            });
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.PASS;
    }
    @Override
    public VoxelShape getShape(BlockState state, IBlockReader reader, BlockPos pos, ISelectionContext context)
    {
        return CAShapes.PROPELLER_BEARING.get(state.getValue(FACING));

    }
    @Override
    public Class<GyroscopicPropellerBearingTileEntity> getTileEntityClass() { return GyroscopicPropellerBearingTileEntity.class; }
}

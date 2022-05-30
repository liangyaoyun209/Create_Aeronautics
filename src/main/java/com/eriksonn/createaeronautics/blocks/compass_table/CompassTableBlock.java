package com.eriksonn.createaeronautics.blocks.compass_table;

import java.util.Random;
import java.util.Vector;

import com.eriksonn.createaeronautics.index.CAShapes;
import com.eriksonn.createaeronautics.index.CATileEntities;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllShapes;
import com.simibubi.create.AllTileEntities;
import com.simibubi.create.content.contraptions.components.crafter.MechanicalCrafterTileEntity;
import com.simibubi.create.content.contraptions.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.contraptions.wrench.IWrenchable;
import com.simibubi.create.content.logistics.block.funnel.FunnelTileEntity;
import com.simibubi.create.foundation.block.ITE;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.belt.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.utility.Iterate;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.CompassItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

public class CompassTableBlock extends HorizontalBlock implements ITE<CompassTableTileEntity>, IWrenchable {


	public CompassTableBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState());
	}

	@Override
	public VoxelShape getShape(BlockState state, IBlockReader pLevel, BlockPos pPos,
		ISelectionContext pContext) {
		return CAShapes.COMPASS_TABLE.get(state.getValue(FACING));
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return CATileEntities.COMPASS_TABLE.create();
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(FACING);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context) {
		return super.getStateForPlacement(context).setValue(FACING, context.getHorizontalDirection());
	}

	@Override
	public boolean isSignalSource(BlockState state) {

		// TODO: Amogus
		return true;
	}

	@Override
	public int getSignal(BlockState blockState, IBlockReader blockAccess, BlockPos pos, Direction side) {
		if(side == Direction.UP || side == Direction.DOWN || !getTileEntity(blockAccess, pos).hasCompass()) {
			return 0;
		}

		float sideRotation = (float) -Math.toRadians(side.toYRot() - 90);
		float compassRotation = getTileEntity(blockAccess, pos).getCompassRotation();

		Vector3d sideVec = new Vector3d(Math.cos(sideRotation), 0, Math.sin(sideRotation)).normalize();
		Vector3d compassVec = new Vector3d(Math.cos(compassRotation), 0, Math.sin(compassRotation)).normalize();
		compassVec = compassVec.scale(1.0 / (Math.abs(Math.cos(compassRotation)) + Math.abs(Math.sin(compassRotation))));
		double dot = sideVec.dot(compassVec);

		return (int) (dot * 15);
	}

	@Override
	public void tick(BlockState state, ServerWorld worldIn, BlockPos pos, Random random) {
		super.tick(state, worldIn, pos, random);
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
	public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
		boolean isMoving) {
		InvManipulationBehaviour behaviour = TileEntityBehaviour.get(worldIn, pos, InvManipulationBehaviour.TYPE);
		if (behaviour != null)
			behaviour.onNeighborChanged(fromPos);
	}

	@Override
	public Class<CompassTableTileEntity> getTileEntityClass() {
		return CompassTableTileEntity.class;
	}


	@Override
	public ActionResultType use(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn,
								BlockRayTraceResult hit) {
		ItemStack heldItem = player.getItemInHand(handIn);
		boolean isHand = heldItem.isEmpty() && handIn == Hand.MAIN_HAND;

		CompassTableTileEntity te = (CompassTableTileEntity) worldIn.getBlockEntity(pos);

		if(!te.hasCompass()) {
			if(heldItem.getItem() == Items.COMPASS && CompassItem.isLodestoneCompass(heldItem)) {
				if(te.getLevel().isClientSide) return ActionResultType.SUCCESS;
				te.inventory.insertItem(0, heldItem.split(1), false);
			}
		} else {
			if(heldItem.isEmpty()) {
					ItemStack stack = te.inventory.extractItem(0, 1, false);
				player.setItemInHand(handIn, stack);
			}
		}

		return ActionResultType.PASS;
	}


}

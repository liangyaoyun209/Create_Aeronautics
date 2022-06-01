package com.eriksonn.createaeronautics.blocks.hot_air;

import com.eriksonn.createaeronautics.blocks.IEnvelope;
import com.eriksonn.createaeronautics.index.CABlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.base.CasingBlock;
import com.simibubi.create.content.contraptions.base.KineticBlock;
import com.simibubi.create.content.contraptions.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.SailBlock;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.placement.IPlacementHelper;
import com.simibubi.create.foundation.utility.placement.PlacementHelpers;
import com.simibubi.create.foundation.utility.placement.PlacementOffset;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShearsItem;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class EnvelopeBlock extends CasingBlock implements IEnvelope {

    protected final DyeColor color;

    public static EnvelopeBlock withCanvas(Properties properties, DyeColor color) {
        return new EnvelopeBlock(properties, color);
    }
    public EnvelopeBlock(Properties properties, DyeColor color) {
        super(properties);
        this.color = color;
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult ray) {
        ItemStack heldItem = player.getItemInHand(hand);

        DyeColor color = DyeColor.getColor(heldItem);
        if (color != null) {
            if (!world.isClientSide)
                EnvelopeBlock.applyDye(state, world, pos, color);
            return ActionResultType.SUCCESS;
        }

        return ActionResultType.PASS;
    }

    protected static void applyDye(BlockState state, World world, BlockPos pos, DyeColor color) {

        BlockState newEnvelopeState = BlockHelper.copyProperties(state, CABlocks.DYED_ENVELOPE_BLOCKS.get(color).getDefaultState());
        BlockState newEncasedEnvelopeState = BlockHelper.copyProperties(state, CABlocks.DYED_ENVELOPE_ENCASED_SHAFT.get(color).getDefaultState());
        // Dye the block itself
        if ((selfDye(world,pos,state,newEnvelopeState) || selfDye(world,pos,state,newEncasedEnvelopeState))) {
            return;
        }

        // Dye all adjacent
        boolean hasDyed=false;
        for (Direction d : Iterate.directions) {
            BlockPos offset = pos.relative(d);
            BlockState adjacentState = world.getBlockState(offset);
            Block block = adjacentState.getBlock();
            if (!(selfDye(world,offset,adjacentState,newEnvelopeState) || selfDye(world,offset,adjacentState,newEncasedEnvelopeState))) {
                continue;
            }
            hasDyed=true;
        }
        if(hasDyed) return;

        // Dye all the things
        List<BlockPos> frontier = new ArrayList<>();
        frontier.add(pos);
        Set<BlockPos> visited = new HashSet<>();
        int timeout = 100;
        while (!frontier.isEmpty()) {
            if (timeout-- < 0)
                break;

            BlockPos currentPos = frontier.remove(0);
            visited.add(currentPos);

            for (Direction d : Iterate.directions) {
                BlockPos offset = currentPos.relative(d);
                if (visited.contains(offset))
                    continue;
                BlockState adjacentState = world.getBlockState(offset);
                Block block = adjacentState.getBlock();
                if (!(multiDye(world,offset,adjacentState,newEnvelopeState) || multiDye(world,offset,adjacentState,newEncasedEnvelopeState))) {
                    continue;
                }
                frontier.add(offset);
                visited.add(offset);
            }
        }

    }
    static boolean selfDye(World world,BlockPos pos,BlockState state,BlockState newState)
    {
        if (state.getBlock() instanceof EnvelopeBlock && newState.getBlock() instanceof EnvelopeBlock && state != newState) {
            world.setBlockAndUpdate(pos, newState);
            return true;
        }

        if (state.getBlock() instanceof EnvelopeEncasedShaftBlock && newState.getBlock() instanceof EnvelopeEncasedShaftBlock && state != newState) {
            world.setBlockAndUpdate(pos, newState);
            return true;
        }

        return false;
    }
    static boolean multiDye(World world,BlockPos pos,BlockState state,BlockState newState)
    {
        if (state.getBlock() instanceof EnvelopeBlock && newState.getBlock() instanceof EnvelopeBlock) {
            if(state != newState) {
                world.setBlockAndUpdate(pos, newState);
            }
            return true;
        }

        if (state.getBlock() instanceof EnvelopeEncasedShaftBlock && newState.getBlock() instanceof EnvelopeEncasedShaftBlock) {
            if(state != newState) {
                Direction.Axis axis =  state.getValue(RotatedPillarKineticBlock.AXIS);
                world.setBlockAndUpdate(pos, newState.setValue(RotatedPillarKineticBlock.AXIS,axis));

            }
            return true;
        }

        return false;
    }

    public DyeColor getColor() {
        return color;
    }


}

package com.eriksonn.createaeronautics.blocks.hot_air;

import com.eriksonn.createaeronautics.blocks.IEnvelope;
import com.eriksonn.createaeronautics.index.CABlocks;
import com.eriksonn.createaeronautics.index.CATileEntities;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTileEntities;
import com.simibubi.create.content.contraptions.base.CasingBlock;
import com.simibubi.create.content.contraptions.relays.encased.AbstractEncasedShaftBlock;
import com.simibubi.create.content.contraptions.relays.encased.EncasedShaftBlock;
import com.simibubi.create.content.schematics.ISpecialBlockItemRequirement;
import com.simibubi.create.content.schematics.ItemRequirement;
import com.simibubi.create.repack.registrate.util.entry.BlockEntry;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class EnvelopeEncasedShaftBlock extends AbstractEncasedShaftBlock implements ISpecialBlockItemRequirement, IEnvelope {

    protected final DyeColor color;

    protected EnvelopeEncasedShaftBlock(Properties properties,DyeColor color) {
        super(properties);
        this.color=color;
    }

    public static EnvelopeEncasedShaftBlock withCanvas(Properties properties, DyeColor color) {
        return new EnvelopeEncasedShaftBlock(properties, color);
    }
    @Override
    public ItemRequirement getRequiredItems(BlockState state, TileEntity te) {
        return ItemRequirement.of(AllBlocks.SHAFT.getDefaultState(), te);
    }
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return CATileEntities.ENCASED_ENVELOPE_SHAFT.create();
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
    public DyeColor getColor() {
        return color;
    }
}

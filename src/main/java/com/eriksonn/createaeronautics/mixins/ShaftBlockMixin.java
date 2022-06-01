package com.eriksonn.createaeronautics.mixins;

import com.eriksonn.createaeronautics.index.CABlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.contraptions.relays.elementary.ShaftBlock;
import com.simibubi.create.content.contraptions.relays.encased.EncasedShaftBlock;
import com.simibubi.create.foundation.advancement.AllTriggers;
import com.simibubi.create.foundation.utility.placement.IPlacementHelper;
import com.simibubi.create.foundation.utility.placement.PlacementHelpers;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ShaftBlock.class)
public class ShaftBlockMixin {

    @Shadow @Final private static int placementHelperId;

    /**
    * @author Eriksonn
    * */
    @Overwrite(remap=false)
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
                                BlockRayTraceResult ray) {
        if (player.isShiftKeyDown() || !player.mayBuild())
            return ActionResultType.PASS;

        ItemStack heldItem = player.getItemInHand(hand);
        for (EncasedShaftBlock encasedShaft : new EncasedShaftBlock[] { AllBlocks.ANDESITE_ENCASED_SHAFT.get(),
                AllBlocks.BRASS_ENCASED_SHAFT.get()}) {

            if (!encasedShaft.getCasing()
                    .isIn(heldItem))
                continue;

            if (world.isClientSide)
                return ActionResultType.SUCCESS;

            AllTriggers.triggerFor(AllTriggers.CASING_SHAFT, player);
            KineticTileEntity.switchToBlockState(world, pos, encasedShaft.defaultBlockState()
                    .setValue(RotatedPillarKineticBlock.AXIS, state.getValue(RotatedPillarKineticBlock.AXIS)));
            return ActionResultType.SUCCESS;
        }
        if(CABlocks.ENVELOPE_BLOCK.isIn(heldItem))
        {
            if (world.isClientSide)
                return ActionResultType.SUCCESS;
            AllTriggers.triggerFor(AllTriggers.CASING_SHAFT, player);
            KineticTileEntity.switchToBlockState(world, pos, CABlocks.ENVELOPE_ENCASED_SHAFT.get().defaultBlockState()
                    .setValue(RotatedPillarKineticBlock.AXIS, state.getValue(RotatedPillarKineticBlock.AXIS)));
            return ActionResultType.SUCCESS;
        }

        IPlacementHelper helper = PlacementHelpers.get(placementHelperId);
        if (helper.matchesItem(heldItem))
            return helper.getOffset(player, world, state, pos, ray).placeInWorld(world, (BlockItem) heldItem.getItem(), player, hand, ray);

        return ActionResultType.PASS;
    }
}

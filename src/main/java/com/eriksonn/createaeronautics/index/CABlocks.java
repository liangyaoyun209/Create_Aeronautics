package com.eriksonn.createaeronautics.index;

import com.eriksonn.createaeronautics.CreateAeronautics;
import com.eriksonn.createaeronautics.blocks.LevititeCasingBlock;
import com.eriksonn.createaeronautics.blocks.airship_assembler.AirshipAssemblerBlock;
import com.eriksonn.createaeronautics.blocks.analog_clutch.AnalogClutchBlock;
import com.eriksonn.createaeronautics.blocks.compass_table.CompassTableBlock;
import com.eriksonn.createaeronautics.blocks.gyroscopic_propeller_bearing.GyroscopicBearingBlock;
import com.eriksonn.createaeronautics.blocks.hot_air.EnvelopeBlock;
import com.eriksonn.createaeronautics.blocks.hot_air.EnvelopeEncasedShaftBlock;
import com.eriksonn.createaeronautics.blocks.optical_sensor.OpticalSensorBlock;
import com.eriksonn.createaeronautics.blocks.propeller_bearing.PropellerBearingBlock;
import com.eriksonn.createaeronautics.blocks.redstone.modulating_redstone_link.ModulatingRedstoneLinkBlock;
import com.eriksonn.createaeronautics.blocks.stationary_potato_cannon.StationaryPotatoCannonBlock;
import com.eriksonn.createaeronautics.blocks.stirling_engine.StirlingEngineBlock;
import com.eriksonn.createaeronautics.blocks.torsion_spring.TorsionSpringBlock;
import com.eriksonn.createaeronautics.groups.CAItemGroups;

import com.eriksonn.createaeronautics.utils.BlockStateUtils;
import com.eriksonn.createaeronautics.utils.ModelUtils;
import com.simibubi.create.AllTags;
import com.simibubi.create.Create;
import com.simibubi.create.content.AllSections;
import com.simibubi.create.content.contraptions.base.CasingBlock;
import com.simibubi.create.foundation.block.BlockStressDefaults;
import com.simibubi.create.foundation.data.*;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.SailBlock;
import com.simibubi.create.foundation.block.DyedBlockList;
import com.simibubi.create.foundation.data.BlockStateGen;
import com.simibubi.create.foundation.data.BuilderTransformers;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.repack.registrate.util.entry.BlockEntry;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraft.item.DyeColor;
import net.minecraft.util.IItemProvider;

import static com.simibubi.create.foundation.data.ModelGen.customItemModel;

public class CABlocks {
    private static final CreateRegistrate REGISTRATE = CreateAeronautics.registrate()
            .itemGroup(() -> CAItemGroups.MAIN_GROUP);

    public static final BlockEntry<TorsionSpringBlock> TORSION_SPRING = REGISTRATE.block("torsion_spring", TorsionSpringBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(AbstractBlock.Properties::noOcclusion)
            .blockstate((ctx,prov) ->
                    BlockStateUtils.torsionSpringBlockstate(ctx, prov,
                            ModelUtils.existingModel(ctx, prov, "torsion_spring/block"), false
                    )
            )
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag) //Dono what this tag means (contraption safe?).
            .item()
            .transform(customItemModel())
            .register();

    public static final BlockEntry<AirshipAssemblerBlock> AIRSHIP_ASSEMBLER = REGISTRATE.block("airship_assembler", AirshipAssemblerBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(AbstractBlock.Properties::noOcclusion)
            .blockstate((ctx,prov) ->
                    BlockStateGen.simpleBlock(ctx, prov,
                            ModelUtils.existingModelActive(ctx, prov, "airship_assembler/block")
                    )
            )
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag) //Dono what this tag means (contraption safe?).
            .item()
            .transform(customItemModel())
            .register();

    public static final BlockEntry<PropellerBearingBlock> PROPELLER_BEARING = REGISTRATE.block("propeller_bearing", PropellerBearingBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(AbstractBlock.Properties::noOcclusion)
            .blockstate((ctx, prov) -> BlockStateUtils.facingBlockstate(ctx, prov, "block/propeller_bearing/block"))
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag) //Dono what this tag means (contraption safe?).
            .item()
            .transform(customItemModel())
            .register();
    public static final BlockEntry<GyroscopicBearingBlock> GYROSCOPIC_PROPELLER_BEARING = REGISTRATE.block("gyroscopic_propeller_bearing",GyroscopicBearingBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(AbstractBlock.Properties::noOcclusion)
            .blockstate((ctx, prov) -> BlockStateUtils.facingBlockstate(ctx, prov, "block/gyroscopic_propeller_bearing/block"))
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag) //Dono what this tag means (contraption safe?).
            .item()
            .transform(customItemModel())
            .register();
    public static final BlockEntry<StationaryPotatoCannonBlock> STATIONARY_POTATO_CANNON = REGISTRATE.block("stationary_potato_cannon", StationaryPotatoCannonBlock::new)
            .initialProperties(SharedProperties::stone)
            .blockstate(BlockStateUtils::directionalPoweredAxisBlockstate)
            .properties(AbstractBlock.Properties::noOcclusion)
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag) //Dono what this tag means (contraption safe?).
            .item()
            .transform(customItemModel())
            .register();

    public static final BlockEntry<LevititeCasingBlock> LEVITITE_CASING = REGISTRATE.block("levitite_casing", LevititeCasingBlock::new)
            .transform(BuilderTransformers.casing(CASpriteShifts.LEVITITE_CASING)).properties((p) -> p.lightLevel(($) -> 12))
            .register();

    public static final BlockEntry<StirlingEngineBlock> STIRLING_ENGINE = REGISTRATE.block("stirling_engine", StirlingEngineBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(AbstractBlock.Properties::noOcclusion)
            .blockstate(BlockStateUtils::horizontalFacingLitBlockstate)
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag) //Dono what this tag means (contraption safe?).
            .item().transform(customItemModel())
            .register();
    public static final BlockEntry<ModulatingRedstoneLinkBlock> MODULATING_REDSTONE_LINK = REGISTRATE.block("modulating_redstone_link", ModulatingRedstoneLinkBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(AbstractBlock.Properties::noOcclusion)
            .blockstate(BlockStateUtils::facingPoweredAxisBlockstate)
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag) //Dono what this tag means (contraption safe?).
            .item().transform(customItemModel())
            .addLayer(() -> RenderType::translucent)
            .register();

    public static final BlockEntry<EnvelopeBlock> ENVELOPE_BLOCK= REGISTRATE.block("white_envelope_block", (p) -> {
        return EnvelopeBlock.withCanvas(p, DyeColor.WHITE);
    })
            .blockstate((c, p) -> p.simpleBlock(c.get(), p.models().cubeAll(c.getName(), p.modLoc("block/envelope_block/envelope_white"))))
            .initialProperties(SharedProperties::wooden)
            .properties(AbstractBlock.Properties::noOcclusion)
            .simpleItem()
            .tag(CATags.ENVELOPE_BLOCKS)
            .register();

    public static final DyedBlockList<EnvelopeBlock> DYED_ENVELOPE_BLOCKS = new DyedBlockList<>(colour -> {
        if (colour == DyeColor.WHITE) {
            return ENVELOPE_BLOCK;
        } else {
            String colourName = colour.getSerializedName();
            return REGISTRATE.block( colourName+"_envelope_block", (p) -> {
                return EnvelopeBlock.withCanvas(p, colour);
            }).blockstate((c, p) -> p.simpleBlock(c.get(), p.models()
                    .cubeAll(c.getName(), p.modLoc("block/envelope_block/envelope_"+colourName))))
                    .properties(AbstractBlock.Properties::noOcclusion)
                    .initialProperties(SharedProperties::wooden)
                    .loot((p, b) -> {
                p.dropOther(b, (IItemProvider) ENVELOPE_BLOCK.get());
            }).tag(CATags.ENVELOPE_BLOCKS)
                    .register();
        }
    });

    public static final BlockEntry<EnvelopeEncasedShaftBlock> ENVELOPE_ENCASED_SHAFT =
            REGISTRATE.block("white_envelope_encased_shaft", p -> EnvelopeEncasedShaftBlock.withCanvas(p, DyeColor.WHITE))
                    .properties(AbstractBlock.Properties::noOcclusion)
                    .initialProperties(SharedProperties::wooden)
                    .blockstate((c, p) -> BlockStateGen.axisBlock(c,p, blockState ->p.models()
                            .withExistingParent("white_envelope_encased_shaft", p.modLoc("block/envelope_encased_shaft/block"))
                            .texture("0", p.modLoc("block/envelope_block/envelope_white"))))
                    //.item().transform(customItemModel())
                    .tag(CATags.ENCASED_ENVELOPE_BLOCKS)
                    .register();

    public static final DyedBlockList<EnvelopeEncasedShaftBlock> DYED_ENVELOPE_ENCASED_SHAFT = new DyedBlockList<>(colour -> {
        if (colour == DyeColor.WHITE) {
            return ENVELOPE_ENCASED_SHAFT;
        }
        String colourName = colour.getSerializedName();
        return REGISTRATE.block(colourName + "_envelope_encased_shaft", p -> EnvelopeEncasedShaftBlock.withCanvas(p, colour))
                .properties(AbstractBlock.Properties::noOcclusion)
                .initialProperties(SharedProperties::wooden)

                .blockstate((c, p) -> BlockStateGen.axisBlock(c,p, blockState ->p.models()
                        .withExistingParent(colourName + "_envelope_encased_shaft", p.modLoc("block/envelope_encased_shaft/block"))
                        .texture("0", p.modLoc("block/envelope_block/envelope_"+colourName))))

                .loot((p, b) -> p.dropOther(b, ENVELOPE_ENCASED_SHAFT.get()))
                .tag(CATags.ENCASED_ENVELOPE_BLOCKS)
                .register();
    });


    public static final BlockEntry<AnalogClutchBlock> ANAlOG_CLUTCH = REGISTRATE.block("analog_clutch", AnalogClutchBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(AbstractBlock.Properties::noOcclusion)
            .transform(BlockStressDefaults.setNoImpact())
            .blockstate((c, p) -> BlockStateGen.axisBlock(c, p, AssetLookup.forPowered(c, p)))
            .item()
            .transform(customItemModel())
            .register();

    public static final BlockEntry<OpticalSensorBlock> OPTICAL_SENSOR = REGISTRATE.block("optical_sensor", OpticalSensorBlock::new)
            .initialProperties(SharedProperties::wooden)
            .properties(AbstractBlock.Properties::noOcclusion)
            .blockstate((ctx, prov) -> prov.getVariantBuilder(ctx.getEntry())
                    .forAllStates(state -> {
                        Direction dir = state.getValue(BlockStateProperties.FACING);
                        return ConfiguredModel.builder()
                                .modelFile(prov.models().getExistingFile(prov.modLoc("block/optical_sensor/block" + (state.getValue(OpticalSensorBlock.POWERED) ? "_powered" : ""))))
                                .rotationX(dir == Direction.DOWN ? 90 : dir.getAxis().isHorizontal() ? 0 : -90)
                                .rotationY(dir.getAxis().isVertical() ? 0 : (((int) dir.toYRot()) + 180) % 360)
                                .build();
                    })
            )
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag) //Dono what this tag means (contraption safe?).
//            .simpleItem()
            .item()
            .transform(customItemModel())
            .register();

    public static final BlockEntry<CompassTableBlock> COMPASS_TABLE = REGISTRATE.block("compass_table", CompassTableBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(AbstractBlock.Properties::noOcclusion)
            .blockstate((ctx, prov) -> prov.horizontalBlock(ctx.get(), blockState -> prov.models()
                    .getExistingFile(prov.modLoc("block/" + ctx.getName() + "/block"))))
            .tag(AllTags.AllBlockTags.SAFE_NBT.tag) //Dono what this tag means (contraption safe?).
            .item().transform(customItemModel())
            .register();



    public static void register() {
        //CreateAeronautics.registrate().addToSection(TORSION_SPRING, AllSections.KINETICS);
    }

}

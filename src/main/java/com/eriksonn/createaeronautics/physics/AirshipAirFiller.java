package com.eriksonn.createaeronautics.physics;
import com.eriksonn.createaeronautics.contraptions.AirshipContraption;
import com.simibubi.create.content.contraptions.processing.burner.BlazeBurnerTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.template.Template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class AirshipAirFiller {
    public void FillAir(AirshipContraption contraption) {
        class SearchHead {
            int layer;
            int group;
            BlockPos parent_pos;
            BlockPos pos;
        }

        class Layer {
            int num_active_heads = 1;
            boolean connected_to_void = false;
        }

        class Group {
            ArrayList<BlockPos> blocks = new ArrayList();
        }

        // Note(!) that here, some elements may be the same Group instance, which allows different group
        // ids to match the same group instance. The reason is that to start we may not know they are supposed
        // to be in the same group, and merging all the group ideas once we figure that out is a pain, it's easier
        // to just make them point to the same object.
        ArrayList<Group> groups = new ArrayList();
        ArrayList<Layer> layers = new ArrayList();
        HashSet<BlockPos> wool_positions = new HashSet();
        HashMap<BlockPos, SearchHead> search_heads = new HashMap();
        ArrayList<SearchHead> active_search_heads = new ArrayList();

        int max_y = Integer.MIN_VALUE;
        for (Map.Entry<BlockPos, TileEntity> entry : contraption.presentTileEntities.entrySet()) {
            BlockPos block_pos = entry.getKey();
            max_y = Integer.max(block_pos.getY(), max_y);
            if (entry.getValue() instanceof BlazeBurnerTileEntity) {
                SearchHead head = new SearchHead();

                head.layer = layers.size();
                layers.add(new Layer());

                head.group = groups.size();
                groups.add(new Group());

                head.parent_pos = null;
                active_search_heads.add(head);
            }
        }

        for (Map.Entry<BlockPos, Template.BlockInfo> entry : contraption.getBlocks().entrySet()) {
            if (entry.getValue().state.is(BlockTags.WOOL)) {
                wool_positions.add(entry.getKey());
            }
        }

        int safety_i = 0;
        while (!active_search_heads.isEmpty() && safety_i < 5) {
            safety_i += 1;

            SearchHead search_head = active_search_heads.remove(active_search_heads.size() - 1);

            layers.get(search_head.layer).num_active_heads -= 1;

            // If we go out of bounds, traverse the search heads up and mark all those layers as
            // connected to void.
            if (search_head.pos.getY() > max_y) {
                BlockPos recurse_pos = search_head.parent_pos;
                while (recurse_pos != null) {
                    SearchHead old_search_head = search_heads.get(recurse_pos);
                    layers.get(old_search_head.layer).connected_to_void = true;
                    recurse_pos = old_search_head.parent_pos;
                }

                continue;
            }

            // If we meet up with an old search head, simply
            SearchHead old_search_head = search_heads.get(search_head.pos);
            if (old_search_head != null) {
                if (search_head.layer != old_search_head.layer) {
                    // The only case this should happen in, is if two different streams of air meet up
                    // in a new place. In the future, this may matter if we want to join streams of hot air together
                    // to make a doubly strong hot air stream.
                    search_head.layer = old_search_head.layer;
                }

                if (search_head.group != old_search_head.group) {
                    // Merge the groups into one.
                    Group destroyed_group = groups.get(search_head.group);
                    Group merged_group = groups.get(old_search_head.group);

                    for (BlockPos moved_block : destroyed_group.blocks) {
                        merged_group.blocks.add(moved_block);
                    }

                    groups.set(search_head.group, merged_group);
                }

                continue;
            }

            if (wool_positions.contains(search_head.pos)) continue;

            search_heads.put(search_head.pos, search_head);
            groups.get(search_head.group).blocks.add(search_head.pos);


            if (search_head.parent_pos != null) {
                boolean do_horizontal = false;
                BlockPos above = search_head.parent_pos.above();

                if (wool_positions.contains(above)) {
                    do_horizontal = true;
                }

                if (!do_horizontal) {
                    SearchHead above_search_head = search_heads.get(above);
                    if (above_search_head != null && layers.get(above_search_head.layer).connected_to_void) {
                        do_horizontal = true;
                    }
                }

                if (do_horizontal) {
                    layers.get(search_head.layer).num_active_heads += 4;
                    BlockPos[] horizontal_positions = {search_head.pos.east(), search_head.pos.south(), search_head.pos.north(), search_head.pos.west()};
                    for (BlockPos new_pos : horizontal_positions) {
                        SearchHead new_search_head = new SearchHead();
                        new_search_head.pos = new_pos;
                        new_search_head.layer = search_head.layer;
                        new_search_head.parent_pos = search_head.pos;
                        new_search_head.group = search_head.group;
                        active_search_heads.add(new_search_head);
                    }
                }
            }

            {
                SearchHead deeper_search_head = new SearchHead();
                deeper_search_head.pos = search_head.pos.above();
                deeper_search_head.layer = layers.size();
                layers.add(new Layer());
                deeper_search_head.group = search_head.group;
                deeper_search_head.parent_pos = search_head.pos;
                active_search_heads.add(deeper_search_head);
            }
        }

        // Now iterate through all blocks again, those with layers that aren't connected to void get to place wool blocks
        for (HashMap.Entry<BlockPos, SearchHead> entry : search_heads.entrySet()) {
            SearchHead search_head = entry.getValue();
            if (!layers.get(search_head.layer).connected_to_void) {
                contraption.setBlockState(entry.getKey(), Blocks.ACACIA_WOOD.defaultBlockState(), null);
            }
        }
    }
}

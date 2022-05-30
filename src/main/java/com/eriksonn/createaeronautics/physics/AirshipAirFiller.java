package com.eriksonn.createaeronautics.physics;
import com.eriksonn.createaeronautics.contraptions.AirshipContraption;
import com.eriksonn.createaeronautics.physics.SimulatedContraptionRigidbody;
import com.simibubi.create.content.contraptions.processing.burner.BlazeBurnerTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Quaternion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AirshipAirFiller {
    // How much lift-force you can at most get proportionally to the air volume. Note it does not change the power you
    // get out of burners.
    final double LOAD_CAPACITY_PER_AIR_UNIT = 5.0;
    // How much load a blaze burner can lift in total (huge for now to allow take-of)
    final double TEMP_BLAZE_BURNER_LIFT = 300.0;

    StaleAirSection stale_air_sections[];
    HashMap<BlockPos, HeatSource> heat_sources = new HashMap<BlockPos, HeatSource>();;
    
    public void FillAir(AirshipContraption contraption) {
        class SearchHead {
            int x, y, z;
            boolean connected_to_air;
            int region_idx;

            SearchHead(int x, int y, int z, int region_idx, boolean connected_to_air) {
                this.x = x;
                this.y = y;
                this.z = z;
                this.region_idx = region_idx;
                this.connected_to_air = connected_to_air;
            }
        }

        // It would be good if the cells weren't heap allocated....
        // I wonder if there's even a way to do that in java?
        class Cell {
            boolean wall = false;
            boolean filled = false;
            boolean connected_to_air = false;
            int region_idx = -1;
            int stale_air_section_idx = -1;
        }

        ArrayList<Region> regions = new ArrayList<Region>();

        // TODO: Is it necessary to add one to each end here? I'm doing that as safety to ensure you can't create air-bubbles with the edges
        // of the contraption bounds, but maybe they're already bigger than they need to be?
        int max_x = (int)contraption.bounds.maxX + 1;
        int max_y = (int)contraption.bounds.maxY + 1;
        int max_z = (int)contraption.bounds.maxZ + 1;
        int min_x = (int)contraption.bounds.minX - 1;
        int min_y = (int)contraption.bounds.minY - 1;
        int min_z = (int)contraption.bounds.minZ - 1;

        // TODO: Is the max inclusive or exclusive? To be safe for now I assumed they're inclusive since that works in both cases.
        int width = (max_x - min_x) + 1;
        int height = (max_y - min_y) + 1;
        int depth = (max_z - min_z) + 1;

        Cell cells[] = new Cell[width * height * depth];
        for (int i = 0; i < cells.length; i++) cells[i] = new Cell();

        // Grab map data
        for (Map.Entry<BlockPos, Template.BlockInfo> entry : contraption.getBlocks().entrySet()) {
            if (entry.getValue().state.is(BlockTags.WOOL)) {
                BlockPos pos = entry.getKey();
                // TODO: Is this sanity check necessary? Presumably all the blocks `contraption.getBlocks()` gives out are within the bounds.
                if (pos.getX() >= min_x && pos.getX() <= max_x && pos.getY() >= min_y && pos.getY() <= max_y && pos.getZ() >= min_z && pos.getZ() <= max_z) {
                    // Would have liked to factor this indexing, but this being java can we trust that to be optimized?
                    cells[(pos.getX() - min_x) + (pos.getY() - min_y) * width + (pos.getZ() - min_z) * width * height].wall = true;
                }
            }
        }

        // Actually search things
        ArrayList<SearchHead> heads = new ArrayList<SearchHead>();
        ArrayList<SearchHead> next_heads = new ArrayList<SearchHead>();

        {
            int y = height - 1;
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    heads.add(new SearchHead(x, y, z, -1, true));
                }
            }
        }

        while (!heads.isEmpty()) {
            while (!heads.isEmpty()) {
                SearchHead head = heads.remove(heads.size() - 1);
                if (head.x < 0 || head.x >= width || head.y < 0 || head.y >= height || head.z < 0 || head.z >= depth) continue;
                int region_idx = get_real_region_idx(regions, head.region_idx);

                Cell cell = cells[head.x + head.y * width + head.z * width * height];
                if (cell.wall) continue;
                if (cell.filled) {
                    int cell_region_idx = get_real_region_idx(regions, cell.region_idx);
                    if (cell_region_idx != -1) {
                        Region merge_into = regions.get(cell_region_idx);
                        if (region_idx != -1) {
                            if (cell_region_idx != region_idx) {
                                Region remove = regions.get(region_idx);
                                remove.actual_region_idx = cell_region_idx;
                            }
                        }
                    }

                    continue;
                }

                if (!head.connected_to_air && head.region_idx == -1) {
                    region_idx = regions.size();
                    Region region = new Region();
                    regions.add(region);
                }

                cell.filled = true;
                cell.connected_to_air = head.connected_to_air;
                cell.region_idx = region_idx;

                // Horizontal movement
                heads.add(new SearchHead(head.x + 1, head.y, head.z, region_idx, head.connected_to_air));
                heads.add(new SearchHead(head.x - 1, head.y, head.z, region_idx, head.connected_to_air));
                heads.add(new SearchHead(head.x, head.y, head.z + 1, region_idx, head.connected_to_air));
                heads.add(new SearchHead(head.x, head.y, head.z - 1, region_idx, head.connected_to_air));

                next_heads.add(new SearchHead(head.x, head.y - 1, head.z, region_idx, head.connected_to_air));

                // Moving up might create an air pocket. This is why it's important to have a distinction between "heads" and "next_heads";
                // If there was some air-hole somewhere, we would have discovered that before this, thus the 'filled' flag gets set, and no air pocket region is created.
                // Why that works is that whenever air flow happens, we know we will get to that point in 'y' iterations, where y is how many blocks lower from the top of the creation we are.
                // However, if we create a pocket, we will always come there later, as we have to go below the entry point first, and then go back up.
                next_heads.add(new SearchHead(head.x, head.y + 1, head.z, region_idx, false));
            }

            ArrayList<SearchHead> temp = heads;
            heads = next_heads;
            next_heads = temp;
        }

        // Create "sections" from the regions, that are the actual regions of air that we know won't be combined anymore
        // and thus it's okay to dump a crapton of data into them.
        int section_counts = 0;
        for (int region_idx = 0; region_idx < regions.size(); region_idx++) {
            Region region = regions.get(region_idx);
            if (region.actual_region_idx == -1) {
                region.section_idx = section_counts;
                section_counts += 1;
            }
        }

        // Combine regions together into `StaleAirSection`s
        stale_air_sections = new StaleAirSection[section_counts];
        ArrayList<Vector3d> section_points[] = new ArrayList[section_counts];
        for (int i = 0; i < section_counts; i++) {
            stale_air_sections[i] = new StaleAirSection();
            section_points[i] = new ArrayList();
        }

        for (int z = 0; z < depth; z++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Cell cell = cells[x + y * width + z * width * height];
                    int real_region_idx = get_real_region_idx(regions, cell.region_idx);
                    if (real_region_idx != -1) {
                        Region region = regions.get(real_region_idx);

                        BlockPos pos = new BlockPos(x + min_x, y + min_y, z + min_z);
                        int section_idx = region.section_idx;
                        cell.stale_air_section_idx = section_idx;
                        stale_air_sections[section_idx].air_volume += 1.0;
                        section_points[section_idx].add(new Vector3d((double)(x + min_x), (double)(y + min_y), (double)(z + min_z)));

                        /* Debugging aid

                        BlockState block_state;
                        switch (real_region_idx % 6) {
                            case 0: block_state = Blocks.BLUE_WOOL.defaultBlockState(); break;
                            case 1: block_state = Blocks.GREEN_WOOL.defaultBlockState(); break;
                            case 2: block_state = Blocks.CYAN_WOOL.defaultBlockState(); break;
                            case 3: block_state = Blocks.MAGENTA_WOOL.defaultBlockState(); break;
                            case 4: block_state = Blocks.RED_WOOL.defaultBlockState(); break;
                            default: block_state = Blocks.WHITE_WOOL.defaultBlockState(); break;
                        }

                        contraption.setBlockState(pos, block_state, null);

                        */
                    }
                }
            }
        }

        for (int i = 0; i < section_counts; i++) {
            SimulatedContraptionRigidbody.BuoyancyController controller = new SimulatedContraptionRigidbody.BuoyancyController(1.0);
            controller.set(section_points[i]);
            stale_air_sections[i].controller = controller;
        }

        // Add blaze burners as heat sources (we add them even if they're not under an air bubble, because otherwise there's no way
        // to distinguish between calling `UpdateHeatStrength` on a de-synced broken heat source, or on a heat source that just didn't
        // end up connecting to an air bubble. I imagine that could be useful debugging information. It's fine to change it to not add things
        // that don't connect if it becomes a performance concern (probably not though))
        for (Map.Entry<BlockPos, TileEntity> entry : contraption.presentTileEntities.entrySet()) {
            TileEntity tile_entity = entry.getValue();
            if (tile_entity instanceof BlazeBurnerTileEntity) {
                BlockPos pos = entry.getKey();

                HeatSource heat_source = new HeatSource();
                heat_source.strength = TEMP_BLAZE_BURNER_LIFT;
                heat_sources.put(pos, heat_source);

                // Find a connecting region
                int x = pos.getX() - min_x;
                int z = pos.getZ() - min_z;
                for (int y = pos.getY() - min_y; y <= max_y; y++) {
                    Cell cell = cells[x + y * width + z * width * height];
                    if (cell.wall) {
                        // Can't send heat through walls.
                        break;
                    }
                    if (cell.stale_air_section_idx != -1) {
                        StaleAirSection section = stale_air_sections[cell.stale_air_section_idx];
                        heat_source.warming_section_idx = cell.stale_air_section_idx;
                        break;
                    }
                }
            }
        }

        RecomputeStrengths();
    }

    void RecomputeStrengths() {
        for (int i = 0; i < stale_air_sections.length; i++) {
            stale_air_sections[i].total_strength = 0.0;
        }

        for (HeatSource source : heat_sources.values()) {
            if (source.warming_section_idx != -1) {
                stale_air_sections[source.warming_section_idx].total_strength += source.strength;
            }
        }

        for (int i = 0; i < stale_air_sections.length; i++) {
            StaleAirSection section = stale_air_sections[i];

            // The actual computation of
            section.controller.strengthScale = Double.min(section.total_strength / section.air_volume, LOAD_CAPACITY_PER_AIR_UNIT);
        }
    }

    // This also updates the strength values for all the StaleAirSections. Should it
    public void ChangeStrength(BlockPos pos, double new_strength) {
        HeatSource source = this.heat_sources.get(pos);
        if (source != null) {
            source.strength = new_strength;
            // It's a bit of a waste going through all heat sources when all we want to do is update one set of them, could be made
            // more linear time by having a list of what heat sources each StaleAirSection has. However, I don't think this will be a problem since you
            // will most likely only have one big set of burners.
            RecomputeStrengths();
        }
    }

    public void apply(SimulatedContraptionRigidbody contraption) {
        for (int i = 0; i < stale_air_sections.length; i++) {
            if (stale_air_sections[i].controller.strengthScale >= 0.001) {
                stale_air_sections[i].controller.apply(contraption, contraption.orientation, contraption.adapter.position());
            }
        }
    }

    public static class HeatSource {
        public double strength;
        BlockPos pos;

        int warming_section_idx = -1;
    }

    public static class StaleAirSection {
        SimulatedContraptionRigidbody.BuoyancyController controller;

        // Non-clamped strength (we just clamp it in `RecomputeBuoyancyStrengths`)
        double total_strength = 0.0;
        double air_volume = 0.0;
    }

    // Why is this here and not somewhere better? Because java doesn't support inline functions like a normal language, that's why.
    class Region {
        // Right now we may get several redirections through this, which isn't that great. Would probably be better to later
        // make sure there's only ever one layer redirection, by going through and updating all previous redirections whenever we add a new one.
        // That is technically more than O(N) time, but there won't be many regions anyway so it shouldn't be a problem. Another option would be
        // having a list that keeps track of all referants, but that seems error-prone and is probably much slower in the general case.
        int actual_region_idx = -1;
        int section_idx = -1;
    }

    static int get_real_region_idx(ArrayList<Region> regions, int region_idx) {
        if (region_idx != -1) {
            while (true) {
                Region region = regions.get(region_idx);
                if (region.actual_region_idx == -1) break;
                region_idx = region.actual_region_idx;
            }
        }

        return region_idx;
    }
}

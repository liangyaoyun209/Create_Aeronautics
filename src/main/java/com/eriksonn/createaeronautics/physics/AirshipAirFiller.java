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
import java.util.Map;

public class AirshipAirFiller {
    StaleAirSection stale_air_sections[];
    ArrayList<HeatSource> heat_sources;
    
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

        // TODO: It would be good if the cells weren't heap allocated....
        // I wonder if there's even a way to do that in java?
        class Cell {
            boolean wall = false;
            boolean filled = false;
            boolean connected_to_air = false;
            int region_idx = -1;
        }

        // Region stuff
        ArrayList<Region> regions = new ArrayList<Region>();

        // We want to find the bounds of the region that the contraption is in, and then make a grid using those.
        // TODO: Is it necessary to add one to each end here? I'm doing that as safety to make sure you can't create air-bubbles with the edges
        // of the contraption bounds, but maybe they're already bigger than they need to be?
        int max_x = (int)contraption.bounds.maxX + 1;
        int max_y = (int)contraption.bounds.maxY + 1;
        int max_z = (int)contraption.bounds.maxZ + 1;
        int min_x = (int)contraption.bounds.minX - 1;
        int min_y = (int)contraption.bounds.minY - 1;
        int min_z = (int)contraption.bounds.minZ - 1;

        // TODO: Is the max inclusive or exclusive? To be safe for now I assumed they're inclusive.
        int width = (max_x - min_x) + 1;
        int height = (max_y - min_y) + 1;
        int depth = (max_z - min_z) + 1;

        Cell cells[] = new Cell[width * height * depth];
        for (int i = 0; i < cells.length; i++) cells[i] = new Cell();

        // Then we want to grab data from the actual map and use that to inform the cells
        for (Map.Entry<BlockPos, Template.BlockInfo> entry : contraption.getBlocks().entrySet()) {
            if (entry.getValue().state.is(Blocks.GLASS)) {
                BlockPos pos = entry.getKey();
                // TODO: Is this safety check necessary?
                if (pos.getX() >= min_x && pos.getX() <= max_x && pos.getY() >= min_y && pos.getY() <= max_y && pos.getZ() >= min_z && pos.getZ() <= max_z) {
                    // TODO: Would have liked to factor this indexing, but this being java can I trust that to be optimized? Should look that up later maybe
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
                                merge_into.highest_air_inflow_y = Integer.max(merge_into.highest_air_inflow_y, remove.highest_air_inflow_y);
                            }
                        } else {
                            if (merge_into.highest_air_inflow_y < head.y) {
                                merge_into.highest_air_inflow_y = head.y;
                            }
                        }
                    }

                    continue;
                }

                if (!head.connected_to_air && head.region_idx == -1) {
                    region_idx = regions.size();
                    Region region = new Region();
                    region.highest_air_inflow_y = head.y;
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
                        if (region.highest_air_inflow_y > y) continue;

                        BlockPos pos = new BlockPos(x + min_x, y + min_y, z + min_z);
                        int section_idx = region.section_idx;
                        stale_air_sections[section_idx].air_volume += 1.0;
                        section_points[section_idx].add(new Vector3d((double)(x + min_x), (double)(y + min_y), (double)(z + min_z)));

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
                    }
                }
            }
        }

        for (int i = 0; i < section_counts; i++) {
            SimulatedContraptionRigidbody.BuoyancyController controller = new SimulatedContraptionRigidbody.BuoyancyController(3.0);
            controller.set(section_points[i]);
            stale_air_sections[i].controller = controller;
        }

    }

    public void apply(SimulatedContraptionRigidbody contraption) {
        for (int i = 0; i < stale_air_sections.length; i++) {
            stale_air_sections[i].controller.apply(contraption, contraption.orientation, contraption.adapter.position());
        }
    }

    public static class HeatSource {
        BlockPos pos;
        int warming_section_idx;
    }

    public static class StaleAirSection {
        SimulatedContraptionRigidbody.BuoyancyController controller;

        double temperature = 0.0;
        double air_volume = 0.0;
    }

    // Why is this here and not somewhere better? Because java doesn't support inline functions like a normal language, that's why.
    class Region {
        // TODO: Right now we may get several redirections through this, which isn't that great. Would probably be better to later
        // make sure there's only ever one layer redirection, by going through and updating all previous redirections whenever we add a new one.
        // That is technically more than O(N) time, but there won't be many regions anyway so it shouldn't be a problem. Another option would be
        // having a list that keeps track of all referants, but that seems error-prone and is probably much slower in the general case.
        int actual_region_idx = -1;
        int section_idx = -1;
        int highest_air_inflow_y;
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

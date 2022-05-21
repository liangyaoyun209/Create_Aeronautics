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
import java.util.Map;

public class AirshipAirFiller {
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

        // TODO: It would be good if the array of cells wasn't heap allocated....
        // I wonder if there's even a way to do that in java?
        class Cell {
            boolean wall = false;
            boolean filled = false;
            boolean connected_to_air = false;
            int region_idx = -1;
        }

        // Region stuff
        ArrayList<Region> regions = new ArrayList();

        // We want to find the bounds of the region that the contraption is in, and then make a grid using those.
        int max_x = (int)contraption.bounds.maxX;
        int max_y = (int)contraption.bounds.maxY;
        int max_z = (int)contraption.bounds.maxZ;
        int min_x = (int)contraption.bounds.minX;
        int min_y = (int)contraption.bounds.minY;
        int min_z = (int)contraption.bounds.minZ;

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
                if (pos.getX() >= min_x && pos.getX() <= max_x && pos.getY() >= min_y && pos.getY() <= max_y && pos.getZ() >= min_z && pos.getZ() <= max_z) {
                    // TODO: Would have liked to factor this indexing, but this being java can I trust that to be optimized? Should look that up later maybe
                    cells[(pos.getX() - min_x) + (pos.getY() - min_y) * width + (pos.getZ() - min_z) * width * height].wall = true;
                }
            }
        }

        // Actually search things
        ArrayList<SearchHead> heads = new ArrayList();
        ArrayList<SearchHead> next_heads = new ArrayList();

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
                    // If someone already came to this cell, we just stop the search, however, if it's a different region than us we need to combine the regions.
                    if (region_idx != -1) {
                        int cell_region_idx = get_real_region_idx(regions, cell.region_idx);
                        if (cell_region_idx != -1 && cell_region_idx != region_idx) {
                            regions.get(region_idx).actual_region_idx = cell_region_idx;
                        }
                    }

                    continue;
                }

                if (!head.connected_to_air && head.region_idx == -1) {
                    region_idx = regions.size();
                    regions.add(new Region());
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

        // Then, commit what we did to the world
        for (int z = 0; z < depth; z++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Cell cell = cells[x + y * width + z * width * height];
                    if (cell.region_idx != -1) {
                        BlockPos pos = new BlockPos(x + min_x, y + min_y, z + min_z);

                        BlockState block_state;
                        switch (cell.region_idx % 6) {
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
    }

    // Why is this here and not somewhere better? Because java doesn't support inline functions like a normal language, that's why.
    class Region {
        int actual_region_idx = -1;
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

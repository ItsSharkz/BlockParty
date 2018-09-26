package de.leonkoth.blockparty.floor;

import de.leonkoth.blockparty.exception.FloorLoaderException;
import de.leonkoth.blockparty.util.Selection;
import de.leonkoth.blockparty.util.Size;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class FloorPattern {

    @Getter
    private String name;

    @Getter
    private Size size;

    @Getter
    private Material[] materials;

    @Getter
    private byte[] data;

    public FloorPattern(String name, Size size, Material[] materials, byte[] data) {
        this.name = name;
        this.size = size;
        this.materials = materials;
        this.data = data;
    }

    public static FloorPattern create(String name, Selection selection) throws FloorLoaderException {

        if(selection.getSize().getHeight() != 1) {
            throw new FloorLoaderException(FloorLoaderException.Error.WRONG_HEIGHT);
        }

        World world = selection.getWorld();
        int width = selection.getSize().getWidth();
        int length = selection.getSize().getLength();

        Material[] blocks = new Material[width * length];
        byte[] data = new byte[width * length];
        for(int x = 0; x < width; x++) {
            for(int z = 0; z < length; z++) {
                blocks[x + z * width] = world.getBlockAt(selection.getMinX() + x, selection.getMinY(), selection.getMinZ() + z).getType();
                data[x + z * width] = world.getBlockAt(selection.getMinX() + x, selection.getMinY(), selection.getMinZ() + z).getData();
            }
        }

        return new FloorPattern(name, new Size(width, 1, length), blocks, data);
    }

    public void place(Location location) {
        for(int x = 0; x < size.getWidth(); x++) {
            for(int z = 0; z < size.getLength(); z++) {
                location.clone().add(x, 0, z).getBlock().setType(materials[x + z * size.getWidth()]);
            }
        }
    }

}
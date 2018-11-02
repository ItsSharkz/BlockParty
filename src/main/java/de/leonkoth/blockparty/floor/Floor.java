package de.leonkoth.blockparty.floor;

import de.leonkoth.blockparty.BlockParty;
import de.leonkoth.blockparty.arena.Arena;
import de.leonkoth.blockparty.exception.FloorLoaderException;
import de.leonkoth.blockparty.floor.generator.AreaGenerator;
import de.leonkoth.blockparty.floor.generator.FloorGenerator;
import de.leonkoth.blockparty.floor.generator.SingleBlockGenerator;
import de.leonkoth.blockparty.floor.generator.StripeGenerator;
import de.leonkoth.blockparty.player.PlayerInfo;
import de.leonkoth.blockparty.player.PlayerState;
import de.leonkoth.blockparty.util.Bounds;
import de.leonkoth.blockparty.util.ColorBlock;
import de.leonkoth.blockparty.util.Size;
import de.pauhull.utils.particle.ParticlePlayer;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Leon on 14.03.2018.
 * Project Blockparty2
 * © 2016 - Leon Koth
 */
public class Floor {

    private static Random random = new Random();

    @Getter
    private Arena arena;

    @Getter
    @Setter
    private World world;

    @Getter
    @Setter
    private Block currentBlock;

    @Setter
    @Getter
    private Size size;

    @Setter
    @Getter
    private Bounds bounds;

    @Setter
    @Getter
    private List<FloorPattern> floorPatterns;

    @Setter
    @Getter
    private List<String> patternNames;

    @Setter
    @Getter
    private List<FloorGenerator> generators = new ArrayList<>();

    public Floor(List<String> patternNames, Bounds bounds, Arena arena, Size size) {
        this.size = size;
        this.arena = arena;
        this.bounds = bounds;
        this.world = bounds.getWorld();
        this.patternNames = patternNames;
        this.floorPatterns = new ArrayList<>();

        for (String name : patternNames) {
            File file = new File(BlockParty.PLUGIN_FOLDER + "Floors/" + name + ".floor");
            try {
                FloorPattern pattern = PatternLoader.readFloorPattern(file);
                floorPatterns.add(pattern);
            } catch (FileNotFoundException | FloorLoaderException e) {
                Bukkit.getConsoleSender().sendMessage("§c[BlockParty] Couldn't find file \"Floors/" + name + ".floor\"");
                //e.printStackTrace();
            }
        }

        this.generators.add(new AreaGenerator());
        this.generators.add(new SingleBlockGenerator());
        this.generators.add(new StripeGenerator());
    }

    public static boolean create(Arena arena, Bounds bounds) {

        Size size = bounds.getSize();
        List<String> floorNames = new ArrayList<>();

        Floor floor;
        if (arena.getFloor() == null) {
            floor = new Floor(floorNames, bounds, arena, size);
        } else {
            floor = arena.getFloor();
        }

        floor.setSize(size);

        try {
            arena.setFloor(floor);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public void placeFloor() {
        if (arena.isUsePatternFloors()) {

            if (this.arena.isUseAutoGeneratedFloors()) {
                int index = random.nextInt(floorPatterns.size() + generators.size());

                if (index < floorPatterns.size()) {
                    floorPatterns.get(index).place(bounds.getA());
                } else {
                    generators.get(index - floorPatterns.size()).generateFloor(this);
                }
            } else {
                floorPatterns.get(random.nextInt(floorPatterns.size())).place(bounds.getA());
            }

        } else {
            if (!this.arena.isUseAutoGeneratedFloors()) {
                Bukkit.getConsoleSender().sendMessage("§c[BlockParty] UsePatternFloors and UseAutoGeneratedFloors disabled. Using auto generated floors now!");
            }

            generateFloor();
        }
    }

    public void setStartFloor() {
        if (this.arena.isUsePatternFloors()) {

            if (floorPatterns.isEmpty()) {
                generateFloor();
                return;
            }

            for (FloorPattern floorPattern : this.getFloorPatterns()) {
                if (floorPattern.getName().equalsIgnoreCase("start")) {
                    floorPattern.place(bounds.getA());
                    return;
                }
            }
            floorPatterns.get(random.nextInt(floorPatterns.size())).place(bounds.getA());
        } else {
            generateFloor();
        }
    }

    public void setEndFloor() {
        if (this.arena.isUsePatternFloors()) {

            if (floorPatterns.isEmpty()) {
                generateFloor();
                return;
            }

            for (FloorPattern floorPattern : this.getFloorPatterns()) {
                if (floorPattern.getName().equalsIgnoreCase("end")) {
                    floorPattern.place(bounds.getA());
                    return;
                }
            }
            floorPatterns.get(random.nextInt(floorPatterns.size())).place(bounds.getA());
        } else {
            generateFloor();
        }
    }

    private void generateFloor() {
        FloorGenerator generator = generators.get(random.nextInt(generators.size()));
        generator.generateFloor(this);
    }

    public void removeBlocks() {
        Byte data = currentBlock.getData();
        Material material = currentBlock.getType();

        for (Block block : getFloorBlocks()) {
            if (block.getData() != data || block.getType() != material) {
                block.setType(Material.AIR);
            }
        }
    }

    public List<Block> getFloorBlocks() {

        int minX = bounds.getA().getBlockX();
        int minZ = bounds.getA().getBlockZ();
        int maxX = bounds.getB().getBlockX();
        int maxZ = bounds.getB().getBlockZ();

        List<Block> blocks = new ArrayList<>();

        int y = bounds.getA().getBlockY();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                blocks.add(world.getBlockAt(x, y, z));
            }
        }

        return blocks;
    }

    public void pickBlock() {
        currentBlock = getRandomBlock();
        updateInventories(currentBlock);
    }

    public void updateInventories(Block block) {

        ItemStack stack = new ItemStack(block.getType(), 1, block.getData());
        ItemMeta meta = stack.getItemMeta();
        String name = ColorBlock.get(block).getName();
        meta.setDisplayName("§f§l§o" + name);
        stack.setItemMeta(meta);

        for (PlayerInfo playerInfo : this.arena.getPlayersInArena()) {
            if (playerInfo.getPlayerState() == PlayerState.INGAME) {
                Player player = playerInfo.asPlayer();

                player.getInventory().setItem(4, stack);
            }
        }
    }

    public void clearInventories() {
        for (PlayerInfo playerInfo : this.arena.getPlayersInArena()) {
            if (playerInfo.getPlayerState() == PlayerState.INGAME) {
                playerInfo.asPlayer().getInventory().clear();
            }
        }
    }

    private Block getRandomBlock() {
        Block block = getRandomLocation().getBlock();
        return block.getType() == Material.AIR ? getRandomBlock() : block;
    }

    public Location getRandomLocation() {

        int minX = bounds.getA().getBlockX();
        int minY = bounds.getA().getBlockY();
        int minZ = bounds.getA().getBlockZ();

        int x = minX + random.nextInt(bounds.getSize().getBlockWidth());
        int z = minZ + random.nextInt(bounds.getSize().getBlockLength());

        return new Location(world, x, minY, z);
    }

    public void playParticles(int amount, int offsetY, int rangeY) {
        ParticlePlayer particlePlayer = arena.getParticlePlayer();

        for (int i = 0; i < amount; i++) {
            particlePlayer.play(pickRandomLocation(offsetY, rangeY), 1);
        }
    }

    public Location pickRandomLocation(int offsetY, int rangeY) {

        int minX = bounds.getA().getBlockX();
        int minY = bounds.getA().getBlockY() + offsetY;
        int minZ = bounds.getA().getBlockZ();

        int x = minX + random.nextInt(size.getBlockWidth());
        int y = minY + random.nextInt(rangeY);
        int z = minZ + random.nextInt(size.getBlockLength());

        return new Location(world, x, y, z);
    }

    public FloorPattern loadPattern(String name) {
        File file = new File(BlockParty.PLUGIN_FOLDER + "Floors/" + name + ".floor");
        try {
            FloorPattern pattern = PatternLoader.readFloorPattern(file);
            return pattern;
        } catch (FileNotFoundException | FloorLoaderException e) {
            return null;
        }
    }

}

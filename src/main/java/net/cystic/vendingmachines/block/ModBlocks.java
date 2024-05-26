package net.cystic.vendingmachines.block;

import net.cystic.vendingmachines.VendingMachines;
import net.cystic.vendingmachines.block.custom.VendingMachineBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {

    public static final Block VENDING_MACHINE = registerBlock("vending_machine",
            new VendingMachineBlock(FabricBlockSettings.create().nonOpaque()));

    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, new Identifier(VendingMachines.MOD_ID, name), block);
    }

    private static Item registerBlockItem(String name, Block block) {
        return Registry.register(Registries.ITEM, new Identifier(VendingMachines.MOD_ID, name),
                new BlockItem(block, new FabricItemSettings()));
    }
}

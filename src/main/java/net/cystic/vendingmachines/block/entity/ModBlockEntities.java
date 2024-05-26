package net.cystic.vendingmachines.block.entity;

import net.cystic.vendingmachines.VendingMachines;
import net.cystic.vendingmachines.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {
    public static BlockEntityType<VendingMachineBlockEntity> VENDING_MACHINE;
    public static void registerBlockEntities() {
        VENDING_MACHINE = Registry.register(Registries.BLOCK_ENTITY_TYPE,
                new Identifier(VendingMachines.MOD_ID, "vending_machine"),
                FabricBlockEntityTypeBuilder.create(VendingMachineBlockEntity::new,
                        ModBlocks.VENDING_MACHINE).build(null));
    }
}

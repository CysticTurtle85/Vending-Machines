package net.cystic.vendingmachines;

import net.cystic.vendingmachines.block.ModBlocks;
import net.cystic.vendingmachines.screen.ModScreenHandlers;
import net.cystic.vendingmachines.screen.VendingMachineScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.minecraft.client.render.RenderLayer;

public class VendingMachinesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.VENDING_MACHINE, RenderLayer.getCutout());

        ScreenRegistry.register(ModScreenHandlers.VENDING_MACHINE_SCREEN_HANDLER, VendingMachineScreen::new);
    }
}

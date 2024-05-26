package net.cystic.vendingmachines.screen;

import net.cystic.vendingmachines.VendingMachines;
import net.cystic.vendingmachines.screen.slot.VendingMachineScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {
    public static ScreenHandlerType<VendingMachineScreenHandler> VENDING_MACHINE_SCREEN_HANDLER =
            ScreenHandlerRegistry.registerSimple(new Identifier(VendingMachines.MOD_ID, "vending_machine"),
                    VendingMachineScreenHandler::new);
}

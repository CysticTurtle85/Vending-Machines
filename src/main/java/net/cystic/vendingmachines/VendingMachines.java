package net.cystic.vendingmachines;

import net.cystic.vendingmachines.block.entity.ModBlockEntities;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VendingMachines implements ModInitializer {
	public static String MOD_ID = "vendingmachines";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlockEntities.registerBlockEntities();
	}
}
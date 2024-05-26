package net.cystic.vendingmachines.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.cystic.vendingmachines.VendingMachines;
import net.cystic.vendingmachines.screen.slot.VendingMachineScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOfferList;

public class VendingMachineScreen extends HandledScreen<VendingMachineScreenHandler> {
    private static final Identifier TEXTURE =
            new Identifier(VendingMachines.MOD_ID, "textures/gui/vending_machine_gui.png");

    public VendingMachineScreen(VendingMachineScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 276;
        this.playerInventoryTitleX = 107;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        context.drawTexture(TEXTURE, x , y, 0, 0, 0, backgroundWidth, backgroundHeight,  512, 256);
        TradeOfferList tradeOfferList = ((VendingMachineScreenHandler)this.handler).getRecipes();
    }

    @Override
    protected void init() {
        super.init();
        this.backgroundWidth = 276;
        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}

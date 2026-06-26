/*
 * Copyright (C) <2025 to Present> <jbredwards>
 *
 * All rights are reserved, except where explicitly granted by the original
 * copyright holder or where explicitly granted by the Mod Permissions License as
 * published by Jbredwards, either version 1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.
 *
 * See the Mod Permissions License for more details
 * <https://www.github.com/jbredwards/mod-permissions-license>.
 */

package git.jbredwards.akashic_goggles.mod.common.compat;

import baubles.api.cap.BaublesCapabilities;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.stream.IntStream;

/**
 *
 * @author jbred
 *
 */
@SideOnly(Side.CLIENT)
public class LayerBaublesArmor extends LayerBipedArmor
{
    public LayerBaublesArmor(@Nonnull final RenderLivingBase<?> rendererIn) { super(rendererIn); }

    @Override
    protected void initArmor() { modelArmor = new ModelBiped(0.25f); /* Use a size of 0.25 to render under helmets. */ }

    @Override
    public void doRenderLayer(@Nonnull final EntityLivingBase entity, final float limbSwing, final float limbSwingAmount, final float partialTicks, final float ageInTicks, final float netHeadYaw, final float headPitch, final float scale) {
        if(entity.isPotionActive(MobEffects.INVISIBILITY) || CompatHandler.test(entity.getItemStackFromSlot(EntityEquipmentSlot.HEAD))) return;
        // A hack to render any armor item, regardless of what the entity actually has equipped.
        @Nullable final IItemHandler inventory = entity.getCapability(BaublesCapabilities.CAPABILITY_BAUBLES, null);
        if(inventory != null) IntStream.range(0, inventory.getSlots()).mapToObj(inventory::getStackInSlot).filter(CompatHandler::test).findFirst().ifPresent(stack -> {
            @Nonnull final NonNullList<ItemStack> armor = ((EntityPlayer)entity).inventory.armorInventory;
            @Nonnull final ItemStack[] previous = armor.toArray(new ItemStack[0]);

            armor.clear();
            armor.set(EntityEquipmentSlot.HEAD.getIndex(), stack);
            try { super.doRenderLayer(entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale); }
            finally { for(int i = previous.length - 1; i > -1; i--) armor.set(i, previous[i]); }
        });
    }
}

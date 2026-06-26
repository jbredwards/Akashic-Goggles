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

import baubles.api.BaublesApi;
import baubles.api.cap.BaublesCapabilities;
import baubles.api.cap.IBaublesItemHandler;
import com.google.common.collect.Iterables;
import git.jbredwards.akashic_goggles.Tags;
import git.jbredwards.akashic_goggles.api.AkashicGogglesUtil;
import git.jbredwards.akashic_goggles.api.IAkashicGoggles;
import git.jbredwards.akashic_goggles.mod.AkashicGoggles;
import git.jbredwards.akashic_goggles.mod.common.AkashicGogglesConfig;
import git.jbredwards.akashic_goggles.mod.common.InventoryAkashicGoggles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vazkii.arl.util.TooltipHandler;
import vazkii.botania.common.core.helper.PlayerHelper;
import vazkii.botania.common.item.equipment.bauble.ItemBauble;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 * @author jbred
 *
 */
public enum CompatHandler
{
    ACTUALLYADDITIONS("actuallyadditions", AkashicGogglesConfig.ModCompat.ActuallyAdditions.class, () -> AkashicGogglesConfig.ModCompat.ActuallyAdditions.baubleType),
    BIBLIOCRAFT("bibliocraft", AkashicGogglesConfig.ModCompat.BiblioCraft.class, () -> AkashicGogglesConfig.ModCompat.BiblioCraft.baubleType),
    EMBERS("embers", AkashicGogglesConfig.ModCompat.Embers.class, () -> AkashicGogglesConfig.ModCompat.Embers.baubleType),
    EREBUS("erebus", AkashicGogglesConfig.ModCompat.Erebus.class, () -> AkashicGogglesConfig.ModCompat.Erebus.baubleType),
    EVILCRAFT("evilcraft", AkashicGogglesConfig.ModCompat.EvilCraft.class, () -> AkashicGogglesConfig.ModCompat.EvilCraft.baubleType),
    GALACTICRAFT("galacticraftcore", AkashicGogglesConfig.ModCompat.Galacticraft.class, () -> AkashicGogglesConfig.ModCompat.Galacticraft.baubleType),
    OPENBLOCKS("openblocks", AkashicGogglesConfig.ModCompat.OpenBlocks.class, () -> AkashicGogglesConfig.ModCompat.OpenBlocks.baubleType),
    RAILCRAFT("railcraft", AkashicGogglesConfig.ModCompat.Railcraft.class, () -> AkashicGogglesConfig.ModCompat.Railcraft.baubleType),
    SIMPLYJETPACKS("simplyjetpacks", AkashicGogglesConfig.ModCompat.SimplyJetpacks.class, () -> AkashicGogglesConfig.ModCompat.SimplyJetpacks.baubleType);

    @Nonnull private final Supplier<AkashicGogglesConfig.BaubleTypeAdapter> bauble;
    @Nonnull public final Class<?> config;
    @Nonnull public final String modid;

    @Nonnull private static final ResourceLocation CAPABILITY_ID = new ResourceLocation(Tags.MOD_ID, "baubles_cap");
    @Nonnull private static final List<CompatHandler> LOADED_HANDLERS = new ArrayList<>();

    CompatHandler(@Nonnull final String modidIn, @Nonnull final Class<?> configIn, @Nonnull final Supplier<AkashicGogglesConfig.BaubleTypeAdapter> baubleIn) {
        bauble = baubleIn;
        config = configIn;
        modid = modidIn;
    }

    public static void preInit() {
        LOADED_HANDLERS.addAll(Arrays.asList(Arrays.stream(values()).filter(ch -> Loader.isModLoaded(ch.modid)).toArray(CompatHandler[]::new)));
        MinecraftForge.EVENT_BUS.register(CompatHandler.class);
    }

    @SideOnly(Side.CLIENT)
    public static void loadCompleteClient() {
        if(AkashicGoggles.HAS_BAUBLES && !LOADED_HANDLERS.isEmpty()) Minecraft.getMinecraft().getRenderManager().getSkinMap().forEach((skin, render) -> render.addLayer(new LayerBaublesArmor(render)));
    }

    @Nonnull
    public static Optional<CompatHandler> findFirst(@Nonnull final ItemStack stack) {
        if(!(stack.getItem() instanceof IAkashicGoggles) || stack.getItem() instanceof ItemArmor && ((ItemArmor)stack.getItem()).armorType != EntityEquipmentSlot.HEAD) return Optional.empty();

        @Nullable final ResourceLocation id = stack.getItem().getRegistryName();
        return id != null ? LOADED_HANDLERS.stream().filter(cl -> cl.modid.equals(id.getNamespace())).findFirst() : Optional.empty();
    }

    // Useful for lambda expressions.
    public static boolean test(@Nonnull final ItemStack stack) { return findFirst(stack).isPresent(); }

    @Nonnull
    public static List<CompatHandler> getLoadedHandlers() { return Collections.unmodifiableList(LOADED_HANDLERS); }

    // ------
    // Events
    // ------

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    static void addApplicableTooltip(@Nonnull final ItemTooltipEvent event) {
        if(!AkashicGogglesConfig.Goggles.addApplicableTooltip) return;

        @Nonnull final ItemStack stack = event.getItemStack();
        @Nonnull final List<String> tooltip = event.getToolTip();

        if(InventoryAkashicGoggles.canDropIn(event.getEntityPlayer(), null, stack)) TooltipHandler.tooltipIfShift(tooltip,
                () -> TooltipHandler.addToTooltip(tooltip, AkashicGoggles.GOGGLES.getTranslationKey() + ".tooltip_applicable"));
    }

    @net.minecraftforge.fml.common.Optional.Method(modid = "baubles")
    @SubscribeEvent
    static void attachBaublesCapability(@Nonnull final AttachCapabilitiesEvent<ItemStack> event) {
        findFirst(event.getObject()).ifPresent(cl -> event.addCapability(CAPABILITY_ID, new ICapabilityProvider() {
            @Nullable
            @Override
            public <T> T getCapability(@Nonnull final Capability<T> capability, @Nullable final EnumFacing facing) {
                @Nonnull final AkashicGogglesConfig.BaubleTypeAdapter adapter = cl.bauble.get();
                return hasCapability(capability, facing) ? BaublesCapabilities.CAPABILITY_ITEM_BAUBLE.cast(adapter) : null;
            }

            @Override
            public boolean hasCapability(@Nonnull final Capability<?> capability, @Nullable final EnumFacing facing) {
                return capability == BaublesCapabilities.CAPABILITY_ITEM_BAUBLE && cl.bauble.get() != AkashicGogglesConfig.BaubleTypeAdapter.NONE;
            }
        }));
    }

    @net.minecraftforge.fml.common.Optional.Method(modid = "baubles")
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    static void renderBaublesOverlays(@Nonnull final RenderGameOverlayEvent.Post event) {
        @Nonnull final Minecraft mc = Minecraft.getMinecraft();
        if(event.getType() == RenderGameOverlayEvent.ElementType.HELMET && mc.gameSettings.thirdPersonView == 0) {
            @Nonnull final List<ItemStack> stacksForRender = new ArrayList<>();

            // Collect distinct items, to not render the same overlay multiple times.
            @Nonnull final List<ItemStack> headStacks = splitRenderCandidates(mc.player.getItemStackFromSlot(EntityEquipmentSlot.HEAD));
            @Nonnull final IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(mc.player);
            IntStream.range(0, baubles.getSlots()).mapToObj(baubles::getStackInSlot).flatMap(stack -> splitRenderCandidates(stack).stream()).forEach(stack -> {
                if(!Iterables.any(Iterables.concat(headStacks, stacksForRender), stack.getHasSubtypes() ? stack::isItemEqual : stack::isItemEqualIgnoreDurability)) stacksForRender.add(stack);
            });

            // Render distinct item overlays.
            stacksForRender.forEach(stack -> {
                GlStateManager.color(1, 1, 1, 1);
                stack.getItem().renderHelmetOverlay(stack, mc.player, event.getResolution(), event.getPartialTicks());
            });
        }
    }

    @net.minecraftforge.fml.common.Optional.Method(modid = "botania")
    @SubscribeEvent
    static void updateBotaniaBaubleAdvancement(@Nonnull final LivingEquipmentChangeEvent event) {
        if(event.getSlot().getSlotType() == EntityEquipmentSlot.Type.ARMOR && event.getEntity() instanceof EntityPlayerMP
        && AkashicGogglesUtil.getContainedStacks(event.getTo()).anyMatch(stack -> stack.getItem() instanceof ItemBauble)) {
            PlayerHelper.grantCriterion((EntityPlayerMP)event.getEntity(), new ResourceLocation("botania", "main/bauble_wear"), "code_triggered");
        }
    }

    @Nonnull
    private static List<ItemStack> splitRenderCandidates(@Nonnull final ItemStack goggles) {
        @Nonnull final List<ItemStack> stacks = AkashicGogglesUtil.getContainedStacks(goggles).collect(Collectors.toList());
        if(test(goggles)) stacks.add(goggles);
        return stacks;
    }
}

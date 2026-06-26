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

package git.jbredwards.akashic_goggles.mod;

import git.jbredwards.akashic_goggles.Tags;
import git.jbredwards.akashic_goggles.mod.client.ModelHeadwear;
import git.jbredwards.akashic_goggles.mod.common.InventoryAkashicGoggles;
import git.jbredwards.akashic_goggles.mod.common.ItemAkashicGoggles;
import git.jbredwards.akashic_goggles.mod.common.RecipeAkashicCombine;
import git.jbredwards.akashic_goggles.mod.common.command.ClickToCopyHandler;
import git.jbredwards.akashic_goggles.mod.common.command.CommandAkashicGoggles;
import git.jbredwards.akashic_goggles.mod.common.compat.CompatHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.datafix.DataFixesManager;
import net.minecraft.util.datafix.FixTypes;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.resource.IResourceType;
import net.minecraftforge.client.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.client.resource.VanillaResourceType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;
import vazkii.arl.recipe.RecipeHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 *
 * @author jbred
 *
 */
@Mod.EventBusSubscriber
@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION, updateJSON = Tags.UPDATE_JSON,
dependencies = "required-before:autoreglib@[1.3-32,);",
guiFactory = "git.jbredwards.akashic_goggles.mod.client.config.AkashicGogglesGuiFactory")
public final class AkashicGoggles
{
    public static final boolean HAS_BAUBLES = Loader.isModLoaded("baubles");

    @Mod.EventHandler
    static void construct(@Nonnull final FMLConstructionEvent event) {
        ClickToCopyHandler.construct();
    }

    @Mod.EventHandler
    static void preInit(@Nonnull final FMLPreInitializationEvent event) {
        CompatHandler.preInit();
        FMLCommonHandler.instance().getDataFixer().registerWalker(FixTypes.ITEM_INSTANCE, (fixer, compound, versionIn) -> {
            DataFixesManager.processItemStack(fixer, compound.getCompoundTag("tag"), versionIn, InventoryAkashicGoggles.NBT_INVENTORY);
            return compound;
        });
    }

    @Mod.EventHandler
    @SideOnly(Side.CLIENT)
    static void loadCompleteClient(@Nonnull final FMLLoadCompleteEvent event) {
        CompatHandler.loadCompleteClient();
        createMetadataTranslated(Objects.requireNonNull(Loader.instance().activeModContainer()));
    }

    @Mod.EventHandler
    static void serverStarting(@Nonnull final FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandAkashicGoggles());
    }

    // -------------
    // Mod container
    // -------------

    @SideOnly(Side.CLIENT)
    private static void createMetadataTranslated(@Nonnull final ModContainer mod) {
        ReflectionHelper.setPrivateValue(FMLModContainer.class, (FMLModContainer)mod, ModContainer.Disableable.NEVER, "disableability");
        @Nonnull final String creditsKey = mod.getMetadata().credits, descKey = mod.getMetadata().description;
        registerResourceListener(VanillaResourceType.LANGUAGES, manager -> {
            mod.getMetadata().credits = I18n.format(creditsKey).replace("\\n", "\n");
            mod.getMetadata().description = I18n.format(descKey);
        });
    }

    @SideOnly(Side.CLIENT)
    public static void registerResourceListener(@Nonnull final IResourceType type, @Nonnull final IResourceManagerReloadListener listener) {
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener((ISelectiveResourceReloadListener)(manager, condition) -> {
            if(condition.test(type)) listener.onResourceManagerReload(manager);
        });
    }

    // ----------
    // Registries
    // ----------

    @Nonnull public static final SoundEvent
            ITEM_GOGGLES_EMPTY = new SoundEvent(new ResourceLocation(Tags.MOD_ID, "item.empty")),
            ITEM_GOGGLES_EQUIP = new SoundEvent(new ResourceLocation(Tags.MOD_ID, "item.equip")),
            ITEM_GOGGLES_INSERT = new SoundEvent(new ResourceLocation(Tags.MOD_ID, "item.insert"));

    @Nullable public static Item GOGGLES;
    @Nonnull public static final CreativeTabs TAB = new CreativeTabs(Tags.MOD_ID + ".tab") {
        @Nonnull
        @SideOnly(Side.CLIENT)
        @Override
        public ItemStack createIcon() { return GOGGLES.getDefaultInstance(); }
    };

    @SubscribeEvent
    static void registerItems(@Nonnull final RegistryEvent.Register<Item> event) {
        GOGGLES = new ItemAkashicGoggles();
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    static void registerModels(@Nonnull final ModelRegistryEvent event) {
        ModelLoaderRegistry.registerLoader(ModelHeadwear.Loader.INSTANCE);
    }

    @SubscribeEvent
    static void registerRecipes(@Nonnull final RegistryEvent.Register<IRecipe> event) {
        RecipeHandler.addShapedRecipe(new ItemStack(GOGGLES), "SSS", "GBG", "WWW", 'S', "string", 'G', RecipeHandler.compound("blockGlass", "paneGlass"), 'B', OreDictionary.doesOreNameExist("bookshelf") ? "bookshelf" : Blocks.BOOKSHELF, 'W', Items.BOOK);
        new RecipeAkashicCombine();
    }

    @SubscribeEvent
    static void registerSounds(@Nonnull final RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().registerAll(ITEM_GOGGLES_EMPTY.setRegistryName(Tags.MOD_ID, "item.empty"), ITEM_GOGGLES_EQUIP.setRegistryName(Tags.MOD_ID, "item.equip"), ITEM_GOGGLES_INSERT.setRegistryName(Tags.MOD_ID, "item.insert"));
    }
}

package zone.moddev.mc.skysgrassslabs.integrationtest;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.advancements.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.network.*;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/** Checks the real server advancement manager and recipe book, not just the JSON files. */
public final class RecipeBookChecks {
    private static final Set<String> MODS = Collections.singleton("skysgrassslabs");

    public static void run(WorldServer world) {
        SortedMap<String, JsonObject> entries = new TreeMap<>();
        List<IRecipe> recipes = new ArrayList<>();
        for (IRecipe recipe : CraftingManager.REGISTRY) {
            ResourceLocation id = recipe.getRegistryName();
            if (id == null || !MODS.contains(id.getNamespace()) || recipe.isDynamic()) continue;
            Ingredient first = recipe.getIngredients().stream().filter(i -> i != Ingredient.EMPTY)
                    .findFirst().orElseThrow(() -> new AssertionError("No book ingredients: " + id));
            JsonObject entry = new JsonObject();
            entry.addProperty("recipe", id.toString());
            entry.addProperty("owner", owner(id));
            JsonArray ingredients = new JsonArray();
            Set<String> seen = new HashSet<>();
            for (ItemStack stack : first.getMatchingStacks()) {
                if (stack.isEmpty()) continue;
                JsonObject predicate = new JsonObject();
                predicate.addProperty("item", stack.getItem().getRegistryName().toString());
                if (stack.getMetadata() != 32767) predicate.addProperty("data", stack.getMetadata());
                if (seen.add(predicate.toString())) ingredients.add(predicate);
            }
            require(ingredients.size() > 0, "Empty book ingredients: " + id);
            entry.add("ingredients", ingredients);
            entries.put(id.toString(), entry);
            recipes.add(recipe);
        }
        File snapshot = new File("recipe-book-snapshot.json");
        try (Writer out = new OutputStreamWriter(new FileOutputStream(snapshot), StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(entries.values(), out);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
        if (Boolean.getBoolean("skysbuildingpieces.snapshotRecipeBook")) {
            System.out.println("RECIPE_BOOK_SNAPSHOT_ONLY recipes=" + recipes.size());
            return;
        }
        EntityPlayerMP player = new EntityPlayerMP(world.getMinecraftServer(), world,
                new GameProfile(UUID.fromString("c64fa100-ff4c-4615-bcbd-90a552320a82"), "RecipeBookProbe"),
                new PlayerInteractionManager(world));
        List<Packet<?>> sentPackets = new ArrayList<>();
        NetworkManager network = new NetworkManager(EnumPacketDirection.SERVERBOUND) {
            private final io.netty.channel.Channel testChannel = new io.netty.channel.embedded.EmbeddedChannel();
            @Override public io.netty.channel.Channel channel() { return testChannel; }
            @Override public void sendPacket(Packet<?> packet) { sentPackets.add(packet); }
        };
        player.connection = new NetHandlerPlayServer(world.getMinecraftServer(), network, player);
        require(!world.getMinecraftServer().getAdvancementManager().hasErrored(), "Advancement parsing failed");
        int checks = 0;
        List<IRecipe> verified = new ArrayList<>();
        for (IRecipe recipe : recipes) {
            ResourceLocation id = recipe.getRegistryName();
            verified.add(recipe);
            Advancement advancement = world.getMinecraftServer().getAdvancementManager()
                    .getAdvancement(new ResourceLocation(owner(id), "recipes/" + id.getPath()));
            require(advancement != null, "Missing recipe advancement: " + id);
            require(!recipe.getRecipeOutput().isEmpty(), "Missing book result: " + id);
            reset(player, recipe, advancement);
            player.inventory.clear();
            player.inventory.setInventorySlotContents(0, new ItemStack(Items.APPLE));
            CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.inventory);
            require(!player.getRecipeBook().isUnlocked(recipe), "Unrelated item unlocked " + id);
            Ingredient first = recipe.getIngredients().stream().filter(i -> i != Ingredient.EMPTY).findFirst().get();
            ItemStack input = first.getMatchingStacks()[0].copy();
            player.inventory.setInventorySlotContents(0, input);
            CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.inventory);
            require(player.getRecipeBook().isUnlocked(recipe), "Ingredient did not unlock " + id);
            if (!id.getNamespace().equals("skysgrassslabs") || !id.getPath().equals("turf")) {
                reset(player, recipe, advancement);
                player.inventory.clear();
                FMLCommonHandler.instance().bus().post(new PlayerEvent.ItemCraftedEvent(player, input, new net.minecraft.inventory.InventoryCrafting(new net.minecraft.inventory.Container() {
                    @Override public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer ignored) { return true; }
                }, 2, 2)));
                require(player.getRecipeBook().isUnlocked(recipe), "Crafted ingredient did not unlock " + id);
            }
            checks++;
        }
        player.getAdvancements().save();
        net.minecraft.nbt.NBTTagCompound savedBook = player.getRecipeBook().write();
        net.minecraft.stats.RecipeBookServer reloaded = new net.minecraft.stats.RecipeBookServer();
        reloaded.read(savedBook);
        for (IRecipe recipe : verified) require(reloaded.isUnlocked(recipe), "Book reload lost " + recipe.getRegistryName());
        require(sentPackets.stream().anyMatch(packet -> packet instanceof net.minecraft.network.play.server.SPacketRecipeBook),
                "Unlocks did not send recipe-book packets");
        player.getAdvancements().dispose();
        network.channel().close();
        System.out.println("GRASS_RECIPE_BOOK_PASS recipes=" + checks + " inventory=true crafting=true reload=true");
    }

    private static void reset(EntityPlayerMP player, IRecipe recipe, Advancement advancement) {
        player.resetRecipes(Collections.singletonList(recipe));
        for (String criterion : advancement.getCriteria().keySet()) {
            player.getAdvancements().revokeCriterion(advancement, criterion);
        }
    }

    private static String owner(ResourceLocation recipe) {
        return recipe.getNamespace();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private RecipeBookChecks() { }
}

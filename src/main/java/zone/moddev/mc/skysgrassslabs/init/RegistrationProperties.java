package zone.moddev.mc.skysgrassslabs.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;

/** Assigns stable registry identities before NeoForge constructs content. */
public final class RegistrationProperties {
    public static BlockBehaviour.Properties block(BlockBehaviour.Properties properties,
            String path) {
        return properties.setId(ResourceKey.create(Registries.BLOCK, id(path)));
    }

    public static Item.Properties item(Item.Properties properties, String path) {
        return properties.setId(ResourceKey.create(Registries.ITEM, id(path)))
                .useBlockDescriptionPrefix();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SkysGrassSlabs.MOD_ID, path);
    }

    private RegistrationProperties() {
    }
}

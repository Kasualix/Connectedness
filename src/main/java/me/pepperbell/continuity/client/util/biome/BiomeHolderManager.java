package me.pepperbell.continuity.client.util.biome;

import java.util.Map;

import org.jetbrains.annotations.ApiStatus;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.pepperbell.continuity.client.util.ClientJoinEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.MinecraftForge;

public final class BiomeHolderManager {
	private static final Map<Identifier, BiomeHolder> HOLDER_CACHE = new Object2ObjectOpenHashMap<>();
	private static DynamicRegistryManager registryManager;

	public static BiomeHolder getOrCreateHolder(Identifier id) {
		BiomeHolder holder = HOLDER_CACHE.get(id);
		if (holder == null) {
			holder = new BiomeHolder(id);
			HOLDER_CACHE.put(id, holder);
		}
		return holder;
	}

	@ApiStatus.Internal
	public static void init() {
		MinecraftForge.EVENT_BUS.addListener(BiomeHolderManager::worldJoin);
	}
	
	public static void worldJoin(ClientJoinEvent event) {
		registryManager = event.getHandler().getRegistryManager();
		refreshHolders();
	}

	public static void refreshHolders() {
		if (registryManager == null) {
			return;
		}

		Map<Identifier, Identifier> compressedIdMap = new Object2ObjectOpenHashMap<>();
		Registry<Biome> biomeRegistry = registryManager.get(Registry.BIOME_KEY);
		for (Identifier id : biomeRegistry.getIds()) {
			String path = id.getPath();
			String compressedPath = path.replace("_", "");
			if (!path.equals(compressedPath)) {
				Identifier compressedId = new Identifier(id.getNamespace(), compressedPath);
				if (!biomeRegistry.containsId(compressedId)) {
					compressedIdMap.put(compressedId, id);
				}
			}
		}

		for (BiomeHolder holder : HOLDER_CACHE.values()) {
			holder.refresh(biomeRegistry, compressedIdMap);
		}
	}

	@ApiStatus.Internal
	public static void clearCache() {
		HOLDER_CACHE.clear();
	}
}

package me.pepperbell.continuity.client;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.pepperbell.continuity.api.client.CTMLoader;
import me.pepperbell.continuity.api.client.CTMLoaderRegistry;
import me.pepperbell.continuity.api.client.CTMProperties;
import me.pepperbell.continuity.api.client.CTMPropertiesFactory;
import me.pepperbell.continuity.api.client.QuadProcessorFactory;
import me.pepperbell.continuity.client.processor.CompactCTMQuadProcessor;
import me.pepperbell.continuity.client.processor.HorizontalQuadProcessor;
import me.pepperbell.continuity.client.processor.HorizontalVerticalQuadProcessor;
import me.pepperbell.continuity.client.processor.ProcessingDataKeys;
import me.pepperbell.continuity.client.processor.TopQuadProcessor;
import me.pepperbell.continuity.client.processor.VerticalHorizontalQuadProcessor;
import me.pepperbell.continuity.client.processor.VerticalQuadProcessor;
import me.pepperbell.continuity.client.processor.overlay.SimpleOverlayQuadProcessor;
import me.pepperbell.continuity.client.processor.overlay.StandardOverlayQuadProcessor;
import me.pepperbell.continuity.client.processor.simple.CTMSpriteProvider;
import me.pepperbell.continuity.client.processor.simple.FixedSpriteProvider;
import me.pepperbell.continuity.client.processor.simple.RandomSpriteProvider;
import me.pepperbell.continuity.client.processor.simple.RepeatSpriteProvider;
import me.pepperbell.continuity.client.processor.simple.SimpleQuadProcessor;
import me.pepperbell.continuity.client.properties.BaseCTMProperties;
import me.pepperbell.continuity.client.properties.CompactConnectingCTMProperties;
import me.pepperbell.continuity.client.properties.PropertiesParsingHelper;
import me.pepperbell.continuity.client.properties.RandomCTMProperties;
import me.pepperbell.continuity.client.properties.RepeatCTMProperties;
import me.pepperbell.continuity.client.properties.StandardConnectingCTMProperties;
import me.pepperbell.continuity.client.properties.TileAmountValidator;
import me.pepperbell.continuity.client.properties.overlay.BaseOverlayCTMProperties;
import me.pepperbell.continuity.client.properties.overlay.RandomOverlayCTMProperties;
import me.pepperbell.continuity.client.properties.overlay.RepeatOverlayCTMProperties;
import me.pepperbell.continuity.client.properties.overlay.StandardConnectingOverlayCTMProperties;
import me.pepperbell.continuity.client.properties.overlay.StandardOverlayCTMProperties;
import me.pepperbell.continuity.client.resource.CustomBlockLayers;
import me.pepperbell.continuity.client.util.RenderUtil;
import me.pepperbell.continuity.client.util.biome.BiomeHolderManager;
import me.pepperbell.continuity.client.util.biome.BiomeRetriever;
import me.pepperbell.continuity.impl.client.ProcessingDataKeyRegistryImpl;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.metadata.PackResourceMetadata;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkConstants;
import net.minecraftforge.resource.PathPackResources;

@Mod(ContinuityClient.ID)
public class ContinuityClient {
	public static final String ID = "connectedness";
	public static final String NAME = "Connectedness";
	public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

	public ContinuityClient() {		
		ProcessingDataKeyRegistryImpl.INSTANCE.init();
		BiomeHolderManager.init();
		BiomeRetriever.init();
		ProcessingDataKeys.init();
		RenderUtil.ReloadListener.init();
		CustomBlockLayers.ReloadListener.init();
		
		FMLJavaModLoadingContext.get().getModEventBus().register(this);
		
		ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));

		// Regular methods

		/*
		"ctm" "glass"
		"ctm_compact"
		"horizontal" "bookshelf"
		"vertical"
		"horizontal+vertical" "h+v"
		"vertical+horizontal" "v+h"
		"top"
		"random"
		"repeat"
		"fixed"
		 */

		CTMLoaderRegistry registry = CTMLoaderRegistry.get();
		CTMLoader<?> loader;

		loader = createLoader(
				StandardConnectingCTMProperties::new,
				new TileAmountValidator.AtLeast<>(47),
				new SimpleQuadProcessor.Factory<>(new CTMSpriteProvider.Factory(true))
		);
		registry.registerLoader("ctm", loader);
		registry.registerLoader("glass", loader);

		loader = createLoader(
				CompactConnectingCTMProperties::new,
				new TileAmountValidator.AtLeast<>(5),
				new CompactCTMQuadProcessor.Factory()
		);
		registry.registerLoader("ctm_compact", loader);

		loader = createLoader(
				StandardConnectingCTMProperties::new,
				new TileAmountValidator.Exactly<>(4),
				new HorizontalQuadProcessor.Factory()
		);
		registry.registerLoader("horizontal", loader);
		registry.registerLoader("bookshelf", loader);

		loader = createLoader(
				StandardConnectingCTMProperties::new,
				new TileAmountValidator.Exactly<>(4),
				new VerticalQuadProcessor.Factory()
		);
		registry.registerLoader("vertical", loader);

		loader = createLoader(
				StandardConnectingCTMProperties::new,
				new TileAmountValidator.Exactly<>(7),
				new HorizontalVerticalQuadProcessor.Factory()
		);
		registry.registerLoader("horizontal+vertical", loader);
		registry.registerLoader("h+v", loader);

		loader = createLoader(
				StandardConnectingCTMProperties::new,
				new TileAmountValidator.Exactly<>(7),
				new VerticalHorizontalQuadProcessor.Factory()
		);
		registry.registerLoader("vertical+horizontal", loader);
		registry.registerLoader("v+h", loader);

		loader = createLoader(
				StandardConnectingCTMProperties::new,
				new TileAmountValidator.Exactly<>(1),
				new TopQuadProcessor.Factory()
		);
		registry.registerLoader("top", loader);

		loader = createLoader(
				RandomCTMProperties::new,
				new SimpleQuadProcessor.Factory<>(new RandomSpriteProvider.Factory())
		);
		registry.registerLoader("random", loader);

		loader = createLoader(
				RepeatCTMProperties::new,
				new RepeatCTMProperties.Validator<>(),
				new SimpleQuadProcessor.Factory<>(new RepeatSpriteProvider.Factory())
		);
		registry.registerLoader("repeat", loader);

		loader = createLoader(
				BaseCTMProperties::new,
				new TileAmountValidator.Exactly<>(1),
				new SimpleQuadProcessor.Factory<>(new FixedSpriteProvider.Factory())
		);
		registry.registerLoader("fixed", loader);

		// Overlay methods

		/*
		"overlay"
		"overlay_ctm"
		"overlay_random"
		"overlay_repeat"
		"overlay_fixed"
		 */

		loader = createLoader(
				StandardOverlayCTMProperties::new,
				new TileAmountValidator.AtLeast<>(17),
				new StandardOverlayQuadProcessor.Factory()
		);
		registry.registerLoader("overlay", loader);

		loader = createLoader(
				StandardConnectingOverlayCTMProperties::new,
				new TileAmountValidator.AtLeast<>(47),
				new SimpleOverlayQuadProcessor.Factory<>(new CTMSpriteProvider.Factory(false))
		);
		registry.registerLoader("overlay_ctm", loader);

		loader = createLoader(
				RandomOverlayCTMProperties::new,
				new SimpleOverlayQuadProcessor.Factory<>(new RandomSpriteProvider.Factory())
		);
		registry.registerLoader("overlay_random", loader);

		loader = createLoader(
				RepeatOverlayCTMProperties::new,
				new RepeatCTMProperties.Validator<>(),
				new SimpleOverlayQuadProcessor.Factory<>(new RepeatSpriteProvider.Factory())
		);
		registry.registerLoader("overlay_repeat", loader);

		loader = createLoader(
				BaseOverlayCTMProperties::new,
				new TileAmountValidator.Exactly<>(1),
				new SimpleOverlayQuadProcessor.Factory<>(new FixedSpriteProvider.Factory())
		);
		registry.registerLoader("overlay_fixed", loader);
	}

	private static <T extends BaseCTMProperties> CTMLoader<T> createLoader(CTMPropertiesFactory<T> propertiesFactory, TileAmountValidator<T> validator, QuadProcessorFactory<T> processorFactory) {
		return CTMLoader.of(wrapWithOptifineOnlyCheck(TileAmountValidator.wrapFactory(BaseCTMProperties.wrapFactory(propertiesFactory), validator)), processorFactory);
	}

	private static <T extends BaseCTMProperties> CTMLoader<T> createLoader(CTMPropertiesFactory<T> propertiesFactory, QuadProcessorFactory<T> processorFactory) {
		return CTMLoader.of(wrapWithOptifineOnlyCheck(BaseCTMProperties.wrapFactory(propertiesFactory)), processorFactory);
	}

	private static <T extends CTMProperties> CTMPropertiesFactory<T> wrapWithOptifineOnlyCheck(CTMPropertiesFactory<T> factory) {
		return (properties, id, packName, packPriority, method) -> {
			if (PropertiesParsingHelper.parseOptifineOnly(properties, id)) {
				return null;
			}
			return factory.createProperties(properties, id, packName, packPriority, method);
		};
	}

	public static Identifier asId(String path) {
		return new Identifier(ID, path);
	}
	
	@SubscribeEvent
	public void addDefaultPack(AddPackFindersEvent event) {
		try {
			if (event.getPackType() == ResourceType.CLIENT_RESOURCES) {
				var resourcePath = ModList.get().getModFileById(ID).getFile().findResource("resourcepacks/default");
				var pack = new PathPackResources(ModList.get().getModFileById(ID).getFile().getFileName() + ":" + resourcePath, resourcePath);
				var metadataSection = pack.parseMetadata(PackResourceMetadata.READER);
				if (metadataSection != null) {
					event.addRepositorySource((packConsumer, packConstructor) ->
							packConsumer.accept(packConstructor.create(
									"builtin/default_ctm_resources", Text.literal("Default CTM"), false,
									() -> pack, metadataSection, ResourcePackProfile.InsertionPosition.TOP, ResourcePackSource.PACK_SOURCE_BUILTIN, false)));
				}
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	@SubscribeEvent
	public void addGlassPack(AddPackFindersEvent event) {
		try {
			if (event.getPackType() == ResourceType.CLIENT_RESOURCES) {
				var resourcePath = ModList.get().getModFileById(ID).getFile().findResource("resourcepacks/glass_pane_culling_fix");
				var pack = new PathPackResources(ModList.get().getModFileById(ID).getFile().getFileName() + ":" + resourcePath, resourcePath);
				var metadataSection = pack.parseMetadata(PackResourceMetadata.READER);
				if (metadataSection != null) {
					event.addRepositorySource((packConsumer, packConstructor) ->
							packConsumer.accept(packConstructor.create(
									"builtin/glass_pane_fix_resources", Text.literal("Glass pane culling fix"), false,
									() -> pack, metadataSection, ResourcePackProfile.InsertionPosition.TOP, ResourcePackSource.PACK_SOURCE_BUILTIN, false)));
				}
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	
}

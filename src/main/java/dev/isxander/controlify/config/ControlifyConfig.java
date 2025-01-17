package dev.isxander.controlify.config;

import com.google.gson.*;
import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.controller.input.mapping.MappingEntry;
import dev.isxander.controlify.controller.input.mapping.MappingEntryTypeAdapter;
import dev.isxander.controlify.utils.DebugLog;
import dev.isxander.controlify.utils.CUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ControlifyConfig {
    public static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("controlify.json");

    public static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .setPrettyPrinting()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeHierarchyAdapter(Class.class, new TypeAdapters.ClassTypeAdapter())
            .registerTypeHierarchyAdapter(Version.class, new TypeAdapters.VersionTypeAdapter())
            .registerTypeHierarchyAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
            .registerTypeAdapter(MappingEntry.class, new MappingEntryTypeAdapter()) // not hierarchy!! otherwise stackoverflow when using default gson record deserializer
            .create();

    private final Controlify controlify;

    private String currentControllerUid;
    private JsonObject controllerData = new JsonObject();
    private GlobalSettings globalSettings = new GlobalSettings();
    private boolean firstLaunch;
    private Version lastSeenVersion = null;
    private final Version zeroVersion;

    private boolean dirty;

    public ControlifyConfig(Controlify controlify) {
        this.controlify = controlify;
        try {
            zeroVersion = Version.parse("0.0.0");
        } catch (VersionParsingException e) {
            throw new RuntimeException(e);
        }
    }

    public void save() {
        CUtil.LOGGER.info("Saving Controlify config...");

        try {
            Files.deleteIfExists(CONFIG_PATH);
            Files.writeString(CONFIG_PATH, GSON.toJson(generateConfig()), StandardOpenOption.CREATE_NEW, StandardOpenOption.TRUNCATE_EXISTING);
            dirty = false;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save config!", e);
        }
    }

    public void load() {
        CUtil.LOGGER.info("Loading Controlify config...");

        if (!Files.exists(CONFIG_PATH)) {
            if (lastSeenVersion == null) {
                firstLaunch = true;
                lastSeenVersion = zeroVersion;
                setDirty();
            }
            save();
            return;
        }

        try {
            applyConfig(GSON.fromJson(Files.readString(CONFIG_PATH), JsonObject.class));
        } catch (Exception e) {
            CUtil.LOGGER.error("Failed to load Controlify config!", e);
            lastSeenVersion = zeroVersion;
        }

        if (dirty) {
            DebugLog.log("Config was dirty after load, saving...");
            save();
        }
    }

    private JsonObject generateConfig() {
        JsonObject config = new JsonObject();

        config.addProperty("last_seen_version", CUtil.VERSION.getFriendlyString());

        JsonObject newControllerData = controllerData.deepCopy(); // we use the old config, so we don't lose disconnected controller data

        controlify.getControllerManager().ifPresent(controllerManager -> {
            for (ControllerEntity controller : controllerManager.getConnectedControllers()) {
                // `add` replaces if already existing
                newControllerData.add(controller.info().uid(), generateControllerConfig(controller));
            }
        });

        controllerData = newControllerData;
        config.addProperty("current_controller", currentControllerUid = controlify.getCurrentController().map(c -> c.info().uid()).orElse(null));
        config.add("controllers", controllerData);
        config.add("global", GSON.toJsonTree(globalSettings));

        return config;
    }

    private JsonObject generateControllerConfig(ControllerEntity controller) {
        JsonObject object = new JsonObject();
        JsonObject config = new JsonObject();
        controller.serializeToObject(config, GSON);

        object.add("config", config);
        object.add("bindings", controller.bindings().toJson());

        return object;
    }

    private void applyConfig(JsonObject object) throws VersionParsingException {
        if (lastSeenVersion == null) {
            boolean hasLastSeenVersion = object.has("last_seen_version");
            lastSeenVersion = hasLastSeenVersion ? Version.parse(object.get("last_seen_version").getAsString()) : Version.parse("0.0.0");
            if (!hasLastSeenVersion || lastSeenVersion.compareTo(CUtil.VERSION) < 0) {
                setDirty();
            }
        }

        globalSettings = GSON.fromJson(object.getAsJsonObject("global"), GlobalSettings.class);
        if (globalSettings == null) {
            globalSettings = new GlobalSettings();
            setDirty();
        }

        JsonObject controllers = object.getAsJsonObject("controllers");
        if (controllers != null) {
            this.controllerData = controllers;
            if (controlify.getControllerManager().isPresent()) {
                for (var controller : controlify.getControllerManager().get().getConnectedControllers()) {
                    loadOrCreateControllerData(controller);
                }
            }
        } else {
            setDirty();
        }

        if (object.has("current_controller")) {
            JsonElement element = object.get("current_controller");
            currentControllerUid = element.isJsonNull() ? null : element.getAsString();
        } else {
            currentControllerUid = controlify.getCurrentController().map(c -> c.info().uid()).orElse(null);
            setDirty();
        }
    }

    public boolean loadOrCreateControllerData(ControllerEntity controller) {
        var uid = controller.info().uid();
        if (controllerData.has(uid)) {
            DebugLog.log("Loading controller data for " + uid);
            applyControllerConfig(controller, controllerData.getAsJsonObject(uid));
            return true;
        } else {
            DebugLog.log("New controller found, setting config dirty ({})", uid);
            setDirty();
            return false;
        }
    }

    private void applyControllerConfig(ControllerEntity controller, JsonObject object) {
        try {
            dirty |= !controller.bindings().fromJson(object.getAsJsonObject("bindings"));
            controller.deserializeFromObject(object.getAsJsonObject("config"), GSON);
        } catch (Exception e) {
            CUtil.LOGGER.error("Failed to load controller data for " + controller.info().uid() + ". Resetting to default!", e);
            controller.resetToDefaultConfig();
            save();
        }
    }

    public void setDirty() {
        dirty = true;
    }

    public void saveIfDirty() {
        if (dirty) {
            DebugLog.log("Config is dirty. Saving...");
            save();
        }
    }

    public GlobalSettings globalSettings() {
        return globalSettings;
    }

    public boolean isFirstLaunch() {
        return firstLaunch;
    }

    public boolean isLastSeenVersionLessThan(Version version) {
        return lastSeenVersion.compareTo(version) < 0;
    }

    public boolean isLastSeenVersionLessThan(String version) {
        try {
            return isLastSeenVersionLessThan(Version.parse(version));
        } catch (VersionParsingException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public String currentControllerUid() {
        return currentControllerUid;
    }
}

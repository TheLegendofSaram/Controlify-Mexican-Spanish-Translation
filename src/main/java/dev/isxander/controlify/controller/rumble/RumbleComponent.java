package dev.isxander.controlify.controller.rumble;

import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.controller.ConfigClass;
import dev.isxander.controlify.controller.ConfigHolder;
import dev.isxander.controlify.controller.ECSComponent;
import dev.isxander.controlify.controller.IConfig;
import dev.isxander.controlify.controller.impl.ConfigImpl;
import dev.isxander.controlify.rumble.RumbleManager;
import dev.isxander.controlify.rumble.RumbleSource;
import dev.isxander.controlify.rumble.RumbleState;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

public class RumbleComponent implements ECSComponent, ConfigHolder<RumbleComponent.Config> {
    public static final ResourceLocation ID = Controlify.id("rumble");

    private RumbleState state = null;
    private final IConfig<Config> config;
    private final RumbleManager rumbleManager;

    public RumbleComponent() {
        this.config = new ConfigImpl<>(Config::new, Config.class);
        this.rumbleManager = new RumbleManager(this);
    }

    public void queueRumble(RumbleState state) {
        this.state = state;
    }

    public Optional<RumbleState> consumeRumble() {
        RumbleState state = this.state;
        this.state = null;
        return Optional.ofNullable(state);
    }

    public RumbleManager rumbleManager() {
        return this.rumbleManager;
    }

    @Override
    public IConfig<Config> config() {
        return this.config;
    }

    public static class Config implements ConfigClass {
        public boolean enabled = true;

        public Map<ResourceLocation, Float> vibrationStrengths = RumbleSource.getDefaultMap();

        public RumbleState applyRumbleStrength(RumbleState state, RumbleSource source) {
            float strength = this.vibrationStrengths.getOrDefault(source.id(), 1f);
            return state.mul(strength);
        }
    }
}

package dev.isxander.controlify.screenop.compat.vanilla;

import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.screenop.ScreenProcessor;
import dev.isxander.controlify.screenop.ComponentProcessor;
import dev.isxander.controlify.mixins.feature.screenop.vanilla.JoinMultiplayerScreenAccessor;

public class ServerSelectionListEntryComponentProcessor implements ComponentProcessor {
    @Override
    public boolean overrideControllerButtons(ScreenProcessor<?> screen, ControllerEntity controller) {
        if (controller.bindings().GUI_PRESS.justPressed()) {
            screen.screen.setFocused(((JoinMultiplayerScreenAccessor) screen.screen).getSelectButton());
            return true;
        }

        return false;
    }
}

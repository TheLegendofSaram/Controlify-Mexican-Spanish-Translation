package dev.isxander.controlify.screenop.compat.vanilla;

import dev.isxander.controlify.api.buttonguide.ButtonGuideApi;
import dev.isxander.controlify.api.buttonguide.ButtonGuidePredicate;
import dev.isxander.controlify.api.buttonguide.ButtonRenderPosition;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.mixins.feature.screenop.vanilla.PauseScreenAccessor;
import dev.isxander.controlify.screenop.ScreenProcessor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.PauseScreen;

import java.util.function.Supplier;

public class PauseScreenProcessor extends ScreenProcessor<PauseScreen> {
    private final Supplier<Button> disconnectButtonSupplier;

    public PauseScreenProcessor(PauseScreen screen, Supplier<Button> disconnectButtonSupplier) {
        super(screen);
        this.disconnectButtonSupplier = disconnectButtonSupplier;
    }

    @Override
    protected void handleButtons(ControllerEntity controller) {
        super.handleButtons(controller);

        if (controller.bindings().GUI_ABSTRACT_ACTION_1.justPressed()) {
            minecraft.setScreen(new OptionsScreen(screen, minecraft.options));
        }
        if (controller.bindings().GUI_ABSTRACT_ACTION_2.justPressed()) {
            screen.setFocused(disconnectButtonSupplier.get());
        }
    }

    @Override
    public void onWidgetRebuild() {
        super.onWidgetRebuild();

        if (((PauseScreenAccessor) screen).getShowPauseMenu()) {
            ButtonGuideApi.addGuideToButtonBuiltin(
                    (AbstractButton) getWidget("menu.returnToGame").orElseThrow(),
                    bindings -> bindings.GUI_BACK,
                    ButtonRenderPosition.TEXT,
                    ButtonGuidePredicate.ALWAYS
            );
            ButtonGuideApi.addGuideToButtonBuiltin(
                    (AbstractButton) getWidget("menu.options").orElseThrow(),
                    bindings -> bindings.GUI_ABSTRACT_ACTION_1,
                    ButtonRenderPosition.TEXT,
                    ButtonGuidePredicate.ALWAYS
            );
            ButtonGuideApi.addGuideToButtonBuiltin(
                    disconnectButtonSupplier.get(),
                    bindings -> disconnectButtonSupplier.get().isFocused() ? bindings.GUI_PRESS : bindings.GUI_ABSTRACT_ACTION_2,
                    ButtonRenderPosition.TEXT,
                    ButtonGuidePredicate.ALWAYS
            );
        }
    }
}

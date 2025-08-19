package net.plat12.hellspawn.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.plat12.hellspawn.util.ModUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(CreateWorldScreen.WorldTab.class)
public class CreateWorldScreenMixin {

    @Final
    @Shadow
    private CreateWorldScreen this$0; // Reference to outer CreateWorldScreen instance

    private CycleButton<ResourceKey<Level>> spawnDimensionButton;

    /**
     * Inject after the customize button is added to create and add our spawn dimension button
     */
    @Inject(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
            ordinal = 1, shift = At.Shift.AFTER)
    )
    private void addSpawnDimensionButton(CreateWorldScreen createWorldScreen, CallbackInfo ci,
                                         @Local GridLayout.RowHelper gridlayout$rowhelper) {

        // Get available dimensions from the UI state
        List<ResourceKey<Level>> dimensions = ModUtils.getAllDimensions(this$0.getUiState());

        // Set default selected dimension if none is selected
        if (ModUtils.selectedDimension == null && !dimensions.isEmpty()) {
            ModUtils.selectedDimension = dimensions.getFirst();
        }

        // Create the spawn dimension cycle button
        this.spawnDimensionButton = CycleButton.builder(ModUtils::getDimensionName)
                .withValues(dimensions)
                .create(
                        0, 0, 150, 20,
                        Component.translatable("hellspawn.gui.spawn_dimension"),
                        (button, dimension) -> {
                            ModUtils.selectedDimension = dimension;
                            updateSpawnDimensionTooltip();
                        }
                );

        // Set initial value and tooltip
        if (ModUtils.selectedDimension != null && dimensions.contains(ModUtils.selectedDimension)) {
            this.spawnDimensionButton.setValue(ModUtils.selectedDimension);
        }
        updateSpawnDimensionTooltip();

        // Add our button to the same row as the customize button
        gridlayout$rowhelper.addChild(this.spawnDimensionButton);

        // Add listener to update button when UI state changes
        this$0.getUiState().addListener(uiState -> {
            updateSpawnDimensionButtonState();
        });
    }

    /**
     * Update the spawn dimension button tooltip
     */
    private void updateSpawnDimensionTooltip() {
        if (this.spawnDimensionButton != null && ModUtils.selectedDimension != null) {
            this.spawnDimensionButton.setTooltip(Tooltip.create(ModUtils.getDimensionDescription(ModUtils.selectedDimension)));
        }
    }

    /**
     * Update the spawn dimension button state when UI changes
     */
    private void updateSpawnDimensionButtonState() {
        if (this.spawnDimensionButton == null) return;

        List<ResourceKey<Level>> newDimensions = ModUtils.getAllDimensions(this$0.getUiState());
        ResourceKey<Level> currentSelection = ModUtils.selectedDimension;

        // Ensure current selection is still valid
        if (currentSelection == null || !newDimensions.contains(currentSelection)) {
            ModUtils.selectedDimension = newDimensions.isEmpty() ? null : newDimensions.getFirst();
        }

        // Update button state
        if (ModUtils.selectedDimension != null) {
            this.spawnDimensionButton.setValue(ModUtils.selectedDimension);
            updateSpawnDimensionTooltip();
        }

        // Enable/disable button based on debug mode and availability of dimensions
        this.spawnDimensionButton.active = !this$0.getUiState().isDebug() && !newDimensions.isEmpty();
    }
}
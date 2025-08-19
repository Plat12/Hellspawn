package net.plat12.hellspawn.mixin;

import com.llamalad7.mixinextras.sugar.Local;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(CreateWorldScreen.class)
public class CreateWorldScreenMixin {


    @Inject(method = "popScreen", at = @At("HEAD"))
    private void onScreenClosed(CallbackInfo ci) {
        ModUtils.selectedDimension = null;
    }

    @Mixin(CreateWorldScreen.WorldTab.class)
    public static class WorldTabMixin {

        @Final
        @Shadow
        CreateWorldScreen this$0;

        @Unique
        private CycleButton<ResourceKey<Level>> hellspawn$spawnDimensionButton;


        @Inject(method = "<init>", at = @At(value = "INVOKE",
                target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
                ordinal = 1, shift = At.Shift.AFTER)
        )
        private void addSpawnDimensionButton(CreateWorldScreen createWorldScreen, CallbackInfo ci,
                                             @Local GridLayout.RowHelper gridlayout$rowhelper) {

            List<ResourceKey<Level>> dimensions = ModUtils.getAllDimensions(this$0.getUiState());

            if (ModUtils.selectedDimension == null && !dimensions.isEmpty()) {
                ModUtils.selectedDimension = dimensions.getFirst();
            }

            this.hellspawn$spawnDimensionButton = CycleButton.builder(ModUtils::getDimensionName)
                    .withValues(dimensions)
                    .create(
                            0, 0, 150, 20,
                            Component.translatable("hellspawn.gui.spawn_dimension"),
                            (button, dimension) -> {
                                ModUtils.selectedDimension = dimension;
                                hellspawn$updateSpawnDimensionTooltip();
                            }
                    );

            if (ModUtils.selectedDimension != null && dimensions.contains(ModUtils.selectedDimension)) {
                this.hellspawn$spawnDimensionButton.setValue(ModUtils.selectedDimension);
            }
            hellspawn$updateSpawnDimensionTooltip();

            gridlayout$rowhelper.addChild(this.hellspawn$spawnDimensionButton);

            this$0.getUiState().addListener(uiState -> hellspawn$updateSpawnDimensionButtonState());
        }


        @Unique
        private void hellspawn$updateSpawnDimensionTooltip() {
            if (this.hellspawn$spawnDimensionButton != null && ModUtils.selectedDimension != null) {
                this.hellspawn$spawnDimensionButton.setTooltip(Tooltip.create(ModUtils.getDimensionDescription(ModUtils.selectedDimension)));
            }
        }


        @Unique
        private void hellspawn$updateSpawnDimensionButtonState() {
            if (this.hellspawn$spawnDimensionButton == null) return;

            List<ResourceKey<Level>> newDimensions = ModUtils.getAllDimensions(this$0.getUiState());
            ResourceKey<Level> currentSelection = ModUtils.selectedDimension;

            if (currentSelection == null || !newDimensions.contains(currentSelection)) {
                ModUtils.selectedDimension = newDimensions.isEmpty() ? null : newDimensions.getFirst();
            }

            if (ModUtils.selectedDimension != null) {
                this.hellspawn$spawnDimensionButton.setValue(ModUtils.selectedDimension);
                hellspawn$updateSpawnDimensionTooltip();
            }

            this.hellspawn$spawnDimensionButton.active = !this$0.getUiState().isDebug() && !newDimensions.isEmpty();
        }
    }
}
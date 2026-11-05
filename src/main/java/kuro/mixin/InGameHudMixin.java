package kuro.mixin;

import kuro.Config;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** FakeScoreboard: sunucunun sidebar'i yerine Chicken SMP panelini cizer. kuro.extra.mixins.json (require=0). */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
   // CustomCrosshair: vanilla artisini gizle (kendi imlecimiz Fx.hud'da cizilir)
   @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
   private void kuro$noCross(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter, CallbackInfo ci) {
      if (kuro.Kuro.on[kuro.Kuro.CROSSHAIR]) {
         ci.cancel();
      }
   }

   @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("HEAD"), cancellable = true, require = 0)
   private void kuro$sidebar(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
      if (Config.fsbOn) {
         ci.cancel();
      }
   }
}

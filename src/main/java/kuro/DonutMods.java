package kuro;

import java.util.HashSet;
import java.util.Set;
import kuro.ui.Toasts;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;

/**
 * DonutSMP odakli modullar: AutoReconnect, AutoSell, AutoCommand, AutoGap, ChatAlert, Tunneler.
 * Hepsi normal oyuncu girdisi yolundan calisir.
 */
public final class DonutMods {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   /** AutoLog / SpawnerProtect / VisualRange gibi bilerek sunucudan cikan moduller bunu true yapar: AutoReconnect geri girmez. */
   public static boolean noReconnect;
   private static long noReconnectAt;

   private DonutMods() {
   }

   public static void markLeft() {
      noReconnect = true;
      noReconnectAt = System.currentTimeMillis();
   }

   /** Kuro.onInitializeClient'tan bir kez cagrilir. */
   public static void init() {
      ClientTickEvents.END_CLIENT_TICK.register(c -> globalTick());
      ClientReceiveMessageEvents.GAME.register((m, overlay) -> {
         if (!overlay) {
            chat(m.getString());
         }
      });
      ClientReceiveMessageEvents.CHAT.register((m, signed, sender, params, ts) -> chat(m.getString()));
   }

   private static boolean usable() {
      return mc.player != null && mc.world != null && mc.currentScreen == null && mc.interactionManager != null && !Freecam.active;
   }

   private static void release(KeyBinding kb) {
      InputUtil.Key k = KeyBindingHelper.getBoundKeyOf(kb);
      long win = mc.getWindow().getHandle();
      boolean ph = k.getCategory() == InputUtil.Type.KEYSYM
         ? InputUtil.isKeyPressed(win, k.getCode())
         : k.getCategory() == InputUtil.Type.MOUSE && GLFW.glfwGetMouseButton(win, k.getCode()) == GLFW.GLFW_PRESS;
      kb.setPressed(ph);
   }

   /** Oyunda degilken de calisan tick (AutoReconnect). */
   private static void globalTick() {
      reconnect();
   }

   /** Oyundayken her tick (Kuro tick'inden). */
   public static void tick() {
      if (mc.player == null || mc.world == null) {
         return;
      }

      if (Kuro.on[Kuro.AUTOSELL]) {
         sell();
      }

      if (Kuro.on[Kuro.AUTOCOMMAND]) {
         command();
      }

      if (Kuro.on[Kuro.AUTOGAP]) {
         gap();
      }

      if (Kuro.on[Kuro.TUNNELER]) {
         tunnel();
      }
   }

   public static void onToggle(int i, boolean v) {
      if (i == Kuro.AUTORECONNECT) {
         reconnectAt = 0L;
      }

      if (i == Kuro.AUTOCOMMAND && v) {
         cmdCd = 40;
      }

      if (v) {
         return;
      }

      if (i == Kuro.AUTOSELL) {
         asScreen = null;
         asSkip.clear();
      } else if (i == Kuro.AUTOGAP) {
         gapOff();
      } else if (i == Kuro.TUNNELER) {
         tunnelOff();
      }
   }

   // ------------------------------------------------------------------ AutoReconnect
   private static ServerInfo lastInfo;
   private static long reconnectAt;

   private static void reconnect() {
      long now = System.currentTimeMillis();
      if (mc.player != null && mc.getCurrentServerEntry() != null) {
         lastInfo = mc.getCurrentServerEntry();
         if (noReconnect && now - noReconnectAt > 5000L) {
            noReconnect = false; // basariyla yeniden girdik
         }
      }

      if (!Kuro.on[Kuro.AUTORECONNECT] || !(mc.currentScreen instanceof DisconnectedScreen) || lastInfo == null || noReconnect) {
         if (!(mc.currentScreen instanceof DisconnectedScreen)) {
            reconnectAt = 0L;
         }

         return;
      }

      if (reconnectAt == 0L) {
         reconnectAt = now + Math.round(Config.arDelay * 1000.0F);
         Toasts.push("AutoReconnect", true, Math.round(Config.arDelay) + " sn sonra yeniden bağlanılacak");
         return;
      }

      if (now >= reconnectAt) {
         reconnectAt = 0L;
         ConnectScreen.connect(new MultiplayerScreen(new TitleScreen()), mc, ServerAddress.parse(lastInfo.address), lastInfo, false, null);
      }
   }

   // ------------------------------------------------------------------ AutoSell
   // /sell menusu (basliginda "sell" gecen sandik ekrani) acilinca envanterdeki esyalari sirayla icine tasir.
   private static Object asScreen;
   private static int asCd;
   private static int asLast = -1;
   private static int asTries;
   private static int asClose = -1;
   private static final Set<Integer> asSkip = new HashSet<>();

   private static boolean keep(ItemStack s) {
      if (s.isOf(Items.TOTEM_OF_UNDYING) || s.isOf(Items.ELYTRA)) {
         return true;
      }

      if (Config.asEnch >= 0.5F) {
         ItemEnchantmentsComponent ec = s.get(DataComponentTypes.ENCHANTMENTS);
         return ec != null && !ec.isEmpty();
      }

      return false;
   }

   private static void sell() {
      if (!(mc.currentScreen instanceof GenericContainerScreen gs) || mc.player == null || mc.interactionManager == null) {
         asScreen = null;
         return;
      }

      if (!gs.getTitle().getString().toLowerCase().contains("sell")) {
         return;
      }

      if (asScreen != gs) {
         asScreen = gs;
         asCd = 6;
         asLast = -1;
         asTries = 0;
         asClose = -1;
         asSkip.clear();
      }

      if (asCd-- > 0) {
         return;
      }

      GenericContainerScreenHandler h = gs.getScreenHandler();
      int n = h.getRows() * 9;
      int total = h.slots.size();
      int end = Config.asHotbar >= 0.5F ? total - 9 : total;
      for (int i = n; i < end; i++) {
         if (asSkip.contains(i)) {
            continue;
         }

         ItemStack s = h.getSlot(i).getStack();
         if (s.isEmpty() || keep(s)) {
            continue;
         }

         if (i == asLast) {
            if (++asTries > 3) {
               asSkip.add(i); // menu dolu olabilir; bu slotu atla
               continue;
            }
         } else {
            asLast = i;
            asTries = 0;
         }

         mc.interactionManager.clickSlot(h.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
         asCd = Math.round(Config.asDelay);
         return;
      }

      if (Config.asClose >= 0.5F) {
         if (asClose < 0) {
            asClose = 8;
         } else if (--asClose == 0) {
            mc.player.closeHandledScreen();
         }
      }
   }

   // ------------------------------------------------------------------ AutoCommand
   // Belirli aralikla bir komut gonderir (varsayilan: rtp). Komut kuro_settings.txt icindeki acCmd satirindan degistirilir.
   private static int cmdCd;

   private static void command() {
      if (mc.player == null || mc.getNetworkHandler() == null || !mc.player.isAlive()) {
         return;
      }

      if (cmdCd-- > 0) {
         return;
      }

      cmdCd = Math.round(Config.acInterval * 20.0F);
      String c = Config.acCmd == null ? "" : Config.acCmd.trim();
      if (c.startsWith("/")) {
         c = c.substring(1);
      }

      if (!c.isEmpty()) {
         mc.getNetworkHandler().sendChatCommand(c);
      }
   }

   // ------------------------------------------------------------------ AutoGap
   // Can esigin altina dusunce hotbar'daki altin elmayi yer.
   private static boolean gapEating;
   private static int gapRestore = -1;
   private static int gapWait;

   private static void gapOff() {
      if (gapEating && mc.player != null) {
         release(mc.options.useKey);
         if (gapRestore >= 0) {
            mc.player.getInventory().selectedSlot = gapRestore;
         }
      }

      gapEating = false;
      gapRestore = -1;
   }

   private static void gap() {
      ClientPlayerEntity p = mc.player;
      if (gapEating) {
         gapWait++;
         mc.options.useKey.setPressed(true);
         if (gapWait > 5 && !p.isUsingItem() || mc.currentScreen != null) {
            gapOff();
         }

         return;
      }

      if (!usable() || p.isUsingItem() || p.getHealth() > Config.agHp) {
         return;
      }

      int slot = -1;
      for (int i = 0; i < 9 && slot < 0; i++) {
         if (p.getInventory().getStack(i).isOf(Items.GOLDEN_APPLE)) {
            slot = i;
         }
      }

      for (int i = 0; i < 9 && slot < 0 && Config.agEnch >= 0.5F; i++) {
         if (p.getInventory().getStack(i).isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
            slot = i;
         }
      }

      if (slot < 0) {
         return;
      }

      gapRestore = p.getInventory().selectedSlot;
      p.getInventory().selectedSlot = slot;
      gapEating = true;
      gapWait = 0;
   }

   // ------------------------------------------------------------------ ChatAlert
   private static long lastAlert;

   private static void chat(String raw) {
      if (!Kuro.on[Kuro.CHATALERT] || mc.player == null || raw == null) {
         return;
      }

      String low = raw.toLowerCase();
      String me = mc.player.getGameProfile().getName().toLowerCase();
      if (low.startsWith("<" + me + ">") || low.startsWith(me + ":") || low.startsWith("[kuro]")) {
         return; // kendi mesajimiz
      }

      String reason = null;
      if (Config.caTpa >= 0.5F && (low.contains("teleport to you") || low.contains("tpaccept") || low.contains("tpa request") || low.contains("teleport request"))) {
         reason = "TPA isteği";
      } else if (Config.caMention >= 0.5F && low.contains(me)) {
         reason = "Adın geçti";
      } else if (Config.caKeywords != null && !Config.caKeywords.isBlank()) {
         for (String k : Config.caKeywords.split(",")) {
            String kw = k.trim().toLowerCase();
            if (!kw.isEmpty() && low.contains(kw)) {
               reason = "Anahtar kelime: " + kw;
               break;
            }
         }
      }

      long now = System.currentTimeMillis();
      if (reason == null || now - lastAlert < 1500L) {
         return;
      }

      lastAlert = now;
      String shown = raw.length() > 70 ? raw.substring(0, 70) + "..." : raw;
      Toasts.push("ChatAlert", true, reason + ": " + shown);
      if (Config.caSound >= 0.5F) {
         mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
      }
   }

   // ------------------------------------------------------------------ Tunneler
   // Baktigin ana yone (kuzey/guney/dogu/bati) 1x2 tunel acar: onundeki 2 blogu kirar, bos olunca ilerler.
   private static boolean mining;

   private static void tunnelOff() {
      if (mining && mc.interactionManager != null) {
         mc.interactionManager.cancelBlockBreaking();
      }

      mining = false;
      if (mc.player != null) {
         release(mc.options.forwardKey);
      }
   }

   private static void tunnelStop(String why) {
      Kuro.setOn(Kuro.TUNNELER, false);
      Mods.msg("Tunneler: " + why);
      Toasts.push("Tunneler", false, why);
   }

   private static void tunnel() {
      ClientPlayerEntity p = mc.player;
      if (!usable()) {
         release(mc.options.forwardKey);
         return;
      }

      p.setYaw(Math.round(p.getYaw() / 90.0F) * 90.0F);
      p.setPitch(0.0F);
      Direction d = p.getHorizontalFacing();
      BlockPos f1 = p.getBlockPos().offset(d);
      BlockPos f2 = f1.up();
      BlockState s1 = mc.world.getBlockState(f1);
      BlockState s2 = mc.world.getBlockState(f2);

      if (Config.tuFull >= 0.5F && p.getInventory().getEmptySlot() == -1) {
         tunnelStop("Envanter doldu");
         return;
      }

      if (Config.tuSafe >= 0.5F) {
         if (!s1.getFluidState().isEmpty() || !s2.getFluidState().isEmpty() || !mc.world.getBlockState(f2.up()).getFluidState().isEmpty()
            || !mc.world.getBlockState(f1.down()).getFluidState().isEmpty()) {
            tunnelStop("Sıvı (lav/su) bulundu");
            return;
         }

         if (s1.isAir() && mc.world.getBlockState(f1.down()).isAir()) {
            tunnelStop("Önünde boşluk var");
            return;
         }
      }

      if (s1.getHardness(mc.world, f1) < 0.0F || s2.getHardness(mc.world, f2) < 0.0F) {
         tunnelStop("Kırılamaz blok");
         return;
      }

      BlockPos target = !s1.isAir() ? f1 : (!s2.isAir() ? f2 : null);
      if (target != null) {
         release(mc.options.forwardKey);
         mining = true;
         mc.interactionManager.updateBlockBreakingProgress(target, d.getOpposite());
         p.swingHand(Hand.MAIN_HAND);
      } else {
         if (mining) {
            mc.interactionManager.cancelBlockBreaking();
            mining = false;
         }

         mc.options.forwardKey.setPressed(true);
      }
   }
}

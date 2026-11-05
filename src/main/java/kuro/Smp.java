package kuro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import kuro.ui.Toasts;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.FallingBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

/**
 * SMP / DonutSMP icin otomasyon modulleri: AntiAFK, AutoFish, VisualRange, AutoMine, AutoWalk, Scaffold, AutoFirework, AutoFarm.
 * Hepsi normal oyuncu girdisi yolundan (tus basma, interactItem / interactBlock) calisir.
 */
public final class Smp {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   private Smp() {
   }

   private static boolean usable() {
      return mc.player != null && mc.world != null && mc.currentScreen == null && mc.interactionManager != null && !Freecam.active;
   }

   private static int rnd(int a, int b) {
      return a >= b ? a : ThreadLocalRandom.current().nextInt(a, b + 1);
   }

   /** Tusu fiziksel durumuna geri dondurur (sahte basma sonrasi). */
   private static void release(KeyBinding kb) {
      InputUtil.Key k = KeyBindingHelper.getBoundKeyOf(kb);
      long win = mc.getWindow().getHandle();
      boolean ph = k.getCategory() == InputUtil.Type.KEYSYM
         ? InputUtil.isKeyPressed(win, k.getCode())
         : k.getCategory() == InputUtil.Type.MOUSE && GLFW.glfwGetMouseButton(win, k.getCode()) == GLFW.GLFW_PRESS;
      kb.setPressed(ph);
   }

   /** Her tick cagrilir; acik moduller calisir. */
   public static void tick() {
      if (mc.player == null || mc.world == null) {
         return;
      }

      if (Kuro.on[Kuro.ANTIAFK]) {
         antiAfk();
      }

      if (Kuro.on[Kuro.AUTOFISH]) {
         autoFish();
      }

      if (Kuro.on[Kuro.VISUALRANGE]) {
         visualRange();
      }

      if (Kuro.on[Kuro.AUTOMINE]) {
         autoMine();
      }

      if (Kuro.on[Kuro.AUTOWALK]) {
         autoWalk();
      }

      if (Kuro.on[Kuro.SCAFFOLD]) {
         scaffold();
      } else {
         scaffoldOff();
      }

      if (Kuro.on[Kuro.AUTOFIREWORK]) {
         firework();
      }

      if (Kuro.on[Kuro.AUTOFARM]) {
         farm();
      }
   }

   /** Modul kapaninca / dunya degisince cagrilir. */
   public static void onToggle(int i, boolean v) {
      if (v) {
         return;
      }

      if (i == Kuro.ANTIAFK) {
         afkOff();
      } else if (i == Kuro.AUTOFISH) {
         fishCd = 0;
         reelWait = 0;
         fishTicks = 0;
      } else if (i == Kuro.VISUALRANGE) {
         inRange.clear();
         alertAt.clear();
      } else if (i == Kuro.AUTOMINE) {
         if (mc.player != null) {
            release(mc.options.attackKey);
         }
      } else if (i == Kuro.AUTOWALK) {
         if (mc.player != null) {
            release(mc.options.forwardKey);
            release(mc.options.sprintKey);
         }
      } else if (i == Kuro.SCAFFOLD) {
         scaffoldOff();
      } else if (i == Kuro.AUTOFIREWORK) {
         fwRestore();
      } else if (i == Kuro.AUTOFARM) {
         replant.clear();
         farmRestore();
      }
   }

   // ------------------------------------------------------------------ AntiAFK
   // Belirli araliklarla kucuk hareket / bakis degisimi / zipla yaparak AFK atilmasini onler.
   private static int afkCd;
   private static int afkPhase;
   private static int afkTicks;
   private static float afkYaw;

   private static void afkOff() {
      afkPhase = 0;
      afkCd = 0;
      if (mc.player != null) {
         release(mc.options.jumpKey);
         release(mc.options.forwardKey);
         release(mc.options.backKey);
      }
   }

   private static void antiAfk() {
      ClientPlayerEntity p = mc.player;
      if (!usable()) {
         return;
      }

      if (afkPhase == 0) {
         if (afkCd <= 0) {
            afkCd = rnd(Math.round(Config.afkMin * 20.0F), Math.round(Config.afkMax * 20.0F));
         }

         if (--afkCd > 0) {
            return;
         }

         afkPhase = 1;
         afkTicks = 0;
         afkYaw = (ThreadLocalRandom.current().nextBoolean() ? 1.0F : -1.0F) * (6.0F + ThreadLocalRandom.current().nextFloat() * 14.0F);
         return;
      }

      afkTicks++;
      if (afkTicks == 1) {
         p.setYaw(p.getYaw() + afkYaw); // bakisi cevir
         p.swingHand(Hand.MAIN_HAND);
      }

      if (Config.afkJump >= 0.5F && afkTicks == 2) {
         mc.options.jumpKey.setPressed(true);
      } else if (afkTicks == 4) {
         release(mc.options.jumpKey);
      }

      if (Config.afkWalk >= 0.5F) {
         if (afkTicks >= 6 && afkTicks < 10) {
            mc.options.forwardKey.setPressed(true);
         } else if (afkTicks == 10) {
            release(mc.options.forwardKey);
         } else if (afkTicks >= 12 && afkTicks < 16) {
            mc.options.backKey.setPressed(true);
         } else if (afkTicks == 16) {
            release(mc.options.backKey);
         }
      }

      if (afkTicks >= 18) {
         p.setYaw(p.getYaw() - afkYaw * 0.5F);
         afkOff();
      }
   }

   // ------------------------------------------------------------------ AutoFish
   // Olta atar; balik vurunca (splash sesi) rastgele gecikmeyle ceker ve yeniden atar.
   private static int fishCd;
   private static int reelWait;
   private static int fishTicks;

   private static Hand rodHand(ClientPlayerEntity p) {
      if (p.getMainHandStack().isOf(Items.FISHING_ROD)) {
         return Hand.MAIN_HAND;
      }

      return p.getOffHandStack().isOf(Items.FISHING_ROD) ? Hand.OFF_HAND : null;
   }

   private static void autoFish() {
      ClientPlayerEntity p = mc.player;
      if (!usable()) {
         return;
      }

      Hand h = rodHand(p);
      if (h == null) {
         return;
      }

      FishingBobberEntity hook = p.fishHook;
      if (reelWait > 0) {
         if (--reelWait == 0 && hook != null) {
            mc.interactionManager.interactItem(p, h); // cek
            p.swingHand(h);
            fishCd = rnd(Math.round(Config.afRecast), Math.round(Config.afRecast) + 8);
            fishTicks = 0;
         }

         return;
      }

      if (hook == null) {
         fishTicks = 0;
         if (fishCd > 0) {
            fishCd--;
            return;
         }

         mc.interactionManager.interactItem(p, h); // at
         p.swingHand(h);
         fishCd = 20;
         return;
      }

      // takildiysa / uzun suredir vurmadiysa yeniden at
      if (++fishTicks > Math.round(Config.afTimeout * 20.0F)) {
         reelWait = 1;
      }
   }

   /** AntiVanishMixin.onPlaySound'dan cagrilir (ana thread). */
   public static void onSound(PlaySoundS2CPacket pk) {
      if (!Kuro.on[Kuro.AUTOFISH] || mc.player == null || reelWait > 0) {
         return;
      }

      FishingBobberEntity hook = mc.player.fishHook;
      if (hook == null || !pk.getSound().getIdAsString().equals("minecraft:entity.fishing_bobber.splash")) {
         return;
      }

      if (hook.getPos().distanceTo(new Vec3d(pk.getX(), pk.getY(), pk.getZ())) > 3.0) {
         return; // baska birinin oltasi
      }

      reelWait = rnd(Math.round(Config.afMin), Math.round(Config.afMax));
   }

   // ------------------------------------------------------------------ VisualRange
   // Gorus alanina yeni oyuncu girince uyarir; istersen yaklasinca sunucudan cikar.
   private static final Set<UUID> inRange = new HashSet<>();
   private static final Map<UUID, Long> alertAt = new HashMap<>();

   private static void visualRange() {
      ClientPlayerEntity p = mc.player;
      if (p == null || mc.getNetworkHandler() == null) {
         return;
      }

      Set<UUID> now = new HashSet<>();
      long t = System.currentTimeMillis();
      for (AbstractClientPlayerEntity o : mc.world.getPlayers()) {
         // FakePlayer (negatif id) ve tab listesinde olmayanlar (NPC) sayilmaz
         if (o == p || o.getId() < 0 || mc.getNetworkHandler().getPlayerListEntry(o.getUuid()) == null) {
            continue;
         }

         UUID id = o.getUuid();
         now.add(id);
         double d = p.getPos().distanceTo(o.getPos());
         String n = o.getGameProfile().getName();
         if (!inRange.contains(id) && (alertAt.get(id) == null || t - alertAt.get(id) > 10000L)) {
            alertAt.put(id, t);
            Mods.msg("VisualRange: §c" + n + "§r görüş alanına girdi (" + Math.round(d) + " blok)");
            Toasts.push("VisualRange", false, n + " yakında (" + Math.round(d) + " blok)");
         }

         if (Config.vrLogout >= 0.5F && !mc.isInSingleplayer() && d <= Config.vrLogoutDist) {
            Kuro.setOn(Kuro.VISUALRANGE, false);
            DonutMods.markLeft();
            mc.getNetworkHandler().getConnection().disconnect(Text.literal("[Kuro] VisualRange: " + n + " " + Math.round(d) + " blok yakınında"));
            return;
         }
      }

      inRange.clear();
      inRange.addAll(now);
      alertAt.values().removeIf(v -> t - v > 60000L);
   }

   // ------------------------------------------------------------------ AutoMine
   // Sol tik basili tutar (AFK maden / blok kirma). Envanter dolunca durur.
   private static int mineCheck;

   private static void autoMine() {
      ClientPlayerEntity p = mc.player;
      if (!usable()) {
         release(mc.options.attackKey);
         return;
      }

      if (Config.amFull >= 0.5F && ++mineCheck >= 20) {
         mineCheck = 0;
         if (p.getInventory().getEmptySlot() == -1) {
            Kuro.setOn(Kuro.AUTOMINE, false);
            Mods.msg("AutoMine: envanter doldu, durdu");
            Toasts.push("AutoMine", false, "Envanter doldu");
            return;
         }
      }

      mc.options.attackKey.setPressed(true);
   }

   // ------------------------------------------------------------------ AutoWalk
   private static void autoWalk() {
      if (!usable()) {
         release(mc.options.forwardKey);
         return;
      }

      mc.options.forwardKey.setPressed(true);
      if (Config.awSprint >= 0.5F) {
         mc.options.sprintKey.setPressed(true);
      }
   }

   // ------------------------------------------------------------------ Scaffold
   // Ayagimizin altina hotbar'daki bloklardan koyar (kopru / kule). Yer bos oldugunda calisir.
   private static int scCd;
   private static int scRestore = -1;
   private static int scIdle;

   private static int blockSlot(ClientPlayerEntity p) {
      int cur = p.getInventory().selectedSlot;
      for (int k = 0; k < 9; k++) {
         int i = (cur + k) % 9;
         ItemStack s = p.getInventory().getStack(i);
         if (s.isEmpty() || !(s.getItem() instanceof BlockItem bi)) {
            continue;
         }

         Block b = bi.getBlock();
         if (b instanceof FallingBlock || b.getDefaultState().hasBlockEntity()) {
            continue;
         }

         return i;
      }

      return -1;
   }

   private static void scaffoldOff() {
      if (scRestore >= 0 && mc.player != null) {
         mc.player.getInventory().selectedSlot = scRestore;
      }

      scRestore = -1;
      scIdle = 0;
   }

   private static void scaffold() {
      ClientPlayerEntity p = mc.player;
      if (!usable() || p.isGliding() || p.getAbilities().flying) {
         return;
      }

      if (scCd > 0) {
         scCd--;
         return;
      }

      // hareket yonunde biraz ileriyi de hesaba kat
      Vec3d v = p.getVelocity();
      BlockPos below = BlockPos.ofFloored(p.getX() + v.x * 2.0, p.getY() - 1.0, p.getZ() + v.z * 2.0);
      if (!mc.world.getBlockState(below).isReplaceable()) {
         if (scRestore >= 0 && ++scIdle > 10) {
            scaffoldOff();
         }

         return;
      }

      int slot = blockSlot(p);
      if (slot < 0) {
         return;
      }

      // komsu kati blok bul
      Direction[] order = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
      for (Direction d : order) {
         BlockPos nb = below.offset(d);
         BlockState st = mc.world.getBlockState(nb);
         if (st.isAir() || st.isReplaceable()) {
            continue;
         }

         if (scRestore < 0) {
            scRestore = p.getInventory().selectedSlot;
         }

         p.getInventory().selectedSlot = slot;
         Direction face = d.getOpposite();
         Vec3d hit = Vec3d.ofCenter(nb).add(face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
         mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, new BlockHitResult(hit, face, nb, false));
         p.swingHand(Hand.MAIN_HAND);
         scCd = Math.round(Config.scDelay);
         scIdle = 0;
         return;
      }
   }

   // ------------------------------------------------------------------ AutoFirework
   // Elytra ile ucarken hiz dusunce hotbar'daki havai fisegi kullanir.
   private static int fwCd;
   private static int fwRestore = -1;
   private static int fwBack;

   private static void fwRestore() {
      if (fwRestore >= 0 && mc.player != null) {
         mc.player.getInventory().selectedSlot = fwRestore;
      }

      fwRestore = -1;
   }

   static void firework() {
      ClientPlayerEntity p = mc.player;
      if (!usable()) {
         return;
      }

      if (fwRestore >= 0 && --fwBack <= 0) {
         fwRestore();
      }

      if (!p.isGliding()) {
         return;
      }

      if (fwCd > 0) {
         fwCd--;
         return;
      }

      Vec3d v = p.getVelocity();
      double speed = Math.sqrt(v.x * v.x + v.z * v.z) * 20.0; // blok / sn
      if (speed >= Config.afwSpeed) {
         return;
      }

      int slot = -1;
      for (int i = 0; i < 9; i++) {
         if (p.getInventory().getStack(i).isOf(Items.FIREWORK_ROCKET)) {
            slot = i;
            break;
         }
      }

      if (slot < 0) {
         return;
      }

      if (fwRestore < 0) {
         fwRestore = p.getInventory().selectedSlot;
      }

      p.getInventory().selectedSlot = slot;
      mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
      p.swingHand(Hand.MAIN_HAND);
      fwBack = 3;
      fwCd = Math.round(Config.afwDelay);
   }

   // ------------------------------------------------------------------ AutoFarm
   // Menzildeki olgun ekinleri (bugday, havuc, patates, pancar) kirar ve tohumla yeniden eker.
   private static final class Replant {
      BlockPos pos;
      Item seed;
   }

   private static final List<Replant> replant = new ArrayList<>();
   private static int fmCd;
   private static int fmRestore = -1;
   private static int fmBack;

   private static void farmRestore() {
      if (fmRestore >= 0 && mc.player != null) {
         mc.player.getInventory().selectedSlot = fmRestore;
      }

      fmRestore = -1;
   }

   private static Item seedFor(Block b) {
      if (b == Blocks.WHEAT) {
         return Items.WHEAT_SEEDS;
      }

      if (b == Blocks.CARROTS) {
         return Items.CARROT;
      }

      if (b == Blocks.POTATOES) {
         return Items.POTATO;
      }

      return b == Blocks.BEETROOTS ? Items.BEETROOT_SEEDS : null;
   }

   private static void farm() {
      ClientPlayerEntity p = mc.player;
      if (!usable()) {
         return;
      }

      if (fmRestore >= 0 && --fmBack <= 0) {
         farmRestore();
      }

      if (fmCd > 0) {
         fmCd--;
         return;
      }

      double reach = Config.fmRange;
      Vec3d eye = p.getEyePos();

      // 1) bekleyen yeniden ekim
      if (Config.fmReplant >= 0.5F && !replant.isEmpty()) {
         Replant r = replant.remove(0);
         if (mc.world.getBlockState(r.pos).isAir() && mc.world.getBlockState(r.pos.down()).isOf(Blocks.FARMLAND)) {
            int slot = -1;
            for (int i = 0; i < 9; i++) {
               if (p.getInventory().getStack(i).isOf(r.seed)) {
                  slot = i;
                  break;
               }
            }

            if (slot >= 0) {
               if (fmRestore < 0) {
                  fmRestore = p.getInventory().selectedSlot;
               }

               p.getInventory().selectedSlot = slot;
               BlockPos fl = r.pos.down();
               mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(fl.getX() + 0.5, fl.getY() + 1.0, fl.getZ() + 0.5), Direction.UP, fl, false));
               p.swingHand(Hand.MAIN_HAND);
               fmBack = 4;
               fmCd = Math.round(Config.fmDelay);
               return;
            }
         }
      }

      // 2) en yakin olgun ekini bul ve kir
      BlockPos best = null;
      double bd = Double.MAX_VALUE;
      int rad = (int)Math.ceil(reach);
      BlockPos base = p.getBlockPos();
      for (BlockPos bp : BlockPos.iterate(base.add(-rad, -2, -rad), base.add(rad, 2, rad))) {
         BlockState st = mc.world.getBlockState(bp);
         if (!(st.getBlock() instanceof CropBlock cb) || !cb.isMature(st) || seedFor(st.getBlock()) == null) {
            continue;
         }

         double d = eye.distanceTo(Vec3d.ofCenter(bp));
         if (d <= reach && d < bd) {
            bd = d;
            best = bp.toImmutable();
         }
      }

      if (best == null) {
         return;
      }

      Block blk = mc.world.getBlockState(best).getBlock();
      mc.interactionManager.attackBlock(best, Direction.UP);
      p.swingHand(Hand.MAIN_HAND);
      if (Config.fmReplant >= 0.5F) {
         Replant rp = new Replant();
         rp.pos = best;
         rp.seed = seedFor(blk);
         replant.add(rp);
      }

      fmCd = Math.round(Config.fmDelay);
   }
}

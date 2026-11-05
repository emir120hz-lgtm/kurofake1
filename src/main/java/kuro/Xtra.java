package kuro;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kuro.ui.Toasts;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

/** ElytraPilot (elytra otopilot) ve UndergroundESP (yer alti base'leri dolu renkle gosterir) + Freecam hiz yazisi. */
public final class Xtra {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   private Xtra() {
   }

   public static void init() {
      HudRenderCallback.EVENT.register(Xtra::hud);
   }

   public static void tick() {
      if (mc.player == null || mc.world == null) {
         return;
      }

      if (Kuro.on[Kuro.ELYTRAPILOT]) {
         pilot();
      }

      if (Kuro.on[Kuro.UNDERGROUNDESP]) {
         ugTick();
      }
   }

   public static void onToggle(int i, boolean v) {
      if (i == Kuro.ELYTRAPILOT) {
         pilotInit = false;
      }

      if (i == Kuro.UNDERGROUNDESP && !v) {
         ug.clear();
         ugQueue.clear();
         ugTimer = 0;
      }
   }

   private static void hud(DrawContext dc, RenderTickCounter tc) {
      if (!Freecam.active || mc.player == null || mc.options.hudHidden) {
         return;
      }

      Text t = Text.literal("Freecam speed " + String.format("%.1f", Config.freecamSpeed) + "x");
      int w = mc.textRenderer.getWidth(t);
      dc.drawTextWithShadow(mc.textRenderer, t, (dc.getScaledWindowWidth() - w) / 2, dc.getScaledWindowHeight() - 62, 0xFFFFFFFF);
   }

   // ------------------------------------------------------------------ ElytraPilot
   // Elytra ile ucarken yonu ve yuksekligi kendisi yonetir, havai fisegi gerekince kendisi kullanir.
   //  - Hedef: kuro_settings.txt'de epX/epZ (0 degilse) ya da "Hedef mesafesi" slider'i (bakis yonunde); ikisi de yoksa bulundugun yonde sonsuza dek.
   //  - Yukseklik: "Seyir yuksekligi" ve onundeki arazinin 12 blok ustu (hangisi yuksekse).
   //  - Kalkis elle yapilir (elytra ac, fisek at); pilot sadece ucarken devreye girer.
   private static boolean pilotInit;
   private static float pilotYaw;
   private static boolean hasTarget;
   private static double tx;
   private static double tz;

   private static void pilotStop(String why) {
      Kuro.setOn(Kuro.ELYTRAPILOT, false);
      Mods.msg("ElytraPilot: " + why);
      Toasts.push("ElytraPilot", false, why);
   }

   private static void pilot() {
      ClientPlayerEntity p = mc.player;
      if (!p.isGliding() || Freecam.active || mc.currentScreen != null) {
         pilotInit = false;
         return;
      }

      if (!pilotInit) {
         pilotInit = true;
         pilotYaw = p.getYaw();
         hasTarget = false;
         if (Config.epX != 0.0F || Config.epZ != 0.0F) {
            tx = Config.epX;
            tz = Config.epZ;
            hasTarget = true;
         } else if (Config.epDist > 0.0F) {
            double rad = Math.toRadians(pilotYaw);
            tx = p.getX() - Math.sin(rad) * Config.epDist;
            tz = p.getZ() + Math.cos(rad) * Config.epDist;
            hasTarget = true;
         }
      }

      ItemStack el = p.getEquippedStack(EquipmentSlot.CHEST);
      if (el.isOf(Items.ELYTRA) && el.getMaxDamage() - el.getDamage() <= 10) {
         pilotStop("Elytra neredeyse kırık, iniş yap");
         return;
      }

      if (hasTarget) {
         double dx = tx - p.getX();
         double dz = tz - p.getZ();
         if (Math.sqrt(dx * dx + dz * dz) < 40.0) {
            pilotStop("Hedefe varıldı");
            return;
         }

         pilotYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
      }

      p.setYaw(p.getYaw() + MathHelper.clamp(MathHelper.wrapDegrees(pilotYaw - p.getYaw()), -8.0F, 8.0F));

      // onumuzdeki arazinin en yuksek noktasi
      double rad = Math.toRadians(p.getYaw());
      double top = Double.NEGATIVE_INFINITY;
      for (int d : new int[]{20, 45, 80}) {
         int x = MathHelper.floor(p.getX() - Math.sin(rad) * d);
         int z = MathHelper.floor(p.getZ() + Math.cos(rad) * d);
         top = Math.max(top, mc.world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z));
      }

      top = Math.max(top, mc.world.getTopY(Heightmap.Type.MOTION_BLOCKING, p.getBlockX(), p.getBlockZ()));
      double wantY = Math.max(Config.epY, top + 12.0);
      double err = wantY - p.getY();
      Vec3d v = p.getVelocity();
      double speed = Math.sqrt(v.x * v.x + v.z * v.z) * 20.0; // blok / sn

      float pitch;
      if (err > 6.0) {
         pitch = speed >= 14.0 ? -40.0F : 8.0F; // yeterli hiz varsa tirman, yoksa hafif dalip hiz topla
      } else if (err < -6.0) {
         pitch = 25.0F;
      } else {
         pitch = speed < 18.0 ? 10.0F : -4.0F;
      }

      p.setPitch(p.getPitch() + MathHelper.clamp(pitch - p.getPitch(), -4.0F, 4.0F));

      if (Config.epRockets >= 0.5F && err > -6.0) {
         Smp.firework();
      }
   }

   // ------------------------------------------------------------------ UndergroundESP
   // Yer altindaki (yuzeyin Config.ugDepth blok altindaki) oyuncu yapimi bloklari (hopper, sandik, varil, shulker, dispenser, piston,
   // cam, obsidyen, deger bloklari...) duvarlarin arkasindan dolu renkle gosterir. Bir chunk'ta en az Config.ugMin tane varsa cizer.
   private record UgHit(int x, int y, int z, int color) {
   }

   private static final Map<Long, List<UgHit>> ug = new HashMap<>();
   private static final ArrayDeque<ChunkPos> ugQueue = new ArrayDeque<>();
   private static final Set<Block> ART = new HashSet<>();
   private static int ugTimer;
   private static Object ugWorld;

   static {
      Block[] a = {
         Blocks.HOPPER, Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL, Blocks.ENDER_CHEST, Blocks.DISPENSER, Blocks.DROPPER, Blocks.OBSERVER,
         Blocks.PISTON, Blocks.STICKY_PISTON, Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER, Blocks.CRAFTING_TABLE, Blocks.ENCHANTING_TABLE,
         Blocks.ANVIL, Blocks.BEACON, Blocks.SPAWNER, Blocks.GLASS, Blocks.TINTED_GLASS, Blocks.LADDER, Blocks.SCAFFOLDING, Blocks.REDSTONE_BLOCK,
         Blocks.REDSTONE_LAMP, Blocks.TNT, Blocks.LECTERN, Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.NETHERITE_BLOCK, Blocks.DIAMOND_BLOCK,
         Blocks.EMERALD_BLOCK, Blocks.GOLD_BLOCK, Blocks.IRON_BLOCK, Blocks.BRICKS, Blocks.POWERED_RAIL, Blocks.DETECTOR_RAIL, Blocks.ACTIVATOR_RAIL,
         Blocks.WATER_CAULDRON, Blocks.CAULDRON, Blocks.COMPOSTER, Blocks.SMITHING_TABLE, Blocks.GRINDSTONE, Blocks.STONECUTTER
      };
      for (Block b : a) {
         ART.add(b);
      }
   }

   private static boolean art(BlockState s) {
      Block b = s.getBlock();
      return ART.contains(b) || b instanceof ShulkerBoxBlock;
   }

   private static int colorOf(Block b) {
      if (b == Blocks.SPAWNER) {
         return 0xFF3030;
      }

      if (b == Blocks.CHEST || b == Blocks.TRAPPED_CHEST || b == Blocks.BARREL || b == Blocks.ENDER_CHEST || b instanceof ShulkerBoxBlock) {
         return 0xFFA040;
      }

      return b == Blocks.HOPPER ? 0x40E0FF : 0x2080FF;
   }

   private static void ugTick() {
      if (ugWorld != mc.world) {
         ug.clear();
         ugQueue.clear();
         ugWorld = mc.world;
         ugTimer = 0;
      }

      if (ugQueue.isEmpty()) {
         if (--ugTimer > 0) {
            return;
         }

         ugTimer = 100;
         int r = Math.round(Config.ugRadius);
         ChunkPos pc = mc.player.getChunkPos();
         List<ChunkPos> l = new ArrayList<>();
         for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
               l.add(new ChunkPos(pc.x + dx, pc.z + dz));
            }
         }

         l.sort((a, b) -> Integer.compare(
            (a.x - pc.x) * (a.x - pc.x) + (a.z - pc.z) * (a.z - pc.z),
            (b.x - pc.x) * (b.x - pc.x) + (b.z - pc.z) * (b.z - pc.z)));
         ugQueue.addAll(l);
         ug.keySet().removeIf(k -> {
            int cx = (int)(k & 0xFFFFFFFFL);
            int cz = (int)(k >>> 32);
            return Math.abs(cx - pc.x) > r + 2 || Math.abs(cz - pc.z) > r + 2;
         });
      }

      long end = System.nanoTime() + 1_500_000L;
      while (!ugQueue.isEmpty() && System.nanoTime() < end) {
         ugScan(ugQueue.poll());
      }
   }

   private static void ugScan(ChunkPos cp) {
      WorldChunk c = mc.world.getChunkManager().getWorldChunk(cp.x, cp.z);
      long key = cp.toLong();
      if (c == null) {
         ug.remove(key);
         return;
      }

      List<UgHit> out = new ArrayList<>();
      ChunkSection[] secs = c.getSectionArray();
      int bottom = c.getBottomSectionCoord();
      int depth = Math.round(Config.ugDepth);
      for (int i = 0; i < secs.length && out.size() < 500; i++) {
         ChunkSection s = secs[i];
         if (s == null || s.isEmpty() || !s.hasAny(Xtra::art)) {
            continue;
         }

         int base = (bottom + i) << 4;
         for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
               for (int x = 0; x < 16; x++) {
                  BlockState st = s.getBlockState(x, y, z);
                  if (!art(st)) {
                     continue;
                  }

                  int wy = base + y;
                  if (wy >= c.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, x, z) - depth) {
                     continue; // yuzeye yakin: dogal / ust yapi
                  }

                  out.add(new UgHit(cp.getStartX() + x, wy, cp.getStartZ() + z, colorOf(st.getBlock())));
                  if (out.size() >= 500) {
                     break;
                  }
               }
            }
         }
      }

      if (out.size() >= Math.round(Config.ugMin)) {
         ug.put(key, out);
      } else {
         ug.remove(key);
      }
   }

   public static boolean anyWorld() {
      return Kuro.on[Kuro.UNDERGROUNDESP] && !ug.isEmpty();
   }

   /** Render.render'dan cagrilir: dolu kutular + ince cerceve. */
   public static void render(Matrix4f m, Vec3d cam) {
      double r = Config.ugRange;
      double r2 = r * r;
      float fa = Config.ugAlpha / 100.0F;
      List<UgHit> vis = new ArrayList<>();
      for (List<UgHit> l : ug.values()) {
         for (UgHit h : l) {
            double dx = h.x + 0.5 - cam.x;
            double dy = h.y + 0.5 - cam.y;
            double dz = h.z + 0.5 - cam.z;
            if (dx * dx + dy * dy + dz * dz <= r2) {
               vis.add(h);
               if (vis.size() >= 3000) {
                  break;
               }
            }
         }

         if (vis.size() >= 3000) {
            break;
         }
      }

      if (vis.isEmpty()) {
         return;
      }

      if (Config.ugFill >= 0.5F) {
         RenderSystem.disableCull();
         BufferBuilder b = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         for (UgHit h : vis) {
            fillBox(b, m, h.x, h.y, h.z, h.color, fa);
         }

         Render.flush(b);
         RenderSystem.enableCull();
      }

      RenderSystem.lineWidth(1.5F);
      BufferBuilder lb = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
      for (UgHit h : vis) {
         float cr = (h.color >> 16 & 0xFF) / 255.0F;
         float cg = (h.color >> 8 & 0xFF) / 255.0F;
         float cb = (h.color & 0xFF) / 255.0F;
         Render.box(lb, m, h.x, h.y, h.z, h.x + 1, h.y + 1, h.z + 1, cr, cg, cb, 1.0F);
      }

      Render.flush(lb);
   }

   private static void q(BufferBuilder b, Matrix4f m, float r, float g, float bl, float a,
                         double x1, double y1, double z1, double x2, double y2, double z2,
                         double x3, double y3, double z3, double x4, double y4, double z4) {
      b.vertex(m, (float)x1, (float)y1, (float)z1).color(r, g, bl, a);
      b.vertex(m, (float)x2, (float)y2, (float)z2).color(r, g, bl, a);
      b.vertex(m, (float)x3, (float)y3, (float)z3).color(r, g, bl, a);
      b.vertex(m, (float)x4, (float)y4, (float)z4).color(r, g, bl, a);
   }

   private static void fillBox(BufferBuilder b, Matrix4f m, int x, int y, int z, int c, float a) {
      float r = (c >> 16 & 0xFF) / 255.0F;
      float g = (c >> 8 & 0xFF) / 255.0F;
      float bl = (c & 0xFF) / 255.0F;
      double x2 = x + 1;
      double y2 = y + 1;
      double z2 = z + 1;
      q(b, m, r, g, bl, a, x, y, z, x2, y, z, x2, y, z2, x, y, z2); // alt
      q(b, m, r, g, bl, a, x, y2, z, x, y2, z2, x2, y2, z2, x2, y2, z); // ust
      q(b, m, r, g, bl, a, x, y, z, x, y2, z, x2, y2, z, x2, y, z); // kuzey
      q(b, m, r, g, bl, a, x, y, z2, x2, y, z2, x2, y2, z2, x, y2, z2); // guney
      q(b, m, r, g, bl, a, x, y, z, x, y, z2, x, y2, z2, x, y2, z); // bati
      q(b, m, r, g, bl, a, x2, y, z, x2, y2, z, x2, y2, z2, x2, y, z2); // dogu
   }
}

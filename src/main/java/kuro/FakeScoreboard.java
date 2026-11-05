package kuro;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * FakeScoreboard: "CHICKEN SMP" panelini sag tarafa cizer (sunucunun scoreboard'unun yerine).
 * Yazilar Minecraft fontuyla degil, gorseldeki piksel yazi tipinin kendi glifleriyle cizilir (1 hucre = 1 GUI pikseli).
 * Yalnizca yerelde gorunur; sunucuya hicbir sey gonderilmez.
 */
public final class FakeScoreboard {
   public static final String[] DEFAULT_LABELS = {"PARA", "KRISTAL", "OLDURME", "OLUM", "ANAHTAR", "OYNAMA", "TAKIM"};
   private static final int[] VALUE_COLORS = {0x00F900, 0xA103F6, 0xF90000, 0xF67503, 0x00A2F9, 0xF2F200, 0xF2F200};
   private static final int[] ICON_COLORS = {0x00F900, 0xA103F6, 0xF90000, 0xF67503, 0x00A2F9, 0xFCFC00, 0xFCFC00};
   private static final int[] TITLE_COLORS = {0xFC7E4F, 0xFC8A47, 0xFC973F, 0xFCA337, 0xFCB02F, 0xFCBD28, 0xFCCA20, 0xF2D90F, 0xFCEF08, 0xFCFC00};

   // Gorselden birebir cikarilmis ikonlar: $, kristal, kilic, kafatasi, kum saati, saat, anahtar
   private static final String[][] ICONS = {
      {"..##....", "######..", "##......", "######..", "....##..", "######..", "..##...."},
      {"...#....", "..###...", "#######.", "#######.", ".#####..", ".#####..", ".##.##.."},
      {"......##", ".....#.#", "....#.#.", ".#.#.#..", "..#.#...", "..##....", ".#..#...", "#......."},
      {".#####..", "#######.", "#..#..#.", "#..#..#.", "###.###.", ".##.##..", ".#.#.#.."},
      {"#######.", ".#...#..", ".#...#..", "..#.#...", ".#...#..", ".#...#..", "#######."},
      {".#####..", "#..#..#.", "#..#..#.", "#..##.#.", "#.....#.", "#.....#.", ".#####.."},
      {"...##...", "..#.#...", ".#..##..", ".###.#..", "...###..", "..#.....", ".#......", "#......."}
   };

   // otomatik uretildi: gorseldeki yazi tipinden cikarilan 5 genislikli piksel glifler
   private static final String[][] SMALL = new String[128][];
   private static final String[][] BIG = new String[128][];
   static {
      SMALL['A'] = new String[]{".###.","#...#","#####","#...#","#...#"};
      SMALL['B'] = new String[]{"####.","#...#","####.","#...#","####."};
      SMALL['C'] = new String[]{".###.","#...#","#....","#...#",".###."};
      SMALL['D'] = new String[]{"####.","#...#","#...#","#...#","####."};
      SMALL['E'] = new String[]{"#####","#....","###..","#....","#####"};
      SMALL['F'] = new String[]{"#####","#....","####.","#....","#...."};
      SMALL['G'] = new String[]{".###.","#....","#.###","#...#",".###."};
      SMALL['H'] = new String[]{"#...#","#...#","#####","#...#","#...#"};
      SMALL['I'] = new String[]{"###",".#.",".#.",".#.","###"};
      SMALL['J'] = new String[]{"....#","....#","....#","#...#",".###."};
      SMALL['K'] = new String[]{"#...#","#..#.","###..","#..#.","#...#"};
      SMALL['L'] = new String[]{"#....","#....","#....","#....","#####"};
      SMALL['M'] = new String[]{"#...#","##.##","#.#.#","#...#","#...#"};
      SMALL['N'] = new String[]{"#...#","##..#","#.#.#","#..##","#...#"};
      SMALL['O'] = new String[]{".###.","#...#","#...#","#...#",".###."};
      SMALL['P'] = new String[]{"####.","#...#","####.","#....","#...."};
      SMALL['Q'] = new String[]{".###.","#...#","#...#","#..##",".####"};
      SMALL['R'] = new String[]{"####.","#...#","####.","#...#","#...#"};
      SMALL['S'] = new String[]{"#####","#....","#####","....#","#####"};
      SMALL['T'] = new String[]{"####",".#..",".#..",".#..",".#.."};
      SMALL['U'] = new String[]{"#...#","#...#","#...#","#...#",".###."};
      SMALL['V'] = new String[]{"#...#","#...#","#...#",".#.#.","..#.."};
      SMALL['W'] = new String[]{"#...#","#...#","#.#.#","##.##","#...#"};
      SMALL['X'] = new String[]{"#...#",".#.#.","..#..",".#.#.","#...#"};
      SMALL['Y'] = new String[]{"#...#",".#.#.","..#..","..#..","..#.."};
      SMALL['Z'] = new String[]{"#####","...#.","..#..",".#...","#####"};
      BIG['+'] = new String[]{".....",".....","..#..","#####","..#.."};
      BIG[','] = new String[]{".",".",".",".",".","#","#"};
      BIG['-'] = new String[]{".....",".....",".....","#####"};
      BIG['.'] = new String[]{".",".",".",".",".",".","#"};
      BIG['0'] = new String[]{"#####","#..##","#.#.#","##..#","##..#","#...#","#####"};
      BIG['1'] = new String[]{"###..","..#..","..#..","..#..","..#..","..#..","#####"};
      BIG['2'] = new String[]{"#####","#...#","....#","#####","#....","#....","#####"};
      BIG['3'] = new String[]{"#####","....#","....#",".####","....#","....#","#####"};
      BIG['4'] = new String[]{"#...#","#...#","#...#","#####","....#","....#","....#"};
      BIG['5'] = new String[]{"#####","#....","#####","....#","....#","#...#","#####"};
      BIG['6'] = new String[]{"#####","#....","#....","#####","#...#","#...#","#####"};
      BIG['7'] = new String[]{"#####","#...#","...##","..##.","..#..","..#..","..#.."};
      BIG['8'] = new String[]{"#####","#...#","#...#","#####","#...#","#...#","#####"};
      BIG['9'] = new String[]{"#####","#...#","#...#","#####","....#","....#","#####"};
      BIG[':'] = new String[]{".",".","#",".",".","#","."};
      BIG['A'] = new String[]{".###.","#...#","#...#","#####","#...#","#...#","#...#"};
      BIG['B'] = new String[]{"####.","#...#","#...#","####.","#...#","#...#","####."};
      BIG['C'] = new String[]{".###.","#...#","#...#","#....","#...#","#...#",".###."};
      BIG['D'] = new String[]{"####.","#...#","#...#","#...#","#...#","#...#","####."};
      BIG['E'] = new String[]{"#####","#....","#....","###..","#....","#....","#####"};
      BIG['F'] = new String[]{"#####","#....","#....","####.","#....","#....","#...."};
      BIG['G'] = new String[]{".###.","#....","#....","#.###","#...#","#...#",".###."};
      BIG['H'] = new String[]{"#...#","#...#","#...#","#####","#...#","#...#","#...#"};
      BIG['I'] = new String[]{"###",".#.",".#.",".#.",".#.",".#.","###"};
      BIG['J'] = new String[]{"....#","....#","....#","....#","#...#","#...#",".###."};
      BIG['K'] = new String[]{"#...#","#..#.","#..#.","###..","#..#.","#..#.","#...#"};
      BIG['L'] = new String[]{"#....","#....","#....","#....","#....","#....","#####"};
      BIG['M'] = new String[]{"#####","#.#.#","#.#.#","#.#.#","#...#","#...#","#...#"};
      BIG['N'] = new String[]{"#...#","##..#","##..#","#.#.#","#..##","#..##","#...#"};
      BIG['O'] = new String[]{".###.","#...#","#...#","#...#","#...#","#...#",".###."};
      BIG['P'] = new String[]{"####.","#...#","#...#","####.","#....","#....","#...."};
      BIG['Q'] = new String[]{".###.","#...#","#...#","#...#","#..##","#..##",".####"};
      BIG['R'] = new String[]{"####.","#...#","#...#","####.","#...#","#...#","#...#"};
      BIG['S'] = new String[]{"#####","#....","#....","#####","....#","....#","#####"};
      BIG['T'] = new String[]{"####",".#..",".#..",".#..",".#..",".#..",".#.."};
      BIG['U'] = new String[]{"#...#","#...#","#...#","#...#","#...#","#...#",".###."};
      BIG['V'] = new String[]{"#...#","#...#","#...#","#...#",".#.#.",".#.#.","..#.."};
      BIG['W'] = new String[]{"#...#","#...#","#...#","#.#.#","##.##","##.##","#...#"};
      BIG['X'] = new String[]{"#...#",".#.#.",".#.#.","..#..",".#.#.",".#.#.","#...#"};
      BIG['Y'] = new String[]{"#...#",".#.#.",".#.#.","..#..","..#..","..#..","..#.."};
      BIG['Z'] = new String[]{"#####","...#.","...#.","..#..",".#...",".#...","#####"};
      BIG['a'] = new String[]{".....",".....",".####","....#","#####","#...#","#####"};
      BIG['b'] = new String[]{"#....","#....","#####","#...#","#...#","#...#","#####"};
      BIG['c'] = new String[]{".....",".....","#####","#....","#....","#....","#####"};
      BIG['d'] = new String[]{"....#","....#","#####","#...#","#...#","#...#","#####"};
      BIG['e'] = new String[]{".....",".....","#####","#...#","#####","#....","#####"};
      BIG['f'] = new String[]{"..###","..#..","#####","..#..","..#..","..#..","..#.."};
      BIG['g'] = new String[]{".....",".....","#####","#...#","#...#","#####","....#","#####"};
      BIG['h'] = new String[]{"#....","#....","#####","#...#","#...#","#...#","#...#"};
      BIG['i'] = new String[]{"#",".","#","#","#","#","#"};
      BIG['j'] = new String[]{"..#.",".....","..#.","..#.","..#.","..#.","..#.","###."};
      BIG['k'] = new String[]{"#....","#..#.","#.#..","###..","###..","#..#.","#..##"};
      BIG['l'] = new String[]{"#","#","#","#","#","#","#"};
      BIG['m'] = new String[]{".....",".....","#####","#.#.#","#.#.#","#.#.#","#...#"};
      BIG['n'] = new String[]{".....",".....","#####","#...#","#...#","#...#","#...#"};
      BIG['o'] = new String[]{".....",".....","#####","#...#","#...#","#...#","#####"};
      BIG['p'] = new String[]{".....",".....","#####","#...#","#...#","#####","#....","#...."};
      BIG['q'] = new String[]{".....",".....","#####","#...#","#...#","#####","....#","....#"};
      BIG['r'] = new String[]{".....",".....","#####","#...#","#....","#....","#...."};
      BIG['s'] = new String[]{".....",".....","#####","#....","#####","....#","#####"};
      BIG['t'] = new String[]{".#...",".#...","#####",".#...",".#...",".#..#",".####"};
      BIG['u'] = new String[]{".....",".....","#...#","#...#","#...#","#...#","#####"};
      BIG['v'] = new String[]{".....",".....","#...#","#...#","#...#",".#.#.","..#.."};
      BIG['w'] = new String[]{".....",".....","#...#","#...#","#.#.#","#.#.#","#####"};
      BIG['x'] = new String[]{".....",".....","#...#",".#.#.","..#..",".#.#.","#...#"};
      BIG['y'] = new String[]{".....",".....","#...#","#...#","#####","....#","....#","#####"};
      BIG['z'] = new String[]{".....",".....","#####","...#.","..#..",".#...","#####"};
   }

   private FakeScoreboard() {
   }

   public static String[] labels() {
      return new String[]{Config.fsbLab0, Config.fsbLab1, Config.fsbLab2, Config.fsbLab3, Config.fsbLab4, Config.fsbLab5, Config.fsbLab6};
   }

   public static String[] values() {
      return new String[]{Config.fsbPara, Config.fsbKristal, Config.fsbOldurme, Config.fsbOlum, Config.fsbAnahtar, Config.fsbOynama, Config.fsbTakim};
   }

   /** "130M", "10k", "1.5B", "10000$" gibi miktari sayiya cevirir; anlasilmazsa -1. */
   public static double parseMoney(String t) {
      if (t == null) {
         return -1;
      }

      String s = t.trim().replace("$", "").replace(",", "").replace("_", "");
      if (s.isEmpty()) {
         return -1;
      }

      double mul = 1;
      char last = Character.toLowerCase(s.charAt(s.length() - 1));
      if (last == 'k') {
         mul = 1e3;
      } else if (last == 'm') {
         mul = 1e6;
      } else if (last == 'b') {
         mul = 1e9;
      } else if (last == 't') {
         mul = 1e12;
      }

      if (mul != 1) {
         s = s.substring(0, s.length() - 1);
      }

      try {
         double v = Double.parseDouble(s) * mul;
         return v < 0 || Double.isNaN(v) || Double.isInfinite(v) ? -1 : v;
      } catch (NumberFormatException e) {
         return -1;
      }
   }

   /** Sayiyi panelde gorunen kisa bicime cevirir (130M, 129.99M, 9500 ...). */
   public static String formatMoney(double v) {
      String suffix = "";
      double d = v;
      if (v >= 1e12) {
         d = v / 1e12;
         suffix = "T";
      } else if (v >= 1e9) {
         d = v / 1e9;
         suffix = "B";
      } else if (v >= 1e6) {
         d = v / 1e6;
         suffix = "M";
      } else if (v >= 1e4) {
         d = v / 1e3;
         suffix = "K";
      }

      String n = java.math.BigDecimal.valueOf(d).setScale(suffix.isEmpty() ? 0 : 3, java.math.RoundingMode.DOWN)
         .stripTrailingZeros().toPlainString();
      return n + suffix;
   }

   /**
    * /pay icin: panel aciksa miktari PARA'dan dusurur. Donus: true = yeterli para var (dusuldu) veya panel kapali;
    * false = yetersiz bakiye.
    */
   public static boolean spend(String amountText) {
      if (!Config.fsbOn) {
         return true;
      }

      double amount = parseMoney(amountText);
      double bal = parseMoney(Config.fsbPara);
      if (amount < 0 || bal < 0) {
         return true;
      }

      if (amount > bal) {
         return false;
      }

      Config.fsbPara = formatMoney(bal - amount);
      Config.save();
      return true;
   }

   /** HudRenderCallback: panel acikken her karede cizilir. */
   public static void hud(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tc) {
      MinecraftClient mc = Kuro.mc;
      if (Config.fsbOn && mc != null && mc.player != null && !mc.options.hudHidden) {
         draw(ctx);
      }
   }

   private static char norm(char c) {
      switch (c) {
         case 'ç': return 'c';
         case 'Ç': return 'C';
         case 'ğ': return 'g';
         case 'Ğ': return 'G';
         case 'ı': return 'i';
         case 'İ': return 'I';
         case 'ö': return 'o';
         case 'Ö': return 'O';
         case 'ş': return 's';
         case 'Ş': return 'S';
         case 'ü': return 'u';
         case 'Ü': return 'U';
         default: return c;
      }
   }

   private static String[] glyph(char c, boolean small) {
      c = norm(c);
      if (c >= 128) {
         return null;
      }

      if (small) {
         String[] g = SMALL[Character.toUpperCase(c)];
         if (g != null) {
            return g;
         }
      }

      return BIG[c];
   }

   /** Satirin piksel genisligi (son harften sonraki bosluk dahil degil). */
   private static int width(String s, boolean small, boolean bold) {
      int x = 0;
      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         if (c == ' ') {
            x += 5;
            continue;
         }

         String[] g = glyph(c, small);
         x += g == null ? 4 : g[0].length() + 1 + (bold ? 1 : 0);
      }

      return Math.max(0, x - 1);
   }

   /** Yazi cizer; donus: imlecin son x konumu. rowShade: BIG yazilarda yukaridan asagi hafif kararma. */
   private static int text(DrawContext ctx, String s, boolean small, boolean bold, int x, int y, int color, boolean gradientTitle, boolean shade) {
      int cx = x;
      int idx = 0;
      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         if (c == ' ') {
            cx += 5;
            continue;
         }

         String[] g = glyph(c, small);
         int col = color;
         if (gradientTitle) {
            col = TITLE_COLORS[Math.min(idx, TITLE_COLORS.length - 1)];
         }

         idx++;
         if (g == null) {
            cx += 4;
            continue;
         }

         boolean isSmall = small && SMALL[Character.toUpperCase(norm(c))] != null;
         int oy = y + (isSmall ? 2 : 0);
         for (int pass = 0; pass < 2; pass++) {
            for (int k = 0; k < (bold ? 2 : 1); k++) {
               glyphDraw(ctx, g, cx + k, oy, col, pass == 0, shade && !isSmall);
            }
         }

         cx += g[0].length() + 1 + (bold ? 1 : 0);
      }

      return cx;
   }

   private static void glyphDraw(DrawContext ctx, String[] g, int x, int y, int color, boolean shadow, boolean shade) {
      for (int r = 0; r < g.length; r++) {
         int c0 = color;
         if (shade) {
            float f = 1.0F - 0.0267F * r;
            int cr = (int)(((color >> 16) & 255) * f);
            int cg = (int)(((color >> 8) & 255) * f);
            int cb = (int)((color & 255) * f);
            c0 = (cr << 16) | (cg << 8) | cb;
         }

         for (int c = 0; c < g[r].length(); c++) {
            if (g[r].charAt(c) == '#') {
               if (shadow) {
                  ctx.fill(x + c + 1, y + r + 1, x + c + 2, y + r + 2, 0xFF000000);
               } else {
                  ctx.fill(x + c, y + r, x + c + 1, y + r + 1, 0xFF000000 | c0);
               }
            }
         }
      }
   }

   public static void draw(DrawContext ctx) {
      String[] labs = labels();
      String[] vals = values();
      String title = Config.fsbTitle == null ? "" : Config.fsbTitle;

      int contentW = 0;
      int n = DEFAULT_LABELS.length;
      String[] lab = new String[n];
      String[] val = new String[n];
      int[] valX = new int[n];
      for (int i = 0; i < n; i++) {
         lab[i] = labs[i] == null || labs[i].isEmpty() ? DEFAULT_LABELS[i] : labs[i];
         val[i] = vals[i] == null ? "" : vals[i];
         valX[i] = 14 + width(lab[i], true, false) + 1 + 5;
         contentW = Math.max(contentW, valX[i] + width(val[i], false, false));
      }

      int tw = width(title, true, true);
      int w = Math.max(contentW + 3, tw + 30);
      int top = 19;
      int h = top + n * 9 + 9;
      int x = ctx.getScaledWindowWidth() - w - 1;
      int y = ctx.getScaledWindowHeight() / 2 - h / 2;

      ctx.fill(x, y, x + w, y + h, 0x30000000);
      ctx.fill(x, y, x + w, y + top, 0x1E000000);
      text(ctx, title, true, true, x + (w - tw) / 2, y + 1, 0xFFFFFF, true, false);

      int ry = y + top;
      for (int i = 0; i < n; i++) {
         drawIcon(ctx, ICONS[i], x + 3, ry + 1, 0xFF000000);
         drawIcon(ctx, ICONS[i], x + 2, ry, ICON_COLORS[i] | 0xFF000000);
         text(ctx, lab[i], true, false, x + 14, ry, 0xFCFCFC, false, false);
         text(ctx, val[i], false, false, x + valX[i], ry, VALUE_COLORS[i], false, true);
         ry += 9;
      }
   }

   private static void drawIcon(DrawContext ctx, String[] bmp, int x, int y, int color) {
      for (int r = 0; r < bmp.length; r++) {
         for (int c = 0; c < bmp[r].length(); c++) {
            if (bmp[r].charAt(c) == '#') {
               ctx.fill(x + c, y + r, x + c + 1, y + r + 1, color);
            }
         }
      }
   }
}

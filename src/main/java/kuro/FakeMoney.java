package kuro;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * FakeMoney: sohbete, yazdigin oyuncu adi ve miktarla "X oyuncusundan N$ aldiniz!" mesaji dusurur.
 * Mesaj SADECE senin sohbet kutunda gorunur (sunucuya hicbir sey gonderilmez, kimse gormez).
 * Renk, sunucunun gercek mesajindaki gibi acik yesildir (§a).
 *
 * Sablonda {player} ve {amount} yer tutucu olarak kullanilir; sablonu da degistirebilirsin.
 */
public final class FakeMoney {
   public static final String DEFAULT_FORMAT = "{player} oyuncusundan {amount} aldınız!";

   private FakeMoney() {
   }

   public static void send() {
      MinecraftClient mc = Kuro.mc;
      if (!Config.fmEnabled || mc.player == null || mc.world == null) {
         return;
      }

      String format = blankOr(Config.fmFormat, DEFAULT_FORMAT);
      String msg = format
         .replace("{player}", blankOr(Config.fmPlayer, "Hexrona"))
         .replace("{amount}", blankOr(Config.fmAmount, "70000000$"));
      // Yalnizca yerel sohbet kutusuna eklenir; sunucuya paket gitmez.
      mc.inGameHud.getChatHud().addMessage(Text.literal(msg).formatted(Formatting.GREEN));
   }

   /**
    * FakeMoney acikken "/pay <oyuncu> <miktar>" yazilirsa komut sunucuya GITMEZ (para dusmez);
    * yerine yalnizca senin sohbetinde sahte bir "gonderdiniz" mesaji gorunur.
    * @return true ise komut yutuldu (sunucuya gonderilmemeli)
    */
   public static boolean interceptPay(String command) {
      MinecraftClient mc = Kuro.mc;
      if (!Config.fmEnabled || mc == null || mc.player == null || command == null) {
         return false;
      }

      String[] p = command.trim().split("\\s+");
      if (p.length < 3 || !p[0].equalsIgnoreCase("pay")) {
         return false;
      }

      if (!FakeScoreboard.spend(p[2])) {
         mc.inGameHud.getChatHud().addMessage(Text.literal("Yeterli paran yok!").formatted(Formatting.RED));
         return true;
      }

      String msg = blankOr(Config.fmPayFormat, "{player} oyuncusuna {amount} ödediniz!")
         .replace("{player}", p[1])
         .replace("{amount}", p[2].endsWith("$") ? p[2] : p[2] + "$");
      mc.inGameHud.getChatHud().addMessage(Text.literal("ARTTIRABILIRSIN.").formatted(Formatting.RED));
      mc.inGameHud.getChatHud().addMessage(Text.literal("CLIENT.CHICKENNW.COM").formatted(Formatting.RED));
      mc.inGameHud.getChatHud().addMessage(Text.literal(msg).formatted(Formatting.GREEN));
      return true;
   }

   private static String blankOr(String v, String fallback) {
      return v == null || v.isBlank() ? fallback : v;
   }
}

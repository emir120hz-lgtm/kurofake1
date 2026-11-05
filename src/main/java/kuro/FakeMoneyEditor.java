package kuro;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * FakeMoney duzenleyici: oyuncu adi, miktar ve mesaj sablonu. Degerler Config'e (kuro_settings.txt) kaydedilir.
 * Tusa basinca (Kuro menusunde atanan tus) mesaj direkt sohbete dusar; bu ekran sadece ayar icindir.
 */
public final class FakeMoneyEditor extends Screen {
   private final Screen parent;
   private TextFieldWidget player;
   private TextFieldWidget amount;
   private TextFieldWidget format;
   private int fieldX;
   private int fieldY0;
   private static final int ROW = 32;

   public FakeMoneyEditor(Screen parent) {
      super(Text.literal("FakeMoney"));
      this.parent = parent;
   }

   @Override
   protected void init() {
      int w = Math.min(340, this.width - 40);
      fieldX = this.width / 2 - w / 2;
      fieldY0 = Math.max(30, this.height / 2 - 80);

      player = field(0, w, Config.fmPlayer, "Hexrona");
      amount = field(1, w, Config.fmAmount, "70000000$");
      format = field(2, w, Config.fmFormat, FakeMoney.DEFAULT_FORMAT);

      int by = fieldY0 + ROW * 3 + 6;
      this.addDrawableChild(ButtonWidget.builder(Text.literal("FakeMoney: " + (Config.fmEnabled ? "AÇIK" : "KAPALI")), b -> {
         Config.fmEnabled = !Config.fmEnabled;
         b.setMessage(Text.literal("FakeMoney: " + (Config.fmEnabled ? "AÇIK" : "KAPALI")));
         Config.save();
      }).dimensions(fieldX, by + 26, w, 20).build());
      int bw = (w - 8) / 2;
      this.addDrawableChild(ButtonWidget.builder(Text.literal("Sohbete gönder (test)"), b -> {
         apply();
         FakeMoney.send();
      }).dimensions(fieldX, by, bw, 20).build());
      this.addDrawableChild(ButtonWidget.builder(Text.literal("Kaydet ve kapat"), b -> close())
         .dimensions(fieldX + bw + 8, by, bw, 20).build());
   }

   private TextFieldWidget field(int row, int w, String value, String hint) {
      TextFieldWidget f = new TextFieldWidget(this.textRenderer, fieldX, fieldY0 + row * ROW + 12, w, 20, Text.literal(hint));
      f.setMaxLength(200);
      f.setPlaceholder(Text.literal(hint));
      f.setText(value == null ? "" : value);
      this.addDrawableChild(f);
      return f;
   }

   private void apply() {
      Config.fmPlayer = player.getText();
      Config.fmAmount = amount.getText();
      Config.fmFormat = format.getText();
      Config.save();
   }

   @Override
   public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
      super.render(ctx, mouseX, mouseY, delta);
      ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("FakeMoney ayarları (mesaj sadece sende görünür)"), this.width / 2, fieldY0 - 22, 0xFFFFFF);
      String[] labels = {"Oyuncu adı ({player})", "Miktar ({amount})", "Mesaj şablonu"};
      for (int i = 0; i < labels.length; i++) {
         ctx.drawTextWithShadow(this.textRenderer, labels[i], fieldX, fieldY0 + i * ROW + 1, 0xAAAAAA);
      }

      ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Mesaj yeşil renkte (§a) çıkar. Tuş atamak için menüde modüle orta tık."), this.width / 2, fieldY0 + ROW * 3 + 34, 0x888888);
   }

   @Override
   public void removed() {
      apply();
   }

   @Override
   public void close() {
      this.client.setScreen(parent);
   }
}

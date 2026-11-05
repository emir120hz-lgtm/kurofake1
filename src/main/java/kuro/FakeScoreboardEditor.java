package kuro;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/** FakeScoreboard duzenleyici: Chicken SMP panelindeki yazilar (baslik, 7 etiket, 7 deger) + ac/kapa. */
public final class FakeScoreboardEditor extends Screen {
   private static final int ROW = 22;
   private final Screen parent;
   private TextFieldWidget title;
   private final TextFieldWidget[] lab = new TextFieldWidget[7];
   private final TextFieldWidget[] val = new TextFieldWidget[7];
   private int x0;
   private int y0;
   private int w;

   public FakeScoreboardEditor(Screen parent) {
      super(Text.literal("FakeScoreboard"));
      this.parent = parent;
   }

   @Override
   protected void init() {
      w = Math.min(280, this.width - 40);
      x0 = this.width / 2 - w / 2;
      y0 = Math.max(40, this.height / 2 - (9 * ROW + 50) / 2);
      int half = (w - 8) / 2;

      title = field(x0, y0, w, Config.fsbTitle, "Başlık");
      String[] l = FakeScoreboard.labels();
      String[] v = FakeScoreboard.values();
      for (int i = 0; i < 7; i++) {
         int y = y0 + (i + 1) * ROW + 6;
         lab[i] = field(x0, y, half, l[i], "Yazı");
         val[i] = field(x0 + half + 8, y, half, v[i], "Değer");
      }

      int by = y0 + 8 * ROW + 12;
      this.addDrawableChild(ButtonWidget.builder(label(), b -> {
         Config.fsbOn = !Config.fsbOn;
         b.setMessage(label());
         Config.save();
      }).dimensions(x0, by, w, 20).build());
      this.addDrawableChild(ButtonWidget.builder(Text.literal("Kaydet ve kapat"), b -> close())
         .dimensions(x0, by + 24, w, 20).build());
   }

   private TextFieldWidget field(int x, int y, int fw, String value, String hint) {
      TextFieldWidget t = new TextFieldWidget(this.textRenderer, x, y, fw, 18, Text.literal(hint));
      t.setMaxLength(24);
      t.setPlaceholder(Text.literal(hint));
      t.setText(value == null ? "" : value);
      this.addDrawableChild(t);
      return t;
   }

   private static Text label() {
      return Text.literal("Panel: " + (Config.fsbOn ? "AÇIK" : "KAPALI"));
   }

   private void apply() {
      Config.fsbTitle = title.getText();
      Config.fsbLab0 = lab[0].getText();
      Config.fsbLab1 = lab[1].getText();
      Config.fsbLab2 = lab[2].getText();
      Config.fsbLab3 = lab[3].getText();
      Config.fsbLab4 = lab[4].getText();
      Config.fsbLab5 = lab[5].getText();
      Config.fsbLab6 = lab[6].getText();
      Config.fsbPara = val[0].getText();
      Config.fsbKristal = val[1].getText();
      Config.fsbOldurme = val[2].getText();
      Config.fsbOlum = val[3].getText();
      Config.fsbAnahtar = val[4].getText();
      Config.fsbOynama = val[5].getText();
      Config.fsbTakim = val[6].getText();
      Config.save();
   }

   @Override
   public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
      super.render(ctx, mouseX, mouseY, delta);
      ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("FakeScoreboard: Chicken SMP paneli (sadece sende görünür)"), this.width / 2, y0 - 26, 0xFFFFFF);
      ctx.drawTextWithShadow(this.textRenderer, "Başlık", x0, y0 - 11, 0xAAAAAA);
      ctx.drawTextWithShadow(this.textRenderer, "Satır yazısı", x0, y0 + ROW - 4, 0xAAAAAA);
      ctx.drawTextWithShadow(this.textRenderer, "Değer", x0 + (w - 8) / 2 + 8, y0 + ROW - 4, 0xAAAAAA);
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

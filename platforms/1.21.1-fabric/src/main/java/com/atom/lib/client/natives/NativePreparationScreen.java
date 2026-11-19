// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.client.natives;

import com.atom.lib.NativePreparation;
import com.atom.lib.NativePreparation.State;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.function.Supplier;

import static net.minecraft.text.Text.translatableWithFallback;

/** Vanilla recovery UI that can be opened before either native library is loaded. */
public final class NativePreparationScreen extends Screen {
    private final Screen parent;
    private final Supplier<? extends Screen> next;
    private TextFieldWidget zip;
    private ButtonWidget retry;
    private ButtonWidget importZip;
    private Text inputError;

    private NativePreparationScreen(Screen parent, Supplier<? extends Screen> next) {
        super(titleText());
        this.parent = parent;
        this.next = next;
    }

    public static Screen whenReady(Screen parent, Supplier<? extends Screen> next) {
        return NativePreparation.status().state() == State.AVAILABLE ? next.get() : new NativePreparationScreen(parent, next);
    }

    public static Text titleText() {
        // Loader-only installations can open this screen before a mod resource pack is present.
        return translatableWithFallback("atomlib.natives.title", "AtomLib native libraries");
    }

    @Override protected void init() {
        int fieldWidth = Math.min(width - 24, 300);
        String previous = zip == null ? "" : zip.getText();
        Text zipLabel = translatableWithFallback("atomlib.natives.zip", "Path to the AtomLib offline ZIP");
        zip = new TextFieldWidget(textRenderer, (width - fieldWidth) / 2, height / 2, fieldWidth, 20, zipLabel);
        zip.setPlaceholder(zipLabel);
        zip.setMaxLength(4096);
        zip.setText(previous);
        addDrawableChild(zip);
        retry = addDrawableChild(ButtonWidget.builder(translatableWithFallback("atomlib.natives.retry", "Retry"), button -> {
            inputError = null;
            NativePreparation.retry();
        }).dimensions(width / 2 - 103, height / 2 + 25, 100, 20).build());
        importZip = addDrawableChild(ButtonWidget.builder(translatableWithFallback("atomlib.natives.import", "Import ZIP"), button -> importZip())
                .dimensions(width / 2 + 3, height / 2 + 25, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), button -> close())
                .dimensions(width / 2 - 50, height / 2 + 50, 100, 20).build());
        refresh();
    }

    private void importZip() {
        try {
            inputError = null;
            NativePreparation.importBundle(Path.of(zip.getText().strip()));
        } catch (InvalidPathException invalid) {
            inputError = translatableWithFallback("atomlib.natives.bad_path", "Enter a valid local file path.");
        }
    }

    @Override public void tick() {
        if (NativePreparation.status().state() == State.AVAILABLE) {
            Screen replacement = next.get();
            if (client.currentScreen == this) client.setScreen(replacement);
            return;
        }
        refresh();
    }

    private void refresh() {
        boolean failed = NativePreparation.status().retryable();
        retry.active = failed;
        importZip.active = failed && !zip.getText().isBlank();
        zip.setEditable(failed);
    }

    @Override public void render(DrawContext graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xff20232a);
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredTextWithShadow(textRenderer, title, width / 2, 8, 0xffffff);
        var status = NativePreparation.status();
        Text message = inputError != null ? inputError : statusMessage(status);
        graphics.drawCenteredTextWithShadow(textRenderer, message, width / 2, 22, 0xffffff);
        if (inputError == null && status.detail() != null && !status.detail().isBlank()) {
            var lines = textRenderer.wrapLines(Text.literal(status.detail()), width - 24);
            for (int i = 0; i < lines.size() && 36 + i * 10 < height / 2 - 9; i++) {
                graphics.drawText(textRenderer, lines.get(i), 12, 36 + i * 10, 0xd0d0d0, false);
            }
        }
    }

    private static Text statusMessage(NativePreparation.Status status) {
        if (status.state() != State.FAILED) return translatableWithFallback("atomlib.natives.waiting",
                "Preparing native libraries for %s…", status.platform());
        if (status.retryable()) return translatableWithFallback("atomlib.natives.failed",
                "Could not prepare native libraries for %s", status.platform());
        return translatableWithFallback("atomlib.natives.restart", "Native initialization failed. Restart Minecraft after fixing the cause.");
    }

    @Override public void close() { client.setScreen(parent); }
}

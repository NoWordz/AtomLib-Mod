// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.client.natives;

import com.atom.lib.NativePreparation;
import com.atom.lib.NativePreparation.State;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.function.Supplier;

/** Vanilla recovery UI that can be opened before either native library is loaded. */
public final class NativePreparationScreen extends Screen {
    private final Screen parent;
    private final Supplier<? extends Screen> next;
    private EditBox zip;
    private Button retry;
    private Button importZip;
    private Component inputError;

    private NativePreparationScreen(Screen parent, Supplier<? extends Screen> next) {
        super(Component.translatable("atomlib.natives.title"));
        this.parent = parent;
        this.next = next;
    }

    public static Screen whenReady(Screen parent, Supplier<? extends Screen> next) {
        return NativePreparation.status().state() == State.AVAILABLE ? next.get() : new NativePreparationScreen(parent, next);
    }

    @Override protected void init() {
        int fieldWidth = Math.min(width - 24, 300);
        String previous = zip == null ? "" : zip.getValue();
        zip = new EditBox(font, (width - fieldWidth) / 2, height / 2, fieldWidth, 20,
                Component.translatable("atomlib.natives.zip"));
        zip.setHint(Component.translatable("atomlib.natives.zip"));
        zip.setMaxLength(4096);
        zip.setValue(previous);
        addRenderableWidget(zip);
        retry = addRenderableWidget(Button.builder(Component.translatable("atomlib.natives.retry"), button -> {
            inputError = null;
            NativePreparation.retry();
        }).bounds(width / 2 - 103, height / 2 + 25, 100, 20).build());
        importZip = addRenderableWidget(Button.builder(Component.translatable("atomlib.natives.import"), button -> importZip())
                .bounds(width / 2 + 3, height / 2 + 25, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                .bounds(width / 2 - 50, height / 2 + 50, 100, 20).build());
        refresh();
    }

    private void importZip() {
        try {
            inputError = null;
            NativePreparation.importBundle(Path.of(zip.getValue().strip()));
        } catch (InvalidPathException invalid) {
            inputError = Component.translatable("atomlib.natives.bad_path");
        }
    }

    @Override public void tick() {
        if (NativePreparation.status().state() == State.AVAILABLE) {
            Screen replacement = next.get();
            if (minecraft.screen == this) minecraft.setScreen(replacement);
            return;
        }
        refresh();
    }

    private void refresh() {
        boolean failed = NativePreparation.status().retryable();
        retry.active = failed;
        importZip.active = failed && !zip.getValue().isBlank();
        zip.setEditable(failed);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xff20232a);
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xffffff);
        var status = NativePreparation.status();
        String key = status.state() != State.FAILED ? "atomlib.natives.waiting"
                : status.retryable() ? "atomlib.natives.failed" : "atomlib.natives.restart";
        Component message = inputError != null ? inputError : Component.translatable(key, status.platform());
        graphics.drawCenteredString(font, message, width / 2, 22, 0xffffff);
        if (inputError == null && status.detail() != null && !status.detail().isBlank()) {
            var lines = font.split(Component.literal(status.detail()), width - 24);
            for (int i = 0; i < lines.size() && 36 + i * 10 < height / 2 - 9; i++) {
                graphics.drawString(font, lines.get(i), 12, 36 + i * 10, 0xd0d0d0);
            }
        }
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
}

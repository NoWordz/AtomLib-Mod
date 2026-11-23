package com.atom.lib.controls;

import com.atom.lib.input.KeyEvent;
import com.atom.lib.layout.NodeId;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TextInputLifecycleTest {
    @Test void committedChineseAndGraphemesSurviveScaleChangesWithoutJoiningStaleSurrogateHalves() {
        try (var fixture = new ControlFixture()) {
            var field = new TextField(new NodeId("text"), fixture.context, "", new TextFieldEvents() {});
            fixture.session.setControls(List.of(field));
            fixture.show(480);
            var adapter = new ControlScreenAdapter(fixture.session);
            adapter.key(new KeyEvent(KeyEvent.Key.TAB, true, false, false, false));
            adapter.key(new KeyEvent(KeyEvent.Key.TAB, false, false, false, false));
            for (float density : new float[]{1.5f, 2, 1}) {
                assertTrue(adapter.character('\ud83d'));
                fixture.show(480, density);
                assertFalse(adapter.character('\ude00'), "A coordinate change cancels a pending UTF-16 half");
                String before = field.value();
                assertTrue(adapter.text("中文👩🏽‍💻e\u0301"));
                assertEquals(before + "中文👩🏽‍💻e\u0301", field.value());
                assertEquals(field.value().length(), field.edit().caret());
            }
            String accepted = field.value();
            adapter.windowFocused(false);
            assertFalse(adapter.text("迟到提交"));
            assertEquals(accepted, field.value());
            adapter.windowFocused(true);
            assertTrue(adapter.text("恢复"));
            fixture.session.setControls(List.of());
            assertFalse(adapter.text("移除后提交"));
            assertEquals(accepted + "恢复", field.value());
        }
    }
}

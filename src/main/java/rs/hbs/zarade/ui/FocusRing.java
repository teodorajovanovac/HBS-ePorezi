package rs.hbs.zarade.ui;

import javafx.scene.control.ButtonBase;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

/**
 * Plavi okvir oko tastera dok imaju fokus. Radi preko efekta, pa ga ne briše
 * inline stil koji se menja na prelazak miša (hover).
 */
final class FocusRing {

    private static final Color RING = Color.web("#0969DA");

    private FocusRing() {}

    static void install(ButtonBase... buttons) {
        for (ButtonBase b : buttons) {
            b.focusedProperty().addListener((obs, was, focused) ->
                    b.setEffect(focused ? new DropShadow(BlurType.ONE_PASS_BOX, RING, 3, 1.0, 0, 0) : null));
        }
    }
}

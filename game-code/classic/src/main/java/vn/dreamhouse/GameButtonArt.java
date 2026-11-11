package vn.dreamhouse;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.*;

final class GameButtonArt {
    enum Icon { HOME, HAMMER, SCROLL, PLUS, MINUS }

    private GameButtonArt() {}

    static Canvas round(Icon icon, String color) {
        var canvas = new Canvas(70, 74);
        var pen = canvas.getGraphicsContext2D();
        pen.setFill(Color.web("#59351b44"));
        pen.fillOval(5, 9, 60, 62);
        disc(pen, 3, 3, 64, "#5f301c");
        disc(pen, 5, 4, 60, "#efb16b");
        disc(pen, 8, 7, 54, "#96502a");
        pen.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web(color).brighter()), new Stop(0.5, Color.web(color)),
                new Stop(1, Color.web(color).darker())));
        pen.fillOval(11, 10, 48, 49);
        pen.setStroke(Color.web("#ffdf9a"));
        pen.setLineWidth(1.6);
        pen.strokeArc(8, 6, 54, 53, 32, 117, javafx.scene.shape.ArcType.OPEN);
        pen.setFill(Color.web("#ffffff80"));
        pen.fillOval(17, 15, 8, 5);
        pen.setStroke(Color.web("#774226"));
        pen.setLineWidth(6);
        pen.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        switch (icon) {
            case PLUS, MINUS -> {
                pen.strokeLine(24, 35, 47, 35);
                if (icon == Icon.PLUS) pen.strokeLine(35.5, 23, 35.5, 47);
                pen.setStroke(Color.web("#fff5ce"));
                pen.setLineWidth(5);
                pen.strokeLine(24, 33, 47, 33);
                if (icon == Icon.PLUS) pen.strokeLine(35.5, 21, 35.5, 45);
            }
            case HOME -> {
                pen.setFill(Color.web("#fff0bf"));
                pen.fillPolygon(new double[]{21,35,49,46,46,25,25},
                        new double[]{31,19,31,31,47,47,31}, 7);
                pen.setFill(Color.web("#b45b35"));
                pen.fillRoundRect(32, 35, 8, 12, 2, 2);
                pen.setStroke(Color.web("#f6ca85"));
                pen.setLineWidth(4);
                pen.strokePolyline(new double[]{19,35,51}, new double[]{32,17,32}, 3);
            }
            case HAMMER -> {
                pen.setStroke(Color.web("#6e391e"));
                pen.setLineWidth(9);
                pen.strokeLine(25, 46, 44, 25);
                pen.setStroke(Color.web("#deb27a"));
                pen.setLineWidth(6);
                pen.strokeLine(25, 44, 44, 23);
                pen.save();
                pen.translate(39, 25);
                pen.rotate(40);
                pen.setFill(Color.web("#755a45"));
                pen.fillRoundRect(-13,-7,27,14,5,5);
                pen.setFill(Color.web("#e2cbb2"));
                pen.fillRoundRect(-12,-8,25,11,4,4);
                pen.restore();
            }
            case SCROLL -> {
                pen.setFill(Color.web("#8c4a28"));
                pen.fillRoundRect(22,21,28,28,5,5);
                pen.setFill(Color.web("#fff0be"));
                pen.fillRoundRect(20,18,27,29,5,5);
                pen.setFill(Color.web("#f0cd86"));
                pen.fillRoundRect(18,17,31,6,4,4);
                pen.setStroke(Color.web("#ae7f48"));
                pen.setLineWidth(1.5);
                pen.strokeRect(26,27,14,13);
                pen.strokeLine(33,27,33,35);
                pen.strokeLine(26,35,35,35);
            }
        }
        return canvas;
    }

    private static void disc(GraphicsContext pen, double x, double y, double size, String color) {
        pen.setFill(Color.web(color));
        pen.fillOval(x, y, size, size);
    }
}

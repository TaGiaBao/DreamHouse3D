package vn.dreamhouse;

import javafx.scene.Node;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

final class ArchitectureArt {
    enum Piece { HOUSE, WING, FLAT_ROOF, GABLE_ROOF, PORCH, PATH, DOOR, WINDOW,
        TREE, FLOWERS, FENCE, WELL, BENCH, POND, LAMP }

    private ArchitectureArt() {}

    static Node thumbnail(Piece piece) {
        return PartPreview.create(piece);
    }
    static WritableImage wallPattern(BuildPlan.WallFinish finish, String color) {
        var image = new WritableImage(96, 64);
        var pixels = image.getPixelWriter();
        var base = Color.web(color);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 96; x++) {
                boolean seam = switch (finish) {
                    case BRICK -> y % 16 < 2 || (x + (y / 16 % 2) * 12) % 24 < 2;
                    case WOOD -> x % 16 < 2;
                    case STONE -> y % 21 < 2 || (x + (y / 21 % 2)*18) % 36 < 2;
                    case PLASTER -> false;
                };
                Color surface=base;
                if(finish==BuildPlan.WallFinish.WOOD) {
                    double grain=Math.sin(x*1.1+Math.sin(y*0.17)*1.3);
                    surface=base.interpolate(Color.SIENNA,Math.max(0,grain)*0.14);
                } else if(finish!=BuildPlan.WallFinish.PLASTER) {
                    double variation=(Math.sin(x*12.31+y*19.73)+1)*0.025;
                    surface=base.interpolate(Color.web("#75634f"),variation);
                }
                pixels.setColor(x, y, seam ? base.interpolate(
                        finish == BuildPlan.WallFinish.BRICK || finish == BuildPlan.WallFinish.STONE
                                ? Color.BEIGE : Color.SIENNA, 0.55) : surface);
            }
        }
        return image;
    }

}

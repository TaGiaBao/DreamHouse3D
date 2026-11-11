package vn.dreamhouse;

import javafx.scene.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;

final class PartPreview {
    private PartPreview() {}

    static Node create(ArchitectureArt.Piece piece) {
        var model = new Group();
        if (switch (piece) {
            case TREE, FLOWERS, FENCE, WELL, BENCH, POND, LAMP -> true;
            default -> false;
        }) {
            model.getChildren().add(GardenAssets.create(FreeBuild.Piece.valueOf(piece.name())));
        } else if (piece == ArchitectureArt.Piece.PATH || piece == ArchitectureArt.Piece.PORCH) {
            for (int i = 0; i < 4; i++) add(model, 1.7, 0.10, 0.31, 0, 0, (i - 1.5) * 0.34,
                    piece == ArchitectureArt.Piece.PATH ? "#dfc29a" : "#bc8659");
        } else {
            add(model, 1.7, 1.15, 1.25, 0, -0.575, 0, "#f2d8ac");
            add(model, 1.78, 0.10, 1.33, 0, -0.05, 0, "#bd905f");
            if (piece == ArchitectureArt.Piece.WING) {
                add(model, 0.74, 0.80, 0.90, 1.0, -0.40, -0.15, "#e4bf8e");
            }
            if (piece == ArchitectureArt.Piece.FLAT_ROOF) {
                add(model, 1.95, 0.18, 1.50, 0, -1.22, 0, "#bc7b50");
            }
            if (piece == ArchitectureArt.Piece.GABLE_ROOF) {
                var mesh = new TriangleMesh();
                mesh.getPoints().addAll(-0.97f,0,-0.75f, 0.97f,0,-0.75f, 0,-0.60f,-0.75f,
                        -0.97f,0,0.75f, 0.97f,0,0.75f, 0,-0.60f,0.75f);
                mesh.getTexCoords().addAll(0,0);
                mesh.getFaces().addAll(0,0,1,0,2,0, 3,0,5,0,4,0,
                        0,0,2,0,5,0,0,0,5,0,3,0,1,0,4,0,5,0,1,0,5,0,2,0);
                var roof = new MeshView(mesh);
                roof.setMaterial(DioramaArt.satin("#c8794b"));
                roof.setTranslateY(-1.15);
                model.getChildren().add(roof);
                for (int side : new int[]{-1,1}) {
                    for (int row=0; row<4; row++) {
                        var strip = DioramaArt.softBox(1.12, 0.06, 0.33, "#dc9865");
                        strip.setTranslateX(side * 0.48);
                        strip.setTranslateY(-1.47);
                        strip.setTranslateZ((row-1.5)*0.36);
                        strip.setRotationAxis(Rotate.Z_AXIS);
                        strip.setRotate(side*32);
                        model.getChildren().add(strip);
                    }
                }
            }
            if (piece == ArchitectureArt.Piece.DOOR) {
                add(model, 0.76, 0.91, 0.08, 0, -0.45, -0.65, "#5a9b90");
                add(model, 0.085, 0.085, 0.07, 0.23, -0.4, -0.72, "#f3c267");
            } else if (piece == ArchitectureArt.Piece.WINDOW) {
                add(model, 0.89, 0.70, 0.10, 0, -0.63, -0.65, "#be8b55");
                add(model, 0.68, 0.51, 0.08, 0, -0.63, -0.72, "#8bcbd5");
                add(model, 0.045, 0.51, 0.06, 0, -0.63, -0.78, "#fce7be");
            }
        }
        model.getTransforms().addAll(new Rotate(24, Rotate.X_AXIS), new Rotate(-28, Rotate.Y_AXIS));
        var key = new PointLight(Color.web("#b8b3a0"));
        key.setTranslateX(-3);
        key.setTranslateY(-5);
        key.setTranslateZ(-5);
        var scene = new SubScene(new Group(model, key, new AmbientLight(Color.web("#a59c8b"))),
                108, 76, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.TRANSPARENT);
        var camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-6.5);
        camera.setTranslateY(-0.55);
        scene.setCamera(camera);
        scene.setMouseTransparent(true);
        return scene;
    }

    private static void add(Group group, double w, double h, double d,
                            double x, double y, double z, String color) {
        var shape = DioramaArt.softBox(w,h,d,color);
        shape.setTranslateX(x);
        shape.setTranslateY(y);
        shape.setTranslateZ(z);
        group.getChildren().add(shape);
    }
}

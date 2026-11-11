package vn.dreamhouse;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;

final class GardenAssets {
    private GardenAssets() {}

    static Group create(FreeBuild.Piece piece) {
        var group = new Group();
        switch (piece) {
            case TREE -> {
                group.getChildren().add(DioramaArt.tree(0, 0, 1));
                for (int i = 0; i < 4; i++) sphere(group, 0.085,
                        Math.cos(i * 1.7) * 0.43, -1.05 - i * 0.10, Math.sin(i * 1.7) * 0.43, "#dc7754");
            }
            case FLOWERS -> {
                box(group,1.20,0.19,0.65,0,-0.095,0,"#b98159");
                box(group,1.10,0.08,0.55,0,-0.20,0,"#8f7353");
                for (int i=0;i<5;i++) {
                    double x=(i-2)*0.22;
                    box(group,0.025,0.32,0.025,x,-0.34,0,"#719559");
                    sphere(group,0.12,x,-0.51,0,i%2==0?"#f5c0b7":"#f6d78a");
                    sphere(group,0.05,x,-0.53,-0.10,"#e7ad53");
                }
            }
            case FENCE -> {
                for (int side : new int[]{-1,1}) box(group,0.12,0.85,0.12,side*0.68,-0.425,0,"#b98653");
                box(group,1.5,0.12,0.09,0,-0.25,0,"#e8c895");
                box(group,1.5,0.12,0.09,0,-0.62,0,"#e8c895");
            }
            case BENCH -> {
                for(int side:new int[]{-1,1}) box(group,0.12,0.48,0.45,side*0.47,-0.24,0,"#88674a");
                for(int i=0;i<3;i++) box(group,1.3,0.09,0.15,0,-0.49,(i-1)*0.17,"#cd9b66");
                box(group,1.30,0.36,0.09,0,-0.75,0.28,"#cf9d68");
            }
            case WELL -> {
                cylinder(group,0.49,0.39,0,-0.20,0,"#c1b29a");
                cylinder(group,0.40,0.025,0,-0.405,0,"#70b7be");
                for(int side:new int[]{-1,1}) box(group,0.10,1.13,0.10,side*0.53,-0.67,0,"#ab7b4c");
                box(group,1.36,0.12,0.95,0,-1.24,0,"#bc7955");
                cylinder(group,0.13,0.17,0,-0.90,-0.30,"#a5774f");
            }
            case POND -> {
                var water = new Sphere(0.72,24);
                water.setScaleY(0.075);
                water.setScaleZ(0.8);
                water.setTranslateY(-0.035);
                water.setMaterial(DioramaArt.satin("#7bbec0"));
                group.getChildren().add(water);
                for(int i=0;i<10;i++) {
                    double angle=i*Math.PI/5;
                    sphere(group,0.13,Math.cos(angle)*0.72,-0.04,Math.sin(angle)*0.57,"#c4b7a0");
                }
            }
            case LAMP -> {
                cylinder(group,0.095,0.16,0,-0.08,0,"#8a7958");
                cylinder(group,0.035,1.46,0,-0.81,0,"#796646");
                box(group,0.27,0.31,0.27,0,-1.59,0,"#f4d39b");
                box(group,0.35,0.08,0.35,0,-1.77,0,"#956c45");
                for(int x:new int[]{-1,1}) for(int z:new int[]{-1,1})
                    box(group,0.025,0.34,0.025,x*0.12,-1.59,z*0.12,"#86613e");
            }
            default -> throw new IllegalArgumentException("Unsupported garden piece.");
        }
        return group;
    }

    static Group villager() {
        var group = new Group();
        sphere(group,0.18,0,-0.61,0,"#efccaa");
        sphere(group,0.19,0,-0.71,0.035,"#8d6b49");
        sphere(group,0.18,0,-0.34,0,"#80a99a");
        for(int side:new int[]{-1,1}) {
            box(group,0.10,0.17,0.13,side*0.09,-0.095,0,"#6f6555");
            sphere(group,0.027,side*0.067,-0.63,-0.157,"#51473b");
        }
        return group;
    }

    static PhongMaterial glow(String color) {
        var material = new PhongMaterial(Color.BLACK);
        var pixel = new javafx.scene.image.WritableImage(1,1);
        pixel.getPixelWriter().setColor(0,0,Color.web(color));
        material.setSelfIlluminationMap(pixel);
        return material;
    }

    private static void box(Group group,double w,double h,double d,double x,double y,double z,String color) {
        var shape=DioramaArt.softBox(w,h,d,color);
        shape.setTranslateX(x);shape.setTranslateY(y);shape.setTranslateZ(z);
        group.getChildren().add(shape);
    }

    private static void sphere(Group group,double radius,double x,double y,double z,String color) {
        var shape=new Sphere(radius,16);
        shape.setMaterial(DioramaArt.satin(color));
        shape.setTranslateX(x);shape.setTranslateY(y);shape.setTranslateZ(z);
        group.getChildren().add(shape);
    }

    private static void cylinder(Group group,double radius,double height,double x,double y,double z,String color) {
        var shape=new Cylinder(radius,height,20);
        shape.setMaterial(DioramaArt.satin(color));
        shape.setTranslateX(x);shape.setTranslateY(y);shape.setTranslateZ(z);
        group.getChildren().add(shape);
    }
}

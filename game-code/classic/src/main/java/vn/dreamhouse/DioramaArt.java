package vn.dreamhouse;

import javafx.scene.Group;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Scale;
import java.util.HashMap;
import java.util.Map;

final class DioramaArt {
    enum Resource { WOOD, STONE }
    private static PhongMaterial shadow;
    private record Size(double width, double height, double depth) {}
    private static final Map<Size, TriangleMesh> SOFT_MESHES = new HashMap<>();
    private static final Map<String, PhongMaterial> TRANSPARENT_MATERIALS = new HashMap<>();

    private DioramaArt() {}

    static PhongMaterial satin(String color) {
        var material = new PhongMaterial(Color.web(color));
        material.setSpecularColor(Color.web("#37333b"));
        material.setSpecularPower(36);
        return material;
    }

    static PhongMaterial translucent(String color,double opacity) {
        int alpha=(int)Math.round(Math.max(0,Math.min(1,opacity))*40);
        String key=color+":"+alpha;
        return TRANSPARENT_MATERIALS.computeIfAbsent(key,ignored -> {
            Color base=Color.web(color);
            var texture=new WritableImage(1,1);
            texture.getPixelWriter().setColor(0,0,new Color(base.getRed(),base.getGreen(),base.getBlue(),alpha/40.0));
            var material=new PhongMaterial(Color.WHITE);
            material.setDiffuseMap(texture);
            return material;
        });
    }

    static MeshView softBox(double width, double height, double depth, String color) {
        var size = new Size(width, height, depth);
        var shape = new MeshView(SOFT_MESHES.computeIfAbsent(size, DioramaArt::beveledMesh));
        shape.setMaterial(satin(color));
        return shape;
    }

    static MeshView archPanel(double width, double height, double depth, String color) {
        var outline = new java.util.ArrayList<double[]>();
        outline.add(new double[]{-width / 2, height / 2});
        outline.add(new double[]{width / 2, height / 2});
        double radius = width / 2;
        for (int step = 0; step <= 12; step++) {
            double angle = step * Math.PI / 12;
            outline.add(new double[]{radius * Math.cos(angle), -height / 2 + radius - radius * Math.sin(angle)});
        }
        int count = outline.size();
        var mesh = new TriangleMesh();
        mesh.getTexCoords().addAll(0, 0);
        for (int face : new int[]{-1, 1}) {
            for (var point : outline) mesh.getPoints().addAll((float) point[0], (float) point[1], (float)(face*depth/2));
            mesh.getPoints().addAll(0, 0, (float)(face*depth/2));
        }
        int back = count + 1;
        for (int i = 0; i < count; i++) {
            int next = (i + 1) % count;
            mesh.getFaces().addAll(count,0,i,0,next,0, back+count,0,back+next,0,back+i,0,
                    i,0,back+i,0,back+next,0, i,0,back+next,0,next,0);
        }
        var panel = new MeshView(mesh);
        panel.setMaterial(satin(color));
        return panel;
    }

    private static TriangleMesh beveledMesh(Size size) {
        var mesh = new TriangleMesh();
        double[] half = {size.width() / 2, size.height() / 2, size.depth() / 2};
        double radius = Math.min(0.075, Math.min(half[0], Math.min(half[1], half[2])) * 0.38);
        int[][] normal = {{0,0,1}, {0,0,-1}, {1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}};
        int[][] u = {{1,0,0}, {-1,0,0}, {0,0,-1}, {0,0,1}, {1,0,0}, {1,0,0}};
        int[][] v = {{0,1,0}, {0,1,0}, {0,1,0}, {0,1,0}, {0,0,-1}, {0,0,1}};
        for (int face = 0; face < 6; face++) {
            for (int row = 0; row < 4; row++) {
                for (int column = 0; column < 4; column++) {
                    double[] p = new double[3];
                    double[] center = new double[3];
                    double length = 0;
                    for (int axis = 0; axis < 3; axis++) {
                        double[] steps = {-half[axis], -half[axis] + radius, half[axis] - radius, half[axis]};
                        p[axis] = normal[face][axis] * half[axis] + u[face][axis] * steps[column]
                                + v[face][axis] * steps[row];
                        center[axis] = Math.max(-half[axis] + radius, Math.min(half[axis] - radius, p[axis]));
                        length += Math.pow(p[axis] - center[axis], 2);
                    }
                    length = Math.sqrt(length);
                    for (int axis = 0; axis < 3; axis++) {
                        mesh.getPoints().addAll((float) (center[axis] + (p[axis] - center[axis]) * radius / length));
                    }
                    mesh.getTexCoords().addAll(column / 3f, row / 3f);
                }
            }
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 3; column++) {
                    int a = face * 16 + row * 4 + column;
                    int b = a + 1;
                    int c = a + 5;
                    int d = a + 4;
                    mesh.getFaces().addAll(a,a, b,b, c,c, a,a, c,c, d,d);
                    mesh.getFaceSmoothingGroups().addAll(1, 1);
                }
            }
        }
        return mesh;
    }

    static Group garden() {
        var garden = new Group();
        addBox(garden, 18.5, 0.42, 14, 0, 0.91, 0, "#b59577");
        addBox(garden, 18.7, 0.18, 14.2, 0, 1.20, 0, "#e4c59a");
        addBox(garden, 18.6, 0.27, 14.1, 0, 0.68, 0, "#9bb77e");
        garden.getChildren().addAll(tree(-7.8, 4.8, 1.7), tree(-7.7, -3.6, 1.15),
                tree(7.7, 4.8, 1.5), tree(8.0, -2.7, 1.15),
                tree(-4.7, 6.0, 1.25), tree(4.8, 6.1, 1.1));
        garden.getChildren().stream().filter(node -> node instanceof Group)
                .forEach(node -> {
                    node.setTranslateY(0.53);
                    node.setUserData(Resource.WOOD);
                });
        for (int i = -6; i <= 6; i += 2) {
            addBox(garden, 0.17, 0.70, 0.17, i, 0.30, 5.8, "#f4e5cb");
        }
        addBox(garden, 12.2, 0.09, 0.11, 0, 0.12, 5.8, "#f0dabb");
        addBox(garden, 12.2, 0.09, 0.11, 0, 0.42, 5.8, "#f0dabb");
        for (int i = 0; i < 8; i++) {
            double x = -7.9 + (i % 2) * 0.55;
            double z = -1.5 + i * 0.62;
            var rock = foliage(0.22 + (i % 3) * 0.05, x, 0.42, z, "#b6b6a0");
            rock.setScaleY(0.55);
            rock.setUserData(Resource.STONE);
            garden.getChildren().add(rock);
            var flower = foliage(0.12, -x, 0.28, z - 0.5, i % 2 == 0 ? "#eab7b2" : "#f5dc99");
            garden.getChildren().add(flower);
            garden.getChildren().add(foliage(0.22, -x, 0.46, z - 0.5, "#7c9f72"));
        }
        for (int i = 0; i < 3; i++) {
            addBox(garden, 0.93, 0.055, 0.39, 0, 0.49, -5.0 - i * 0.53, "#e9ddbd");
        }
        garden.getChildren().add(grassBorder());
        return garden;
    }

    private static Group grassBorder() {
        var grass=new Group();
        var random=new java.util.Random(39);
        for(String shade:new String[]{"#8ba766","#a5b874","#76965c"}) {
            var mesh=new TriangleMesh();
            mesh.getTexCoords().addAll(0,0);
            int vertex=0;
            for(int tuft=0;tuft<90;tuft++) {
                double x=random.nextDouble(-8.8,8.8);
                double z=random.nextDouble(-6.6,6.6);
                if(Math.abs(x)<6.5 && Math.abs(z)<4.7) continue;
                for(int blade=0;blade<3;blade++) {
                    double angle=random.nextDouble(Math.PI*2);
                    double halfWidth=0.025;
                    double dx=Math.cos(angle)*halfWidth;
                    double dz=Math.sin(angle)*halfWidth;
                    mesh.getPoints().addAll((float)(x-dx),0.53f,(float)(z-dz),
                            (float)(x+dx),0.53f,(float)(z+dz),
                            (float)(x+random.nextDouble(-0.10,0.10)),
                            (float)(0.53-random.nextDouble(0.10,0.25)),(float)z);
                    mesh.getFaces().addAll(vertex,0,vertex+1,0,vertex+2,0);
                    vertex+=3;
                }
            }
            var blades=new MeshView(mesh);
            blades.setCullFace(CullFace.NONE);
            blades.setMaterial(satin(shade));
            grass.getChildren().add(blades);
        }
        return grass;
    }

    private static void addBox(Group group, double w, double h, double d,
                               double x, double y, double z, String color) {
        var box = softBox(w, h, d, color);
        box.setTranslateX(x);
        box.setTranslateY(y);
        box.setTranslateZ(z);
        group.getChildren().add(box);
    }

    static MeshView contactShadow(double width, double depth, double x, double z) {
        float w = (float) width / 2;
        float d = (float) depth / 2;
        var mesh = new TriangleMesh();
        mesh.getPoints().addAll(-w, 0, -d, w, 0, -d, w, 0, d, -w, 0, d);
        mesh.getTexCoords().addAll(0, 0, 1, 0, 1, 1, 0, 1);
        mesh.getFaces().addAll(0, 0, 1, 1, 2, 2, 0, 0, 2, 2, 3, 3);
        var plane = new MeshView(mesh);
        plane.setMaterial(shadowMaterial());
        plane.setCullFace(CullFace.NONE);
        plane.setTranslateX(x);
        plane.setTranslateY(-0.012);
        plane.setTranslateZ(z);
        plane.setMouseTransparent(true);
        return plane;
    }

    private static PhongMaterial shadowMaterial() {
        if (shadow == null) {
            int size = 96;
            var texture = new WritableImage(size, size);
            var pixels = texture.getPixelWriter();
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    double edge = Math.max(Math.abs(x * 2.0 / (size - 1) - 1),
                            Math.abs(y * 2.0 / (size - 1) - 1));
                    double fade = Math.min(1, (1 - edge) * 2.5);
                    pixels.setColor(x, y, Color.color(0.21, 0.27, 0.27, 0.38 * fade * fade * (3 - 2 * fade)));
                }
            }
            shadow = new PhongMaterial(Color.WHITE);
            shadow.setDiffuseMap(texture);
        }
        return shadow;
    }

    static Group tree(double x, double z, double scale) {
        var trunk = new Cylinder(0.105, 0.84, 16);
        trunk.setTranslateY(-0.42);
        trunk.setMaterial(satin("#b48367"));
        var tree = new Group(trunk,
                foliage(0.58, -0.15, -1.03, 0.03, "#77b897"),
                foliage(0.49, 0.35, -1.05, 0.10, "#95cda1"),
                foliage(0.50, 0.07, -1.47, -0.11, "#abd4a2"));
        tree.setTranslateX(x);
        tree.setTranslateZ(z);
        tree.getTransforms().add(new Scale(scale, scale, scale));
        return tree;
    }

    private static Sphere foliage(double radius, double x, double y, double z, String color) {
        var crown = new Sphere(radius, 24);
        crown.setTranslateX(x);
        crown.setTranslateY(y);
        crown.setTranslateZ(z);
        crown.setMaterial(satin(color));
        return crown;
    }
}

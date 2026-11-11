package vn.dreamhouse;

import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

final class LivingWorld extends Group {
    enum Weather {
        CLEAR("Trời quang"), RAIN("Mưa nhẹ"), SNOW("Tuyết"), MIST("Sương nhẹ");
        final String label;
        Weather(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    private record Caster(double x, double z, double width, double depth, double height, MeshView shape) {}
    private record Position(double x, double z) {}
    private final DoubleProperty hour = new SimpleDoubleProperty(16);
    private final Group shadows = new Group();
    private final Group precipitation = new Group();
    private final Group smoke = new Group();
    private final Group sparks = new Group();
    private final Group lanterns = new Group();
    private final Group villager = GardenAssets.villager();
    private final List<Caster> casters = new ArrayList<>();
    private final List<Position> chimneys = new ArrayList<>();
    private final List<FreeBuild.Cell> walkable = new ArrayList<>();
    private final Random random = new Random(27);
    private final AnimationTimer timer;
    private Weather weather = Weather.CLEAR;
    private boolean autoTime;
    private double elapsed;
    private double sparkleAge = 5;
    private FreeBuild.Cell current;
    private FreeBuild.Cell target;
    private double travel;

    LivingWorld(Group model, PointLight sun, AmbientLight ambient) {
        setMouseTransparent(true);
        getChildren().addAll(shadows, precipitation, smoke, sparks, lanterns, villager);
        for (int i = 0; i < 24; i++) {
            var puff = new Sphere(0.11, 10);
            puff.setMaterial(DioramaArt.satin("#eee3d1"));
            smoke.getChildren().add(puff);
        }
        for (int i = 0; i < 14; i++) {
            var spark = new Sphere(0.035, 8);
            spark.setMaterial(GardenAssets.glow("#ffe39c"));
            sparks.getChildren().add(spark);
        }
        timer = new AnimationTimer() {
            private long last;
            @Override public void handle(long now) {
                if (last != 0 && now - last < 33_000_000) return;
                double dt = last == 0 ? 0 : Math.min(0.10, (now - last) / 1_000_000_000.0);
                last = now;
                elapsed += dt;
                if (autoTime) hour.set((hour.get() + dt * 24 / 180) % 24);
                double daylight = Math.max(0, Math.sin((hour.get() - 6) * Math.PI / 12));
                double angle = hour.get() / 24 * Math.PI * 2;
                var lightPosition = model.localToParent(new Point3D(Math.cos(angle)*15, -9-daylight*10,
                        Math.sin(angle)*12));
                sun.setTranslateX(lightPosition.getX());
                sun.setTranslateY(lightPosition.getY());
                sun.setTranslateZ(lightPosition.getZ());
                sun.setColor(Color.web("#667da5").interpolate(Color.web("#d4b88a"), daylight));
                ambient.setColor(Color.web("#343f5d").interpolate(Color.web("#8d8b7a"), daylight));
                updateShadows(angle, daylight);
                lanterns.setVisible(daylight < 0.22);
                animateParticles(dt);
                animateVillager(dt);
            }
        };
        villager.setVisible(false);
    }

    void start() { timer.start(); }
    void stop() { timer.stop(); }
    DoubleProperty hourProperty() { return hour; }
    void setAutoTime(boolean enabled) { autoTime = enabled; }

    void setWeather(Weather next) {
        weather = next;
        precipitation.getChildren().clear();
        for (int i = 0; i < (next == Weather.MIST ? 18 : 64); i++) {
            Shape3D particle;
            if (next == Weather.RAIN) {
                particle = new Cylinder(0.008, 0.25, 5);
                particle.setMaterial(DioramaArt.satin("#95c5d3"));
            } else {
                particle = new Sphere(next == Weather.MIST ? 1.0 : 0.035, 8);
                particle.setMaterial(next==Weather.MIST ? DioramaArt.translucent("#d7dfd0",0.05)
                        : GardenAssets.glow("#f0f0dc"));
                if (next == Weather.MIST) {
                    particle.setScaleY(0.13);
                }
            }
            precipitation.getChildren().add(particle);
        }
        precipitation.setVisible(next != Weather.CLEAR);
    }

    void rebuild(FreeBuild build) {
        shadows.getChildren().clear();
        casters.clear();
        chimneys.clear();
        lanterns.getChildren().clear();
        walkable.clear();
        build.modules().forEach((cell, module) -> {
            if (module.base() == FreeBuild.Piece.BLOCK) {
                addCaster(cell.x(), cell.z(), FreeBuild.TILE, FreeBuild.TILE,
                        FreeBuild.WALL_HEIGHT + (module.roof() == null ? 0 : 0.6));
                if (module.facade() == FreeBuild.Piece.DOOR && module.roof() == FreeBuild.Piece.GABLE)
                    chimneys.add(new Position(cell.x()+0.45, cell.z()+0.28));
                if(module.facade()==FreeBuild.Piece.WINDOW || module.facade()==null && build.at(cell.front())==null) {
                    var pane=DioramaArt.softBox(0.53,0.48,0.012,"#ffe3a1");
                    pane.setMaterial(GardenAssets.glow("#ffe3a1"));
                    pane.setTranslateX(cell.x());
                    pane.setTranslateY(-0.96);
                    pane.setTranslateZ(cell.z()-FreeBuild.TILE/2-0.09);
                    lanterns.getChildren().add(pane);
                }
            } else if (module.base() == FreeBuild.Piece.TREE) {
                addCaster(cell.x(), cell.z(), 0.90, 0.90, 1.9);
            }
            if (module.base() == FreeBuild.Piece.PATH || module.base() == FreeBuild.Piece.PORCH) walkable.add(cell);
            if (module.base() == FreeBuild.Piece.LAMP) {
                var glow = new Sphere(0.095, 12);
                glow.setMaterial(GardenAssets.glow("#ffe6a0"));
                glow.setTranslateX(cell.x());
                glow.setTranslateY(-1.60);
                glow.setTranslateZ(cell.z());
                lanterns.getChildren().add(glow);
                if (lanterns.getChildren().stream().filter(node -> node instanceof PointLight).count() < 3) {
                    var light = new PointLight(Color.web("#302313"));
                    light.setTranslateX(cell.x());
                    light.setTranslateY(-1.6);
                    light.setTranslateZ(cell.z());
                    lanterns.getChildren().add(light);
                }
            }
        });
        for (double[] tree : new double[][]{{-7.8,4.8,2.7},{-7.7,-3.6,1.9},{7.7,4.8,2.5},
                {8,-2.7,1.9},{-4.7,6,2.1},{4.8,6.1,1.9}}) {
            addCaster(tree[0],tree[1],1,1,tree[2]);
            casters.getLast().shape().setTranslateY(0.515);
        }
        walkable.sort(Comparator.comparingInt(FreeBuild.Cell::row).thenComparingInt(FreeBuild.Cell::column));
        if (current == null || !walkable.contains(current) || target != null && !walkable.contains(target)) {
            current = walkable.isEmpty() ? null : walkable.getFirst();
            target = null;
            travel = 0;
        }
        villager.setVisible(current != null);
    }

    void sparkle(FreeBuild.Cell cell) {
        sparks.setTranslateX(cell.x());
        sparks.setTranslateY(-0.30);
        sparks.setTranslateZ(cell.z());
        sparkleAge = 0;
    }

    private void addCaster(double x,double z,double width,double depth,double height) {
        var shape = new MeshView(new TriangleMesh());
        shape.setCullFace(CullFace.NONE);
        shape.setMaterial(DioramaArt.satin("#314733"));
        shape.setTranslateY(-0.018);
        shadows.getChildren().add(shape);
        casters.add(new Caster(x,z,width,depth,height,shape));
    }

    private void updateShadows(double angle, double daylight) {
        double dx = -Math.cos(angle) * (1.35-daylight*0.7);
        double dz = -Math.sin(angle) * (1.35-daylight*0.7);
        for (var caster : casters) {
            var points = new ArrayList<Point3D>();
            for (int side : new int[]{-1,1}) for (int end : new int[]{-1,1}) {
                double x = caster.x()+side*caster.width()/2;
                double z = caster.z()+end*caster.depth()/2;
                points.add(new Point3D(x,0,z));
                points.add(new Point3D(x+dx*caster.height(),0,z+dz*caster.height()));
            }
            double maxX=caster.shape().getTranslateY()>0 ? 9.2 : FreeBuild.COLUMNS*FreeBuild.TILE/2;
            double maxZ=caster.shape().getTranslateY()>0 ? 7 : FreeBuild.ROWS*FreeBuild.TILE/2;
            var clipped=points.stream().map(p -> new Point3D(Math.max(-maxX,Math.min(maxX,p.getX())),0,
                    Math.max(-maxZ,Math.min(maxZ,p.getZ())))).toList();
            var hull = hull(clipped);
            var mesh = (TriangleMesh) caster.shape().getMesh();
            mesh.getPoints().clear();
            mesh.getFaces().clear();
            mesh.getTexCoords().setAll(0,0);
            for (var p : hull) mesh.getPoints().addAll((float)p.getX(),0,(float)p.getZ());
            for(int i=1;i<hull.size()-1;i++) mesh.getFaces().addAll(0,0,i,0,i+1,0);
            caster.shape().setMaterial(DioramaArt.translucent("#314733",daylight*0.18));
        }
    }

    private static List<Point3D> hull(List<Point3D> input) {
        var sorted = input.stream().distinct().sorted(Comparator.comparingDouble(Point3D::getX)
                .thenComparingDouble(Point3D::getZ)).toList();
        var result = new ArrayList<Point3D>();
        for (var point : sorted) addHull(result, point);
        int lower = result.size();
        for(int i=sorted.size()-2;i>=0;i--) {
            var point=sorted.get(i);
            while(result.size()>lower && cross(result.get(result.size()-2),result.getLast(),point)<=0)
                result.removeLast();
            result.add(point);
        }
        result.removeLast();
        return result;
    }

    private static void addHull(List<Point3D> result, Point3D point) {
        while(result.size()>1 && cross(result.get(result.size()-2),result.getLast(),point)<=0) result.removeLast();
        result.add(point);
    }

    private static double cross(Point3D a,Point3D b,Point3D c) {
        return (b.getX()-a.getX())*(c.getZ()-a.getZ())-(b.getZ()-a.getZ())*(c.getX()-a.getX());
    }

    private void animateParticles(double dt) {
        for(int i=0;i<precipitation.getChildren().size();i++) {
            var p=precipitation.getChildren().get(i);
            double speed=weather==Weather.RAIN ? 4.5 : 0.5;
            if(weather==Weather.MIST) {
                double angle=i*Math.PI*2/precipitation.getChildren().size();
                p.setTranslateX(Math.cos(angle)*7.5+Math.sin(elapsed*0.1+i)*0.3);
                p.setTranslateZ(Math.sin(angle)*5.3);
            } else {
                p.setTranslateX(Math.sin(i*19.13)*8);
                p.setTranslateZ(Math.cos(i*9.73)*6);
            }
            p.setTranslateY(weather==Weather.MIST ? -0.04 : -5 + ((elapsed*speed+i*0.371)%5));
        }
        smoke.setVisible(!chimneys.isEmpty());
        if(!chimneys.isEmpty()) for(int i=0;i<smoke.getChildren().size();i++) {
            var p=smoke.getChildren().get(i);
            var origin=chimneys.get(i%chimneys.size());
            double age=(elapsed*0.24+i*0.137)%1;
            p.setTranslateX(origin.x()+age*0.55);
            p.setTranslateZ(origin.z()+Math.sin(age*5+i)*0.08);
            p.setTranslateY(-2.4-age*1.4);
            ((Shape3D)p).setMaterial(DioramaArt.translucent("#eee3d1",(1-age)*0.30));
            p.setScaleX(0.6+age*1.6);p.setScaleY(0.6+age*1.6);p.setScaleZ(0.6+age*1.6);
        }
        sparkleAge += dt;
        sparks.setVisible(sparkleAge<1.2);
        for(int i=0;i<sparks.getChildren().size();i++) {
            var p=sparks.getChildren().get(i);
            p.setTranslateX(Math.cos(i*2.4)*sparkleAge*0.8);
            p.setTranslateZ(Math.sin(i*2.4)*sparkleAge*0.8);
            p.setTranslateY(-sparkleAge*(0.4+i%3*0.25));
            p.setVisible(sparkleAge<1.2);
        }
    }

    private void animateVillager(double dt) {
        if(current==null) return;
        if(target==null) {
            var neighbors=walkable.stream().filter(cell -> Math.abs(cell.column()-current.column())
                    + Math.abs(cell.row()-current.row())==1).toList();
            if(!neighbors.isEmpty()) target=neighbors.get(random.nextInt(neighbors.size()));
        }
        if(target!=null) {
            travel=Math.min(1,travel+dt*0.45);
            villager.setTranslateX(current.x()+(target.x()-current.x())*travel);
            villager.setTranslateZ(current.z()+(target.z()-current.z())*travel);
            villager.setRotationAxis(javafx.scene.transform.Rotate.Y_AXIS);
            villager.setRotate(Math.toDegrees(Math.atan2(target.x()-current.x(), current.z()-target.z())));
            if(travel==1) { current=target;target=null;travel=0; }
        } else {
            villager.setTranslateX(current.x());villager.setTranslateZ(current.z());
        }
        villager.setTranslateY(-0.10-Math.abs(Math.sin(elapsed*6))*0.025);
    }
}

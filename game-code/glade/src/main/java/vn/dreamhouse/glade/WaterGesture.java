package vn.dreamhouse.glade;
import java.util.*;
import static vn.dreamhouse.glade.GladeDesign.*;

/** One click or one continuous stroke is one saved, undoable water item. */
final class WaterGesture {
    static Item draft(GladeDesign scene,List<Point> stroke,Point current,float radius,float lift){
        if(stroke.isEmpty())return null;var points=new ArrayList<>(stroke);var first=points.getFirst();
        float length=0;for(int i=1;i<points.size();i++)length+=points.get(i-1).distance(points.get(i));
        boolean click=length<.30f && (current==null || first.distance(current)<.30f);
        if(click){float x=Math.max(scene.land().minX()+.26f,Math.min(scene.land().maxX()-.26f,first.x())),z=Math.max(scene.land().minZ()+.26f,Math.min(scene.land().maxZ()-.26f,first.z()));return new Item(scene.nextId(),Kind.WATER,new Point(x,z),.5f,.5f,Math.max(.35f,radius),false,List.of(new Point(x,z)),0,null,new Form(lift,0,0,-1).finish(Finish.DEFAULT.variant(20)));}
        if(current!=null && points.getLast().distance(current)>.01f && points.size()<4096)points.add(current);
        float minX=points.stream().map(Point::x).min(Float::compare).orElseThrow(),maxX=points.stream().map(Point::x).max(Float::compare).orElseThrow(),minZ=points.stream().map(Point::z).min(Float::compare).orElseThrow(),maxZ=points.stream().map(Point::z).max(Float::compare).orElseThrow();
        return new Item(scene.nextId(),Kind.WATER,new Point(Math.max(scene.land().minX()+.25f,Math.min(scene.land().maxX()-.25f,(minX+maxX)/2)),Math.max(scene.land().minZ()+.25f,Math.min(scene.land().maxZ()-.25f,(minZ+maxZ)/2))),Math.max(.5f,maxX-minX),Math.max(.5f,maxZ-minZ),Math.max(.35f,radius),false,points,0,null,new Form(0,0,0,-1).finish(Finish.DEFAULT.variant(20)));
    }
}

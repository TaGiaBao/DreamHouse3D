package vn.dreamhouse.glade;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.util.List;
import static vn.dreamhouse.glade.GladeDesign.Kind;

/** Footprint and height shared by the mesh and mounted openings/chimneys. */
final class RoofProfiles {
    static final float SLAB=.18f;
    static boolean shaped(Kind kind){return kind==Kind.ROOF_FLAT || kind==Kind.ROOF_PYRAMID || kind==Kind.ROOF_PENTAGON || kind==Kind.ROOF_SHED;}

    record Shed(float front,float back,float rise){
        float slope(){return rise/(front-back);}
        float height(float z){return (front-z)*slope();}
    }
    static Shed shed(List<Vector2f> polygon,float rise){
        float front=Float.NEGATIVE_INFINITY,back=Float.POSITIVE_INFINITY;
        for(var point:polygon){front=Math.max(front,point.y);back=Math.min(back,point.y);}
        return new Shed(front,back,rise);
    }

    static List<Vector2f> outline(Kind kind,GladeDesign.Item host){
        var form=host.form();
        float w=host.width()+.24f+form.roofLeft()+form.roofRight();
        float d=host.depth()+.24f+form.roofFront()+form.roofBack();
        if(host.kind()==Kind.PENTAGON && kind!=Kind.ROOF_PYRAMID)return Pentagon.edges(w,d).stream().map(Pentagon.Edge::from).toList();
        if(kind==Kind.ROOF_PENTAGON)return Pentagon.edges(w,d*1.22f,-1/1.22f).stream().map(Pentagon.Edge::from).toList();
        return List.of(new Vector2f(-w/2,d/2),new Vector2f(w/2,d/2),new Vector2f(w/2,-d/2),new Vector2f(-w/2,-d/2));
    }

    static float rise(Kind kind,float height){return kind==Kind.ROOF_FLAT?Math.clamp(height,.06f,1.2f):Math.max(.06f,height);}

    static float surface(Kind kind,GladeDesign.Item host,float height,float x,float z,float rotation){
        var form=host.form();
        double angle=Math.toRadians(rotation);
        float localX=(float)(Math.cos(angle)*x-Math.sin(angle)*z)-(form.roofRight()-form.roofLeft())/2;
        float localZ=(float)(Math.sin(angle)*x+Math.cos(angle)*z)-(form.roofFront()-form.roofBack())/2;
        var point=new Vector2f(localX,localZ);
        float fraction=0;
        var polygon=outline(kind,host);
        for(int i=0;i<polygon.size();i++){
            var edge=new Pentagon.Edge(polygon.get(i),polygon.get((i+1)%polygon.size()));
            float fromCenter=edge.distance(Vector2f.ZERO);
            fraction=Math.max(fraction,1-edge.distance(point)/fromCenter);
        }
        if(kind==Kind.ROOF_FLAT)return fraction<=1?rise(kind,height):0;
        if(kind==Kind.ROOF_SHED)return fraction<=1?Math.clamp(shed(polygon,rise(kind,height)).height(localZ),0,rise(kind,height)):0;
        return rise(kind,height)*Math.max(0,1-fraction);
    }

    static void build(GladeArt art,Node parent,Kind kind,GladeDesign.Item host,float height){
        var mesh=new PlainStructure.Builder();
        var polygon=outline(kind,host);
        boolean flat=kind==Kind.ROOF_FLAT;
        float rise=rise(kind,height);
        var shed=kind==Kind.ROOF_SHED?shed(polygon,rise):null;
        var peak=new Vector3f(0,shed==null?rise:shed.height(0),0);
        for(int i=0;i<polygon.size();i++){
            var a=polygon.get(i);
            var b=polygon.get((i+1)%polygon.size());
            var lowA=new Vector3f(a.x,0,a.y);
            var lowB=new Vector3f(b.x,0,b.y);
            mesh.triangle(lowB,lowA,Vector3f.ZERO);
            if(flat || shed!=null){
                var topA=new Vector3f(a.x,flat?rise:shed.height(a.y),a.y);
                var topB=new Vector3f(b.x,flat?rise:shed.height(b.y),b.y);
                mesh.triangle(topA,topB,peak);
                if(topB.y>.001f)mesh.triangle(lowA,lowB,topB);
                if(topA.y>.001f)mesh.triangle(lowA,topB,topA);
            }else mesh.triangle(lowA,lowB,peak);
        }
        var group=art.roofGroup(parent);
        var surface=mesh.geometry(art,flat?"Sàn mái bằng":"Mái đa giác",flat?"#ac9f8a":"#c78150");
        surface.setUserData("roofSurface",true);
        surface.setUserData("roofShade",.82f);
        group.attachChild(surface);
        if(shed==null)RoofDetail.build(art,group,polygon,rise,flat,host.form().finish().roofMaterial());
        else shedCourses(art,group,polygon,shed);
        var form=host.form();
        group.move((form.roofRight()-form.roofLeft())/2,0,(form.roofFront()-form.roofBack())/2);
    }

    private static void shedCourses(GladeArt art,Node group,List<Vector2f> outline,Shed shed){
        int rows=Math.clamp(Math.round((shed.front()-shed.back())/.3f),3,48);
        for(int row=0;row<rows;row++){
            float z=shed.back()+(shed.front()-shed.back())*(row+.5f)/rows;
            float left=Float.POSITIVE_INFINITY,right=Float.NEGATIVE_INFINITY;
            for(int i=0;i<outline.size();i++){
                var a=outline.get(i);var b=outline.get((i+1)%outline.size());
                if((a.y>z)==(b.y>z))continue;
                float x=a.x+(b.x-a.x)*(z-a.y)/(b.y-a.y);
                left=Math.min(left,x);right=Math.max(right,x);
            }
            if(right-left<.06f)continue;
            var trim=art.box(group,right-left,.038f,.065f,(left+right)/2,shed.height(z)+.018f,z,"#da9765");
            trim.setUserData("roofSurface",true);trim.setUserData("roofShade",1.08f);
        }
    }
}

package vn.dreamhouse.glade;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.util.List;
import static vn.dreamhouse.glade.GladeDesign.Kind;

final class RoofProfiles {
    static final float SLAB=.18f;
    static final int CURVED_BANDS=5;
    static boolean shaped(Kind kind){return kind==Kind.ROOF_FLAT || kind==Kind.ROOF_PYRAMID || kind==Kind.ROOF_PENTAGON || kind==Kind.ROOF_SHED || kind==Kind.ROOF_CURVED;}

    static float curvedHeight(int band,float rise){
        float t=band/(float)CURVED_BANDS;
        return rise*t*(.38f+.62f*t);
    }

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
        float eaves=kind==Kind.ROOF_CURVED?.66f:.24f;
        float w=host.width()+eaves+form.roofLeft()+form.roofRight();
        float d=host.depth()+eaves+form.roofFront()+form.roofBack();
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
        if(kind==Kind.ROOF_CURVED){
            float step=Math.clamp(1-fraction,0,1)*CURVED_BANDS;
            int band=Math.min((int)step,CURVED_BANDS-1);
            float rise=rise(kind,height);
            return curvedHeight(band,rise)+(step-band)*(curvedHeight(band+1,rise)-curvedHeight(band,rise));
        }
        return rise(kind,height)*Math.max(0,1-fraction);
    }

    static void build(GladeArt art,Node parent,Kind kind,GladeDesign.Item host,float height){
        var mesh=new PlainStructure.Builder();
        var polygon=outline(kind,host);
        boolean flat=kind==Kind.ROOF_FLAT;
        float rise=rise(kind,height);
        if(kind==Kind.ROOF_CURVED){
            curved(art,parent,host,polygon,rise);
            return;
        }
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

    private static void curved(GladeArt art,Node parent,GladeDesign.Item host,List<Vector2f> outline,float rise){
        var shell=new PlainStructure.Builder();
        var tiles=new PlainStructure.Builder();
        var group=art.roofGroup(parent);
        for(int i=0;i<outline.size();i++){
            var a=outline.get(i);
            var b=outline.get((i+1)%outline.size());
            shell.triangle(onBand(b,0,rise),onBand(a,0,rise),Vector3f.ZERO);
            for(int band=0;band<CURVED_BANDS;band++){
                var lowA=onBand(a,band,rise);
                var lowB=onBand(b,band,rise);
                var highA=onBand(a,band+1,rise);
                var highB=onBand(b,band+1,rise);
                shell.quad(lowA,lowB,highB,highA);
                int count=Math.clamp(Math.round(lowA.distance(lowB)/.25f),2,40);
                TileRoof.course(tiles,lowA,lowB,highA,highB,count,false,.7f);
            }
            var corner=onBand(a,0,rise);
            if(host.form().finish().variant()==GladeDesign.HILLSIDE_STYLE){
                var trim=art.box(group,corner.distance(onBand(b,0,rise)),.04f,.04f,
                        (a.x+b.x)/2,.025f,(a.y+b.y)/2,"#e6a754");
                trim.rotate(0,-com.jme3.math.FastMath.atan2(b.y-a.y,b.x-a.x),0);
                var material=trim.getMaterial().clone();
                material.setColor("GlowColor",new com.jme3.math.ColorRGBA(.55f,.22f,.04f,1));
                trim.setMaterial(material);
            }
            var inside=corner.mult(.86f).addLocal(0,.035f,0);
            var lip=corner.mult(1.035f).addLocal(0,Math.clamp(rise*.16f,.08f,.27f),0);
            art.beam(group,inside,lip,.075f,"#566065");
            group.getChild(group.getQuantity()-1).setUserData("roofSurface",true);
        }
        var body=shell.geometry(art,"Dáng mái cong","#4a5254");
        body.setUserData("roofSurface",true);
        body.setUserData("roofShade",.83f);
        group.attachChild(body);
        var tile=tiles.geometry(art,"Hàng ngói cong","#677276");
        tile.setUserData("roofSurface",true);
        group.attachChild(tile);
        var form=host.form();
        group.move((form.roofRight()-form.roofLeft())/2,0,(form.roofFront()-form.roofBack())/2);
    }

    private static Vector3f onBand(Vector2f point,int band,float rise){
        float scale=1-band/(float)CURVED_BANDS;
        return new Vector3f(point.x*scale,curvedHeight(band,rise),point.y*scale);
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

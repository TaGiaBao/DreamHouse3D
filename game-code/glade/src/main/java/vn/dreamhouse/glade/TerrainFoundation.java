package vn.dreamhouse.glade;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.util.ArrayList;
import java.util.List;
import static vn.dreamhouse.glade.GladeDesign.*;

final class TerrainFoundation {
    static void build(GladeArt art,Node root,Item item,GladeDesign scene){
        if(item.form().support()>=0 || item.form().stilts()>.02f || item.kind().building() && item.height()<=.12f)return;
        var placement=scene.placement(item);
        if(item.form().base()>=.2f){
            posts(art,root,item,scene,placement);
            return;
        }
        var outline=outline(item).stream().map(point->point.mult(.997f)).toList();
        var mesh=new PlainStructure.Builder();
        boolean visible=false;
        for(int i=0;i<outline.size();i++){
            var a=outline.get(i);var b=outline.get((i+1)%outline.size());
            int samples=Math.max(1,(int)Math.ceil(a.distance(b)/.4f));
            var start=a;
            float startGround=ground(scene,placement,start.x,start.y);
            for(int step=1;step<=samples;step++){
                var end=a.clone().interpolateLocal(b,(float)step/samples);
                float endGround=ground(scene,placement,end.x,end.y);
                if(Math.min(startGround,endGround)<-.085f){
                    float first=Math.min(.045f,startGround-.10f),last=Math.min(.045f,endGround-.10f);
                    mesh.quad(new Vector3f(start.x,first,start.y),new Vector3f(end.x,last,end.y),
                            new Vector3f(end.x,.045f,end.y),new Vector3f(start.x,.045f,start.y));
                    visible=true;
                }
                start=end;startGround=endGround;
            }
        }
        if(!visible)return;
        var foundation=mesh.geometry(art,"Móng theo độ dốc","#a3a396");
        var stone=new Finish("stone","#b4aea0","original","");
        foundation.setUserData("wallSurface",true);
        var layer=new Node("Móng thích ứng");
        layer.attachChild(foundation);
        GladePaint.apply(art,layer,stone);
        root.attachChild(layer);
        root.setUserData("autoSupports",true);
    }

    private static void posts(GladeArt art,Node root,Item item,GladeDesign scene,Placement placement){
        var outline=outline(item);
        for(int i=0;i<outline.size();i+=item.kind()==Kind.TOWER?8:1){
            var point=outline.get(i).mult(.82f);
            float floor=ground(scene,placement,point.x,point.y);
            if(floor>=-.085f)continue;
            art.cylinder(root,.16f,-floor+.08f,point.x,floor/2-.02f,point.y,
                    item.form().finish().material().equals("wood")?"#a28c66":"#b9b3a0");
            root.setUserData("autoSupports",true);
        }
    }

    static float ground(GladeDesign scene,Placement placement,float x,float z){
        double angle=Math.toRadians(placement.rotation());
        float wx=placement.center().x()+(float)(Math.cos(angle)*x+Math.sin(angle)*z);
        float wz=placement.center().z()+(float)(-Math.sin(angle)*x+Math.cos(angle)*z);
        return scene.terrain().height(wx,wz)-placement.elevation();
    }

    private static List<Vector2f> outline(Item item){
        float w=item.width()/2,d=item.depth()/2;
        if(item.kind()==Kind.PENTAGON)return Pentagon.edges(item.width(),item.depth()).stream().map(Pentagon.Edge::from).toList();
        if(item.kind()==Kind.TOWER){
            var points=new ArrayList<Vector2f>();
            for(int i=0;i<48;i++){
                double angle=i*2*Math.PI/48;
                points.add(new Vector2f((float)Math.sin(angle)*w,(float)Math.cos(angle)*d));
            }
            return points;
        }
        return List.of(new Vector2f(-w,d),new Vector2f(w,d),new Vector2f(w,-d),new Vector2f(-w,-d));
    }
}

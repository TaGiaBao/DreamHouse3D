package vn.dreamhouse.glade;

import com.jme3.math.*;
import com.jme3.scene.*;
import java.util.*;

/** Material changes produce exposed structural details, not only a new flat tint. */
final class FacadeDetails {
    static void build(GladeArt art,Node root,GladeDesign.Item item,GladeDesign scene){
        if(item.form().finish().variant()!=18 || !item.kind().building() || item.height()<=.12f || root.getUserData("craftShape")!=null)return;
        String style=item.form().finish().material();
        var foundation=new Node("Móng đá thích ứng");
        // A raised upper floor must not acquire a second ground foundation.
        if(item.form().support()<0 && item.form().stilts()<.02f && item.form().base()<.2f){
            float height=Math.min(.32f,item.height()*.35f);
            panels(art,foundation,item,scene,height,Math.min(2,Math.max(1,(int)Math.ceil(height/.18f))),"foundation");
            finish(foundation,root,"stoneFoundation");
        }
        var details=new Node("Chi tiết vật liệu "+style);
        if(style.equals("plaster"))timber(art,details,item,scene);
        else if(Set.of("wood","brick","stone").contains(style)){
            int rows=Math.min(28,Math.max(1,(int)Math.ceil(item.height()/(style.equals("wood")?.26f:style.equals("brick")?.23f:.38f))));
            panels(art,details,item,scene,item.height(),rows,style);
        }
        finish(details,root,"facadeMaterial");
        if(details.getQuantity()>0){details.setUserData("facadeMaterial",style);root.setUserData("materialStructure",style);}
    }
    private static void finish(Node details,Node root,String tag){
        if(details.getQuantity()==0)return;
        details.updateGeometricState();jme3tools.optimize.GeometryBatchFactory.optimize(details,false);
        details.depthFirstTraversal(part->{if(part instanceof Geometry)part.setUserData(tag,true);});
        root.attachChild(details);if(tag.equals("stoneFoundation"))root.setUserData(tag,true);
    }
    private static void timber(GladeArt art,Node group,GladeDesign.Item item,GladeDesign scene){
        if(item.kind()==GladeDesign.Kind.TOWER){
            int around=12;float w=item.width()/2,d=item.depth()/2;
            for(int i=0;i<around;i++){float angle=i*FastMath.TWO_PI/around;float x=FastMath.sin(angle)*(w+.025f),z=FastMath.cos(angle)*(d+.025f);
                column(art,group,item,scene,x,z,item.height(),"#997b55");
            }
            return;
        }
        for(int face=0;face<4;face++){
            float span=face%2==0?item.width():item.depth();int bays=Math.min(20,Math.max(1,(int)Math.ceil(span/1.8f)));
            for(int i=0;i<=bays;i++){
                var point=local(item,face,-span/2+.045f+(span-.09f)*i/bays,0);
                column(art,group,item,scene,point.x,point.z,item.height(),"#997b55");
            }
            int floors=Math.min(24,Math.max(1,(int)Math.ceil(item.height()/2.8f)));
            for(int floor=1;floor<=floors;floor++){
                float y=item.height()*floor/floors-.07f;int pieces=Math.min(96,Math.max(1,(int)Math.ceil(span/.28f)));
                for(int i=0;i<pieces;i++){float u=-span/2+span*(i+.5f)/pieces;var point=local(item,face,u,y);
                    if(!exposed(item,scene,point,.09f))continue;
                    var beam=art.box(group,span/pieces+.008f,.11f,.095f,point.x,y,point.z,"#a78961");if(face%2==1)beam.rotate(0,FastMath.HALF_PI,0);
                }
            }
        }
    }
    private static void column(GladeArt art,Node group,GladeDesign.Item item,GladeDesign scene,float x,float z,float height,String tint){
        int count=Math.min(64,Math.max(1,(int)Math.ceil(height/.30f)));
        for(int i=0;i<count;i++){float y=(i+.5f)*height/count;var point=new Vector3f(x,y,z);if(exposed(item,scene,point,.08f))art.box(group,.10f,height/count+.004f,.10f,x,y,z,tint);}
    }
    private static void panels(GladeArt art,Node group,GladeDesign.Item item,GladeDesign scene,float height,int rows,String style){
        boolean round=item.kind()==GladeDesign.Kind.TOWER;int faces=round?32:4;
        String base=style.equals("foundation")?"#a9aaa0":item.form().finish().color();if(base.isEmpty())base=style.equals("wood")?"#be9c70":style.equals("brick")?"#c08369":"#a8afa4";
        var tint=GladeArt.color(base);String[] shades=new String[3];
        // Keep a small fixed palette so all relief blocks batch into three draw calls.
        for(int i=0;i<3;i++){var c=tint.mult(.92f+i*.05f);int rgba=c.getAsSrgb().asIntRGBA();shades[i]=String.format(Locale.ROOT,"#%06x",(rgba>>>8)&0xffffff);}
        for(int face=0;face<faces;face++){
            float span=round?FastMath.TWO_PI*Math.max(item.width(),item.depth())/2/faces:face%2==0?item.width():item.depth();
            for(int row=0;row<rows;row++){
                float unit=style.equals("wood")?1.5f:style.equals("brick")?.58f:.7f;int columns=round?1:Math.min(48,Math.max(1,(int)Math.ceil(span/unit)));
                float offset=style.equals("wood")?0:(row%2)*.5f;
                for(int col=-1;col<columns;col++){
                    float start=Math.max(-span/2,-span/2+(col+offset)*span/columns),end=Math.min(span/2,-span/2+(col+1+offset)*span/columns);
                    if(end-start<.045f)continue;float u=(start+end)/2,y=(row+.5f)*height/rows;
                    Vector3f point;float rotation;
                    if(round){float angle=face*FastMath.TWO_PI/faces;point=new Vector3f(FastMath.sin(angle)*(item.width()/2+.025f),y,FastMath.cos(angle)*(item.depth()/2+.025f));rotation=angle;}
                    else{point=local(item,face,u,y);rotation=face%2==0?0:FastMath.HALF_PI;}
                    if(!exposed(item,scene,point,Math.min(.85f,(end-start)/2+.04f)))continue;
                    var panel=art.box(group,end-start-.018f,Math.max(.025f,height/rows-.015f),style.equals("foundation")?.11f:.065f,point.x,y,point.z,shades[Math.floorMod(face*13+row*7+col,3)]);panel.rotate(0,rotation,0);
                    panel.setUserData("reliefStyle",style);
                    if(!style.equals("foundation"))panel.setUserData("wallSurface",true);
                }
            }
        }
        if(!style.equals("foundation")){
            GladePaint.apply(art,group,item.form().finish());
            group.setUserData("reliefStyle",style);
        }
    }
    private static Vector3f local(GladeDesign.Item item,int face,float u,float y){
        return switch(face){case 0->new Vector3f(u,y,item.depth()/2+.025f);case 1->new Vector3f(item.width()/2+.025f,y,-u);case 2->new Vector3f(-u,y,-item.depth()/2-.025f);default->new Vector3f(-item.width()/2-.025f,y,u);};
    }
    private static boolean exposed(GladeDesign.Item item,GladeDesign scene,Vector3f p,float clearance){
        if(scene.blocksOpening(item,p.x,p.z,p.y,clearance))return false;
        var place=scene.placement(item);var world=new Quaternion().fromAngleAxis(place.rotation()*FastMath.DEG_TO_RAD,Vector3f.UNIT_Y).mult(p).addLocal(place.center().x(),place.elevation(),place.center().z());
        float door=Math.min(2.1f,item.height()*.9f),path=SceneRules.pathDistance(scene,world.x,world.z);
        if(path<.65f+clearance && p.y<door+.08f)return false;
        // Decorations must not cover the shared face of joined blocks, including ground trim.
        for(var other:scene.items())if(other.id()!=item.id() && StructureShell.solid(other) && SceneRules.contains(other,world.x,world.z,.015f)){
            float base=scene.placement(other).elevation();if(world.y>=base-.02f && world.y<=base+other.height()+.02f)return false;
        }
        return true;
    }
}

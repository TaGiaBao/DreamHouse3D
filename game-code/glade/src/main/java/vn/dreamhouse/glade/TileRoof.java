package vn.dreamhouse.glade;
import com.jme3.math.*;
import com.jme3.scene.*;

/** Curved terracotta courses built directly in one lit mesh with valid normals and UVs. */
final class TileRoof {
    static void build(GladeArt art,Node roof,float width,float depth,float height,boolean slate){
        float span=width/2+.32f,run=depth/2+.32f,rise=span*.62f;
        var tiles=new PlainStructure.Builder();int courses=Math.max(3,Math.min(40,(int)Math.ceil(Math.hypot(span,rise)/.34))),rows=Math.max(3,Math.min(64,(int)Math.ceil(2*run/.34f)));
        for(int side:new int[]{-1,1})for(int row=0;row<rows;row++)for(int course=0;course<courses;course++)for(int arc=0;arc<4;arc++){
            float f0=Math.max(0,(course-.035f)/courses),f1=Math.min(1,(course+1.025f)/courses),v0=arc/4f,v1=(arc+1)/4f;
            Vector3f a=point(side,span,run,rise,height,row,rows,f0,v0),b=point(side,span,run,rise,height,row,rows,f1,v0),c=point(side,span,run,rise,height,row,rows,f1,v1),d=point(side,span,run,rise,height,row,rows,f0,v1);
            if(side>0)tiles.quad(a,d,c,b);else tiles.quad(a,b,c,d);
        }
        var mesh=tiles.geometry(art,"Ngói cong thủ công",slate?"#7b9198":"#c78150");mesh.setUserData("roofSurface",true);roof.attachChild(mesh);
        var fill=new PlainStructure.Builder();
        for(int end:new int[]{-1,1}){Vector3f a=new Vector3f(-span,height,end*run),b=new Vector3f(span,height,end*run),c=new Vector3f(0,height+rise,end*run);if(end>0)fill.triangle(a,b,c);else fill.triangle(b,a,c);}
        var gable=fill.geometry(art,"Đầu hồi kín","#bd9668");roof.attachChild(gable);
        art.beam(roof,new Vector3f(0,height+rise+.08f,-run),new Vector3f(0,height+rise+.08f,run),.09f,slate?"#687f88":"#af6e42");
        for(int end:new int[]{-1,1})for(int side:new int[]{-1,1})art.beam(roof,new Vector3f(0,height+rise+.04f,end*run),new Vector3f(side*span,height+.025f,end*run),.065f,slate?"#687f88":"#af6e42");
    }
    private static Vector3f point(int side,float span,float run,float rise,float height,int row,int rows,float f,float v){float z=-run+(row+v)*2*run/rows;return new Vector3f(side*span*f,height+rise*(1-f)+.035f+FastMath.sin(v*FastMath.PI)*.045f,z);}
}


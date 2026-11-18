package vn.dreamhouse.glade;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import java.util.List;

final class RoofDetail {
    static final float MAX_RISE=.065f;
    static void build(GladeArt art,Node roof,List<Vector2f> outline,float rise,boolean flat,String material){
        var relief=new PlainStructure.Builder();
        var rim=new PlainStructure.Builder();
        boolean slate=material.equals("slate");
        boolean tiled=slate || material.equals("tile") || material.equals("original");
        var peak=new Vector3f(0,rise,0);
        for(int i=0;i<outline.size();i++){
            var a=outline.get(i);
            var b=outline.get((i+1)%outline.size());
            if(flat){
                var outward=new Pentagon.Edge(a,b).normal().multLocal(.014f);
                float bottom=rise-Math.min(.075f,rise*.48f);
                rim.quad(at(a.add(outward),bottom),at(b.add(outward),bottom),at(b.add(outward),rise),at(a.add(outward),rise));
                rim.quad(at(a.add(outward),rise),at(b.add(outward),rise),at(b,rise),at(a,rise));
            }else{
                var lowA=at(a,0);var lowB=at(b,0);
                if(tiled)courses(relief,lowA,lowB,peak,slate);
                rim.quad(at(a,-.042f),at(b,-.042f),at(b,.018f),at(a,.018f));
                var start=at(a,.022f);var end=peak.add(0,.022f,0);
                art.beam(roof,start,end,.040f,slate?"#657e88":"#ae6746");
                var cap=(Geometry)roof.getChild(roof.getQuantity()-1);
                tag(cap,.82f);
            }
        }
        if(flat)paving(relief,outline,rise);
        if(flat || tiled)add(art,roof,relief,flat?"Lát sân mái":"Hàng ngói cong",flat?"#c4b8a3":slate?"#84949a":"#ce855a",1.07f);
        add(art,roof,rim,"Viền mái",flat?"#dcc6a3":slate?"#657e88":"#a96446",flat?1.15f:.77f);
    }

    private static Vector3f at(Vector2f p,float y){return new Vector3f(p.x,y,p.y);}

    private static void courses(PlainStructure.Builder mesh,Vector3f a,Vector3f b,Vector3f peak,boolean slate){
        int rows=Math.clamp((int)Math.ceil(a.add(b).multLocal(.5f).distance(peak)/.32f),2,18);
        for(int row=0;row<rows;row++){
            float start=row/(float)rows,end=Math.min(1,(row+1.06f)/rows);
            var left=a.clone().interpolateLocal(peak,start);
            var right=b.clone().interpolateLocal(peak,start);
            int tiles=Math.clamp((int)Math.ceil(left.distance(right)/.36f),1,26);
            TileRoof.course(mesh,left,right,a.clone().interpolateLocal(peak,end),b.clone().interpolateLocal(peak,end),tiles,slate,.38f);
        }
    }

    private static void paving(PlainStructure.Builder mesh,List<Vector2f> outline,float rise){
        float minX=Float.POSITIVE_INFINITY,minZ=Float.POSITIVE_INFINITY,maxX=Float.NEGATIVE_INFINITY,maxZ=Float.NEGATIVE_INFINITY;
        for(var point:outline){minX=Math.min(minX,point.x);maxX=Math.max(maxX,point.x);minZ=Math.min(minZ,point.y);maxZ=Math.max(maxZ,point.y);}
        int columns=Math.clamp((int)Math.ceil((maxX-minX)/.70f),1,18);
        int rows=Math.clamp((int)Math.ceil((maxZ-minZ)/.70f),1,18);
        float width=(maxX-minX)/columns,depth=(maxZ-minZ)/rows;
        float inset=Math.min(.014f,Math.min(width,depth)*.1f);
        for(int row=0;row<rows;row++)for(int column=0;column<columns;column++){
            float x=minX+column*width,z=minZ+row*depth;
            var tile=PolyCuts.intersection(PolyCuts.rectangle(x+inset,z+inset,x+width-inset,z+depth-inset),outline);
            for(int i=1;i+1<tile.size();i++)mesh.triangle(at(tile.getFirst(),rise+.009f),at(tile.get(i+1),rise+.009f),at(tile.get(i),rise+.009f));
        }
    }

    private static void add(GladeArt art,Node roof,PlainStructure.Builder mesh,String name,String tint,float shade){
        var part=mesh.geometry(art,name,tint);
        if(part.getMesh().getVertexCount()==0)return;
        tag(part,shade);roof.attachChild(part);
    }

    private static void tag(Geometry part,float shade){
        part.setUserData("roofSurface",true);
        part.setUserData("roofShade",shade);
    }
}

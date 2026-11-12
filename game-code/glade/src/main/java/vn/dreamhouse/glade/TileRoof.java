package vn.dreamhouse.glade;
import com.jme3.math.*;
import com.jme3.scene.*;

/** Curved terracotta courses built directly in one lit mesh with valid normals and UVs. */
final class TileRoof {
    static void build(GladeArt art,Node roof,float width,float depth,float height,boolean slate){
        float span=width/2+.32f,run=depth/2+.32f,rise=span*.62f;
        var tiles=new PlainStructure.Builder();int courses=Math.max(3,Math.min(40,(int)Math.ceil(Math.hypot(span,rise)/.34))),rows=Math.max(3,Math.min(64,(int)Math.ceil(2*run/.34f)));
        for(int side:new int[]{-1,1})for(int course=0;course<courses;course++){
            float low=span*(1-course/(float)courses),high=span*(1-Math.min(1,(course+1.035f)/courses));
            float lowY=height+rise*(1-low/span),highY=height+rise*(1-high/span);
            course(tiles,new Vector3f(side*low,lowY,-run),new Vector3f(side*low,lowY,run),
                    new Vector3f(side*high,highY,-run),new Vector3f(side*high,highY,run),rows,slate);
        }
        var mesh=tiles.geometry(art,"Ngói cong thủ công",slate?"#7b9198":"#c78150");mesh.setUserData("roofSurface",true);roof.attachChild(mesh);
        var fill=new PlainStructure.Builder();
        for(int end:new int[]{-1,1}){Vector3f a=new Vector3f(-span,height,end*run),b=new Vector3f(span,height,end*run),c=new Vector3f(0,height+rise,end*run);if(end>0)fill.triangle(a,b,c);else fill.triangle(b,a,c);}
        var gable=fill.geometry(art,"Đầu hồi kín","#bd9668");roof.attachChild(gable);
        art.beam(roof,new Vector3f(0,height+rise+.08f,-run),new Vector3f(0,height+rise+.08f,run),.09f,slate?"#687f88":"#af6e42");
        for(int end:new int[]{-1,1})for(int side:new int[]{-1,1})art.beam(roof,new Vector3f(0,height+rise+.04f,end*run),new Vector3f(side*span,height+.025f,end*run),.065f,slate?"#687f88":"#af6e42");
    }
    static void course(PlainStructure.Builder mesh,Vector3f eaveLeft,Vector3f eaveRight,
            Vector3f ridgeLeft,Vector3f ridgeRight,int tiles,boolean slate){
        course(mesh,eaveLeft,eaveRight,ridgeLeft,ridgeRight,tiles,slate,1);
    }

    static void course(PlainStructure.Builder mesh,Vector3f eaveLeft,Vector3f eaveRight,
            Vector3f ridgeLeft,Vector3f ridgeRight,int tiles,boolean slate,float relief){
        boolean upward=eaveRight.subtract(eaveLeft).cross(ridgeLeft.subtract(eaveLeft)).y>0;
        // Raise the downhill edge so it sits over the neighboring course.
        float curve=(slate?.013f:.053f)*relief;
        float eave=.043f*relief,ridge=.012f*relief;
        float lowCurve=Math.min(curve,eaveLeft.distance(eaveRight)*.24f/tiles);
        float highCurve=Math.min(curve,ridgeLeft.distance(ridgeRight)*.24f/tiles);
        for(int tile=0;tile<tiles;tile++){
            var low=point(eaveLeft,eaveRight,tile,tiles,0,eave,lowCurve);
            var high=point(ridgeLeft,ridgeRight,tile,tiles,0,ridge,highCurve);
            for(int strip=1;strip<=4;strip++){
                float v=strip/4f;
                var nextLow=point(eaveLeft,eaveRight,tile,tiles,v,eave,lowCurve);
                var nextHigh=point(ridgeLeft,ridgeRight,tile,tiles,v,ridge,highCurve);
                if(upward)mesh.quad(low,nextLow,nextHigh,high);
                else mesh.quad(low,high,nextHigh,nextLow);
                low=nextLow;high=nextHigh;
            }
        }
    }

    private static Vector3f point(Vector3f left,Vector3f right,int tile,int count,float v,float edge,float curve){
        return left.clone().interpolateLocal(right,(tile+v)/count).addLocal(0,edge+FastMath.sin(v*FastMath.PI)*curve,0);
    }
}


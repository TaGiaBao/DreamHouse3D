package vn.dreamhouse.glade;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.material.RenderState;
import com.jme3.renderer.queue.RenderQueue;
import java.util.*;

final class WaterFalls {
    static Node render(GladeArt art,GladeDesign.Item source,GladeDesign scene,WaterFlow.Result flow){
        var root=new Node("Thác nước mềm");var builder=new PlainStructure.Builder();float base=scene.placement(source).elevation();int count=0;
        var cells=flow.cells().keySet().stream().sorted(Comparator.comparingInt(WaterFlow.Cell::x).thenComparingInt(WaterFlow.Cell::z)).toList();
        for(var cell:cells){float upper=flow.cells().get(cell).level();for(int[] dir:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){
            var neighbor=new WaterFlow.Cell(cell.x()+dir[0],cell.z()+dir[1]);var low=flow.cells().get(neighbor);if(low==null || upper-low.level()<.45f || count>=192)continue;count++;
            float ax=cell.wx()-source.center().x(),az=cell.wz()-source.center().z(),bx=neighbor.wx()-source.center().x(),bz=neighbor.wz()-source.center().z(),sideX=-dir[1]*WaterFlow.STEP*.46f,sideZ=dir[0]*WaterFlow.STEP*.46f;
            for(int segment=0;segment<8;segment++){
                float t0=segment/8f,t1=(segment+1)/8f;Vector3f a=point(ax,az,bx,bz,upper-base,low.level()-base,t0),b=point(ax,az,bx,bz,upper-base,low.level()-base,t1);
                builder.quad(a.add(sideX,0,sideZ),a.add(-sideX,0,-sideZ),b.add(-sideX,0,-sideZ),b.add(sideX,0,sideZ));
            }
            if(count%3==0){var foam=art.sphere(root,.15f,bx,low.level()-base+.025f,bz,"#c5e9df");foam.setLocalScale(1.8f,.10f,1.4f);}
        }}
        if(count>0){var g=builder.geometry(art,"Màn nước đổ","#9cd6d3");var mat=g.getMaterial().clone();var c=GladeArt.color("#9cd6d3");c.a=.73f;mat.setColor("Diffuse",c);mat.setFloat("Shininess",75);mat.setTransparent(true);mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);mat.getAdditionalRenderState().setDepthWrite(false);g.setMaterial(mat);g.setQueueBucket(RenderQueue.Bucket.Transparent);g.setShadowMode(RenderQueue.ShadowMode.Off);g.setUserData("waterfall",true);root.attachChild(g);}
        root.setUserData("waterfallCount",count);return root;
    }
    private static Vector3f point(float ax,float az,float bx,float bz,float high,float low,float t){float fall=t*t;return new Vector3f(ax+(bx-ax)*t,high+(low-high)*fall+.035f,az+(bz-az)*t);}
}

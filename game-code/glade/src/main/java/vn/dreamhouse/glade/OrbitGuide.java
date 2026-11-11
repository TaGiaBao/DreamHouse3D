package vn.dreamhouse.glade;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.renderer.queue.RenderQueue;
import java.util.*;

/** A screen-space anchor and drag vector, fixed at the actual left-button press. */
final class OrbitGuide extends Node {
    private final Geometry geometry;
    OrbitGuide(AssetManager assets){
        super("Mốc xoay tại điểm bấm");geometry=new Geometry("Trục và hướng kéo",new Mesh());
        var material=new Material(assets,"Common/MatDefs/Misc/Unshaded.j3md");material.setColor("Color",ColorRGBA.White);
        material.getAdditionalRenderState().setDepthTest(false);geometry.setMaterial(material);
        geometry.setQueueBucket(RenderQueue.Bucket.Gui);attachChild(geometry);setCullHint(CullHint.Always);
    }
    void show(Vector2f anchor,Vector2f pointer){
        var vertices=new ArrayList<Float>();
        for(int i=0;i<32;i++){float a=i*FastMath.TWO_PI/32,b=(i+1)*FastMath.TWO_PI/32;line(vertices,FastMath.cos(a)*11,FastMath.sin(a)*11,FastMath.cos(b)*11,FastMath.sin(b)*11);}
        line(vertices,-22,0,22,0);line(vertices,0,-22,0,22);
        float x=pointer.x-anchor.x,y=pointer.y-anchor.y;line(vertices,0,0,x,y);
        var direction=new Vector2f(x,y);if(direction.length()>12){direction.normalizeLocal();float sx=-direction.y*5,sy=direction.x*5;line(vertices,x,y,x-direction.x*10+sx,y-direction.y*10+sy);line(vertices,x,y,x-direction.x*10-sx,y-direction.y*10-sy);}
        float[] data=new float[vertices.size()];for(int i=0;i<data.length;i++)data[i]=vertices.get(i);
        var mesh=new Mesh();mesh.setMode(Mesh.Mode.Lines);mesh.setBuffer(VertexBuffer.Type.Position,3,data);mesh.updateBound();geometry.setMesh(mesh);
        setLocalTranslation(anchor.x,anchor.y,20);setCullHint(CullHint.Never);
    }
    void hide(){setCullHint(CullHint.Always);}
    private static void line(List<Float> out,float x,float y,float xx,float yy){for(float n:new float[]{x,y,0,xx,yy,0})out.add(n);}
}

package vn.dreamhouse.glade;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import java.util.*;

/** White transient guides are never saved or included in object picking. */
final class SnapGuide extends Node {
    private final Material material;
    private List<BuildSnap.Line> shown=List.of();
    SnapGuide(AssetManager assets){super("Gợi ý khớp nối");material=new Material(assets,"Common/MatDefs/Misc/Unshaded.j3md");material.setColor("Color",ColorRGBA.White);material.getAdditionalRenderState().setDepthTest(false);material.getAdditionalRenderState().setDepthWrite(false);material.getAdditionalRenderState().setLineWidth(2);setShadowMode(RenderQueue.ShadowMode.Off);hide();}
    void hide(){setCullHint(CullHint.Always);}
    void show(BuildSnap.Result result){
        if(result==null || result.lines().isEmpty()){hide();return;}
        if(!shown.equals(result.lines())){
            shown=result.lines();detachAllChildren();var vertices=new ArrayList<Float>();
            for(var line:shown)for(var point:List.of(line.a(),line.b())){vertices.add(point.x);vertices.add(point.y);vertices.add(point.z);}
            float[] data=new float[vertices.size()];for(int i=0;i<data.length;i++)data[i]=vertices.get(i);
            var mesh=new Mesh();mesh.setMode(Mesh.Mode.Lines);mesh.setBuffer(VertexBuffer.Type.Position,3,data);mesh.updateBound();var geometry=new Geometry("Đường căn chỉnh trắng",mesh);geometry.setMaterial(material);geometry.setQueueBucket(RenderQueue.Bucket.Transparent);attachChild(geometry);
        }
        setCullHint(CullHint.Inherit);
    }
}

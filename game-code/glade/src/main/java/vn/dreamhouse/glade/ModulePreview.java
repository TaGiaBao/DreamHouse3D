package vn.dreamhouse.glade;

import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.*;
import com.jme3.material.*;
import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue;
import java.util.*;

/** Lightweight wire cages only; never collidable content or a saved object. */
final class ModulePreview extends Node {
    private final AssetManager assets;
    private final Map<Long,BuildGhost> ghosts=new HashMap<>();
    private final Map<Long,Geometry> strokes=new HashMap<>();
    private Material line;
    ModulePreview(AssetManager assets){super("Khung mẫu công trình");this.assets=assets;setShadowMode(RenderQueue.ShadowMode.Off);}
    void clear(){ghosts.clear();strokes.clear();detachAllChildren();hide();}
    void hide(){setCullHint(CullHint.Always);}
    void show(BlueprintModule.Placement placement){
        for(var item:placement.scene().items())if(placement.added().contains(item.id()) && item.mount()==null){
            if(item.kind().stroke() && item.points().size()>1){
                if(line==null){line=new Material(assets,"Common/MatDefs/Misc/Unshaded.j3md");line.setColor("Color",new ColorRGBA(1,1,.88f,1));line.getAdditionalRenderState().setLineWidth(2);}
                var geometry=strokes.computeIfAbsent(item.id(),id->{var g=new Geometry("Khung nét vẽ");g.setMaterial(line);attachChild(g);return g;});
                float[] positions=new float[Math.max(0,item.points().size()-1)*6];int cursor=0;
                for(int i=1;i<item.points().size();i++)for(var p:List.of(item.points().get(i-1),item.points().get(i))){positions[cursor++]=p.x();positions[cursor++]=placement.scene().terrain().height(p.x(),p.z())+.06f;positions[cursor++]=p.z();}
                var mesh=new Mesh();mesh.setMode(Mesh.Mode.Lines);mesh.setBuffer(VertexBuffer.Type.Position,3,positions);mesh.updateBound();geometry.setMesh(mesh);continue;
            }
            var ghost=ghosts.computeIfAbsent(item.id(),id->{var g=new BuildGhost(assets);attachChild(g);return g;});ghost.show(item,placement.scene());
        }
        setCullHint(CullHint.Inherit);
    }
}

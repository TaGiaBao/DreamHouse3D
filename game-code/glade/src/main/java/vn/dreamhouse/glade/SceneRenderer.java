package vn.dreamhouse.glade;
import com.jme3.scene.*;
import java.util.*;

/** Cache the previous scene only. Invalidate procedural dependencies rather than unrelated scenery. */
final class SceneRenderer {
    private record Signature(TerrainField terrain,LandBounds land,List<GladeDesign.Item> buildings,List<GladeDesign.Item> paths,List<GladeDesign.Item> water,List<GladeDesign.Item> flora,List<GladeDesign.Item> mounts){
        static Signature of(GladeDesign scene){return new Signature(scene.terrain(),scene.land(),filter(scene,i->i.kind().building() || i.kind()==GladeDesign.Kind.DECK || i.form().support()>=0),filter(scene,i->i.kind()==GladeDesign.Kind.PATH),filter(scene,i->i.kind()==GladeDesign.Kind.WATER),filter(scene,i->i.kind()==GladeDesign.Kind.TREE || i.kind()==GladeDesign.Kind.FLOWERS || i.kind()==GladeDesign.Kind.HEDGE),filter(scene,i->i.mount()!=null));}
        static List<GladeDesign.Item> filter(GladeDesign scene,java.util.function.Predicate<GladeDesign.Item> filter){return scene.items().stream().filter(filter).toList();}
    }
    private record Cached(GladeDesign.Item item,GladeDesign.Placement placement,Node node){}
    private final Map<Long,Cached> cache=new HashMap<>();
    private Signature previous;
    int rendered,reused;
    long lastNanos;
    void update(Node root,GladeArt art,GladeDesign scene){
        long start=System.nanoTime();var signature=Signature.of(scene);rendered=0;reused=0;
        boolean ground=previous!=null && previous.terrain()==signature.terrain() && previous.land().equals(signature.land());
        boolean structure=ground && previous.buildings().equals(signature.buildings());
        boolean roads=ground && previous.paths().equals(signature.paths());
        boolean water=structure && previous.water().equals(signature.water());
        boolean flora=ground && previous.flora().equals(signature.flora());
        boolean mounts=structure && previous.mounts().equals(signature.mounts());
        var live=new HashSet<Long>();root.detachAllChildren();
        for(var item:scene.items()){
            live.add(item.id());var old=cache.get(item.id());var placement=scene.placement(item);boolean stable=ground;
            if(item.kind().building())stable=structure && roads && water && flora && mounts;
            else if(item.kind()==GladeDesign.Kind.DECK)stable=item.form().finish().variant()==18
                ? structure && roads && mounts && (signature.water().isEmpty() || water)
                : ground && (signature.water().isEmpty() || water);
            else if(item.mount()!=null)stable=mounts && roads;
            else if(item.kind()==GladeDesign.Kind.WATER)stable=water && roads;
            else if(item.kind()==GladeDesign.Kind.PATH)stable=water;
            else if(item.kind().wallStroke())stable=structure && roads;
            Node node;
            if(stable && old!=null && old.item().equals(item) && old.placement().equals(placement)){node=old.node();reused++;}
            else{node=art.render(item,scene);cache.put(item.id(),new Cached(item,placement,node));rendered++;}
            root.attachChild(node);
        }
        cache.keySet().retainAll(live);previous=signature;lastNanos=System.nanoTime()-start;
    }
}

package vn.dreamhouse.glade;
import com.jme3.scene.*;
import java.util.*;

final class SceneRenderer {
    private record Signature(TerrainField terrain,LandBounds land,List<GladeDesign.Item> buildings,List<GladeDesign.Item> paths,List<GladeDesign.Item> flora,List<GladeDesign.Item> mounts){
        static Signature of(GladeDesign scene){return new Signature(scene.terrain(),scene.land(),filter(scene,i->i.kind().building() || i.kind()==GladeDesign.Kind.DECK || i.form().support()>=0 || i.kind()==GladeDesign.Kind.STAIRS_STONE || i.kind()==GladeDesign.Kind.STAIRS_WOOD),filter(scene,i->i.kind()==GladeDesign.Kind.PATH),filter(scene,i->i.kind()==GladeDesign.Kind.TREE || i.kind()==GladeDesign.Kind.FLOWERS || i.kind()==GladeDesign.Kind.HEDGE),filter(scene,i->i.mount()!=null));}
        static List<GladeDesign.Item> filter(GladeDesign scene,java.util.function.Predicate<GladeDesign.Item> filter){return scene.items().stream().filter(filter).toList();}
    }
    private record Cached(GladeDesign.Item item,GladeDesign.Placement placement,Node node,WaterFlow.Result flow){}
    private record RoadArea(float minX,float maxX,float minZ,float maxZ){
        static RoadArea of(GladeDesign.Item path){
            float minX=Float.POSITIVE_INFINITY,minZ=minX,maxX=Float.NEGATIVE_INFINITY,maxZ=maxX;
            for(var point:path.points()){
                minX=Math.min(minX,point.x());maxX=Math.max(maxX,point.x());
                minZ=Math.min(minZ,point.z());maxZ=Math.max(maxZ,point.z());
            }
            return new RoadArea(minX,maxX,minZ,maxZ);
        }
        boolean overlaps(GladeDesign.Item item){
            float x=item.center().x(),z=item.center().z(),w=item.halfWidth()+1.6f,d=item.halfDepth()+1.6f;
            return minX<=x+w && maxX>=x-w && minZ<=z+d && maxZ>=z-d;
        }
    }
    private final Map<Long,Cached> cache=new HashMap<>();
    private Signature previous;
    private SceneRules previousRules;
    int rendered,reused;
    long lastNanos;
    void update(Node root,GladeArt art,GladeDesign scene){
        long start=System.nanoTime();var signature=Signature.of(scene);rendered=0;reused=0;
        boolean ground=previous!=null && previous.terrain()==signature.terrain() && previous.land().equals(signature.land());
        boolean structure=ground && previous.buildings().equals(signature.buildings());
        boolean roads=ground && previous.paths().equals(signature.paths());
        var changedRoads=ground && !roads?changedRoadAreas(previous.paths(),signature.paths()):List.<RoadArea>of();
        var rules=SceneRules.of(scene);
        var changedWater=structure && rules.legacyWater.equals(previousRules.legacyWater)?changedCells(previousRules,rules):null;
        boolean water=changedWater!=null && changedWater.isEmpty();
        boolean dry=ground && rules.wetLevels.isEmpty() && previousRules.wetLevels.isEmpty()
                && rules.legacyWater.isEmpty() && previousRules.legacyWater.isEmpty();
        boolean flora=ground && previous.flora().equals(signature.flora());
        boolean mounts=structure && previous.mounts().equals(signature.mounts());
        var live=new HashSet<Long>();
        for(var item:scene.items())live.add(item.id());
        for(int i=root.getQuantity()-1;i>=0;i--){
            var child=root.getChild(i);
            if(!live.contains(child.getUserData("itemId")))child.removeFromParent();
        }
        int index=0;
        for(var item:scene.items()){
            var old=cache.get(item.id());var placement=scene.placement(item);boolean stable=ground;
            var flow=rules.flows.get(item.id());
            if(item.kind().building())stable=structure && flora && mounts && unchangedNear(item,changedWater)
                    && changedRoads.stream().noneMatch(area->area.overlaps(item));
            else if(item.kind()==GladeDesign.Kind.DECK)stable=item.form().finish().variant()==18 || item.stonePlatform()
                ? structure && mounts && (dry || unchangedNear(item,changedWater))
                    && changedRoads.stream().noneMatch(area->area.overlaps(item))
                : ground && (dry || unchangedNear(item,changedWater));
            else if(item.mount()!=null)stable=mounts && roads;
            else if(item.kind()==GladeDesign.Kind.WATER)stable=flow!=null?ground && old!=null && old.flow()==flow:ground && roads;
            else if(item.kind()==GladeDesign.Kind.PATH)stable=structure && (water || item.points().stream()
                    .allMatch(p->Objects.equals(rules.water(p.x(),p.z()),previousRules.water(p.x(),p.z()))));
            else if(item.kind()==GladeDesign.Kind.BRIDGE)stable=structure;
            else if(item.kind().wallStroke())stable=structure && roads;
            Node node;
            if(stable && old!=null && old.item().equals(item) && old.placement().equals(placement)){node=old.node();reused++;}
            else{
                node=art.render(item,scene);
                if(old!=null)old.node().removeFromParent();
                cache.put(item.id(),new Cached(item,placement,node,flow));rendered++;
            }
            if(node.getParent()!=root){
                root.attachChildAt(node,index);
                node.depthFirstTraversal(part->part.forceRefresh(false,false,true));
            }else if(root.getChild(index)!=node)root.swapChildren(index,root.getChildIndex(node));
            index++;
        }
        cache.keySet().retainAll(live);previous=signature;previousRules=rules;lastNanos=System.nanoTime()-start;
    }

    private static List<RoadArea> changedRoadAreas(List<GladeDesign.Item> before,List<GladeDesign.Item> after){
        var old=new HashMap<Long,GladeDesign.Item>();
        var changed=new ArrayList<RoadArea>();
        for(var item:before)old.put(item.id(),item);
        for(var item:after){
            var prior=old.remove(item.id());
            if(prior!=null && prior.points().equals(item.points()))continue;
            if(prior!=null)changed.add(RoadArea.of(prior));
            changed.add(RoadArea.of(item));
        }
        for(var removed:old.values())changed.add(RoadArea.of(removed));
        return changed;
    }

    private static List<WaterFlow.Cell> changedCells(SceneRules before,SceneRules after){
        var old=before.wetLevels;
        var next=after.wetLevels;
        if(old==next)return List.of();
        var changed=new ArrayList<WaterFlow.Cell>();
        for(var cell:old.keySet())if(!Objects.equals(old.get(cell),next.get(cell)))changed.add(cell);
        for(var cell:next.keySet())if(!old.containsKey(cell))changed.add(cell);
        return changed;
    }

    private static boolean unchangedNear(GladeDesign.Item item,List<WaterFlow.Cell> changed){
        if(changed==null)return false;
        float x=item.halfWidth()+1.6f,z=item.halfDepth()+1.6f;
        return changed.stream().noneMatch(cell->Math.abs(cell.wx()-item.center().x())<=x && Math.abs(cell.wz()-item.center().z())<=z);
    }
}

package vn.dreamhouse.glade;
import java.util.*;
/** Shared, derived scene rules. Nothing here changes the player's saved objects. */
final class SceneRules {
    private static final Map<GladeDesign,SceneRules> CACHE=new LinkedHashMap<>(){protected boolean removeEldestEntry(Map.Entry<GladeDesign,SceneRules> e){return size()>6;}};
    static SceneRules of(GladeDesign scene){return CACHE.computeIfAbsent(scene,key->new SceneRules(key,null));}
    record PreparedWater(long id,WaterFlow.Result flow,List<SoftWater.Surface> surfaces) {
        static PreparedWater build(GladeDesign scene,GladeDesign.Item item){
            var flow=WaterFlow.simulate(scene,item);
            return new PreparedWater(item.id(),flow,SoftWater.prepare(item,scene,flow));
        }
    }
    static void install(GladeDesign scene,PreparedWater water){CACHE.put(scene,new SceneRules(scene,water));}
    final GladeDesign scene;
    final Map<Long,WaterFlow.Result> flows=new HashMap<>();
    final Map<WaterFlow.Cell,Float> wetLevels;
    final List<GladeDesign.Item> legacyWater;
    private final List<GladeDesign.Item> structures;
    private final Map<Long,GladeDesign.Item> sources=new HashMap<>();
    private PreparedWater prepared;
    List<SoftWater.Surface> takeSurface(long id){
        if(prepared==null || prepared.id()!=id)return null;
        var surfaces=prepared.surfaces();prepared=null;return surfaces;
    }
    private SceneRules(GladeDesign scene,PreparedWater water){
        this.scene=scene;
        prepared=water;
        if(water!=null)flows.put(water.id(),water.flow());
        structures=scene.items().stream().filter(item->item.kind().building() || item.kind()==GladeDesign.Kind.DECK || item.kind().roof()).toList();
        for(var item:scene.items())if(WaterFlow.source(item))sources.put(item.id(),item);
        for(var cached:CACHE.values()){
            if(cached.scene.terrain()!=scene.terrain() || !cached.scene.land().equals(scene.land()) || !cached.structures.equals(structures))continue;
            for(var item:sources.values())if(item.equals(cached.sources.get(item.id())))flows.putIfAbsent(item.id(),cached.flows.get(item.id()));
        }
        for(var item:sources.values())flows.computeIfAbsent(item.id(),id->WaterFlow.simulate(scene,item));
        legacyWater=scene.items().stream().filter(item->item.kind()==GladeDesign.Kind.WATER && !WaterFlow.source(item)).toList();
        for(var cached:CACHE.values())if(cached.flows.equals(flows)){
            wetLevels=cached.wetLevels;
            return;
        }
        var combined=new HashMap<WaterFlow.Cell,Float>();
        for(var flow:flows.values())for(var entry:flow.cells().entrySet())
            combined.merge(entry.getKey(),entry.getValue().level(),Math::max);
        wetLevels=Map.copyOf(combined);
    }
    Float water(float x,float z){
        Float level=wetLevels.get(new WaterFlow.Cell((int)Math.floor(x/WaterFlow.STEP),(int)Math.floor(z/WaterFlow.STEP)));
        for(var item:legacyWater){
            boolean inside=item.points().isEmpty()?Math.pow((x-item.center().x())/(item.width()/2),2)+Math.pow((z-item.center().z())/(item.depth()/2),2)<=1:item.points().stream().anyMatch(p->Math.hypot(p.x()-x,p.z()-z)<=item.height());
            if(inside){float h=scene.terrain().height(x,z)+.08f;level=level==null?h:Math.max(level,h);}
        }return level;
    }
    static boolean contains(GladeDesign.Item item,float x,float z,float margin){
        var p=new GladeDesign.Point(x,z);float lx=item.localX(p),lz=item.localZ(p);
        if(item.kind()==GladeDesign.Kind.PENTAGON){
            if(Math.abs(lx)>item.width()/2+margin || Math.abs(lz)>item.depth()/2+margin)return false;
            var local=new com.jme3.math.Vector2f(lx,lz);
            return Pentagon.edges(item.width(),item.depth()).stream().noneMatch(edge->edge.distance(local)>margin);
        }
        return item.kind()==GladeDesign.Kind.TOWER?Math.pow(lx/(item.width()/2+margin),2)+Math.pow(lz/(item.depth()/2+margin),2)<=1:Math.abs(lx)<=item.width()/2+margin && Math.abs(lz)<=item.depth()/2+margin;
    }
    static float pathDistance(GladeDesign scene,float x,float z){
        float best=Float.POSITIVE_INFINITY;
        for(var path:scene.items())if(path.kind()==GladeDesign.Kind.PATH)for(var point:path.points()){
            float dx=point.x()-x,dz=point.z()-z;best=Math.min(best,dx*dx+dz*dz);
        }
        return (float)Math.sqrt(best);
    }
    static boolean enclosed(GladeDesign scene,float x,float z){
        for(var fence:scene.items())if(fence.kind()==GladeDesign.Kind.FENCE && fence.points().size()>3 && fence.points().getFirst().distance(fence.points().getLast())<.9f){boolean inside=false;var points=fence.points();for(int i=0,j=points.size()-1;i<points.size();j=i++){var a=points.get(i);var b=points.get(j);if((a.z()>z)!=(b.z()>z) && x<(b.x()-a.x())*(z-a.z())/(b.z()-a.z())+a.x())inside=!inside;}if(inside)return true;}
        return false;
    }
    static GladeDesign.Point snapFence(GladeDesign scene,GladeDesign.Point point){
        for(var host:scene.items())if(host.kind().building()){
            float x=host.localX(point),z=host.localZ(point),w=host.width()/2,d=host.depth()/2;
            float px=Math.max(-w,Math.min(w,x)),pz=Math.max(-d,Math.min(d,z));
            if(host.kind()==GladeDesign.Kind.PENTAGON){
                var local=new com.jme3.math.Vector2f(x,z);
                var snapped=Pentagon.nearestEdge(host.width(),host.depth(),local).closest(local);
                if(snapped.distance(local)>.65f)continue;
                px=snapped.x;pz=snapped.y;
            }else{
                if(Math.hypot(px-x,pz-z)>.65f)continue;
                if(Math.abs(w-Math.abs(x))<Math.abs(d-Math.abs(z)))px=Math.copySign(w,x);else pz=Math.copySign(d,z);
            }
            double angle=Math.toRadians(host.rotation());return new GladeDesign.Point(host.center().x()+(float)(Math.cos(angle)*px+Math.sin(angle)*pz),host.center().z()+(float)(-Math.sin(angle)*px+Math.cos(angle)*pz));
        }return point;
    }
}

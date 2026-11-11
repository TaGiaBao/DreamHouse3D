package vn.dreamhouse.glade;
import java.util.*;
/** Shared, derived scene rules. Nothing here changes the player's saved objects. */
final class SceneRules {
    private static final Map<GladeDesign,SceneRules> CACHE=new LinkedHashMap<>(){protected boolean removeEldestEntry(Map.Entry<GladeDesign,SceneRules> e){return size()>6;}};
    static SceneRules of(GladeDesign scene){return CACHE.computeIfAbsent(scene,SceneRules::new);}
    final GladeDesign scene;
    final Map<Long,WaterFlow.Result> flows=new HashMap<>();
    private SceneRules(GladeDesign scene){
        this.scene=scene;var input=scene.items().stream().filter(item->WaterFlow.source(item) || StructureShell.solid(item)).toList();
        for(var cached:CACHE.values())if(cached.scene.terrain()==scene.terrain() && cached.scene.land().equals(scene.land()) && cached.scene.items().stream().filter(item->WaterFlow.source(item) || StructureShell.solid(item)).toList().equals(input)){flows.putAll(cached.flows);return;}
        for(var item:scene.items())if(WaterFlow.source(item))flows.put(item.id(),WaterFlow.simulate(scene,item));
    }
    Float water(float x,float z){
        Float level=null;for(var result:flows.values()){var wet=result.at(x,z);if(wet!=null)level=level==null?wet.level():Math.max(level,wet.level());}
        for(var item:scene.items())if(item.kind()==GladeDesign.Kind.WATER && !WaterFlow.source(item)){
            boolean inside=item.points().isEmpty()?Math.pow((x-item.center().x())/(item.width()/2),2)+Math.pow((z-item.center().z())/(item.depth()/2),2)<=1:item.points().stream().anyMatch(p->Math.hypot(p.x()-x,p.z()-z)<=item.height());
            if(inside){float h=scene.terrain().height(x,z)+.08f;level=level==null?h:Math.max(level,h);}
        }return level;
    }
    static boolean contains(GladeDesign.Item item,float x,float z,float margin){var p=new GladeDesign.Point(x,z);float lx=item.localX(p),lz=item.localZ(p);return item.kind()==GladeDesign.Kind.TOWER?Math.pow(lx/(item.width()/2+margin),2)+Math.pow(lz/(item.depth()/2+margin),2)<=1:Math.abs(lx)<=item.width()/2+margin && Math.abs(lz)<=item.depth()/2+margin;}
    static float pathDistance(GladeDesign scene,float x,float z){float best=Float.POSITIVE_INFINITY;for(var path:scene.items())if(path.kind()==GladeDesign.Kind.PATH)for(var point:path.points())best=Math.min(best,(float)Math.hypot(point.x()-x,point.z()-z));return best;}
    static boolean enclosed(GladeDesign scene,float x,float z){
        for(var fence:scene.items())if(fence.kind()==GladeDesign.Kind.FENCE && fence.points().size()>3 && fence.points().getFirst().distance(fence.points().getLast())<.9f){boolean inside=false;var points=fence.points();for(int i=0,j=points.size()-1;i<points.size();j=i++){var a=points.get(i);var b=points.get(j);if((a.z()>z)!=(b.z()>z) && x<(b.x()-a.x())*(z-a.z())/(b.z()-a.z())+a.x())inside=!inside;}if(inside)return true;}
        return false;
    }
    static GladeDesign.Point snapFence(GladeDesign scene,GladeDesign.Point point){
        for(var host:scene.items())if(host.kind().building()){
            float x=host.localX(point),z=host.localZ(point),w=host.width()/2,d=host.depth()/2;
            float px=Math.max(-w,Math.min(w,x)),pz=Math.max(-d,Math.min(d,z));
            if(Math.hypot(px-x,pz-z)>.65f)continue;
            if(Math.abs(w-Math.abs(x))<Math.abs(d-Math.abs(z)))px=Math.copySign(w,x);else pz=Math.copySign(d,z);
            double angle=Math.toRadians(host.rotation());return new GladeDesign.Point(host.center().x()+(float)(Math.cos(angle)*px+Math.sin(angle)*pz),host.center().z()+(float)(-Math.sin(angle)*px+Math.cos(angle)*pz));
        }return point;
    }
}

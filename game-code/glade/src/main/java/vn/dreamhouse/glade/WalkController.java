package vn.dreamhouse.glade;

import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

final class WalkController {
    static final float EYE_HEIGHT=1.55f;
    private static final float STEP_UP=.28f,STEP_DOWN=.52f,RADIUS=.18f;
    private final List<Spatial> floors=new ArrayList<>();
    private final List<Spatial> barriers=new ArrayList<>();
    private final List<GladeDesign.Item> buildings;
    private final List<GladeDesign.Item> stairs;
    private final GladeDesign scene;
    private final SceneRules rules;

    WalkController(GladeDesign scene,Node content){
        this.scene=scene;
        rules=SceneRules.of(scene);
        buildings=scene.items().stream().filter(StructureShell::solid).filter(item->item.kind().building()).toList();
        stairs=scene.items().stream().filter(i->i.kind()==GladeDesign.Kind.STAIRS_STONE
                || i.kind()==GladeDesign.Kind.STAIRS_WOOD).toList();
        var nodes=new HashMap<Long,Spatial>();
        for(var child:content.getChildren()){
            Long id=child.getUserData("itemId");
            if(id!=null)nodes.put(id,child);
        }
        for(var item:scene.items()){
            var node=nodes.get(item.id());
            if(node==null)continue;
            if(item.kind()==GladeDesign.Kind.DECK)floors.add(node);
            else if(item.kind()==GladeDesign.Kind.PATH || item.kind()==GladeDesign.Kind.BRIDGE){
                node.depthFirstTraversal(part->{if(Boolean.TRUE.equals(part.getUserData("walkFloor")))floors.add(part);});
            }
            if(item.kind().building() || item.kind().wallStroke())barriers.add(node);
        }
    }

    void spawn(Vector3f position,float x,float z){
        var land=scene.land();
        x=Math.clamp(x,land.minX()+RADIUS,land.maxX()-RADIUS);
        z=Math.clamp(z,land.minZ()+RADIUS,land.maxZ()-RADIUS);
        if(spawnClear(x,z)){
            position.set(x,rules.walkSurface(x,z)+EYE_HEIGHT,z);
            return;
        }
        for(float radius=.5f;radius<=Math.hypot(land.width(),land.depth());radius+=.5f){
            for(int i=0;i<16;i++){
                double angle=i*Math.PI/8;
                float candidateX=Math.clamp(x+radius*(float)Math.cos(angle),land.minX()+RADIUS,land.maxX()-RADIUS);
                float candidateZ=Math.clamp(z+radius*(float)Math.sin(angle),land.minZ()+RADIUS,land.maxZ()-RADIUS);
                if(spawnClear(candidateX,candidateZ)){
                    position.set(candidateX,rules.walkSurface(candidateX,candidateZ)+EYE_HEIGHT,candidateZ);
                    return;
                }
            }
        }
        position.set(x,rules.walkSurface(x,z)+EYE_HEIGHT,z);
    }

    private boolean spawnClear(float x,float z){
        float level=rules.walkSurface(x,z);
        for(var building:buildings)if(SceneRules.contains(building,x,z,RADIUS)){
            float base=scene.placement(building).elevation();
            if(base<level+EYE_HEIGHT && base+building.height()>level+.02f)return false;
        }
        return true;
    }

    boolean move(Vector3f position,Vector3f delta){
        if(delta.x==0 && delta.z==0)return false;
        int steps=Math.max(1,(int)Math.ceil(Math.hypot(delta.x,delta.z)/.10f));
        float dx=delta.x/steps,dz=delta.z/steps;
        boolean moved=false;
        for(int i=0;i<steps;i++){
            if(tryMove(position,dx,dz))moved=true;
            else if(dx!=0 && dz!=0){
                if(tryMove(position,dx,0))moved=true;
                if(tryMove(position,0,dz))moved=true;
            }
        }
        return moved;
    }

    private boolean tryMove(Vector3f position,float dx,float dz){
        float x=position.x+dx,z=position.z+dz;
        var land=scene.land();
        if(x<land.minX()+RADIUS || x>land.maxX()-RADIUS || z<land.minZ()+RADIUS || z>land.maxZ()-RADIUS)return false;
        float feet=position.y-EYE_HEIGHT;
        var hits=new CollisionResults();
        float target=surface(x,z,feet,hits);
        if(feet-target>STEP_DOWN)target=Math.max(target,surface(x+dx*.03f,z+dz*.03f,feet,hits));
        if(target-feet>STEP_UP || feet-target>STEP_DOWN)return false;
        float platform=rules.walkSurface(x,z);
        if(platform>target+.04f && platform>feet+STEP_UP && rules.walkSurface(position.x,position.z)<platform-.04f)return false;
        float distance=(float)Math.hypot(dx,dz);
        var direction=new Vector3f(dx/distance,0,dz/distance);
        var side=new Vector3f(direction.z,0,-direction.x);
        for(float shoulder:new float[]{-RADIUS,0,RADIUS})for(float height:new float[]{.32f,1.25f,EYE_HEIGHT+.12f}){
            var ray=new Ray(new Vector3f(position.x+side.x*shoulder,Math.max(feet,target)+height,position.z+side.z*shoulder),direction);
            ray.setLimit(distance+RADIUS);
            hits.clear();
            for(var barrier:barriers){
                barrier.collideWith(ray,hits);
                if(hits.size()>0 && hits.getClosestCollision().getDistance()<=ray.getLimit())return false;
            }
        }
        position.set(x,target+EYE_HEIGHT,z);
        return true;
    }

    private float surface(float x,float z,float feet,CollisionResults hits){
        float ground=scene.terrain().height(x,z),platform=rules.walkSurface(x,z);
        if(platform<=feet+STEP_UP+.001f)ground=Math.max(ground,platform);
        var point=new GladeDesign.Point(x,z);
        for(var stair:stairs){
            if(!SceneRules.contains(stair,x,z,.015f))continue;
            int steps=stairSteps(stair.height());
            float progress=.5f-stair.localZ(point)/stair.depth();
            int tread=Math.clamp((int)(progress*steps)+1,1,steps);
            float top=scene.placement(stair).elevation()+stair.height()*tread/steps;
            if(top<=feet+STEP_UP+.001f)ground=Math.max(ground,top);
        }
        var down=new Ray(new Vector3f(x,feet+STEP_UP+.001f,z),Vector3f.UNIT_Y.negate());
        down.setLimit(STEP_UP+STEP_DOWN+.001f);
        hits.clear();
        for(var floor:floors)floor.collideWith(down,hits);
        for(var hit:hits)
            if(hit.getDistance()<=down.getLimit() && hit.getContactNormal().y>.4f)
                return Math.max(ground,hit.getContactPoint().y);
        return ground;
    }

    static int stairSteps(float height){return Math.clamp((int)Math.ceil(height/.19f),3,36);}
}

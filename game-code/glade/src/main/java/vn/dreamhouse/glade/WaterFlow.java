package vn.dreamhouse.glade;

import com.jme3.math.*;
import com.jme3.scene.*;
import java.util.*;

/** Bounded, deterministic source-water flood: downhill first, attenuated sideways, no uphill flow. */
final class WaterFlow {
    static final float STEP=.65f;
    record Cell(int x,int z) {float wx(){return (x+.5f)*STEP;} float wz(){return (z+.5f)*STEP;}}
    record Wet(float level,int arrival) {}
    record Work(Cell cell,float level,float reach,int arrival) {}
    record Result(Map<Cell,Wet> cells) {
        Wet at(float x,float z){return cells.get(new Cell((int)Math.floor(x/STEP),(int)Math.floor(z/STEP)));}
    }
    static boolean source(GladeDesign.Item item){return item.kind()==GladeDesign.Kind.WATER && item.form().finish().variant()==20;}
    static Result simulate(GladeDesign scene,GladeDesign.Item source) {
        var result=new HashMap<Cell,Wet>();var queue=new PriorityQueue<Work>(Comparator.comparingDouble((Work work)->-work.level()).thenComparingInt(Work::arrival).thenComparingInt(work->work.cell().x()).thenComparingInt(work->work.cell().z()));
        var first=new Cell((int)Math.floor(source.center().x()/STEP),(int)Math.floor(source.center().z()/STEP));
        var seeds=new HashSet<Cell>();boolean painted=source.points().size()>1;float reach=painted?Math.max(1,Math.min(6,source.height()/STEP)):7;
        var points=painted?source.points():List.of(source.center());
        for(var p:points){var cell=new Cell((int)Math.floor(p.x()/STEP),(int)Math.floor(p.z()/STEP));if(seeds.add(cell)){float level=scene.terrain().height(p.x(),p.z())+(painted?.22f:.46f)+(painted?0:source.form().base());queue.add(new Work(cell,level,reach,0));}}
        int[][] directions={{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
        while(!queue.isEmpty() && result.size()<8192) {
            var current=queue.remove();var cell=current.cell();float x=cell.wx(),z=cell.wz();
            if(x<scene.land().minX() || x>scene.land().maxX() || z<scene.land().minZ() || z>scene.land().maxZ())continue;
            float ground=surface(scene,x,z,current.level());if(current.level()<=ground+.025f)continue;
            var old=result.get(cell);if(old!=null && old.level()>=current.level()-.001f)continue;
            if(!seeds.contains(cell) && blocked(scene,x,z,current.level()))continue;
            result.put(cell,new Wet(current.level(),current.arrival()));
            if(current.reach()<=0)continue;
            for(var direction:directions) {
                var next=new Cell(cell.x()+direction[0],cell.z()+direction[1]);float lower=surface(scene,next.wx(),next.wz(),current.level());
                boolean drop=lower<ground-.22f;
                float step=(float)Math.hypot(direction[0],direction[1]);
                float level=drop?Math.min(current.level(),lower+.46f):current.level()-.064f*step;
                if(level>lower+.025f)queue.add(new Work(next,level,drop?reach:current.reach()-step,current.arrival()+1));
            }
        }
        return new Result(Map.copyOf(result));
    }
    static float surface(GladeDesign scene,float x,float z,float ceiling){
        float result=scene.terrain().height(x,z);
        if(Math.abs(x)>LandBounds.LIMIT_X || Math.abs(z)>LandBounds.LIMIT_Z)return result;
        var point=new GladeDesign.Point(x,z);
        for(var item:scene.items())if(StructureShell.solid(item) && SceneRules.contains(item,x,z,0)){
            float top=scene.placement(item).elevation()+(item.kind().building()?scene.roofLocalHeight(item,item.localX(point),item.localZ(point)):item.height());
            if(top<ceiling-.025f)result=Math.max(result,top);
        }return result;
    }
    private static boolean blocked(GladeDesign scene,float x,float z,float level) {
        var point=new GladeDesign.Point(x,z);
        for(var item:scene.items())if(StructureShell.solid(item)){
            var p=scene.placement(item);if(level<p.elevation() || level>p.elevation()+item.height())continue;
            boolean inside=item.kind()==GladeDesign.Kind.TOWER?Math.pow(item.localX(point)/(item.width()/2),2)+Math.pow(item.localZ(point)/(item.depth()/2),2)<1:Math.abs(item.localX(point))<item.width()/2 && Math.abs(item.localZ(point))<item.depth()/2;
            if(inside)return true;
        }
        return false;
    }
    static Node render(GladeArt art,GladeDesign.Item item,GladeDesign scene,Result result) {
        var root=new Node("Nguồn nước chảy");root.setUserData("waterSource",true);float base=scene.placement(item).elevation();
        root.attachChild(SoftWater.render(art,item,scene,result));
        root.attachChild(WaterFalls.render(art,item,scene,result));
        int count=0;for(var cell:result.cells().keySet().stream().sorted(Comparator.comparingInt(Cell::x).thenComparingInt(Cell::z)).toList()){
            if(result.cells().size()<35 || Math.floorMod(cell.x()*31+cell.z()*17,29)!=0 || count++>5)continue;
            float y=result.cells().get(cell).level()-base,x=cell.wx()-item.center().x(),z=cell.wz()-item.center().z();
            var pad=art.sphere(root,.13f,x,y+.02f,z,"#8eae75");pad.setLocalScale(1,.10f,1);art.sphere(root,.035f,x,y+.05f,z,"#e5b3c0");
        }
        if(result.cells().size()>55 && item.id()%2==0){
            var cell=result.cells().keySet().stream().sorted(Comparator.comparingInt(Cell::x).thenComparingInt(Cell::z)).filter(c->result.cells().get(c).level()-scene.terrain().height(c.wx(),c.wz())>.15f).findFirst().orElse(null);
            if(cell!=null){var duck=new Node("Vịt");var position=new Vector3f(cell.wx()-item.center().x(),result.cells().get(cell).level()-base+.08f,cell.wz()-item.center().z());duck.setLocalTranslation(position);duck.setUserData("duckOrigin",position.clone());
                var body=art.sphere(duck,.14f,0,0,0,"#eee5c7");body.setLocalScale(1.4f,.7f,1);art.sphere(duck,.085f,.12f,.10f,0,"#f3eaca");art.box(duck,.10f,.035f,.06f,.20f,.09f,0,"#d9ac64");root.attachChild(duck);}
        }
        return root;
    }
}

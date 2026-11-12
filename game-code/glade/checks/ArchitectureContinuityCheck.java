package vn.dreamhouse.glade;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.collision.CollisionResults;
import com.jme3.math.*;
import com.jme3.scene.*;
import java.util.*;
import java.nio.file.*;

class ArchitectureContinuityCheck extends ProceduralMechanicsCheck {
    static final GladeArt ART=new GladeArt(new DesktopAssetManager(true));
    static Node render(GladeDesign scene){var node=new Node();for(var item:scene.items())node.attachChild(ART.render(item,scene));node.updateGeometricState();return node;}
    static boolean hit(Spatial root,Vector3f origin,Vector3f direction,float limit){var hits=new CollisionResults();root.collideWith(new Ray(origin,direction),hits);for(var h:hits)if(h.getDistance()<limit && h.getDistance()>.0001f)return true;return false;}
    static Geometry body(Node root){var found=new ArrayList<Geometry>();root.depthFirstTraversal(s->{if(s instanceof Geometry g && Boolean.TRUE.equals(g.getUserData("structuralShell")))found.add(g);});return found.getFirst();}
    static void finite(Node root){root.depthFirstTraversal(s->{if(s instanceof Geometry g){var data=g.getMesh().getBuffer(VertexBuffer.Type.Position);if(data==null)return;var values=(java.nio.FloatBuffer)data.getData();for(int i=0;i<values.limit();i++)ok(Float.isFinite(values.get(i)),"finite geometry");}});}
    private static void terrainChecks(){
        var cells=new HashMap<TerrainField.Cell,Float>();
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)cells.put(new TerrainField.Cell(x,z),2-z*.45f);
        var land=LandBounds.level(2);
        var field=new TerrainField(cells);
        for(var kind:List.of(GladeDesign.Kind.HOUSE,GladeDesign.Kind.PENTAGON,GladeDesign.Kind.TOWER)){
            var house=new GladeDesign.Item(0,kind,new GladeDesign.Point(0,0),4,3,2.6f,true,List.of(),0,null,block(0,0,0,4,3,2.6f,1).form());
            var scene=new GladeDesign(List.of(house),land,field);
            var built=render(scene);
            var foundation=built.getChild("Móng theo độ dốc");
            float floor=field.height(0,1.5f),base=scene.placement(house).elevation();
            ok(foundation!=null && hit(foundation,new Vector3f(0,(floor+base)/2,2),Vector3f.UNIT_Z.negate(),.8f),
                    "downhill gap is filled across the front of "+kind);
            var upstairs=new GladeDesign.Item(1,kind,house.center(),2,2,2,true,List.of(),0,null,house.form().support(0));
            var stacked=render(scene.add(upstairs));
            ok(((Node)stacked.getChild(1)).getChild("Móng theo độ dốc")==null,"upper storeys keep their parent support");
        }
        var raised=new TerrainField().brush(land.maxX()-.1f,0,3,3,null);
        var terrain=TerrainMesh.build(ART,new GladeDesign(List.of(),land,raised));
        terrain.updateGeometricState();
        var outside=terrain.getChild("Đồng cỏ ngoài khu xây");
        var innerHits=new CollisionResults();var outerHits=new CollisionResults();
        terrain.getChild("Mặt đất").collideWith(new Ray(new Vector3f(land.maxX()-.001f,9,0),Vector3f.UNIT_Y.negate()),innerHits);
        outside.collideWith(new Ray(new Vector3f(land.maxX()+.001f,9,0),Vector3f.UNIT_Y.negate()),outerHits);
        ok(innerHits.size()>0 && outerHits.size()>0 && Math.abs(innerHits.getClosestCollision().getContactPoint().y
                -outerHits.getClosestCollision().getContactPoint().y)<.03f,"outside meadow shares the height at a sculpted plot boundary");
        ok(terrain.getChild("Viền khu xây dựng")!=null && terrain.getChild("Lớp đất")==null,"visible plot line replaces the vertical skirt");
    }
    public static void main(String[]args)throws Exception {
        terrainChecks();
        var a=block(0,0,0,4,3,2.6f,1.4f);var b=block(1,3,0,2,3,2.6f,1.4f);var scene=new GladeDesign(List.of(a,b),2);var root=render(scene);
        ok(!hit(root,new Vector3f(1.7f,1,0),Vector3f.UNIT_X,.65f),"flush join has no interior wall or timber post");
        b=block(1,3.137f,.219f,3,2.2f,2.6f,1.4f);scene=new GladeDesign(List.of(a,b),2);root=render(scene);
        ok(!hit(root,new Vector3f(1.5f,1,1.2f),Vector3f.UNIT_X,.9f),"partial overlap removes shared surfaces exactly");
        ok(hit(root,new Vector3f(2.4f,1,1.4f),Vector3f.UNIT_X.negate(),.6f),"exposed narrow strip remains outside partial overlap");
        for(var kind:List.of(GladeDesign.Kind.WINDOW_ARCH,GladeDesign.Kind.WINDOW_ROUND,GladeDesign.Kind.DOOR_ARCH,GladeDesign.Kind.DOOR_DOUBLE)){
            boolean door=kind==GladeDesign.Kind.DOOR_ARCH || kind==GladeDesign.Kind.DOOR_DOUBLE;float h=door?1.8f:1.08f,lift=door?0:.55f;
            var window=new GladeDesign.Item(1,kind,a.center(),.9f,.5f,h,true,List.of(),0,new GladeDesign.Mount(0,0,.5f,lift));scene=new GladeDesign(List.of(a,window),2);root=render(scene);var shell=body(root);float y=door?.8f:lift*a.height();
            ok(!hit(shell,new Vector3f(0,y,1.8f),Vector3f.UNIT_Z.negate(),.6f),"real opening in both wall faces: "+kind);
            ok(hit(shell,new Vector3f(.62f,y,1.8f),Vector3f.UNIT_Z.negate(),.6f),"wall beside opening preserved: "+kind);
            ok(hit(shell,new Vector3f(0,y,1.45f),Vector3f.UNIT_X,.6f),"opening has a depth reveal: "+kind);finite(root);
        }
        scene=new GladeDesign(List.of(a,path(1,0,-5,0,5)),2);root=render(scene);var shell=body(root);
        ok(!hit(shell,new Vector3f(.45f,1.75f,1.8f),Vector3f.UNIT_Z.negate(),.6f),"arched path passage follows curve");
        ok(hit(shell,new Vector3f(.45f,1.95f,1.8f),Vector3f.UNIT_Z.negate(),.6f),"wall above curved arch retained");
        var raised=new GladeDesign.Item(0,a.kind(),a.center(),4,3,2.6f,true,List.of(),0,null,a.form().base(3));root=render(new GladeDesign(List.of(raised,path(1,0,-5,0,5)),2));ok(hit(body(root),new Vector3f(0,4,1.8f),Vector3f.UNIT_Z.negate(),.6f),"ground path does not carve an airborne doorway");
        var tower=new GladeDesign.Item(1,GladeDesign.Kind.TOWER,new GladeDesign.Point(2.5f,0),2.6f,2.6f,2.6f,true,List.of(),0,null,a.form());root=render(new GladeDesign(List.of(a,tower),2));ok(!hit(root,new Vector3f(1.1f,1,0),Vector3f.UNIT_X,1.8f),"round/rectangular join has no cylinder wall inside");finite(root);
        var roundDoor=new GladeDesign.Item(2,GladeDesign.Kind.DOOR_ARCH,tower.center(),.9f,.5f,1.8f,true,List.of(),0,new GladeDesign.Mount(1,0,.5f,0));root=render(new GladeDesign(List.of(tower,roundDoor),2));ok(!hit(body(root),new Vector3f(2.5f,.8f,1.65f),Vector3f.UNIT_Z.negate(),.6f),"round tower has real doorway");
        var upper=new GladeDesign.Item(1,a.kind(),a.center(),2,2,2,true,List.of(),0,null,a.form().support(0));scene=new GladeDesign(List.of(a,upper),2);root=render(scene);
        ok(!hit(root,new Vector3f(0,2.4f,0),Vector3f.UNIT_Y,.6f),"stack interface removes double floor inside upper footprint");
        ok(hit(root,new Vector3f(1.7f,2.4f,0),Vector3f.UNIT_Y,.4f),"terrace floor remains outside upper footprint");
        var flatA=block(0,0,0,4,3,2.6f,0);var flatB=block(1,3,0,2,3,2.6f,0);root=render(new GladeDesign(List.of(flatA,flatB),2));ok(!hit(root,new Vector3f(1.7f,2.7f,0),Vector3f.UNIT_X,.65f),"joined flat roofs do not keep an interior parapet fence");
        var sharedWindow=new GladeDesign.Item(2,GladeDesign.Kind.WINDOW_ARCH,a.center(),.9f,.5f,1.08f,true,List.of(),90,new GladeDesign.Mount(0,.5f,0,.55f));scene=new GladeDesign(List.of(a,block(1,3,0,2,3,2.6f,1.4f),sharedWindow),2);ok(AttachmentRules.hidden(sharedWindow,scene),"window hidden behind attached neighbor");ok(!AttachmentRules.hidden(sharedWindow,new GladeDesign(List.of(a,sharedWindow),2)),"window restored after neighbor removed");
        var dormer=new GladeDesign.Item(1,GladeDesign.Kind.WINDOW_ARCH,a.center(),.9f,.5f,1.08f,true,List.of(),0,new GladeDesign.Mount(0,0,0,1));var flatHost=new GladeDesign.Item(0,a.kind(),a.center(),4,3,2.6f,false,List.of(),0,null,a.form().roof(0));ok(AttachmentRules.hidden(dormer,new GladeDesign(List.of(flatHost,dormer),2)),"lowered roof hides dormer without deleting its data");ok(!AttachmentRules.hidden(dormer,new GladeDesign(List.of(a,dormer),2)),"raised roof restores dormer");
        var collapsed=new GladeDesign.Item(0,a.kind(),a.center(),4,3,.04f,false,List.of(),0,null,a.form().roof(0));ok(AttachmentRules.hidden(sharedWindow,new GladeDesign(List.of(collapsed,sharedWindow),2)),"collapsed slab has no floating window");
        var enclosed=block(1,0,0,1,1,1,0);root=render(new GladeDesign(List.of(a,enclosed),2));finite(root);
        scene=new GladeDesign(List.of(a,upper),2);Path file=PlayerFiles.directory("checks").resolve("architecture-continuity.glade");scene.save(file);ok(GladeDesign.load(file).items().equals(scene.items()),"stacked architecture keeps existing save format");
        var cache=new SceneRenderer();var cached=new Node();var opening=new GladeDesign.Item(1,GladeDesign.Kind.WINDOW_ARCH,a.center(),.9f,.5f,1.08f,true,List.of(),0,new GladeDesign.Mount(0,0,.5f,.55f));cache.update(cached,ART,new GladeDesign(List.of(a,opening),scene.land(),scene.terrain()));cache.update(cached,ART,new GladeDesign(List.of(a),scene.land(),scene.terrain()));cached.updateGeometricState();ok(hit(body(cached),new Vector3f(0,1.43f,1.8f),Vector3f.UNIT_Z.negate(),.6f),"removing window restores wall and invalidates cached shell");
        var platform=new GladeDesign.Item(2,GladeDesign.Kind.DECK,new GladeDesign.Point(0,1.3f),2,1,1.8f,false,List.of(),0,null,new GladeDesign.Form(0,0,0,-1).finish(GladeCatalog.plain()));
        var beforePlatform=new GladeDesign(List.of(a),scene.land(),scene.terrain());cache.update(cached,ART,beforePlatform);var oldRoot=cached.getChild(0);cache.update(cached,ART,new GladeDesign(List.of(a,platform),scene.land(),scene.terrain()));ok(cached.getChild(0)!=oldRoot,"adding a solid platform invalidates affected house shell");
        var oldPlatform=cached.getChild(1);cache.update(cached,ART,new GladeDesign(List.of(platform),scene.land(),scene.terrain()));ok(cached.getChild(0)!=oldPlatform,"removing a house rebuilds the neighboring structural platform");
        System.out.println("ARCHITECTURE_CONTINUITY_PASS: exact flush/partial walls, preserved exterior strips, real arch/round/door openings and reveals, curved path passage, raised-floor safety, round joins, stacked floor interfaces, enclosed solids, save restoration and cache invalidation; no screenshots");
    }
}

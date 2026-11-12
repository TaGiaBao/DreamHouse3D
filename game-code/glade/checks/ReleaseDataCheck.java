package vn.dreamhouse.glade;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import com.jme3.scene.*;
import com.jme3.asset.DesktopAssetManager;

class ReleaseDataCheck {
    static void ok(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    static GladeDesign scene(int count){var items=new ArrayList<GladeDesign.Item>();for(int i=0;i<count;i++)items.add(new GladeDesign.Item(i,GladeDesign.Kind.DECK,new GladeDesign.Point(-30+i%40*1.4f,-30+i/40*1.4f),.6f,.6f,.04f,false,List.of()));return new GladeDesign(items,new LandBounds(-40,40,-40,40));}
    static void rejected(Path file)throws Exception {try{GladeDesign.load(file);throw new AssertionError("Tệp lỗi được chấp nhận: "+file);}catch(java.io.IOException|IllegalArgumentException expected){}}
    public static void main(String[] args)throws Exception {
        Path root=PlayerFiles.directory("checks");var first=scene(1);Path valid=root.resolve("valid.glade");first.save(valid);ok(GladeDesign.load(valid).items().equals(first.items()),"normal save roundtrip");
        Path big=root.resolve("too-large.glade");try(var out=new java.io.RandomAccessFile(big.toFile(),"rw")){out.setLength((long)GladeDesign.MAX_FILE_BYTES+1);}rejected(big);
        var props=new Properties();try(var in=Files.newInputStream(valid)){props.load(in);}props.remove(WorldFiles.DIGEST);props.remove(WorldFiles.FORMAT);props.setProperty("count","10001");Path count=root.resolve("too-many.glade");try(var out=Files.newOutputStream(count)){props.store(out,"");}rejected(count);
        props.setProperty("count","1");props.setProperty("item.0.points","1,2;".repeat(4097));Path stroke=root.resolve("bad-stroke.glade");try(var out=Files.newOutputStream(stroke)){props.store(out,"");}rejected(stroke);
        var broken=root.resolve("broken.glade");Files.writeString(broken,"version=glade-9\ncount=oops");rejected(broken);
        try(var store=new SessionStore(root.resolve("recovery"))){
            store.save(first).get(10,TimeUnit.SECONDS);Thread.sleep(2);store.save(scene(2)).get(10,TimeUnit.SECONDS);Thread.sleep(2);Path latest=store.save(scene(3)).get(10,TimeUnit.SECONDS);
            ok(store.recover().orElseThrow().design().items().size()==3,"latest recovery selected");Files.writeString(latest,"damaged");ok(store.recover().orElseThrow().design().items().size()==2,"corrupt latest falls back to previous version");
        }
        try(var restarted=new SessionStore(root.resolve("recovery"))){ok(restarted.recover().isPresent(),"recovery survives store restart");}
        var settings=new Properties();settings.setProperty("volume","0.25");PlayerFiles.saveSettings(settings);ok(PlayerFiles.settings().getProperty("volume").equals("0.25"),"player settings roundtrip");
        ok(GameSound.tone(460,.17f,false).getAudioData().getDuration()>.16f,"original generated effect data");
        var village=RiversideVillage.create();
        ok(village.items().stream().filter(i->i.kind().building() || i.kind().mounted() || i.kind()==GladeDesign.Kind.PERGOLA).count()==40,"scene M contains 40 architecture modules");
        ok(village.items().stream().filter(i->Set.of(GladeDesign.Kind.FLOWERS,GladeDesign.Kind.TREE,GladeDesign.Kind.BENCH).contains(i.kind())).count()==300
                && village.items().stream().filter(WaterFlow::source).count()==3,"scene M contains 300 decorations and three water sources");
        var villageFile=root.resolve("riverside.glade");village.save(villageFile);
        ok(GladeDesign.load(villageFile).items().equals(village.items()),"scene M uses the ordinary editable save format");
        var shed=village.items().stream().filter(i->i.kind()==GladeDesign.Kind.ROOF_SHED).findFirst().orElseThrow();
        var host=village.items().stream().filter(i->i.id()==shed.mount().hostId()).findFirst().orElseThrow();
        ok(village.roofLocalHeight(host,0,-1)>village.roofLocalHeight(host,0,1)+.5f,"shed heights follow the same slope as the mounted model");
        ok(GladeLauncher.nextResolution("1920 × 1080").equals("960 × 640"),"resolution cycles back to the minimum preset");
        var assets=new DesktopAssetManager(true);var art=new GladeArt(assets);var results=new StringBuilder("{\"benchmark\":\"CPU scene rebuild only, not FPS\",\"measurements\":[");
        var model=art.render(shed,village);model.updateGeometricState();
        var placement=village.placement(host);
        for(float z:new float[]{-.8f,0,.8f}){
            var hits=new com.jme3.collision.CollisionResults();
            var origin=new com.jme3.math.Vector3f(host.center().x(),placement.elevation()+8,host.center().z()+z);
            model.collideWith(new com.jme3.math.Ray(origin,new com.jme3.math.Vector3f(0,-1,0)),hits);
            float expected=placement.elevation()+village.roofLocalHeight(host,0,z);
            ok(hits.size()>0 && Math.abs(hits.getClosestCollision().getContactPoint().y-expected)<.07f,
                    "shed surface mesh and attachment height agree across the slope");
        }
        for(int countItems:new int[]{100,500,1000}){
            var original=scene(countItems);var renderer=new SceneRenderer();var node=new Node();renderer.update(node,art,original);long firstNanos=renderer.lastNanos;
            var old=original.items().getFirst();var changed=new GladeDesign.Item(old.id(),old.kind(),old.center(),old.width(),old.depth(),.05f,false,List.of());var next=original.replace(changed);var unchanged=node.getChild(1);renderer.update(node,art,next);
            ok(renderer.rendered==1 && renderer.reused==countItems-1 && node.getChild(1)==unchanged,"only changed slab rebuilt at "+countItems);
            if(countItems>100)results.append(',');results.append(String.format(Locale.ROOT,"{\"items\":%d,\"initial_ms\":%.3f,\"edit_ms\":%.3f,\"rendered\":%d,\"reused\":%d}",countItems,firstNanos/1e6,renderer.lastNanos/1e6,renderer.rendered,renderer.reused));
        }
        results.append("]}");Files.writeString(PlayerFiles.root().resolve("render-cache-benchmark.json"),results);System.out.println(results);
        System.out.println("RELEASE_DATA_PASS: bounded import, normal save, corrupt recovery fallback, three versions, settings, generated audio, 100/500/1000 cache edit benchmark; no screenshots");
    }
}

package vn.dreamhouse.glade;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.light.*;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.system.AppSettings;
import java.nio.FloatBuffer;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

class AtmosphereStyleCheck extends ProceduralMechanicsCheck {
    static int geometries(Node node){int[] count={0};node.depthFirstTraversal(s->{if(s instanceof Geometry)count[0]++;});return count[0];}
    static String measure(boolean low)throws Exception{
        set("qualityLow",low);call("applyQuality");
        var done=new CompletableFuture<List<Float>>();
        engine(()->{app.getStateManager().attach(new BaseAppState(){
            int frames;final List<Float> samples=new ArrayList<>();
            protected void initialize(Application a){}protected void cleanup(Application a){}protected void onEnable(){}protected void onDisable(){}
            public void update(float dt){if(++frames>60 && Float.isFinite(dt))samples.add(dt*1000);if(samples.size()==120){done.complete(List.copyOf(samples));app.getStateManager().detach(this);}}
        });return true;});
        var values=new ArrayList<>(done.get(40,TimeUnit.SECONDS));Collections.sort(values);
        return String.format(Locale.ROOT,"{\"quality\":\"%s\",\"frames\":120,\"median_ms\":%.3f,\"p95_ms\":%.3f}",low?"low":"beautiful",values.get(60),values.get(113));
    }
    public static void main(String[] args)throws Exception{
        var art=new GladeArt(new DesktopAssetManager(true));
        for(int variant=0;variant<8;variant++){
            var tree=new GladeDesign.Item(0,GladeDesign.Kind.TREE,new GladeDesign.Point(5,2),2,2,3,false,List.of(),0,null,new GladeDesign.Form(0,0,0,-1).finish(GladeDesign.Finish.DEFAULT.variant(variant)));
            var node=art.render(tree,new GladeDesign(List.of(tree),2));ok(geometries(node)<=4,"bounded foliage draw groups variant "+variant);
            node.updateGeometricState();ok(Math.abs(node.getWorldBound().getCenter().x-5)<.4f && Math.abs(node.getWorldBound().getCenter().z-2)<.4f,"batch keeps tree at saved position");
            int[] shaded={0};node.depthFirstTraversal(s->{if(s instanceof Geometry g && g.getMaterial().getParam("UseVertexColor")!=null){shaded[0]++;var colors=(FloatBuffer)g.getMesh().getBuffer(VertexBuffer.Type.Color).getData();float min=1,max=0;for(int i=0;i<colors.limit();i+=4){min=Math.min(min,colors.get(i));max=Math.max(max,colors.get(i));}ok(max-min>.12f && min>=.75f && max<=1,"canopy gradient retained through batching");}});ok(shaded[0]>0,"leaf gradient exists");
        }
        ok(art.material("#92ad69").getParam("UseVertexColor")==null,"leaf shading does not mutate shared materials");
        var flower=new GladeDesign.Item(0,GladeDesign.Kind.FLOWERS,new GladeDesign.Point(0,0),1,1,.5f,false,List.of());ok(geometries(art.render(flower,new GladeDesign(List.of(flower),2)))<=3,"flower petals and stems batch");
        var scene=CozyShowcase.create();var save=PlayerFiles.directory("checks").resolve("cozy-showcase.glade");scene.save(save);ok(GladeDesign.load(save).items().equals(scene.items()),"showcase round trip with player save format");
        var meadow=art.meadow(scene);ok(geometries(meadow)<40,"fixed meadow decor has bounded draw groups");
        app=new GladeApp();var settings=new AppSettings(true);settings.setResolution(1000,680);settings.setSamples(4);settings.setGammaCorrection(true);settings.setRenderer(AppSettings.LWJGL_OPENGL33);settings.setAudioRenderer(null);settings.setTitle("DreamHouse · Ánh sáng và cảnh mẫu");app.setSettings(settings);app.setShowSettings(false);app.setPauseOnLostFocus(false);app.start();
        try{
            engine(()->true);fixture(scene);
            var atmosphere=(CozyAtmosphere)field("atmosphere");var sun=(DirectionalLight)field("sun");
            engine(()->{var before=sun.getColor().clone();atmosphere.select(1);ok(sun.getColor().equals(before),"selection does not snap lighting");atmosphere.update(.01f,40);ok(Math.abs(sun.getColor().r-before.r)<.02f,"small first transition step");atmosphere.update(2,40);ok(Math.abs(sun.getColor().r-CozyAtmosphere.sunlight(1).r)<.001f,"sunset transition reaches target");atmosphere.select(2);atmosphere.update(.25f,40);before=sun.getColor().clone();atmosphere.select(0);ok(before.equals(sun.getColor()),"interrupting transition has no discontinuity");atmosphere.update(2,40);ok(Math.abs(sun.getColor().b-CozyAtmosphere.sunlight(0).b)<.001f,"daylight restored");atmosphere.update(.1f,10000);ok(atmosphere.haze.getFogDistance()==1600,"haze distance bounded");return true;});
            var beautiful=measure(false);var low=measure(true);set("qualityLow",false);call("applyQuality");
            String report="{\"scene\":\"CozyShowcase: houses, tower, windows, chimney, trees, flowers, source water and path\",\"objects\":13,\"resolution\":\"1000x680\",\"samples\":4,\"gamma\":true,\"warmup_frames_each\":60,\"measurements\":["+beautiful+","+low+"],\"limits\":\"Current machine and fixed camera only; no interactive stress, other hardware or visual approval measured\"}";
            Files.writeString(PlayerFiles.root().resolve("scene-frame-times.json"),report);System.out.println(report);
            System.out.println("ATMOSPHERE_STYLE_PASS: smooth/interrupted/reversible lighting, bounded haze, batched foliage gradients, flower batches, actual showcase save roundtrip, high/low GPU rendering and frame-time sample; no screenshots");
        }finally{app.stop();}
    }
}

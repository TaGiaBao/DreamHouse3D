package vn.dreamhouse.glade;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.InputManager;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import static vn.dreamhouse.glade.GladeDesign.*;

/** Repeatable frame-time sample using the same live tools as the game. */
class StrokePerformanceCheck extends ProceduralMechanicsCheck {
    public static void main(String[] args)throws Exception {
        var scene=switch(args[0]){
            case "--scene-s" -> sceneS();
            case "--scene-m" -> RiversideVillage.create();
            default -> GladeDesign.load(Path.of(args[0]));
        };
        boolean low=java.util.Arrays.asList(args).contains("--low");
        app=new GladeApp();var settings=new AppSettings(true);
        settings.setResolution(1100,720);settings.setSamples(2);settings.setGammaCorrection(true);
        settings.setVSync(false);settings.setFrameRate(120);settings.setRenderer(AppSettings.LWJGL_OPENGL33);
        settings.setAudioRenderer(null);app.setSettings(settings);app.setShowSettings(false);app.setPauseOnLostFocus(false);app.start();
        try {
            engine(()->true);fixture(scene);
            set("qualityLow",low);call("applyQuality");
            System.out.println("SCENE_STROKES objects="+scene.items().size()+" quality="+(low?"low":"beautiful"));
            set("distance",42f);set("yaw",0f);set("elevation",.88f);call("updateCamera");
            set("waterRadius",4f);set("pathWidth",3f);
            var times=new ArrayList<Float>();
            var dragTimes=new ArrayList<Float>();
            var drawing=GladeApp.class.getDeclaredField("drawing");drawing.setAccessible(true);
            engine(()->{
                app.getStateManager().attach(new BaseAppState(){
                    protected void initialize(Application a){}
                    protected void cleanup(Application a){}
                    protected void onEnable(){}
                    protected void onDisable(){}
                    public void update(float dt){
                        if(dt<=0 || !Float.isFinite(dt))return;
                        times.add(dt*1000);
                        try{if(drawing.getBoolean(app))dragTimes.add(dt*1000);}
                        catch(IllegalAccessException error){throw new IllegalStateException(error);}
                    }
                });
                return true;
            });
            Thread.sleep(1800);
            var kinds=List.of(Kind.PATH,Kind.WATER);
            for(var kind:kinds){
                clickRaw(kind==Kind.PATH?"landscape":"water");
                engine(()->{times.clear();dragTimes.clear();return true;});
                var commits=new ArrayList<Float>();
                var meshes=new ArrayList<Float>();
                for(int i=0;i<5;i++){
                    commits.add(draw(-3f+i*1.25f+(kind==Kind.WATER?.55f:0)));
                    meshes.add(((SceneRenderer)field("sceneRenderer")).lastNanos/1_000_000f);
                }
                var results=engine(()->List.copyOf(times));
                print(kind+" frames",results);
                print(kind+" drag",engine(()->List.copyOf(dragTimes)));
                print(kind+" release",commits);
                print(kind+" scene rebuild",meshes);
                ok(design().items().stream().filter(it->it.kind()==kind).count()==scene.items().stream().filter(it->it.kind()==kind).count()+5,"all five strokes were committed");
            }
            int waterVertices=engine(()->{
                var content=(Node)read("content");int[] total={0};
                content.depthFirstTraversal(part->{if(Boolean.TRUE.equals(part.getUserData("softSurface")) && part instanceof com.jme3.scene.Geometry g)total[0]+=g.getMesh().getVertexCount();});
                return total[0];
            });
            var snapshot=design();var file=PlayerFiles.directory("checks").resolve("scene-edited.glade");
            snapshot.save(file);ok(GladeDesign.load(file).items().equals(snapshot.items()),"edited scene reloads");
            System.out.println("STROKE_PERFORMANCE_PASS objects="+scene.items().size()+"->"+snapshot.items().size()+" waterVertices="+waterVertices+" resolution=1100x720");
        }finally{app.stop();}
    }

    private static Object read(String name)throws Exception {
        var member=GladeApp.class.getDeclaredField(name);member.setAccessible(true);return member.get(app);
    }

    private static GladeDesign sceneS(){
        var items=new ArrayList<Item>();
        for(int i=0;i<10;i++)
            items.add(block(items.size(),-12+(i%5)*6,i<5?-7:7,3.3f,2.8f,2.4f,1.1f));
        for(int i=0;i<60;i++){
            float x=-15+(i%15)*2.14f,z=(i/15<2?-1:1)*(i%2==0?10.7f:8.9f);
            var kind=i%5==0?Kind.TREE:i%10==2?Kind.BENCH:Kind.FLOWERS;
            float height=kind==Kind.TREE?2.6f:kind==Kind.BENCH?.85f:.38f;
            items.add(new Item(items.size(),kind,new Point(x,z),1.1f,.8f,height,false,List.of()));
        }
        items.add(source(items.size(),13,1));
        return new GladeDesign(items,new LandBounds(-17,17,-13,13),new TerrainField());
    }

    private static float draw(float z)throws Exception {
        var points=engine(()->{
            var result=new ArrayList<Vector3f>();
            for(int step=0;step<=34;step++){
                float t=step/34f;
                result.add(app.getCamera().getScreenCoordinates(new Vector3f(-9+t*18,0,z+(float)Math.sin(t*6.28)*1.35f)));
            }
            return result;
        });
        for(int i=0;i<points.size();i++){
            inject(points.get(i),i==0?Boolean.TRUE:null);
            Thread.sleep(16);
        }
        long release=System.nanoTime();
        inject(points.getLast(),false);
        while((Boolean)field("drawing") || preparingWater()){
            if(System.nanoTime()-release>5_000_000_000L)throw new AssertionError("Stroke release did not complete");
        }
        return (System.nanoTime()-release)/1_000_000f;
    }

    private static boolean preparingWater()throws Exception{
        try{return field("pendingWater")!=null;}
        catch(NoSuchFieldException olderBuild){return false;}
    }

    private static void inject(Vector3f point,Boolean pressed)throws Exception {
        engine(()->{
            int x=Math.round(point.x),y=Math.round(point.y);
            var input=app.getInputManager();var gate=InputManager.class.getDeclaredField("eventsPermitted");
            gate.setAccessible(true);gate.setBoolean(input,true);
            try{
                var motion=new MouseMotionEvent(x,y,5,5,0,0);motion.setTime(System.nanoTime());input.onMouseMotionEvent(motion);
                if(pressed!=null){var click=new MouseButtonEvent(0,pressed,x,y);click.setTime(System.nanoTime());input.onMouseButtonEvent(click);}
            }finally{gate.setBoolean(input,false);}
            return true;
        });
    }

    private static void print(String label,List<Float> values) {
        var sorted=values.stream().sorted().toList();int n=sorted.size();
        System.out.printf(Locale.ROOT,"%s n=%d median_ms=%.2f p95_ms=%.2f max_ms=%.2f%n",label,n,sorted.get(n/2),sorted.get((int)Math.ceil(n*.95)-1),sorted.getLast());
    }
}

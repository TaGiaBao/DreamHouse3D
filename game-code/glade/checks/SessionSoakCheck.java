package vn.dreamhouse.glade;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.system.AppSettings;
import java.lang.management.*;
import java.nio.FloatBuffer;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static vn.dreamhouse.glade.GladeDesign.*;

/** Real wall-clock render/edit soak in an isolated player directory; intentionally separate from quick regression. */
class SessionSoakCheck extends ProceduralMechanicsCheck {
    static final List<Float> frameTimes=new ArrayList<>();
    static final AtomicBoolean diagnostic=new AtomicBoolean();
    static long frames,excluded;
    static long placedRoot=-1;
    static final List<String> windows=new ArrayList<>();
    static Object invoke(String name,Class<?>[] types,Object...args)throws Exception{return engine(()->{var method=GladeApp.class.getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(app,args);});}
    static void commit(GladeDesign next)throws Exception{invoke("commit",new Class<?>[]{GladeDesign.class,String.class},next,"Kiểm thử phiên chơi: xây, sửa và lưu trong dữ liệu thử riêng.");}
    static Item changed(Item old,float width,float height,float rotation,Form form){return new Item(old.id(),old.kind(),old.center(),width,old.depth(),height,old.pitched(),old.points(),rotation,old.mount(),form);}
    static void edit(int step)throws Exception{
        var scene=design();var root=scene.items().stream().filter(i->i.id()==0).findFirst().orElseThrow();var items=scene.items();GladeDesign next=scene;
        switch(step%12){
            case 0 -> next=scene.replace(changed(root,root.width()>4.6f?4.5f:4.8f,root.height(),root.rotation(),root.form()));
            case 1 -> {var f=root.form().finish();String color=new String[]{"#eadbb4","#d7b595","#b5c3a0","#a7c4bc","#dcc3a2","#c7b5c9","#e1cfaa","#d4a788"}[(step/12)%8];next=scene.replace(changed(root,root.width(),root.height(),root.rotation(),root.form().finish(new Finish(f.material(),color,f.roofMaterial(),f.roofColor(),f.variant()))));}
            case 2 -> next=scene.replace(changed(root,root.width(),root.height(),root.rotation(),root.form().roof(root.form().roof()>.1f?0:1.5f)));
            case 3 -> next=items.stream().anyMatch(i->i.id()==100)?scene.remove(100):scene.add(new Item(100,Kind.TOWER,root.center(),1.8f,1.8f,1.8f,true,List.of(),0,null,new Form(0,1,0,0).finish(GladeCatalog.plain())));
            case 4 -> next=items.stream().anyMatch(i->i.id()==101)?scene.remove(101):scene.add(new Item(101,Kind.FLOWERS,new Point(-6,2),1.2f,1,.5f,false,List.of()));
            case 5 -> {invoke("action",new Class<?>[]{String.class},"undo");return;}
            case 6 -> {invoke("action",new Class<?>[]{String.class},"redo");return;}
            case 7 -> next=scene.replace(changed(root,root.width(),root.height(),(root.rotation()+15)%45,root.form()));
            case 8 -> {if(items.stream().anyMatch(i->i.id()==101))next=scene.remove(101);else if(items.stream().noneMatch(i->i.id()==102))next=scene.add(new Item(102,Kind.TREE,new Point(-7,4),1.8f,1.8f,2.6f,false,List.of()));}
            case 9 -> {var points=List.of(new Point(-7,1),new Point(-6,2.5f),new Point(-5,4),new Point(-2,5));next=items.stream().anyMatch(i->i.id()==103)?scene.remove(103):scene.add(new Item(103,Kind.PATH,points.getFirst(),1,1,1,false,points));}
            case 10 -> next=scene.withTerrain(scene.terrain().brush(6,-5,1.8f,step%24<12?.12f:-.12f,null));
            case 11 -> {if(placedRoot>=0 && items.stream().anyMatch(i->i.id()==placedRoot)){next=scene.remove(placedRoot);placedRoot=-1;}else{var placed=BlueprintModule.capture(scene,0).place(scene,new Point(6,-4));placedRoot=placed.rootId();next=placed.scene();}}
        }
        if(next!=scene)commit(next);
    }
    static void finiteScene()throws Exception{
        var content=(Node)field("content");engine(()->{content.depthFirstTraversal(s->{if(s instanceof Geometry g){var buffer=g.getMesh().getBuffer(VertexBuffer.Type.Position);if(buffer!=null){var data=(FloatBuffer)buffer.getData();for(int i=0;i<data.limit();i++)ok(Float.isFinite(data.get(i)),"all rendered mesh coordinates finite");}}});return true;});
    }
    static String sample(int seconds,int operations,SessionStore store,int manualSaves)throws Exception{
        var values=engine(()->{var copy=new ArrayList<>(frameTimes);frameTimes.clear();return copy;});Collections.sort(values);long heap=ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        long direct=ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class).stream().filter(b->b.getName().equals("direct")).mapToLong(BufferPoolMXBean::getMemoryUsed).sum();
        var art=field("art");var map=GladeArt.class.getDeclaredField("materials");map.setAccessible(true);int materialCount=((Map<?,?>)map.get(art)).size();
        var stats=engine(()->{var renderer=app.getRenderer().getStatistics();var labels=renderer.getLabels();var data=new int[labels.length];renderer.getData(data);var parts=new ArrayList<String>();for(int i=0;i<labels.length;i++)parts.add("\""+labels[i].replace("\"","")+"\":"+data[i]);return "{"+String.join(",",parts)+"}";});
        float median=values.isEmpty()?0:values.get(values.size()/2),p95=values.isEmpty()?0:values.get(Math.min(values.size()-1,(int)(values.size()*.95)));
        var state=store.stats();ok(state.active()<=1 && state.pending()<=1 && materialCount<=512,"bounded writer and material lookup cache");
        return String.format(Locale.ROOT,"{\"seconds\":%d,\"operations\":%d,\"objects\":%d,\"frame_samples\":%d,\"median_ms\":%.3f,\"p95_ms\":%.3f,\"heap_used_bytes\":%d,\"direct_buffer_bytes\":%d,\"material_cache_entries\":%d,\"autosave_active\":%d,\"autosave_pending\":%d,\"autosave_written\":%d,\"manual_roundtrips\":%d,\"renderer_statistics\":%s}",seconds,operations,design().items().size(),values.size(),median,p95,heap,direct,materialCount,state.active(),state.pending(),state.written(),manualSaves,stats);
    }
    static void report(Path file,int requested,int elapsed,int operations,int manual,long pid,boolean complete)throws Exception{
        String json="{\"requested_seconds\":"+requested+",\"elapsed_seconds\":"+elapsed+",\"operations\":"+operations+",\"manual_roundtrips\":"+manual+",\"pid\":"+pid+",\"complete\":"+complete+",\"screenshots\":false,\"resolution\":\"1000x680\",\"quality\":\"beautiful\",\"frame_count\":"+frames+",\"excluded_diagnostic_frames\":"+excluded+",\"samples\":["+String.join(",",windows)+"],\"limits\":\"Scripted edits on current machine; first 15 seconds and explicit diagnostic windows excluded from frame samples; heap/direct memory are not total GPU or process memory; no claim of two-hour/human acceptance\"}";
        var temporary=file.resolveSibling(file.getFileName()+".tmp");Files.writeString(temporary,json);PlayerFiles.move(temporary,file);
    }
    public static void main(String[]args)throws Exception{
        int duration=Integer.getInteger("dreamhouse.soakSeconds",720);if(duration<30 || duration>7200)throw new IllegalArgumentException("soak duration out of range");long pid=ProcessHandle.current().pid();
        app=new GladeApp();var settings=new AppSettings(true);settings.setResolution(1000,680);settings.setSamples(4);settings.setGammaCorrection(true);settings.setVSync(true);settings.setRenderer(AppSettings.LWJGL_OPENGL33);settings.setAudioRenderer(null);settings.setTitle("DreamHouse · M0-02 · Kiểm thử phiên chơi");app.setSettings(settings);app.setShowSettings(false);app.setPauseOnLostFocus(false);app.start();
        var store=new SessionStore(PlayerFiles.directory("soak-recovery"));var manual=Executors.newSingleThreadExecutor();var manualCount=new AtomicInteger();Future<?> pending=null;
        Path progress=PlayerFiles.root().resolve("session-soak.json");int operations=0;
        try{
            engine(()->true);fixture(CozyShowcase.create());set("sessionStore",store);set("managedSession",true);set("mainMenuOpen",false);set("savedDesign",design());
            long start=System.nanoTime();engine(()->{app.getRenderer().getStatistics().setEnabled(true);app.getStateManager().attach(new BaseAppState(){protected void initialize(Application a){}protected void cleanup(Application a){}protected void onEnable(){}protected void onDisable(){}public void update(float dt){frames++;if(diagnostic.get() || System.nanoTime()-start<15_000_000_000L){excluded++;return;}if(Float.isFinite(dt) && dt>0)frameTimes.add(dt*1000);}});return true;});
            int nextEdit=0,nextSample=60,nextManual=20;report(progress,duration,0,0,0,pid,false);
            while(true){
                int seconds=(int)((System.nanoTime()-start)/1_000_000_000L);if(seconds>=duration)break;
                if(seconds>=nextEdit){edit(operations++);final int step=operations;engine(()->{var field=GladeApp.class.getDeclaredField("yaw");field.setAccessible(true);field.setFloat(app,FastMath.normalize(-.65f+step*.02f,-FastMath.PI,FastMath.PI));var method=GladeApp.class.getDeclaredMethod("updateCamera");method.setAccessible(true);method.invoke(app);return true;});nextEdit=seconds+1;}
                if(seconds>=nextManual && (pending==null || pending.isDone())){if(pending!=null)pending.get();var snapshot=design();pending=manual.submit(()->{try{var file=PlayerFiles.root().resolve("soak-manual.glade");snapshot.save(file);if(!GladeDesign.load(file).items().equals(snapshot.items()))throw new AssertionError("manual roundtrip mismatch");manualCount.incrementAndGet();}catch(Exception error){throw new CompletionException(error);}});nextManual=seconds+20;}
                if(seconds>=nextSample){diagnostic.set(true);finiteScene();if((seconds/60)%2==0){System.gc();Thread.sleep(150);}String window=sample(seconds,operations,store,manualCount.get());windows.add(window);report(progress,duration,seconds,operations,manualCount.get(),pid,false);System.out.println("SOAK_PROGRESS "+window);diagnostic.set(false);nextSample=seconds+60;}
                Thread.sleep(25);
            }
            if(pending!=null)pending.get(15,TimeUnit.SECONDS);var finalDesign=design();store.save(finalDesign).get(15,TimeUnit.SECONDS);ok(store.recover().orElseThrow().design().items().equals(finalDesign.items()),"final recovery matches edited scene");
            diagnostic.set(true);finiteScene();System.gc();Thread.sleep(150);int elapsed=(int)((System.nanoTime()-start)/1_000_000_000L);windows.add(sample(elapsed,operations,store,manualCount.get()));report(progress,duration,elapsed,operations,manualCount.get(),pid,true);
            ok(frames>duration*5L && operations>=duration/2 && manualCount.get()>=duration/30,"session actually rendered, edited and saved throughout wall-clock duration");
            System.out.println("SESSION_SOAK_PASS seconds="+elapsed+" operations="+operations+" frames="+frames+" manual_roundtrips="+manualCount.get()+" no screenshots");
        }finally{manual.shutdown();manual.awaitTermination(15,TimeUnit.SECONDS);app.stop();}
    }
}

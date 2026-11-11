package vn.dreamhouse.glade;
import com.jme3.input.event.*;
import com.jme3.input.KeyInput;
import com.jme3.math.*;
import com.jme3.system.AppSettings;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.scene.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

class DirectEditCheck {
    static GladeApp app;
    static <T> T engine(Callable<T> task) throws Exception {
        Object[] result=app.enqueue(()->{try{return new Object[]{task.call(),null};}catch(Throwable failure){return new Object[]{null,failure};}}).get(25,TimeUnit.SECONDS);
        if(result[1] instanceof Exception failure)throw failure;
        if(result[1] instanceof Error failure)throw failure;
        @SuppressWarnings("unchecked") T value=(T)result[0];
        return value;
    }
    static Object field(String name) throws Exception {
        return engine(()->{
            var member=GladeApp.class.getDeclaredField(name);
            member.setAccessible(true);
            return member.get(app);
        });
    }
    static GladeDesign design() throws Exception { return (GladeDesign)field("design"); }
    static void fixture(GladeDesign design) throws Exception {
        engine(()->{
            var member=GladeApp.class.getDeclaredField("design");member.setAccessible(true);member.set(app,design);
            var selected=GladeApp.class.getDeclaredField("selected");selected.setAccessible(true);selected.set(app,null);
            for(String name:List.of("rebuild","rebuildHud")) {var method=GladeApp.class.getDeclaredMethod(name);method.setAccessible(true);method.invoke(app);}
            return true;
        });
    }
    static void mouse(int x,int y,Boolean down) throws Exception {
        engine(()->{
            var input=app.getInputManager();
            var gate=com.jme3.input.InputManager.class.getDeclaredField("eventsPermitted");
            gate.setAccessible(true);
            gate.setBoolean(input,true);
            try {
                var event=new MouseMotionEvent(x,y,5,5,0,0);
                event.setTime(System.nanoTime());
                input.onMouseMotionEvent(event);
                if(down!=null) {
                    var click=new MouseButtonEvent(0,down,x,y);
                    click.setTime(System.nanoTime());
                    input.onMouseButtonEvent(click);
                }
            } finally { gate.setBoolean(input,false); }
            return true;
        });
        Thread.sleep(180);
    }
    static void key(int code,boolean down) throws Exception {
        engine(()->{
            var input=app.getInputManager();
            var gate=com.jme3.input.InputManager.class.getDeclaredField("eventsPermitted");
            gate.setAccessible(true);
            gate.setBoolean(input,true);
            try {
                var event=new KeyInputEvent(code,'\0',down,false);
                event.setTime(System.nanoTime());
                input.onKeyEvent(event);
            } finally { gate.setBoolean(input,false); }
            return true;
        });
        Thread.sleep(100);
    }
    static void clickId(String id) throws Exception {
        if(!ArchitectureCheck.buttons().containsKey(id)) {
            if(Set.of("sample","clear","expand","collection","viewAll","sceneLight","focus").contains(id)) {
                clickId("sceneMenu");
                clickRaw(id.equals("focus")?"viewAll":id);return;
            }
            if(Set.of("save","load").contains(id)) {clickId("blueprints");clickRaw(id);return;}
        }
        clickRaw(id);
    }
    static void clickRaw(String id) throws Exception {
        var rectangle=engine(()->{
            var member=GladeApp.class.getDeclaredField("hud");
            member.setAccessible(true);
            var hud=member.get(app);
            var buttons=GladeHud.class.getDeclaredField("buttons");
            buttons.setAccessible(true);
            @SuppressWarnings("unchecked")
            var map=(Map<String,java.awt.Rectangle>)buttons.get(hud);
            if(!map.containsKey(id)) throw new AssertionError("Missing "+id);
            return map.get(id);
        });
        engine(()->{
            var member=GladeApp.class.getDeclaredField("hud");member.setAccessible(true);
            var hud=(GladeHud)member.get(app);
            if(!id.equals(hud.hit((float)rectangle.getCenterX(),(float)rectangle.getCenterY())))
                throw new AssertionError("HUD hit "+id);
            var action=GladeApp.class.getDeclaredMethod("action",String.class);action.setAccessible(true);
            action.invoke(app,id);
            return true;
        });
    }
    static Vector3f screen(Vector3f world) throws Exception { return engine(()->app.getCamera().getScreenCoordinates(world)); }
    static Vector3f handle(String groupName,String tag,String value) throws Exception {
        return engine(()->{
            var member=GladeApp.class.getDeclaredField(groupName);
            member.setAccessible(true);
            var group=(Node)member.get(app);
            var found=new java.util.concurrent.atomic.AtomicReference<Spatial>();
            group.depthFirstTraversal(node->{
                if(value.equals(node.getUserData(tag))) found.set(node);
            });
            if(found.get()==null) throw new AssertionError("Missing handle "+value);
            return found.get().getWorldTranslation().clone();
        });
    }
    static void drag(Vector3f from,Vector3f to) throws Exception {
        mouse((int)from.x,(int)from.y,true);
        mouse((int)to.x,(int)to.y,null);
        mouse((int)to.x,(int)to.y,false);
    }
    static void dataChecks() throws Exception {
        var base=GladeDesign.sample();
        var stretched=base.resizeLand(LandBounds.Edge.LEFT,-20);
        if(stretched.land().maxX()!=base.land().maxX() || stretched.land().minX()!=-20
                || !stretched.items().equals(base.items())) throw new AssertionError("One-sided resize");
        var shrunk=stretched.resizeLand(LandBounds.Edge.RIGHT,-5);
        if(!shrunk.items().equals(base.items())) throw new AssertionError("Shrink lost buildings");
        var file=Path.of("glade/classes/direct-edit-test.glade");
        var original=base.items().getFirst();
        var rotated=new GladeDesign.Item(original.id(),original.kind(),original.center(),original.width(),
            original.depth(),original.height(),true,original.points(),37);
        stretched=stretched.replace(rotated);
        stretched.save(file);
        var restored=GladeDesign.load(file);
        if(!restored.land().equals(stretched.land()) || !restored.items().equals(stretched.items()))
            throw new AssertionError("Rotation or land save");
        Files.writeString(file,"version=glade-1\ncount=1\nitem.0.id=0\nitem.0.kind=HOUSE\nitem.0.shape=0,0,4,3,2.3,true\nitem.0.points=\n");
        if(GladeDesign.load(file).land().width()!=18) throw new AssertionError("Old save");
        Files.writeString(file,"version=glade-2\nlandLevel=2\ncount=1\nitem.0.id=0\nitem.0.kind=HOUSE\nitem.0.shape=12,0,1,1,2.3,true\nitem.0.points=\n");
        if(GladeDesign.load(file).land().width()!=26) throw new AssertionError("Version 2 save");
        Files.delete(file);
    }
    public static void main(String[] args) throws Exception {
        dataChecks();
        app=new GladeApp();
        var settings=new AppSettings(true);
        settings.setResolution(1360,860);
        settings.setRenderer(AppSettings.LWJGL_OPENGL33);
        settings.setAudioRenderer(null);
        settings.setSamples(4);
        settings.setGammaCorrection(true);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
        try {
            engine(()->true);fixture(GladeDesign.village());Thread.sleep(400);
            var before=design();
            clickId("expand");
            var knob=screen(handle("landHandles","landEdge","LEFT"));
            var destination=screen(new Vector3f(-17,0,0));
            drag(knob,destination);
            var stretched=design();
            if(stretched.land().minX()>-16.7 || stretched.land().maxX()!=before.land().maxX()
                    || !stretched.items().equals(before.items())) throw new AssertionError("Land drag");
            clickId("undo");
            if(!design().land().equals(before.land())) throw new AssertionError("Land undo");
            clickId("redo");
            if(!design().land().equals(stretched.land())) throw new AssertionError("Land redo");
            clickId("architecture");clickId("tool:SELECT");
            var house=screen(new Vector3f(0,0,0));
            mouse((int)house.x,(int)house.y,true);mouse((int)house.x,(int)house.y,false);
            if(!Objects.equals(field("selected"),0L)) throw new AssertionError("Selection");
            var widthKnob=screen(handle("objectHandles","resizeAxis","WIDTH"));
            drag(widthKnob,screen(new Vector3f(3.3f,0,0)));
            var widened=design().items().getFirst();
            if(widened.width()<5) throw new AssertionError("Width handle");
            var heightKnob=screen(handle("objectHandles","resizeAxis","HEIGHT"));
            drag(heightKnob,new Vector3f(heightKnob.x,heightKnob.y+35,0));
            if(design().items().getFirst().height()<=widened.height()+0.25f) throw new AssertionError("Height handle");
            clickId("rotate+");
            if(design().items().getFirst().rotation()!=15) throw new AssertionError("Rotation");
            clickId("copy");
            if(design().items().getLast().rotation()!=15) throw new AssertionError("Copy orientation");
            clickId("structures");clickId("tool:WALL");
            var a=screen(new Vector3f(-3,0,-4.8f));
            var b=screen(new Vector3f(-1,0,-7));
            var c=screen(new Vector3f(1,0,-4.8f));
            mouse((int)a.x,(int)a.y,true);
            mouse((int)b.x,(int)b.y,null);
            mouse((int)c.x,(int)c.y,null);
            mouse((int)c.x,(int)c.y,false);
            var wall=design().items().getLast();
            if(wall.kind()!=GladeDesign.Kind.WALL || wall.points().size()<10
                    || wall.points().stream().mapToDouble(GladeDesign.Point::z).min().orElse(0)>-6.5)
                throw new AssertionError("Curved wall stroke");
            clickId("landscape");clickId("tool:FENCE");
            a=screen(new Vector3f(-7,0,-5));b=screen(new Vector3f(-9,0,-6));c=screen(new Vector3f(-11,0,-5));
            mouse((int)a.x,(int)a.y,true);mouse((int)b.x,(int)b.y,null);
            mouse((int)c.x,(int)c.y,null);mouse((int)c.x,(int)c.y,false);
            var fence=design().items().getLast();
            if(fence.kind()!=GladeDesign.Kind.FENCE || fence.points().size()<10)
                throw new AssertionError("Curved fence stroke: "+fence.kind()+" points="+fence.points().size()+" tool="+field("tool"));
            var save=Path.of("glade/classes/curves-test.glade");
            design().save(save);
            if(!GladeDesign.load(save).items().equals(design().items())) throw new AssertionError("Curve save");
            Files.delete(save);
            clickId("expand");clickId("focus");
            engine(()->{app.getStateManager().getState(ScreenshotAppState.class).takeScreenshot();return true;});
            Thread.sleep(600);
            System.out.println("DIRECT_EDIT_PASS: one-edge land drag, undo/redo, width/height handles, rotate, clone, curved wall/fence, save compatibility");
        } finally { app.stop(); }
    }
}

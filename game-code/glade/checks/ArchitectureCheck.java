package vn.dreamhouse.glade;

import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;
import com.jme3.app.state.ScreenshotAppState;
import java.nio.file.*;
import java.util.*;

class ArchitectureCheck extends DirectEditCheck {
    @SuppressWarnings("unchecked")
    static Map<String,java.awt.Rectangle> buttons() throws Exception {
        return engine(()->{
            var member=GladeHud.class.getDeclaredField("buttons");member.setAccessible(true);
            var h=GladeApp.class.getDeclaredField("hud");h.setAccessible(true);
            return new LinkedHashMap<>((Map<String,java.awt.Rectangle>)member.get(h.get(app)));
        });
    }
    static void layout() throws Exception {
        var entries=new ArrayList<>(buttons().entrySet());
        for(int i=0;i<entries.size();i++) for(int j=i+1;j<entries.size();j++)
            if(entries.get(i).getValue().intersects(entries.get(j).getValue()))
                throw new AssertionError("Overlapping controls: "+entries.get(i).getKey()+" / "+entries.get(j).getKey());
    }
    public static void main(String[] args) throws Exception {
        dataChecks();app=new GladeApp();
        var settings=new AppSettings(true);settings.setResolution(1360,860);
        settings.setRenderer(AppSettings.LWJGL_OPENGL33);settings.setAudioRenderer(null);
        settings.setSamples(4);settings.setGammaCorrection(true);settings.setVSync(true);
        app.setSettings(settings);app.setShowSettings(false);app.start();
        try {
            engine(()->true);fixture(GladeDesign.village());Thread.sleep(400);layout();
            clickId("structures");layout();
            if(!buttons().containsKey("tool:BRIDGE") || !buttons().containsKey("tool:WALL")) throw new AssertionError("Structures category");
            clickId("landscape");layout();
            if(!buttons().containsKey("tool:LAND")) throw new AssertionError("Landscape category");
            clickId("tools");
            if(buttons().containsKey("tool:LAND")) throw new AssertionError("Toolbox closed");
            clickId("tools");clickId("clear");clickId("focus");clickId("architecture");
            clickId("tool:CANAL_HOUSE");
            drag(screen(new Vector3f(-6,0,-2)),screen(new Vector3f(-3,0,1)));
            if(design().items().size()!=1 || design().items().getFirst().kind()!=GladeDesign.Kind.CANAL_HOUSE)
                throw new AssertionError("Canal house placement");
            clickId("tool:BRICK_HOUSE");
            drag(screen(new Vector3f(1,0,-2)),screen(new Vector3f(4,0,1)));
            if(design().items().size()!=2 || design().items().getLast().kind()!=GladeDesign.Kind.BRICK_HOUSE)
                throw new AssertionError("Brick house placement");
            clickId("undo");if(design().items().size()!=1) throw new AssertionError("Architecture undo");
            clickId("redo");if(design().items().size()!=2) throw new AssertionError("Architecture redo");
            clickId("tool:SELECT");
            var point=screen(new Vector3f(2.5f,0,-0.5f));
            mouse((int)point.x,(int)point.y,true);mouse((int)point.x,(int)point.y,false);
            layout();clickId("rotate+");clickId("copy");
            if(design().items().getLast().kind()!=GladeDesign.Kind.BRICK_HOUSE || design().items().getLast().rotation()!=15)
                throw new AssertionError("Architecture rotate/copy");
            var save=Path.of("glade/classes/new-architecture-test.glade");design().save(save);
            if(!GladeDesign.load(save).items().equals(design().items())) throw new AssertionError("Architecture round trip");
            Files.delete(save);clickId("undo");clickId("architecture");
            engine(()->{
                var d=GladeApp.class.getDeclaredField("distance");d.setAccessible(true);d.setFloat(app,25);
                var f=GladeApp.class.getDeclaredField("focus");f.setAccessible(true);((Vector3f)f.get(app)).set(-0.5f,1.0f,0);
                var update=GladeApp.class.getDeclaredMethod("updateCamera");update.setAccessible(true);update.invoke(app);
                return true;
            });
            engine(()->{app.getStateManager().getState(ScreenshotAppState.class).takeScreenshot();return true;});
            Thread.sleep(600);
            System.out.println("ARCHITECTURE_PASS: category hit areas, toolbox open/close, canal and brick placement, undo/redo, rotation/copy, save compatibility");
        } finally {app.stop();}
    }
}

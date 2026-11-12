package vn.dreamhouse.glade;
import com.jme3.system.AppSettings;
import com.jme3.math.*;
import java.util.*;
import java.util.concurrent.*;

class ReleaseUiCheck extends ProceduralMechanicsCheck {
    static Object invoke(String name)throws Exception{return engine(()->{var m=GladeApp.class.getDeclaredMethod(name);m.setAccessible(true);return m.invoke(app);});}
    public static void main(String[] args)throws Exception {
        app=new GladeApp();var settings=new AppSettings(true);settings.setResolution(960,640);settings.setRenderer(AppSettings.LWJGL_OPENGL33);settings.setAudioRenderer(null);settings.setTitle("DreamHouse · Kiểm tra phiên chơi");app.setSettings(settings);app.setShowSettings(false);app.setPauseOnLostFocus(false);app.start();
        try {engine(()->true);Thread.sleep(250);fixture(new GladeDesign(List.of(block(0,0,0,4,3,2.6f,1.4f)),2));
            clickRaw("mainMenu");ok((Boolean)field("mainMenuOpen"),"main menu opens");
            ok(ArchitectureCheck.buttons().keySet().containsAll(Set.of("menu:continue","menu:new","menu:load","menu:save","menu:settings","menu:help","menu:exit")),"main menu actions available");
            for(var r:ArchitectureCheck.buttons().entrySet())if(r.getKey().startsWith("menu:"))ok(r.getValue().y>=0 && r.getValue().getMaxY()<=640,"menu fits 960×640 window");
            clickRaw("menu:settings");clickRaw("settings:quality");ok((Boolean)field("qualityLow"),"low quality available");clickRaw("settings:quality");ok(!(Boolean)field("qualityLow"),"quality restored");
            float before=(Float)field("cameraSensitivity");clickRaw("settings:sensitivity");ok((Float)field("cameraSensitivity")!=before,"camera sensitivity adjustable");clickRaw("settings:volume");ok(PlayerFiles.settings().containsKey("volume"),"settings persist");clickRaw("menu:back");ok(!(Boolean)field("mainMenuOpen") && !(Boolean)field("settingsOpen"),"back resumes scene");
            ok(engine(()->{app.getCamera().resize(680,400,true);var redraw=GladeApp.class.getDeclaredMethod("rebuildHud");redraw.setAccessible(true);redraw.invoke(app);var width=GladeApp.class.getDeclaredField("knownWidth");width.setAccessible(true);var hud=GladeApp.class.getDeclaredField("hud");hud.setAccessible(true);return width.getInt(app)==680 && ((GladeHud)hud.get(app)).bounds("architecture")==null;}),"small viewport produces fresh fallback instead of stale HUD");engine(()->{app.getCamera().resize(960,640,true);return true;});call("rebuildHud");ok(ArchitectureCheck.buttons().containsKey("architecture"),"HUD restored after resize");
            clickRaw("sceneMenu");ok(ArchitectureCheck.buttons().containsKey("village"),"editable village is discoverable");clickRaw("sceneMenu");
            clickRaw("terrain");clickRaw("photo");
            ok((Boolean)field("photoMode") && ArchitectureCheck.buttons().containsKey("photoTake") && !ArchitectureCheck.buttons().containsKey("architecture"),"photo preview replaces build controls");
            var photoScene=design();float yaw=(Float)field("yaw");
            right(550,390,true);mouse(610,415,null);right(610,415,false);
            ok(design()==photoScene && Math.abs((Float)field("yaw")-yaw)>.01f,"right drag frames photo without editing terrain");
            esc();ok(!(Boolean)field("photoMode") && ArchitectureCheck.buttons().containsKey("terrain"),"Esc restores build controls");
            set("managedSession",true);
            set("savedDesign",design());
            clickRaw("mainMenu");
            clickRaw("menu:new");
            ok(design().items().isEmpty() && !(Boolean)invoke("unsaved"),
                    "untouched new world does not immediately count as unsaved");
            clickRaw("mainMenu");
            clickRaw("menu:help");
            clickRaw("helpStart");
            ok(((BuildGuide)field("buildGuide")).active && design().items().isEmpty(),
                    "menu help starts guide on the new world at 960×640");
            clickRaw("guideHide");
            fixture(new GladeDesign(List.of(block(0,0,0,4,3,2.6f,1.4f)),2));
            var store=new SessionStore(PlayerFiles.directory("ui-recovery"));set("sessionStore",store);set("managedSession",true);set("savedDesign",design());ok(!(Boolean)invoke("unsaved"),"loaded/saved scene starts clean");
            var original=design();engine(()->{var m=GladeApp.class.getDeclaredMethod("commit",GladeDesign.class,String.class);m.setAccessible(true);m.invoke(app,original.add(block(1,7,0,2,2,2,1)),"test");return true;});ok((Boolean)invoke("unsaved"),"edit marks unsaved");
            call("queueAutosave");for(int i=0;i<30 && (Boolean)field("autosavePending");i++)Thread.sleep(100);ok(!(Boolean)field("autosavePending") && store.recover().orElseThrow().design().items().size()==2,"background autosave writes complete snapshot");
            var beforeBad=design();try{GladeDesign.load(PlayerFiles.root().resolve("missing.glade"));throw new AssertionError("missing accepted");}catch(java.io.IOException expected){}ok(design()==beforeBad,"failed input retains active scene");
            set("managedSession",false);System.out.println("RELEASE_UI_PASS: menu, settings persistence, quality toggles, small viewport fallback, dirty state, asynchronous recovery snapshot, failed load preserves scene; no screenshots");
        }finally {app.stop();}
    }
}

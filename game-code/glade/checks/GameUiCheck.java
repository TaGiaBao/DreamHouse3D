package vn.dreamhouse.glade;

import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.system.AppSettings;
import java.nio.file.*;
import java.util.*;

class GameUiCheck extends DirectEditCheck {
    static void require(boolean ok,String label) {if(!ok) throw new AssertionError(label);}
    static void clickWorld(Vector3f world) throws Exception {
        var point=screen(world);mouse((int)point.x,(int)point.y,true);mouse((int)point.x,(int)point.y,false);
    }
    static void selectedOnly(GladeDesign before,long id) throws Exception {
        for(var item:before.items()) if(item.id()!=id)
            require(design().items().stream().filter(other->other.id()==item.id()).findFirst().orElseThrow().equals(item),"Unselected block changed "+item.id());
    }
    public static void main(String[] args) throws Exception {
        PartsCheck.domain();app=new GladeApp();var settings=new AppSettings(true);
        settings.setResolution(1360,860);settings.setRenderer(AppSettings.LWJGL_OPENGL33);settings.setAudioRenderer(null);
        settings.setSamples(4);settings.setGammaCorrection(true);settings.setVSync(true);
        app.setSettings(settings);app.setShowSettings(false);app.setPauseOnLostFocus(false);app.start();
        try {
            engine(()->true);fixture(GladeDesign.village());Thread.sleep(400);ArchitectureCheck.layout();
            require(!ArchitectureCheck.buttons().containsKey("sample"),"Menu starts collapsed");
            clickId("sceneMenu");ArchitectureCheck.layout();
            for(String id:List.of("sample","clear","expand","viewAll","sceneLight","collection"))
                require(ArchitectureCheck.buttons().containsKey(id),"Menu item missing "+id);
            clickId("sceneLight");require(!(Boolean)field("sceneMenuOpen"),"Menu dismisses after selection");
            clickId("blueprints");ArchitectureCheck.layout();
            require(ArchitectureCheck.buttons().containsKey("save") && ArchitectureCheck.buttons().containsKey("load"),"Blueprint actions");
            var before=design();mouse(700,400,true);mouse(700,400,false);
            require(!(Boolean)field("blueprintsOpen") && design().items().equals(before.items()),"Outside click closes without editing scene");
            clickId("sceneMenu");clickId("sceneMenu");require(!(Boolean)field("sceneMenuOpen"),"Hamburger toggles closed");
            clickId("sceneMenu");ArchitectureCheck.layout();
            require(ArchitectureCheck.buttons().containsKey("help"),"Help menu item");
            clickId("help");require((Boolean)field("helpOpen"),"Help opens from menu");
            ArchitectureCheck.layout();
            require(ArchitectureCheck.buttons().containsKey("helpClose"),"Help close button");
            clickId("helpClose");require(!(Boolean)field("helpOpen"),"Help closes from button");
            key(com.jme3.input.KeyInput.KEY_F1,true);require((Boolean)field("helpOpen"),"F1 opens help");
            key(com.jme3.input.KeyInput.KEY_F1,false);
            key(com.jme3.input.KeyInput.KEY_ESCAPE,true);require(!(Boolean)field("helpOpen"),"Escape closes help");
            key(com.jme3.input.KeyInput.KEY_ESCAPE,false);
            System.out.println("UI_STAGE: menus and help passed");
            clickId("architecture");clickId("tool:SELECT");clickWorld(new Vector3f(0,0,0));
            require(Objects.equals(field("selected"),0L),"Select independent house; selected="+field("selected")+" tool="+field("tool"));
            for(String axis:List.of("WIDTH","WIDTH_NEG","DEPTH","DEPTH_NEG","HEIGHT","HEIGHT_NEG"))
                handle("objectHandles","resizeAxis",axis);
            before=design();var width=screen(handle("objectHandles","resizeAxis","WIDTH_NEG"));
            drag(width,screen(new Vector3f(-3.5f,0,0)));
            require(design().items().getFirst().width()>before.items().getFirst().width()+.3f,"Negative X resize arrow");selectedOnly(before,0);
            before=design();var depth=screen(handle("objectHandles","resizeAxis","DEPTH"));
            drag(depth,screen(new Vector3f(0,0,3.2f)));
            require(design().items().getFirst().depth()>before.items().getFirst().depth()+.3f,"Positive Z resize arrow");selectedOnly(before,0);
            before=design();var height=screen(handle("objectHandles","resizeAxis","HEIGHT"));
            drag(height,new Vector3f(height.x,height.y+28,0));
            require(design().items().getFirst().height()>before.items().getFirst().height()+.2f,"Height resize arrow");selectedOnly(before,0);
            before=design();height=screen(handle("objectHandles","resizeAxis","HEIGHT_NEG"));
            drag(height,new Vector3f(height.x,height.y-20,0));
            require(design().items().getFirst().height()<before.items().getFirst().height()-.15f,"Downward resize arrow");selectedOnly(before,0);
            System.out.println("UI_STAGE: house resize isolation passed");
            clickId("collection");clickId("architecture");clickId("tool:SELECT");
            clickWorld(new Vector3f(-5,3.8f,-2));require(Objects.equals(field("selected"),2L),"Select roof block");
            before=design();width=screen(handle("objectHandles","resizeAxis","WIDTH_NEG"));
            drag(width,new Vector3f(width.x-30,width.y,0));
            var roof=design().items().stream().filter(item->item.id()==2).findFirst().orElseThrow();
            require(roof.width()!=before.items().get(2).width(),"Resize mounted roof");selectedOnly(before,2);
            System.out.println("UI_STAGE: independent roof resize passed");
            var file=Path.of("glade/classes/game-ui-test.glade");design().save(file);
            require(GladeDesign.load(file).items().equals(design().items()),"Independent roof save round trip");
            var properties=new Properties();try(var input=Files.newInputStream(file)){properties.load(input);}
            properties.setProperty("version","glade-4");
            properties.remove(WorldFiles.DIGEST);properties.remove(WorldFiles.FORMAT);
            try(var output=Files.newOutputStream(file)){properties.store(output,"Legacy roof migration");}
            require(GladeDesign.load(file).items().get(2).width()==design().items().getFirst().width(),"Version 4 roof migration");Files.delete(file);
            before=design();clickId("hideRoof");
            require((Boolean)field("roofsHidden") && design().items().equals(before.items()),"Roof toggle leaves design intact");
            engine(()->{
                var member=GladeApp.class.getDeclaredField("content");member.setAccessible(true);
                var content=(Node)member.get(app);
                content.depthFirstTraversal(spatial->{if(Boolean.TRUE.equals(spatial.getUserData("roofPart")) && spatial.getCullHint()!=Spatial.CullHint.Always)
                    throw new AssertionError("Roof still visible");});return true;
            });
            clickId("hideRoof");clickId("collection");clickId("garden");
            engine(()->{app.getStateManager().getState(ScreenshotAppState.class).takeScreenshot();return true;});Thread.sleep(600);
            clickId("sceneMenu");
            engine(()->{app.getStateManager().getState(ScreenshotAppState.class).takeScreenshot();return true;});Thread.sleep(600);
            System.out.println("GAME_UI_PASS: hamburger/blueprints, outside dismissal, 6 resize arrows, selected block only, mounted roof independence, roof visibility, v4 migration/v5 persistence");
        } finally {app.stop();}
    }
}

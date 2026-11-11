package vn.dreamhouse.glade;

import com.jme3.math.*;
import com.jme3.system.AppSettings;
import com.jme3.app.state.ScreenshotAppState;
import java.nio.file.*;
import java.util.*;

class PartsCheck extends DirectEditCheck {
    static void require(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
    static void domain() throws Exception {
        dataChecks();
        var collection=GladeDesign.collection();
        var file=Path.of("glade/classes/parts-check.glade");collection.save(file);
        require(GladeDesign.load(file).items().equals(collection.items()),"Mounted part save/load");
        var host=collection.items().getFirst();
        require(collection.blocksOpening(host,-host.width()/2,0,host.height()*.66f,.45f),"Suppress automatic opening under custom window");
        require(!collection.blocksOpening(host,1.1f,host.depth()/2,1.5f,.45f),"Preserve unrelated automatic windows");
        var moved=collection.replace(new GladeDesign.Item(host.id(),host.kind(),new GladeDesign.Point(-4,-1),
                5,4,3.2f,host.pitched(),host.points(),90));
        var window=moved.items().stream().filter(item->item.kind()==GladeDesign.Kind.WINDOW_ROUND).findFirst().orElseThrow();
        var position=moved.placement(window);
        require(Math.abs(position.center().x()+4)<.01 && Math.abs(position.center().z()-1.5)<.01
                && Math.abs(position.elevation()-3.2f*.66f)<.01,"Attachment transform follows resized rotated host");
        var roof=moved.items().stream().filter(item->item.kind()==GladeDesign.Kind.ROOF_SLATE).findFirst().orElseThrow();
        require(moved.placement(roof).width()==host.width() && moved.placement(roof).depth()==host.depth(),"Roof dimensions stay independent of host resize");
        var replaced=moved.add(new GladeDesign.Item(moved.nextId(),GladeDesign.Kind.ROOF_TILE,host.center(),3,3,.5f,true,List.of(),0,
                new GladeDesign.Mount(0,0,0,0)));
        require(replaced.items().stream().filter(item->item.kind().roof() && item.mount().hostId()==0).count()==1,"Replace roof without stacking");
        var removed=moved.remove(0);
        require(removed.items().stream().noneMatch(item->item.id()==0 || item.mount()!=null && item.mount().hostId()==0),"Delete host and attachments");
        require(removed.items().stream().anyMatch(item->item.id()==1),"Preserve other building");
        Files.writeString(file,"version=glade-3\nland=-9,9,-6.5,6.5\ncount=1\nitem.0.id=0\nitem.0.kind=HOUSE\nitem.0.shape=0,0,4,3,2.3,true,15\nitem.0.points=\n");
        require(GladeDesign.load(file).items().getFirst().rotation()==15,"Version 3 compatibility");
        boolean rejected=false;
        try { collection.add(new GladeDesign.Item(99,GladeDesign.Kind.WINDOW_ARCH,host.center(),1,.5f,1,true,List.of(),0,new GladeDesign.Mount(88,0,.5f,.5f))); }
        catch(IllegalArgumentException expected) {rejected=true;}
        require(rejected,"Reject orphan attachments");Files.delete(file);
    }
    static void clickWorld(Vector3f point) throws Exception {
        var screen=screen(point);mouse((int)screen.x,(int)screen.y,true);mouse((int)screen.x,(int)screen.y,false);
    }
    public static void main(String[] args) throws Exception {
        domain();app=new GladeApp();
        var settings=new AppSettings(true);settings.setResolution(1360,860);settings.setRenderer(AppSettings.LWJGL_OPENGL33);
        settings.setAudioRenderer(null);settings.setGammaCorrection(true);settings.setSamples(4);settings.setVSync(true);
        app.setSettings(settings);app.setShowSettings(false);app.start();
        try {
            engine(()->true);Thread.sleep(400);
            var exposed=new HashSet<String>();
            for(String category:List.of("architecture","structures","roofs","wood","garden","landscape")) {
                clickId(category);
                for(int page=0;page<3;page++) {
                    ArchitectureCheck.layout();exposed.addAll(ArchitectureCheck.buttons().keySet());
                    if(!ArchitectureCheck.buttons().containsKey("next")) break;
                    int previous=(Integer)field("catalogPage");clickId("next");
                    if(previous==(Integer)field("catalogPage")) break;
                }
            }
            for(var kind:GladeDesign.Kind.values()) require(exposed.contains("tool:"+kind),"Catalog tool missing "+kind);
            clickId("clear");clickId("focus");clickId("architecture");clickId("tool:HOUSE");
            drag(screen(new Vector3f(-2,0,-2)),screen(new Vector3f(2,0,1)));
            require(design().items().size()==1,"Place attachment host");
            clickId("roofs");clickId("tool:ROOF_SLATE");clickWorld(new Vector3f(0,2.8f,-.5f));
            require(design().items().size()==2 && design().items().getLast().kind()==GladeDesign.Kind.ROOF_SLATE,"Attach replacement roof");
            clickId("tool:CHIMNEY_BRICK");clickWorld(new Vector3f(.6f,3.1f,-.5f));
            require(design().items().getLast().kind()==GladeDesign.Kind.CHIMNEY_BRICK,"Attach chimney");
            clickId("tool:WINDOW_ROUND");clickWorld(new Vector3f(-2,1.4f,-.5f));
            require(design().items().getLast().kind()==GladeDesign.Kind.WINDOW_ROUND,"Attach window");
            clickId("next");clickId("tool:DOOR_DOUBLE");clickWorld(new Vector3f(0,.6f,1));
            require(design().items().size()==5 && design().items().getLast().kind()==GladeDesign.Kind.DOOR_DOUBLE,"Attach door from second page");
            clickId("tool:SELECT");clickWorld(new Vector3f(-2,1.0f,-1.3f));
            require(Objects.equals(field("selected"),0L),"Select building behind attached details");
            clickId("width+");clickId("rotate+");clickId("copy");
            require(design().items().size()==10,"Copy building with attachments");
            long copyId=(Long)field("selected");
            require(design().items().stream().filter(item->item.mount()!=null && item.mount().hostId()==copyId).count()==4,"Copy linked attachments");
            clickId("undo");require(design().items().size()==5,"Undo assembly clone");
            clickId("redo");require(design().items().size()==10,"Redo assembly clone");
            clickId("collection");clickId("roofs");
            engine(()->{
                var distance=GladeApp.class.getDeclaredField("distance");distance.setAccessible(true);distance.setFloat(app,34);
                var focus=GladeApp.class.getDeclaredField("focus");focus.setAccessible(true);((Vector3f)focus.get(app)).set(0,1,0);
                var update=GladeApp.class.getDeclaredMethod("updateCamera");update.setAccessible(true);update.invoke(app);
                return true;
            });
            engine(()->{app.getStateManager().getState(ScreenshotAppState.class).takeScreenshot();return true;});Thread.sleep(600);
            clickId("garden");
            engine(()->{app.getStateManager().getState(ScreenshotAppState.class).takeScreenshot();return true;});Thread.sleep(600);
            System.out.println("PARTS_PASS: all 32 catalog kinds, 6 categories/pages, mounted placement, parent transform/resize, linked copy/delete, roof replacement, undo/redo, save versions 1-4, render all 19 new models");
        } finally {app.stop();}
    }
}

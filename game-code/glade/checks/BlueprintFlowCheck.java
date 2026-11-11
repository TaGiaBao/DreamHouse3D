package vn.dreamhouse.glade;

import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.system.AppSettings;
import java.nio.file.*;
import java.util.*;

class BlueprintFlowCheck extends ProceduralMechanicsCheck {
    static Object invoke(String name,Class<?>[] types,Object...args)throws Exception{return engine(()->{var method=GladeApp.class.getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(app,args);});}
    static void action(String id)throws Exception{invoke("action",new Class<?>[]{String.class},id);}
    static void bounds()throws Exception{for(var entry:ArchitectureCheck.buttons().entrySet())if(entry.getKey().startsWith("blueprint:") || Set.of("save","load").contains(entry.getKey())){var r=entry.getValue();ok(r.x>=0 && r.y>=0 && r.getMaxX()<=1000 && r.getMaxY()<=680,"blueprint control fits viewport: "+entry.getKey());}}
    public static void main(String[] args)throws Exception{
        app=new GladeApp();var settings=new AppSettings(true);settings.setResolution(1000,680);settings.setRenderer(AppSettings.LWJGL_OPENGL33);settings.setSamples(4);settings.setGammaCorrection(true);settings.setAudioRenderer(null);settings.setTitle("DreamHouse · Hộp bản thiết kế và học nhanh");app.setSettings(settings);app.setShowSettings(false);app.setPauseOnLostFocus(false);app.start();
        try{
            engine(()->true);fixture(BlueprintDataCheck.source());select(10);var shelf=(BlueprintShelf)field("blueprintShelf");var stored=shelf.save("Nhà thử nghiệm",BlueprintModule.capture(design(),10));call("reloadBlueprints");
            chord(com.jme3.input.KeyInput.KEY_C);clickRaw("blueprints");bounds();ok(ArchitectureCheck.buttons().containsKey("blueprint:pick:"+stored.id()),"saved module appears in personal shelf");
            var modalScene=design();float modalDistance=(Float)field("distance");chord(com.jme3.input.KeyInput.KEY_V);chord(com.jme3.input.KeyInput.KEY_Z);scroll(120);ok(design()==modalScene && (Float)field("distance")==modalDistance,"shelf blocks paste, undo and zoom behind modal");
            clickRaw("blueprint:pick:"+stored.id());var before=design();clickRaw("blueprint:place");ok(field("activeModule")!=null && design()==before,"choosing template starts preview without modifying world");
            var p=screen(new Vector3f(-5,0,4));mouse((int)p.x,(int)p.y,null);Thread.sleep(180);ok(engine(()->{var f=GladeApp.class.getDeclaredField("modulePreview");f.setAccessible(true);var node=(Node)f.get(app);return node.getQuantity()==2 && node.getCullHint()!=Spatial.CullHint.Always;}),"wire preview shows root and upper floor only, outside pickable content");
            float yaw=(Float)field("yaw");right(720,410,true);mouse(770,430,null);right(770,430,false);ok(design()==before && Math.abs((Float)field("yaw")-yaw)>.01f,"right orbit works during module preview without placement");
            set("yaw",-.65f);call("updateCamera");click(new Vector3f(-5,0,4));ok(design().items().size()==before.items().size()+3 && field("activeModule")==null,"one click places whole assembly and exits preview");
            long root=(Long)field("selected");ok(design().items().stream().anyMatch(i->i.form().support()==root) && design().items().stream().anyMatch(i->i.mount()!=null && i.mount().hostId()==root),"placed assembly has its own support and attachment links");
            chord(com.jme3.input.KeyInput.KEY_Z);ok(design()==before,"single undo restores previous scene");chord(com.jme3.input.KeyInput.KEY_Y);int count=design().items().size();
            clickRaw("blueprints");clickRaw("blueprint:place");esc();ok(field("activeModule")==null && design().items().size()==count,"escape cancels placement without side effects");
            clickRaw("blueprints");clickRaw("blueprint:delete");ok(shelf.list().entries().size()==1,"first delete click only asks for confirmation");clickRaw("blueprint:delete");ok(shelf.list().entries().isEmpty() && design().items().size()==count,"second delete removes template, not placed objects");clickRaw("blueprint:close");
            fixture(new GladeDesign(List.of(),2));action("mainMenu");key(com.jme3.input.KeyInput.KEY_F1,true);key(com.jme3.input.KeyInput.KEY_F1,false);ok(!(Boolean)field("mainMenuOpen") && (Boolean)field("helpOpen"),"F1 opens help above main menu");ok(ArchitectureCheck.buttons().containsKey("helpStart"),"help offers optional practice");
            ok(engine(()->{var f=GladeApp.class.getDeclaredField("hud");f.setAccessible(true);var ui=(GladeHud)f.get(app);return ui.node().getChildren().stream().anyMatch(n->"helpStart".equals(n.getUserData("buttonId")) && ((Number)n.getUserData("hudLayer")).floatValue()>5);}),"practice button renders above help panel");
            clickRaw("helpStart");var guide=(BuildGuide)field("buildGuide");ok(guide.active && guide.count()==0 && design().items().isEmpty(),"starting guide preserves current world");
            var overlay=(java.awt.Rectangle)field("guideBounds");mouse(overlay.x+25,overlay.y+65,true);mouse(overlay.x+25,overlay.y+65,false);ok(design().items().isEmpty(),"guide panel does not leak clicks to building tools");
            clickRaw("architecture");clickRaw("house:HOUSE");click(new Vector3f(0,0,0));ok(guide.next()==BuildGuide.Goal.SHAPE,"real house placement advances practice");pull("HEIGHT",65);ok(guide.next()==BuildGuide.Goal.PAINT,"real shape drag advances practice");
            clickRaw("palette");clickRaw("paintMaterial:brick");ok(guide.next()==BuildGuide.Goal.BLUEPRINT,"real material action advances practice");
            long chosen=(Long)field("selected");var personal=shelf.save("Mẫu học nhanh",BlueprintModule.capture(design(),chosen));invoke("acceptBlueprint",new Class<?>[]{BlueprintShelf.Entry.class,boolean.class},personal,false);ok(guide.next()==BuildGuide.Goal.SAVE,"successful module storage advances to manual save");
            var snapshot=design();var file=PlayerFiles.directory("saves").resolve("guided-world.glade");snapshot.save(file);invoke("acceptWorldSave",new Class<?>[]{GladeDesign.class,Path.class},snapshot,file);ok(guide.next()==null && guide.count()==5 && GladeDesign.load(file).items().equals(snapshot.items()),"manual save success completes guide with reloadable world");
            esc();clickRaw("guideHide");ok(!guide.active && field("guideBounds")==null,"guide can be dismissed cleanly");
            for(int i=0;i<7;i++)shelf.save("Mẫu trang "+i,personal.module());call("reloadBlueprints");clickRaw("blueprints");clickRaw("blueprint:next");ok((Integer)field("blueprintPage")==1,"personal shelf pagination works");bounds();clickRaw("blueprint:previous");ok((Integer)field("blueprintPage")==0,"can return to first page");
            System.out.println("BLUEPRINT_FLOW_PASS: saved shelf UI and bounds, cancellable wire preview, right camera orbit, atomic assembly placement/undo, independent links, two-step template deletion, optional guide without scene reset, real build/shape/paint progress, stored module and world save completion, pagination; no screenshots");
        }finally{app.stop();}
    }
}

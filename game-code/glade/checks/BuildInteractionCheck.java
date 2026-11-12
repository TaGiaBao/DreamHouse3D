package vn.dreamhouse.glade;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.system.AppSettings;
import com.jme3.input.KeyInput;
import java.util.*;

class BuildInteractionCheck extends ProceduralMechanicsCheck {
    static void model(){
        var house=block(0,0,0,4,3,2.6f,1.4f);var scene=new GladeDesign(List.of(house),2);var movable=block(10,3.13f,0,2,2,2,1);
        var snapped=BuildSnap.resolve(scene,movable,.2f,null);ok(Math.abs(snapped.center().x()-3)<.001f && snapped.lines().size()==2,"flush edge and center guide");
        var held=BuildSnap.resolve(scene,BuildSnap.copy(movable,new GladeDesign.Point(3.25f,0)),.2f,snapped);ok(Math.abs(held.center().x()-3)<.001f,"hysteresis keeps near snapped edge stable");
        var released=BuildSnap.resolve(scene,BuildSnap.copy(movable,new GladeDesign.Point(3.5f,.6f)),.2f,held);ok(released.neighbor()<0,"drag beyond release radius becomes free");
        var rotated=new GladeDesign.Item(0,house.kind(),house.center(),4,3,2.6f,true,List.of(),45,null,house.form());scene=new GladeDesign(List.of(rotated),2);
        var center=new GladeDesign.Point(3.13f*FastMath.cos(FastMath.QUARTER_PI),-3.13f*FastMath.sin(FastMath.QUARTER_PI));var moving=new GladeDesign.Item(10,movable.kind(),center,2,2,2,true,List.of(),45,null,movable.form());snapped=BuildSnap.resolve(scene,moving,.2f,null);ok(snapped.neighbor()==0 && Math.abs(rotated.localX(snapped.center())-3)<.001f,"alignment follows rotated neighbor axes");
        var tower=new GladeDesign.Item(0,GladeDesign.Kind.TOWER,house.center(),4,3,2.6f,true,List.of(),0,null,house.form());ok(BuildSnap.resolve(new GladeDesign(List.of(tower),2),movable,.2f,null).neighbor()<0,"round blocks do not use rectangular fake edges");
        var child=new GladeDesign.Item(1,house.kind(),new GladeDesign.Point(1,0),5,3,2,true,List.of(),0,null,house.form().support(0));scene=new GladeDesign(List.of(house,child,block(2,14,0,2,2,2,1)),new LandBounds(-17,17,-12.5f,12.5f));var candidate=BuildSnap.copy(house,new GladeDesign.Point(13.9f,0));ok(BuildSnap.resolve(scene,candidate,.2f,null).neighbor()<0,"snap cannot push supported assembly outside plot");
        polygonModel(house);
        PrecisionAndCraftCheck.axisMath();
    }
    static void polygonModel(GladeDesign.Item house){
        var scene=new GladeDesign(List.of(house),2);
        var pentagon=new GladeDesign.Item(10,GladeDesign.Kind.PENTAGON,new GladeDesign.Point(3.13f,.42f),2,2,2,true,List.of(),0,null,house.form());
        var snapped=BuildSnap.resolve(scene,pentagon,.2f,null);
        ok(snapped.neighbor()==house.id() && snapped.lines().size()==1 && Math.abs(snapped.center().x()-3)<.001f
                && Math.abs(snapped.center().z()-.42f)<.001f,"pentagon snaps by its real side and can slide along it");
        var held=BuildSnap.resolve(scene,BuildSnap.copy(pentagon,new GladeDesign.Point(3.25f,.5f)),.2f,snapped);
        ok(held.neighbor()==house.id() && Math.abs(held.center().x()-3)<.001f,"edge pair stays locked within the release distance");
        var separated=BuildSnap.resolve(scene,BuildSnap.copy(pentagon,new GladeDesign.Point(3.4f,.5f)),.2f,held);
        ok(separated.neighbor()<0,"pentagon releases beyond its snap range");

        var polygon=new GladeDesign.Item(0,GladeDesign.Kind.PENTAGON,house.center(),4,4,2,true,List.of(),0,null,house.form());
        var target=new GladeDesign(List.of(polygon),2);
        var diagonal=Pentagon.edges(4,4).get(2);
        var normal=diagonal.normal();
        var midpoint=diagonal.from().add(diagonal.to()).multLocal(.5f);
        var requested=midpoint.add(normal.mult(.93f));
        float angle=FastMath.atan2(-normal.x,-normal.y)*FastMath.RAD_TO_DEG;
        var square=new GladeDesign.Item(3,GladeDesign.Kind.HOUSE,new GladeDesign.Point(requested.x,requested.y),
                1.6f,1.6f,2,true,List.of(),angle,null,house.form());
        var result=BuildSnap.resolve(target,square,.2f,null);
        var expected=midpoint.add(normal.mult(.8f));
        ok(result.neighbor()==0 && result.center().distance(new GladeDesign.Point(expected.x,expected.y))<.001f,
                "rectangle snaps to an angled pentagon wall when rotated to face it");
        var corner=new GladeDesign.Item(3,GladeDesign.Kind.HOUSE,new GladeDesign.Point(1.55f,-2.48f),
                .6f,.6f,2,true,List.of(),0,null,house.form());
        ok(BuildSnap.resolve(target,corner,.2f,null).neighbor()<0,"empty corner behind the pentagon is not a snap edge");
    }

    static void polygonUi()throws Exception{
        var house=block(0,0,0,4,3,2.6f,1.4f);
        fixture(new GladeDesign(List.of(house),2));esc();
        set("qualityLow",true);call("applyQuality");
        set("yaw",0f);set("elevation",.72f);set("distance",29f);call("updateCamera");
        BuildMorphCheck.choose("PENTAGON");
        var placement=screen(new Vector3f(4.02f,0,.35f));
        mouse(Math.round(placement.x),Math.round(placement.y),null);
        var ghost=(Node)field("buildGhost");
        ok(Math.abs(ghost.getLocalTranslation().x-3.9f)<.02f,"preview shows the snapped pentagon");
        click(new Vector3f(4.02f,0,.35f));
        var pentagon=design().items().getLast();
        ok(pentagon.kind()==GladeDesign.Kind.PENTAGON && Math.abs(pentagon.center().x()-3.9f)<.02f,"committed placement agrees with preview");
        select(pentagon.id());
        var face=screen(new Vector3f(pentagon.center().x()+.25f,.9f,pentagon.center().z()+pentagon.depth()/2));
        int x=Math.round(face.x),y=Math.round(face.y);
        mouse(x,y,null);
        var anchor=engine(()->{var ground=GladeApp.class.getDeclaredMethod("ground");ground.setAccessible(true);return (GladeDesign.Point)ground.invoke(app);});
        var out=screen(new Vector3f(anchor.x()+1.35f,0,anchor.z()));
        var near=screen(new Vector3f(anchor.x()+.12f,0,anchor.z()));
        mouse(x,y,true);
        mouse(Math.round(out.x),Math.round(out.y),null);
        ok(field("moving")!=null,"selected pentagon begins a move");
        mouse(Math.round(near.x),Math.round(near.y),null);
        mouse(Math.round(near.x),Math.round(near.y),false);
        ok(Math.abs(item(pentagon.id()).center().x()-3.9f)<.02f,"moving a selected pentagon snaps back to the same edge");
        key(KeyInput.KEY_LMENU,true);
        try{
            var free=BuildSnap.copy(pentagon,new GladeDesign.Point(4.02f,.35f));
            ok(snap(free).center().equals(free.center()),"Alt bypasses polygon snapping");
        }finally{key(KeyInput.KEY_LMENU,false);}
    }
    static GladeDesign.Item snap(GladeDesign.Item item)throws Exception{return engine(()->{var m=GladeApp.class.getDeclaredMethod("snapPlacement",GladeDesign.Item.class);m.setAccessible(true);return (GladeDesign.Item)m.invoke(app,item);});}
    public static void main(String[]args)throws Exception {
        model();if(Arrays.asList(args).contains("--model-only")){System.out.println("BUILD_SNAP_MODEL_PASS");return;}
        app=new GladeApp();var settings=new AppSettings(true);settings.setResolution(1360,860);settings.setRenderer(AppSettings.LWJGL_OPENGL33);settings.setAudioRenderer(null);settings.setTitle("DreamHouse · Kiểm tra thao tác xây dựng");app.setSettings(settings);app.setShowSettings(false);app.setPauseOnLostFocus(false);app.start();
        try {app.enqueue(()->true).get(90,java.util.concurrent.TimeUnit.SECONDS);Thread.sleep(300);
            if(Arrays.asList(args).contains("--polygon-snap-only")){polygonUi();System.out.println("POLYGON_SNAP_UI_PASS");return;}
            fixture(new GladeDesign(List.of(block(0,0,0,4,3,2.6f,1.4f)),2));esc();
            var candidate=block(10,3.13f,0,2,2,2,1);ok(Math.abs(snap(candidate).center().x()-3)<.001f,"new and moved placement use same snap calculation");
            key(KeyInput.KEY_LMENU,true);ok((Boolean)field("freePlacement") && Math.abs(snap(candidate).center().x()-3.13f)<.001f,"Alt bypasses alignment without modifying scene");key(KeyInput.KEY_LMENU,false);ok(!(Boolean)field("freePlacement"),"Alt release restores snapping");
            select(0);var point=screen(new Vector3f(0,1,1.5f));float yaw=(Float)field("yaw");mouse((int)point.x,(int)point.y,true);right((int)point.x,(int)point.y,true);ok(!(Boolean)field("rotating"),"right camera cannot steal a pending left edit");mouse((int)point.x+45,(int)point.y+8,null);mouse((int)point.x+45,(int)point.y+8,false);right((int)point.x+45,(int)point.y+8,false);ok((Float)field("yaw")==yaw,"left drag never rotates camera");
            var before=design();right(1000,600,true);mouse(1000,600,true);mouse(1080,630,null);mouse(1080,630,false);right(1080,630,false);ok(design()==before && !(Boolean)field("drawing"),"left clicks during right orbit cannot edit objects");
            select(0);point=screen(handle("objectHandles","resizeAxis","HEIGHT"));mouse((int)point.x,(int)point.y,true);float distance=(Float)field("distance");scroll(120);ok((Float)field("distance")==distance,"wheel cannot change ray/axis camera while resizing");esc();mouse((int)point.x,(int)point.y,false);ok(design()==before,"Escape cancels edit without committing");
            fixture(new GladeDesign(List.of(),2));clickRaw("terrain");point=screen(new Vector3f(0,0,0));right((int)point.x,(int)point.y,true);mouse(1100,600,true);mouse(1100,600,false);right((int)point.x,(int)point.y,false);fixture(new GladeDesign(List.of(block(0,0,0,4,3,2.6f,1.4f)),2));esc();click(new Vector3f(0,1,1.5f));ok(Objects.equals(field("selected"),0L),"terrain right stroke cannot leave stale left-click coordinates");
            fixture(new GladeDesign(List.of(block(0,0,0,4,3,2.6f,1.4f)),2));BuildMorphCheck.choose("HOUSE");var top=screen(new Vector3f(0,4,0));mouse((int)top.x,(int)top.y,null);Thread.sleep(150);
            ok(engine(()->{var f=GladeApp.class.getDeclaredField("buildGhost");f.setAccessible(true);return Math.abs(((Node)f.get(app)).getLocalTranslation().y-2.6f)<.01f;}),"stack preview starts exactly at supporting floor");
            mouse((int)top.x,(int)top.y,true);mouse((int)top.x,(int)top.y,false);ok(design().items().size()==2 && design().items().getLast().form().support()==0,"stack preview and placed support agree");ok(design().items().getLast().center().distance(new GladeDesign.Point(0,0))<.15f,"stack placement stays under actual roof hit instead of jumping behind it");
            fixture(new GladeDesign(List.of(block(0,0,0,4,3,2.6f,1.4f)),2));esc();select(0);
            for(float zoom:new float[]{20,65}){set("distance",zoom);call("updateCamera");call("refreshHandles");ok(engine(()->{var f=GladeApp.class.getDeclaredField("objectHandles");f.setAccessible(true);var handles=(Node)f.get(app);handles.updateGeometricState();float[] pixels={0};handles.depthFirstTraversal(n->{if("WIDTH".equals(n.getUserData("resizeAxis"))){var origin=n.getWorldTranslation();var a=app.getCamera().getScreenCoordinates(origin);var b=app.getCamera().getScreenCoordinates(origin.add(app.getCamera().getLeft().mult(.17f*n.getLocalScale().x)));pixels[0]=new Vector2f(a.x,a.y).distance(new Vector2f(b.x,b.y));}});return pixels[0]>5.5f && pixels[0]<6.5f;}),"grip keeps readable pixel radius at zoom "+zoom);}
            key(KeyInput.KEY_LMENU,true);engine(()->{app.loseFocus();return true;});ok(!(Boolean)field("freePlacement"),"focus loss releases temporary modifier");engine(()->{app.gainFocus();return true;});
            polygonUi();
            System.out.println("BUILD_INTERACTION_PASS: gridless edge/center and rotated snapping, stable release, Alt freedom, assembly bounds, 54 projected drags, exclusive camera/edit gestures, zoom during resize blocked, cancel, terrain button queue, stack hit/preview, readable grip size; no screenshots");
        }finally {app.stop();}
    }
}

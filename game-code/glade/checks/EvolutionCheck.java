package vn.dreamhouse.glade;

import com.jme3.app.state.ScreenshotAppState;
import com.jme3.input.event.*;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.system.AppSettings;
import java.nio.file.*;
import java.util.*;

class EvolutionCheck extends DirectEditCheck {
    static void require(boolean value,String reason) {if(!value) throw new AssertionError(reason);}
    static void show(GladeDesign next) throws Exception {
        engine(()->{
            var member=GladeApp.class.getDeclaredField("design");member.setAccessible(true);member.set(app,next);
            var selected=GladeApp.class.getDeclaredField("selected");selected.setAccessible(true);selected.set(app,null);
            for(String name:List.of("rebuild","rebuildHud")) {var method=GladeApp.class.getDeclaredMethod(name);method.setAccessible(true);method.invoke(app);}
            return true;
        });
    }
    static void clickWorld(Vector3f point) throws Exception {var p=screen(point);mouse((int)p.x,(int)p.y,true);mouse((int)p.x,(int)p.y,false);}
    static GladeDesign.Item item(long id) throws Exception {return design().items().stream().filter(i->i.id()==id).findFirst().orElseThrow();}
    static void pull(String axis,float pixels) throws Exception {
        var point=screen(handle("objectHandles","resizeAxis",axis));
        mouse((int)point.x,(int)point.y,true);
        require(field("resizeAxis")!=null && field("resizeAxis").toString().equals(axis),"Requested "+axis+" but grabbed "+field("resizeAxis")+" at "+point);
        mouse((int)point.x,(int)(point.y+pixels),null);mouse((int)point.x,(int)(point.y+pixels),false);
    }
    static void domain() throws Exception {
        dataChecks();
        var raised=new TerrainField().brush(0,0,2,2,null);
        require(raised.height(0,0)>1.8f && raised.height(5,5)==0,"Local terrain brush");
        require(raised.brush(0,0,2,-1,null).height(0,0)<raised.height(0,0),"Lower terrain");
        var design=GladeDesign.evolutionSample();
        require(Math.abs(design.placement(design.items().get(3)).elevation()-design.top(design.items().get(2)))<.01,"Stack height");
        require(new BlockFusion(design).joined(0) && new BlockFusion(design).joined(1),"Connected roof envelope");
        var host=design.items().get(2);
        var moved=design.replace(new GladeDesign.Item(host.id(),host.kind(),new GladeDesign.Point(7,-2),host.width(),host.depth(),host.height(),host.pitched(),host.points(),90,null,host.form()));
        require(moved.items().get(3).center().equals(new GladeDesign.Point(7,-2)),"Move supported block assembly");
        require(moved.remove(2).items().stream().noneMatch(i->i.id()==2 || i.form().support()==2),"Delete support assembly");
        var file=Path.of("glade/classes/evolution-roundtrip.glade");design.save(file);var loaded=GladeDesign.load(file);
        require(loaded.items().equals(design.items()) && loaded.terrain().cells().equals(design.terrain().cells()),"Terrain/profile/stack persistence");
        var many=new ArrayList<GladeDesign.Item>();
        for(int i=0;i<401;i++) many.add(new GladeDesign.Item(i,GladeDesign.Kind.FLOWERS,new GladeDesign.Point(0,0),1,1,1,false,List.of()));
        var unrestricted=new GladeDesign(many,2);unrestricted.save(file);require(GladeDesign.load(file).items().size()==401,"Removed 300-object gameplay limit");
        var expanded=unrestricted.resizeLand(LandBounds.Edge.LEFT,-80);require(expanded.land().width()>64,"Expanded land beyond old limit");
        String legacy="land=-9,9,-6.5,6.5\ncount=2\nitem.0.id=0\nitem.0.kind=HOUSE\nitem.0.shape=0,0,4,3,2.3,true,0\nitem.0.points=\n"
                +"item.1.id=1\nitem.1.kind=ROOF_TILE\nitem.1.shape=0,0,3.6,2.8,0.5,true,0\nitem.1.mount=0,0,0,0\nitem.1.points=\n";
        for(String version:List.of("glade-4","glade-5")) {
            Files.writeString(file,"version="+version+"\n"+legacy);
            require(GladeDesign.load(file).items().get(1).width()==(version.equals("glade-4")?4:3.6f),"Legacy attached roof "+version);
        }
        Files.writeString(file,"version=glade-3\nland=-9,9,-6.5,6.5\ncount=1\nitem.0.id=0\nitem.0.kind=HOUSE\nitem.0.shape=0,0,4,3,2.3,true,0\nitem.0.points=\n");
        require(GladeDesign.load(file).items().getFirst().form().roof()>0,"Version 3 migration");
        Files.delete(file);
        System.out.println("EVOLUTION_DOMAIN_PASS");
    }
    static void cameraEvent(int dx,int dy,Boolean down) throws Exception {
        engine(()->{
            var input=app.getInputManager();var gate=com.jme3.input.InputManager.class.getDeclaredField("eventsPermitted");gate.setAccessible(true);gate.setBoolean(input,true);
            try {
                var event=new MouseMotionEvent(700,450,dx,dy,0,0);event.setTime(System.nanoTime());input.onMouseMotionEvent(event);
                if(down!=null) {var button=new MouseButtonEvent(1,down,700,450);button.setTime(System.nanoTime());input.onMouseButtonEvent(button);}
            } finally {gate.setBoolean(input,false);}
            return true;
        });Thread.sleep(100);
    }
    public static void main(String[] args) throws Exception {
        domain();app=new GladeApp();var settings=new AppSettings(true);settings.setResolution(1360,860);
        settings.setRenderer(AppSettings.LWJGL_OPENGL33);settings.setSamples(4);settings.setGammaCorrection(true);settings.setAudioRenderer(null);settings.setVSync(true);
        app.setSettings(settings);app.setShowSettings(false);app.setPauseOnLostFocus(false);app.start();
        try {
            engine(()->true);Thread.sleep(350);
            var tab=ArchitectureCheck.buttons().get("architecture");mouse((int)tab.getCenterX(),(int)tab.getCenterY(),null);
            require((Boolean)field("houseFlyout"),"House hover opens palette");
            require(ArchitectureCheck.buttons().keySet().stream().filter(k->k.startsWith("house:")).count()==6,"Exactly six hover house types");
            mouse(950,440,null);Thread.sleep(450);engine(()->true);require(!(Boolean)field("houseFlyout"),"House palette closes naturally");
            show(new GladeDesign(List.of(),2));clickId("landscape");clickId("tool:RAISE");
            var p=screen(new Vector3f(-4,0,-2));mouse((int)p.x,(int)p.y,true);Thread.sleep(650);
            var q=screen(new Vector3f(2,0,3));mouse((int)q.x,(int)q.y,null);Thread.sleep(500);mouse((int)q.x,(int)q.y,false);
            float first=design().terrain().height(-4,-2),second=design().terrain().height(2,3);
            require(first>.2f && second>.2f,"Click/hold/drag terrain sculpt");
            clickId("undo");require(design().terrain().cells().isEmpty(),"Terrain stroke is one undo step");
            clickId("redo");require(design().terrain().height(-4,-2)>.2f,"Terrain redo");
            clickId("tool:LOWER");p=screen(new Vector3f(-4,first,-2));mouse((int)p.x,(int)p.y,true);Thread.sleep(600);mouse((int)p.x,(int)p.y,false);
            require(design().terrain().height(-4,-2)<first,"Interactive lower brush");
            System.out.println("EVOLUTION_TERRAIN_PASS");
            show(new GladeDesign(List.of(),2));clickId("architecture");clickId("tool:HOUSE");
            drag(screen(new Vector3f(-2,0,-2)),screen(new Vector3f(2,0,1)));
            require(design().items().size()==1 && item(0).form().roof()==0,"New square block starts flat");
            clickId("tool:SELECT");clickWorld(new Vector3f(0,.8f,1));require(Objects.equals(field("selected"),0L),"Select square block");
            engine(()->{
                var member=GladeApp.class.getDeclaredField("objectHandles");member.setAccessible(true);var root=(Node)member.get(app);
                root.depthFirstTraversal(s->{if(s instanceof Geometry g) {
                    var color=(ColorRGBA)g.getMaterial().getParam("Color").getValue();
                    require(color.r==1 && color.g==1 && color.b==1 && color.a==1,"Uniform opaque white gizmos");
                }});return true;
            });
            float height=item(0).height();pull("ROOF",65);require(item(0).form().roof()>.4f && item(0).height()==height,"Upper surface creates roof without stretching walls");
            pull("ROOF",-220);require(item(0).form().roof()==0 && !item(0).pitched(),"Pull upper surface down removes roof");
            pull("BASE",60);require(item(0).form().stilts()>.4f,"Lower surface creates four stilts");
            pull("BASE",-140);require(item(0).form().stilts()==0,"Lower surface returns to solid base: "+item(0).form());
            clickId("tool:TOWER");clickWorld(new Vector3f(6,0,-2));require(item(1).form().roof()>0,"Cylinder starts with pencil roof");
            clickId("tool:SELECT");clickWorld(new Vector3f(6,4.9f,-2));require(Objects.equals(field("selected"),1L),"Select cylinder");
            pull("ROOF",-200);require(item(1).form().roof()==0 && item(1).kind()==GladeDesign.Kind.TOWER,"Cylinder can lose roof while preserving body");
            clickId("tool:HOUSE");clickWorld(new Vector3f(0,2.34f,-.5f));
            require(item(2).form().support()==0,"Place block on another block");
            clickId("tool:TOWER");var upper=item(2);clickWorld(new Vector3f(upper.center().x(),design().top(upper)+.04f,upper.center().z()));
            require(item(3).form().support()==2,"Stack a further level");
            clickId("tool:SELECT");clickWorld(new Vector3f(0,.8f,1));require(Objects.equals(field("selected"),0L),"Select stack foundation");
            clickId("copy");long copied=(Long)field("selected");
            require(design().items().size()==7 && design().assemblyIds(copied).size()==3,"Clone nested stack assembly");
            clickId("undo");require(design().items().size()==4,"Undo stack clone");
            System.out.println("EVOLUTION_SURFACES_STACK_PASS");
            show(new GladeDesign(List.of(),2));if((Boolean)field("stackMode")) clickId("stack");
            clickId("tool:MANOR");clickWorld(new Vector3f(-4,0,0));clickWorld(new Vector3f(-.15f,0,0));
            require(Math.abs(item(1).center().x()-item(0).center().x()-4)<.08f,"Magnetic side-by-side placement");
            require(new BlockFusion(design()).joined(0),"Interactive placed blocks share joined roof");
            clickId("expand");var knob=screen(handle("landHandles","landEdge","LEFT"));drag(knob,screen(new Vector3f(-21,0,0)));
            require(design().land().minX()<-20 && design().land().maxX()==13,"Actual land edge expansion");
            float orbitBefore=(Float)field("yaw");cameraEvent(0,0,true);cameraEvent(1,0,null);
            for(int i=0;i<8;i++) cameraEvent(200,0,null);
            cameraEvent(0,0,false);float orbitAfter=(Float)field("yaw");
            require(Math.abs(orbitAfter-orbitBefore)>.1f && orbitAfter>=-FastMath.PI && orbitAfter<=FastMath.PI,"Continuous relative 360 camera orbit");
            System.out.println("EVOLUTION_JOIN_EXPAND_ORBIT_PASS");
            show(GladeDesign.evolutionSample());clickId("architecture");
            engine(()->{var y=GladeApp.class.getDeclaredField("yaw");y.setAccessible(true);y.setFloat(app,-.65f);
                var d=GladeApp.class.getDeclaredField("distance");d.setAccessible(true);d.setFloat(app,44);
                var focus=GladeApp.class.getDeclaredField("focus");focus.setAccessible(true);((Vector3f)focus.get(app)).set(0,.65f,0);
                var camera=GladeApp.class.getDeclaredMethod("updateCamera");camera.setAccessible(true);camera.invoke(app);return true;});
            engine(()->{app.getStateManager().getState(ScreenshotAppState.class).takeScreenshot();return true;});Thread.sleep(500);
            mouse(950,440,null);Thread.sleep(400);clickId("tool:SELECT");clickWorld(new Vector3f(-1,1,-.5f));
            require(Objects.equals(field("selected"),1L),"Select individual fused block; selected="+field("selected"));
            engine(()->{app.getStateManager().getState(ScreenshotAppState.class).takeScreenshot();return true;});Thread.sleep(500);
            tab=ArchitectureCheck.buttons().get("architecture");mouse((int)tab.getCenterX(),(int)tab.getCenterY(),null);
            engine(()->{app.getStateManager().getState(ScreenshotAppState.class).takeScreenshot();return true;});Thread.sleep(500);
            System.out.println("EVOLUTION_PASS: terrain hold/drag and undo, hover six-house palette, opaque white handles, roof/base surfaces, nesting, joined roofs, expansion, 360 orbit, unlimited count, save/load");
        } finally {app.stop();}
    }
}

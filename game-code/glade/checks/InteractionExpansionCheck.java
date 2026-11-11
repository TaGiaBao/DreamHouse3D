package vn.dreamhouse.glade;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.input.KeyInput;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.system.AppSettings;
import com.jme3.collision.CollisionResults;
import java.nio.file.*;
import java.util.*;
class InteractionExpansionCheck extends DirectEditCheck {
 static void ok(boolean yes,String why){if(!yes)throw new AssertionError(why);}
 static void call(String name)throws Exception{engine(()->{var m=GladeApp.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(app);return true;});}
 static void set(String name,Object value)throws Exception{engine(()->{var f=GladeApp.class.getDeclaredField(name);f.setAccessible(true);f.set(app,value);return true;});}
 static void select(long id)throws Exception{set("selected",id);call("rebuildHud");}
 static void chord(int code)throws Exception{key(KeyInput.KEY_LCONTROL,true);key(code,true);key(code,false);key(KeyInput.KEY_LCONTROL,false);}
 static void esc()throws Exception{key(KeyInput.KEY_ESCAPE,true);key(KeyInput.KEY_ESCAPE,false);}
 static void click(Vector3f world)throws Exception{var p=screen(world);mouse((int)p.x,(int)p.y,true);mouse((int)p.x,(int)p.y,false);}
 static void shot()throws Exception{if(!Boolean.getBoolean("dreamhouse.captureTests"))return;engine(()->{app.getStateManager().getState(ScreenshotAppState.class).takeScreenshot();return true;});Thread.sleep(500);}
 static GladeDesign.Item item(long id)throws Exception{return design().items().stream().filter(i->i.id()==id).findFirst().orElseThrow();}
 static void pull(String axis,float pixels)throws Exception{var p=screen(handle("objectHandles","resizeAxis",axis));mouse((int)p.x,(int)p.y,true);ok(Objects.toString(field("resizeAxis")).equals(axis),"hit "+axis+" instead "+field("resizeAxis"));mouse((int)p.x,(int)(p.y+pixels),null);mouse((int)p.x,(int)(p.y+pixels),false);}
 static void fixtureHouse()throws Exception{fixture(new GladeDesign(List.of(new GladeDesign.Item(0,GladeDesign.Kind.HOUSE,new GladeDesign.Point(0,0),4,3,2.3f,true,List.of())),2));}
 static boolean rayWater(float x,float z)throws Exception{return engine(()->{var f=GladeApp.class.getDeclaredField("content");f.setAccessible(true);var content=(Node)f.get(app);var hits=new CollisionResults();content.collideWith(new Ray(new Vector3f(x,20,z),new Vector3f(0,-1,0)),hits);return hits.size()>0;});}
 public static void main(String[] args)throws Exception{
  CameraDockCheck.controller();EvolutionCheck.domain();app=new GladeApp();var settings=new AppSettings(true);settings.setResolution(1360,860);settings.setRenderer(AppSettings.LWJGL_OPENGL33);settings.setSamples(4);settings.setGammaCorrection(true);settings.setAudioRenderer(null);settings.setTitle("DreamHouse · Kiểm tra thư viện và tương tác");app.setSettings(settings);app.setShowSettings(false);app.setPauseOnLostFocus(false);app.start();
  try{engine(()->true);Thread.sleep(600);mouse(1000,500,null);
   for(String id:List.of("tool:SELECT","undo","redo","stack","snap"))ok(!ArchitectureCheck.buttons().containsKey(id),"removed utility strip "+id);
   for(String group:List.of("architecture","windows","trees","garden","landscape","terrain","water")){
    clickRaw(group);ok((Boolean)field("libraryOpen"),"click opens catalog "+group);ok(ArchitectureCheck.buttons().keySet().stream().filter(k->k.startsWith("libraryPick:")).count()==GladeCatalog.entries(group).size(),"complete catalog "+group);if(group.equals("trees"))shot();clickRaw("libraryClose");
   }
   ok(GladeCatalog.entries("landscape").stream().allMatch(e->e.tool().equals("PATH")),"road only");ok(GladeCatalog.entries("terrain").stream().noneMatch(e->Set.of("LEVEL","PATH","WATER").contains(e.tool())),"terrain separated");
   fixtureHouse();var palette=ArchitectureCheck.buttons().get("palette");mouse((int)palette.getCenterX(),(int)palette.getCenterY(),null);ok(!(Boolean)field("houseFlyout"),"palette no hover asset pile");clickRaw("palette");ok(field("tool").toString().equals("PAINT"),"dedicated paint mode");click(new Vector3f(0,.7f,1.5f));ok(Objects.equals(field("selected"),0L),"paint selects block without move");
   clickRaw("paintMaterial:brick");clickRaw("paintColor:#ae7783");ok(item(0).form().finish().material().equals("brick") && item(0).form().finish().color().equals("#ae7783"),"real wall paint");
   clickRaw("paintTarget:roof");clickRaw("paintMaterial:slate");clickRaw("paintColor:#7298b8");ok(item(0).form().finish().roofMaterial().equals("slate"),"independent roof paint");shot();
   engine(()->{var f=GladeApp.class.getDeclaredField("content");f.setAccessible(true);var n=(Node)f.get(app);var count=new int[2];n.depthFirstTraversal(s->{if(s instanceof Geometry g && g.getUserData("paintMaterial")!=null){if(Boolean.TRUE.equals(g.getUserData("wallSurface")))count[0]++;if(Boolean.TRUE.equals(g.getUserData("roofSurface")))count[1]++;ok(g.getMesh().getBuffer(VertexBuffer.Type.TexCoord)!=null,"painted material has UVs");}});ok(count[0]>0 && count[1]>0,"wall and roof render material");return true;});
   var file=Path.of("glade/classes/interaction-v8.glade");design().save(file);ok(GladeDesign.load(file).items().equals(design().items()),"finish v8 roundtrip");esc();chord(KeyInput.KEY_C);chord(KeyInput.KEY_V);long copied=(Long)field("selected");ok(item(copied).form().finish().equals(item(0).form().finish()),"paint survives clipboard");
   System.out.println("PALETTE_CATALOG_PASS");
   fixtureHouse();set("selected",null);call("rebuildHud");var body=screen(new Vector3f(0,.6f,1.5f));mouse((int)body.x,(int)body.y,true);mouse((int)body.x,(int)body.y,false);ok(Objects.equals(field("selected"),0L),"click selects");var before=item(0).center();
   var target=screen(new Vector3f(2,0,3));mouse((int)body.x,(int)body.y,true);mouse((int)target.x,(int)target.y,null);mouse((int)target.x,(int)target.y,false);ok(!item(0).center().equals(before),"drag selected body moves");
   var saved=design().items();float yaw=(Float)field("yaw");mouse(1070,480,true);mouse(1170,510,null);mouse(1170,510,false);ok(Math.abs((Float)field("yaw")-yaw)>.1 && design().items().equals(saved),"empty left drag orbits without moving selected object");
   set("yaw",-.65f);set("elevation",.62f);call("updateCamera");esc();
   float quickYaw=(Float)field("yaw");var snapshot=design().items();
   engine(()->{var input=app.getInputManager();var gate=com.jme3.input.InputManager.class.getDeclaredField("eventsPermitted");gate.setAccessible(true);gate.setBoolean(input,true);
    try{input.onMouseMotionEvent(new com.jme3.input.event.MouseMotionEvent(1020,440,0,0,0,0));input.onMouseMotionEvent(new com.jme3.input.event.MouseMotionEvent(1140,480,120,40,0,0));input.onMouseButtonEvent(new com.jme3.input.event.MouseButtonEvent(0,true,1020,440));input.onMouseButtonEvent(new com.jme3.input.event.MouseButtonEvent(0,false,1140,480));}finally{gate.setBoolean(input,false);}return true;});Thread.sleep(300);
   ok(Math.abs((Float)field("yaw")-quickYaw)>.3 && design().items().equals(snapshot),"rapid native-style move-before-buttons batch preserves initial click anchor");
   set("yaw",-.65f);set("elevation",.62f);call("updateCamera");
   System.out.println("LEFT_GESTURE_PASS");
   fixture(new GladeDesign(List.of(),2));clickRaw("water");clickRaw("libraryPick:1");var a=screen(new Vector3f(-5,0,-2));var b=screen(new Vector3f(-5,0,3));var c=screen(new Vector3f(2,0,3));
   mouse((int)a.x,(int)a.y,true);mouse((int)b.x,(int)b.y,null);mouse((int)c.x,(int)c.y,null);mouse((int)c.x,(int)c.y,false);
   var water=design().items().getFirst();ok(water.kind()==GladeDesign.Kind.WATER && water.points().size()>30,"water stores drawn path");ok(rayWater(-5,0) && rayWater(0,3) && !rayWater(2,-2),"free L-shaped continuous water surface, not bounding ellipse");shot();chord(KeyInput.KEY_Z);ok(design().items().isEmpty(),"one water stroke one undo");chord(KeyInput.KEY_Y);ok(design().items().size()==1,"water redo");
   System.out.println("FREE_WATER_PASS");
   fixture(new GladeDesign(List.of(),2));clickRaw("architecture");clickRaw("libraryPick:7");click(new Vector3f(0,0,0));esc();select(0);ok(item(0).form().finish().variant()==7 && item(0).form().finish().material().equals("wood"),"craft template");
   pull("HEIGHT",55);ok(item(0).height()>1.2f,"upper point makes elevated craft platform");shot();
   pull("HEIGHT",-300);ok(item(0).height()<=.12f,"upper point can flatten object");pull("HEIGHT",30);float top=design().top(item(0));pull("BOTTOM",15);ok(Math.abs(design().top(item(0))-top)<.02 && item(0).form().base()>.2f,"lower point moves independently with fixed upper plane");float elevation=design().placement(item(0)).elevation();pull("ELEVATION",15);ok(design().placement(item(0)).elevation()>elevation,"center point raises whole object");
   fixtureHouse();select(0);var left=handle("objectHandles","resizeAxis","ROOF_LEFT");var right=handle("objectHandles","resizeAxis","ROOF_RIGHT");ok(left.x<0 && right.x>0 && Math.abs(left.z-right.z)<.01,"roof arrows sit horizontally on two sides");
   var turnPoint=engine(()->{var f=GladeApp.class.getDeclaredField("objectHandles");f.setAccessible(true);var root=(Node)f.get(app);var result=new Vector3f[1];root.depthFirstTraversal(spatial->{if(spatial.getName().equals("Nút xoay"))result[0]=spatial.getWorldTranslation().clone();});return result[0];});
   var turnScreen=screen(turnPoint);mouse((int)turnScreen.x,(int)turnScreen.y,true);ok(Objects.toString(field("resizeAxis")).equals("ROTATE"),"rotation arc hit");
   var initial=(GladeDesign.Point)field("start");var turnEnd=screen(new Vector3f(initial.z(),(Float)field("resizePlaneY"),-initial.x()));mouse((int)turnEnd.x,(int)turnEnd.y,null);mouse((int)turnEnd.x,(int)turnEnd.y,false);ok(item(0).rotation()>70 && item(0).rotation()<110,"angular object axis follows quarter turn: "+item(0).rotation());
   var trees=new ArrayList<GladeDesign.Item>();for(int i=0;i<8;i++)trees.add(new GladeDesign.Item(i,GladeDesign.Kind.TREE,new GladeDesign.Point(-7+i%4*4.5f,-3+i/4*6),1.8f,1.8f,3.2f,false,List.of(),0,null,new GladeDesign.Form(0,0,0,-1).finish(GladeDesign.Finish.DEFAULT.variant(i))));fixture(new GladeDesign(trees,2));shot();
   System.out.println("INTERACTION_EXPANSION_PASS: full click catalogs, true material/color, paint save/copy, left click/move/orbit, independent craft points and flattening, free-form water/undo, roof side arrows");
  }finally{app.stop();}
 }
}


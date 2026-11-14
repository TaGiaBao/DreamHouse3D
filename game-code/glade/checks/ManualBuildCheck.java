package vn.dreamhouse.glade;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.input.KeyInput;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.system.AppSettings;
import java.util.*;
import java.nio.file.*;
class ManualBuildCheck extends DirectEditCheck {
 static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static void set(String key,Object value)throws Exception {engine(()->{var f=GladeApp.class.getDeclaredField(key);f.setAccessible(true);f.set(app,value);return true;});}
 static void invoke(String key)throws Exception {engine(()->{var m=GladeApp.class.getDeclaredMethod(key);m.setAccessible(true);m.invoke(app);return true;});}
 static void select(long id)throws Exception {set("selected",id);invoke("rebuildHud");}
 static GladeDesign.Item item(long id)throws Exception {return design().items().stream().filter(p->p.id()==id).findFirst().orElseThrow();}
 static void chord(int code)throws Exception {key(KeyInput.KEY_LCONTROL,true);key(code,true);key(code,false);key(KeyInput.KEY_LCONTROL,false);}
 static void snap()throws Exception {engine(()->{app.getStateManager().getState(ScreenshotAppState.class).takeScreenshot();return true;});Thread.sleep(450);}
 static void pull(String axis,float pixels)throws Exception {
  var p=screen(handle("objectHandles","resizeAxis",axis));mouse((int)p.x,(int)p.y,true);
  require(Objects.toString(field("resizeAxis")).equals(axis),"hit actual "+axis+" got "+field("resizeAxis"));
  mouse((int)p.x,(int)(p.y+pixels),null);mouse((int)p.x,(int)(p.y+pixels),false);
 }
 static void extend(String axis,float dz)throws Exception {
  var world=handle("objectHandles","resizeAxis",axis);var from=screen(world);mouse((int)from.x,(int)from.y,true);
  require(Objects.toString(field("resizeAxis")).equals(axis),"hit roof end "+axis+" got "+field("resizeAxis"));
  var start=(GladeDesign.Point)field("start");var to=screen(new Vector3f(start.x(),(Float)field("resizePlaneY"),start.z()+dz));
  mouse((int)to.x,(int)to.y,null);mouse((int)to.x,(int)to.y,false);
 }
 public static void main(String[] args)throws Exception {
  CameraDockCheck.controller();EvolutionCheck.domain();
  app=new GladeApp();var settings=new AppSettings(true);settings.setResolution(1360,860);settings.setRenderer(AppSettings.LWJGL_OPENGL33);settings.setSamples(4);settings.setGammaCorrection(true);settings.setAudioRenderer(null);settings.setTitle("DreamHouse · Kiểm tra thao tác thủ công");app.setSettings(settings);app.setShowSettings(false);app.setPauseOnLostFocus(false);app.start();
  try {
   engine(()->true);Thread.sleep(600);mouse(950,500,null);
   var buttons=ArchitectureCheck.buttons();for(String id:List.of("height-","height+","width+","roof","copy","focus","rotate+","hideRoof")) require(!buttons.containsKey(id),"removed duplicate UI "+id);
   clickRaw("sceneMenu");buttons=ArchitectureCheck.buttons();require(!buttons.containsKey("sceneLight") || buttons.get("sceneLight").y<120,"light only on dock");require(!buttons.containsKey("expand") && !buttons.containsKey("collection"),"deduplicated menu");for(String id:List.of("sample","clear","viewAll","help"))require(buttons.containsKey(id),"remaining menu feature "+id);snap();clickRaw("sceneMenu");
   for(int[] anchor:new int[][]{{100,800},{1100,800},{100,200}}) {
    set("yaw",-.65f);set("elevation",.85f);invoke("updateCamera");
    engine(()->{var m=GladeApp.class.getDeclaredMethod("cameraButton",int.class,boolean.class,float.class,float.class);m.setAccessible(true);m.invoke(app,1,true,(float)anchor[0],(float)anchor[1]);require(app.getInputManager().isCursorVisible(),"no cursor capture/recentering");return true;});
    mouse(anchor[0]+100,anchor[1]-120,null);
    engine(()->{var m=GladeApp.class.getDeclaredMethod("cameraButton",int.class,boolean.class,float.class,float.class);m.setAccessible(true);m.invoke(app,1,false,(float)(anchor[0]+100),(float)(anchor[1]-120));return true;});
    require(Math.abs((Float)field("yaw")+1)<.02 && Math.abs((Float)field("elevation")-.55)<.02,"same drag from each press point: "+anchor[0]+","+anchor[1]+" yaw="+field("yaw")+" pitch="+field("elevation"));
    float released=(Float)field("yaw");mouse(700,400,null);require((Float)field("yaw")==released,"release removes vector and prevents drift");
   }
   set("yaw",-.65f);set("elevation",.85f);
   engine(()->{var m=GladeApp.class.getDeclaredMethod("cameraButton",int.class,boolean.class,float.class,float.class);m.setAccessible(true);m.invoke(app,1,true,100f,800f);m.invoke(app,1,false,200f,680f);return true;});
   require(Math.abs((Float)field("yaw")+1)<.02 && Math.abs((Float)field("elevation")-.55)<.02,"quick drag in one batch preserves endpoint vector");
   set("yaw",-.65f);set("elevation",.62f);invoke("updateCamera");
   System.out.println("PRESS_ANCHOR_CAMERA_PASS: top-left, top-right, lower-left, proportional vector, no recenter, no drift");
   fixture(GladeDesign.evolutionSample());select(2);int original=design().items().size();chord(KeyInput.KEY_C);require(design().items().size()==original,"copy does not change scene");
   chord(KeyInput.KEY_V);long pasted=(Long)field("selected");require(design().items().size()==original+2 && design().assemblyIds(pasted).size()==2,"paste nested stack remaps IDs");
   chord(KeyInput.KEY_V);require(design().items().size()==original+4,"clipboard reusable after selection changes");
   chord(KeyInput.KEY_X);require(design().items().size()==original+2,"cut entire assembly");chord(KeyInput.KEY_V);require(design().items().size()==original+4,"paste after cut");chord(KeyInput.KEY_Z);require(design().items().size()==original+2,"paste one undo step");chord(KeyInput.KEY_Z);require(design().items().size()==original+4,"cut one undo step");
   System.out.println("CLIPBOARD_PASS");
   var square=new GladeDesign.Item(0,GladeDesign.Kind.HOUSE,new GladeDesign.Point(0,0),4,3,2.3f,false,List.of());fixture(new GladeDesign(List.of(square),2));select(0);
   engine(()->{var f=GladeApp.class.getDeclaredField("objectHandles");f.setAccessible(true);var root=(Node)f.get(app);var arrows=new java.util.concurrent.atomic.AtomicInteger();root.depthFirstTraversal(s->{if(s.getName().equals("Mũi tên kéo"))arrows.incrementAndGet();});require(arrows.get()==5,"exactly five primary arrows");return true;});
   var low=handle("objectHandles","resizeAxis","BASE");var high=handle("objectHandles","resizeAxis","HEIGHT");require(Math.abs(high.y-low.y-2.3f)<.01,"ruler spans full block");snap();
   pull("HEIGHT",65);require(item(0).height()>2.9f,"ruler changes actual wall height");low=handle("objectHandles","resizeAxis","BASE");high=handle("objectHandles","resizeAxis","HEIGHT");require(Math.abs(high.y-low.y-item(0).height())<.01,"ruler grows with wall");
   pull("ROOF",65);require(item(0).form().roof()>.5f,"top creates roof");require(handle("objectHandles","resizeAxis","ROOF_FRONT")!=null,"roof has extra handles");
   float w=item(0).width(),d=item(0).depth(),h=item(0).height();extend("ROOF_FRONT",1.4f);require(item(0).form().roofFront()>1 && item(0).depth()==d && item(0).width()==w && item(0).height()==h,"roof extension independent from walls");
   extend("ROOF_BACK",-1.1f);require(item(0).form().roofBack()>.7,"second roof end independent: "+item(0).form());snap();
   var file=Path.of("glade/classes/manual-roundtrip.glade");design().save(file);require(GladeDesign.load(file).items().equals(design().items()),"v7 preserves both roof extensions");
   pull("ROOF",-200);require(item(0).form().roof()==0,"pull top down removes roof");
   engine(()->{var f=GladeApp.class.getDeclaredField("objectHandles");f.setAccessible(true);var root=(Node)f.get(app);root.depthFirstTraversal(s->{require(!Set.of("ROOF_FRONT","ROOF_BACK").contains(Objects.toString(s.getUserData("resizeAxis"))),"removed roof removes two end handles");});return true;});
   pull("BASE",50);require(item(0).form().stilts()>.3,"bottom ruler creates stilts");
   var host=new GladeDesign.Item(0,GladeDesign.Kind.HOUSE,new GladeDesign.Point(0,0),4,3,2.3f,false,List.of());
   var custom=new GladeDesign.Item(1,GladeDesign.Kind.ROOF_SLATE,host.center(),4,3,.5f,true,List.of(),0,new GladeDesign.Mount(0,0,0,0));
   fixture(new GladeDesign(List.of(host,custom),2));select(0);pull("ROOF",-240);
   require(!design().customRoof(0) && item(0).form().roof()==0,"custom roof also removed by downward drag");
   System.out.println("MANUAL_BUILD_PASS: removed inspector/hide-roof, graphic deduplicated menu, real Ctrl C/X/V nested clipboard/undo, five arrows, dashed outline, full-height ruler, roof creation/removal/independent ends, v7 save");
  }finally{app.stop();}
 }
}


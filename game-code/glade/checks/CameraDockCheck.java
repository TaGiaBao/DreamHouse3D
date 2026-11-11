package vn.dreamhouse.glade;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.system.AppSettings;
import java.util.*;
class CameraDockCheck extends DirectEditCheck {
 static void require(boolean yes,String why) {if(!yes)throw new AssertionError(why);}
 static void controller() {
  var d=new CameraDrag();d.viewport(1360,860);
  for(float[] press:new float[][]{{20,830},{1100,830},{200,80},{680,430}}) {
   d.begin(CameraDrag.Mode.ORBIT,press[0],press[1]);d.pointer(press[0]+120,press[1]-600);
   var delta=d.finishBatch();require(delta.x()==120 && delta.y()==-600,"long drag anchored to press anywhere");
   d.pointer(press[0]+120,press[1]-600);require(d.finishBatch().x()==0,"stationary pointer does not drift");
  }
  d.begin(CameraDrag.Mode.ORBIT,400,400);d.pointer(420,700);d.pointer(420,400);var same=d.finishBatch();require(same.x()==20 && same.y()==0,"aggregate before clamp");
  d.begin(CameraDrag.Mode.NONE,0,0);d.pointer(900,200);require(d.finishBatch().x()==0,"release stops rotation");
  d.begin(CameraDrag.Mode.PAN,20,20);d.reset();require(d.mode()==CameraDrag.Mode.NONE,"focus reset releases gesture");
  System.out.println("CAMERA_CONTROLLER_PASS");
 }
 static void call(String name,Class<?>[] types,Object... args)throws Exception {engine(()->{var m=GladeApp.class.getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(app,args);return true;});}
 static void snap()throws Exception {engine(()->{app.getStateManager().getState(ScreenshotAppState.class).takeScreenshot();return true;});Thread.sleep(400);}
 public static void main(String[] args)throws Exception {
  controller();EvolutionCheck.domain();app=new GladeApp();var settings=new AppSettings(true);settings.setResolution(1360,860);settings.setRenderer(AppSettings.LWJGL_OPENGL33);settings.setSamples(4);settings.setGammaCorrection(true);settings.setAudioRenderer(null);settings.setTitle("DreamHouse · Kiểm tra giao diện");app.setSettings(settings);app.setShowSettings(false);app.setPauseOnLostFocus(false);app.start();
  try {engine(()->true);Thread.sleep(650);mouse(1000,500,null);Thread.sleep(400);
   var buttons=ArchitectureCheck.buttons();
   for(String id:List.of("architecture","landscape","windows","palette","tool:ERASE","garden","tool:TREE","tool:WATER","photo","sceneLight")) require(buttons.containsKey(id),"dock icon "+id);
   require(buttons.keySet().stream().noneMatch(k->k.startsWith("house:")),"no permanent duplicated model strip");snap();
   for(String group:List.of("architecture","landscape","windows","palette","garden")) {
    var r=ArchitectureCheck.buttons().get(group);mouse((int)r.getCenterX(),(int)r.getCenterY(),null);Thread.sleep(200);
    require((Boolean)field("houseFlyout"),"hover opens "+group);
    require(ArchitectureCheck.buttons().keySet().stream().filter(k->k.startsWith("house:")).count()==6,"six choices "+group);
    if(group.equals("architecture")) {snap();clickRaw("catalog:wood");require(ArchitectureCheck.buttons().containsKey("house:PAVILION"),"wood retained");clickRaw("catalog:architecture");clickRaw("house:TOWER");require(field("tool").toString().equals("TOWER"),"select tower from popup");}
    mouse(1050,500,null);Thread.sleep(450);require(!(Boolean)field("houseFlyout"),"close flyout "+group);
   }
   float before=(Float)field("yaw");
   call("cameraButton",new Class<?>[]{int.class,boolean.class},1,true);
   engine(()->{var f=GladeApp.class.getDeclaredField("cameraDrag");f.setAccessible(true);var d=(CameraDrag)f.get(app);d.begin(CameraDrag.Mode.ORBIT,0,0);var m=GladeApp.class.getDeclaredMethod("applyCameraDrag",CameraDrag.Delta.class);m.setAccessible(true);
    for(int i=1;i<=50;i++){d.pointer(i*40,0);m.invoke(app,d.finishBatch());}return true;});
   float after=(Float)field("yaw");require(Math.abs(after-before)>.1f && after>=-Math.PI && after<=Math.PI,"bounded continuous >360 orbit");
   app.loseFocus();require(!(Boolean)field("rotating") && !(Boolean)field("panning"),"focus loss stops camera");app.gainFocus();
   require(design().items().equals(GladeDesign.evolutionSample().items()),"UI and camera leave buildings intact");
   System.out.println("CAMERA_DOCK_PASS: compact dock, hover-only six choices, subcategories, close/select, >360 orbit, focus reset, save/domain regression");
  }finally{app.stop();}
 }
}

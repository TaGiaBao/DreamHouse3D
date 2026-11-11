package vn.dreamhouse.glade;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.input.KeyInput;
import com.jme3.system.AppSettings;
import java.util.*;
class FreeCameraAndFlattenCheck extends ProceduralMechanicsCheck {
 static Vector3f vector(String name)throws Exception{return ((Vector3f)field(name)).clone();}
 public static void main(String[] args)throws Exception {
  // This is the exact former crash input: float subtraction produces 0.03999996, below the item minimum.
  ok(2.3f-(2.3f-.04f)<.04f,"original floating-point failure reproduced");
  app=new GladeApp();var settings=new AppSettings(true);settings.setResolution(1360,860);settings.setRenderer(AppSettings.LWJGL_OPENGL33);settings.setAudioRenderer(null);settings.setTitle("DreamHouse · Kiểm thử camera và ép phẳng");app.setSettings(settings);app.setShowSettings(false);app.setPauseOnLostFocus(false);app.start();
  try {engine(()->true);Thread.sleep(350);fixture(new GladeDesign(List.of(block(0,0,0,4,3,2.3f,1.4f)),2));esc();
   var initial=vector("focus");key(KeyInput.KEY_W,true);Thread.sleep(450);key(KeyInput.KEY_W,false);Thread.sleep(350);ok(vector("focus").distance(initial)>1,"W pans focus instead of staying centered");
   initial=vector("focus");key(KeyInput.KEY_RIGHT,true);Thread.sleep(350);key(KeyInput.KEY_RIGHT,false);Thread.sleep(350);ok(vector("focus").distance(initial)>1,"arrow key pans camera");
   Thread.sleep(400);initial=vector("focus");float yaw=(Float)field("yaw");right(750,500,true);mouse(860,530,null);right(860,530,false);ok(Math.abs((Float)field("yaw")-yaw)>.1f && vector("focus").distance(initial)<.001f,"right orbit keeps the new translated center");
   mouse(1000,600,true);mouse(1100,650,null);mouse(1100,650,false);ok(vector("focus").distance(initial)<.001f,"left blank drag cannot move or orbit camera");
   engine(()->{var m=GladeApp.class.getDeclaredMethod("cameraButton",int.class,boolean.class,float.class,float.class);m.setAccessible(true);m.invoke(app,2,true,750f,500f);return true;});mouse(840,550,null);engine(()->{var m=GladeApp.class.getDeclaredMethod("cameraButton",int.class,boolean.class,float.class,float.class);m.setAccessible(true);m.invoke(app,2,false,840f,550f);return true;});ok(vector("focus").distance(initial)>.2f,"middle drag pans camera");
   clickRaw("walk");ok((Boolean)field("walking"),"walking button enters ground view");var walk=vector("walkPosition");ok(Math.abs(app.getCamera().getLocation().y-design().terrain().height(walk.x,walk.z)-1.55f)<.001f,"eye stays above terrain");key(KeyInput.KEY_D,true);Thread.sleep(400);key(KeyInput.KEY_D,false);Thread.sleep(350);ok(vector("walkPosition").distance(walk)>.4f,"WASD walking works");esc();ok(!(Boolean)field("walking"),"escape returns to building");
   key(KeyInput.KEY_W,true);engine(()->{app.loseFocus();return true;});var stopped=vector("focus");Thread.sleep(250);ok(vector("focus").distance(stopped)<.001f,"losing focus releases movement");key(KeyInput.KEY_W,false);engine(()->{app.gainFocus();return true;});
   for(float height:new float[]{2.3f,2.6f,4.4f,.3f}){
    System.out.println("FLATTEN_HEIGHT "+height);
    fixture(new GladeDesign(List.of(block(0,0,0,4,3,height,1.4f)),2));engine(()->{var f=GladeApp.class.getDeclaredField("focus");f.setAccessible(true);((Vector3f)f.get(app)).set(0,.65f,0);return true;});set("yaw",-.65f);set("elevation",.62f);set("distance",24f);call("updateCamera");esc();select(0);pull("BOTTOM",400);
    var flat=item(0);System.out.println("FLAT_RESULT h="+flat.height()+" base="+flat.form().base()+" roof="+flat.form().roof());ok(flat.height()>=.04f && flat.height()<.041f && flat.form().roof()==0,"bottom clamps safely to flat plate at height "+height);ok(Math.abs(flat.height()+flat.form().base()-height)<.0001f,"bottom preserves upper surface");
    ok(tagged("craftShape","Tấm phẳng"),"flat geometry rendered");
    var file=PlayerFiles.directory("checks").resolve("flatten-safe.glade");design().save(file);ok(GladeDesign.load(file).items().equals(design().items()),"flat plate saves and reloads");chord(KeyInput.KEY_Z);ok(item(0).height()==height,"undo restores original block");chord(KeyInput.KEY_Y);ok(item(0).height()>=.04f,"redo remains valid");
   }
   fixture(new GladeDesign(List.of(block(0,0,0,4,3,2.3f,1.4f)),2));esc();select(0);pull("HEIGHT",-400);ok(item(0).height()==.04f,"upper ruler safely collapses block too");pull("HEIGHT",80);ok(item(0).height()>.5f,"flat plate can be expanded again with separated ruler grips");
   System.out.println("FREE_CAMERA_FLATTEN_PASS: WASD/arrows, translated orbit center, middle pan, walking/escape/terrain height, focus loss, no left orbit, former float crash, both flatten grips, save/undo/redo; no screenshots");
  }finally {app.stop();}
 }
}

package vn.dreamhouse.glade;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.system.AppSettings;
import com.jme3.input.KeyInput;
import java.nio.file.*;
import java.util.*;
class BuildMorphCheck extends ProceduralMechanicsCheck {
 static void choose(String id)throws Exception {var icon=ArchitectureCheck.buttons().get("architecture");mouse((int)icon.getCenterX(),(int)icon.getCenterY(),null);clickRaw("architecture");clickRaw("house:"+id);}
 static void camera(float distance)throws Exception {set("distance",distance);engine(()->{var f=GladeApp.class.getDeclaredField("focus");f.setAccessible(true);((Vector3f)f.get(app)).set(0,1,0);return true;});call("updateCamera");}
 public static void main(String[] args)throws Exception {
  app=new GladeApp();var settings=new AppSettings(true);settings.setResolution(1360,860);settings.setRenderer(AppSettings.LWJGL_OPENGL33);settings.setSamples(4);settings.setGammaCorrection(true);settings.setAudioRenderer(null);settings.setTitle("DreamHouse · Kiểm tra khung và mái biến đổi");app.setSettings(settings);app.setShowSettings(false);app.setPauseOnLostFocus(false);app.start();
  try {engine(()->true);Thread.sleep(500);fixture(new GladeDesign(List.of(),2));camera(24);choose("HOUSE");var p=screen(new Vector3f(0,0,0));mouse((int)p.x,(int)p.y,null);Thread.sleep(180);
   ok(design().items().isEmpty(),"ghost does not place object early");ok(engine(()->{var f=GladeApp.class.getDeclaredField("buildGhost");f.setAccessible(true);var ghost=(Node)f.get(app);return ghost.getCullHint()!=Spatial.CullHint.Always && ghost.getQuantity()==2;}),"wireframe cage and dashed footprint before placement");shot();
   click(new Vector3f(0,0,0));ok(item(0).form().roof()>.5f && item(0).form().finish().roofMaterial().equals("tile"),"placement creates finished house with tile roof");ok(field("tool").toString().equals("SELECT") && Objects.equals(field("selected"),0L),"placement enters editing directly");ok(tagged("hipRoof","true"),"four-sided roof generated");
   ok(engine(()->{var f=GladeApp.class.getDeclaredField("content");f.setAccessible(true);var content=(Node)f.get(app);int[] roofs={0};content.depthFirstTraversal(n->{if(n instanceof Geometry && Boolean.TRUE.equals(n.getUserData("roofSurface")))roofs[0]++;});return roofs[0]>0 && roofs[0]<8;}),"roof tiles batched efficiently");shot();
   float width=item(0).width(),height=item(0).height();pull("ROOF",-160);ok(item(0).form().roof()==0 && tagged("battlements","true") && !tagged("hipRoof","true"),"lowering roof creates crenellated wall");ok(item(0).width()==width && item(0).height()==height,"morph keeps wall dimensions");shot();
   var file=PlayerFiles.directory("checks").resolve("crenellated-v9.glade");design().save(file);fixture(GladeDesign.load(file));select(0);ok(tagged("battlements","true"),"morph restored from saved design");pull("ROOF",80);ok(tagged("hipRoof","true") && !tagged("battlements","true"),"raise roof reverses morph");
   fixture(new GladeDesign(List.of(),2));camera(44);choose("TOWER");click(new Vector3f(0,0,0));ok(item(0).kind()==GladeDesign.Kind.TOWER && item(0).form().roof()>0,"round structure starts with cone roof");pull("ROOF",-200);ok(tagged("battlements","true"),"round tower also gains crenellations");
   fixture(new GladeDesign(List.of(),2));clickRaw("trees");clickRaw("libraryPick:0");var a=screen(new Vector3f(-6,0,-3));var b=screen(new Vector3f(6,0,-3));mouse((int)a.x,(int)a.y,true);mouse((int)b.x,(int)b.y,null);mouse((int)b.x,(int)b.y,false);int forest=design().items().size();ok(forest>=5,"held brush paints multiple trees");chord(KeyInput.KEY_Z);ok(design().items().isEmpty(),"nature stroke one undo step");chord(KeyInput.KEY_Y);ok(design().items().size()==forest,"nature stroke redo");
   var middle=design().items().get(forest/2);clickRaw("tool:ERASE");click(new Vector3f(middle.center().x(),.5f,middle.center().z()));ok(design().items().size()==forest-1,"erase opens a clearing in planted forest");chord(KeyInput.KEY_Z);ok(design().items().size()==forest,"clearing undo");
   fixture(new GladeDesign(List.of(block(0,0,0,4,3,2.6f,1.4f)),2));clickRaw("garden");clickRaw("libraryPick:0");a=screen(new Vector3f(-2.7f,0,-.8f));b=screen(new Vector3f(-2.7f,0,.8f));mouse((int)a.x,(int)a.y,true);mouse((int)b.x,(int)b.y,null);mouse((int)b.x,(int)b.y,false);ok(design().items().stream().filter(i->i.kind()==GladeDesign.Kind.FLOWERS).count()>=2 && tagged("ivy","true"),"painted flora produces wall ivy");
   System.out.println("BUILD_MORPH_PASS: wireframe before placement, finished hip-roof house, reversible crenellation, round tower morph, efficient roof batches, save restoration, nature painting/undo/clearing/ivy");
  }finally{app.stop();}
 }
}

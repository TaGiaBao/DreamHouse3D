package vn.dreamhouse.glade;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.system.AppSettings;
import com.jme3.scene.*;
import com.jme3.app.state.ScreenshotAppState;
import java.nio.file.*;
import java.util.*;
class PrecisionAndCraftCheck extends InteractionExpansionCheck {
 static void axisMath(){
  for(float yaw:new float[]{-.7f,.8f,2.2f})for(float elevation:new float[]{.25f,.7f,1.25f})for(float distance:new float[]{20,65}) {
   var camera=new Camera(1360,860);camera.setFrustumPerspective(38,1360f/860,.1f,1000);
   camera.setLocation(new Vector3f(distance*FastMath.cos(elevation)*FastMath.sin(yaw),distance*FastMath.sin(elevation),distance*FastMath.cos(elevation)*FastMath.cos(yaw)));camera.lookAt(new Vector3f(0,1,0),Vector3f.UNIT_Y);
   var origin=new Vector3f(2,2,1);
   for(var axis:List.of(Vector3f.UNIT_Y,Vector3f.UNIT_X,Vector3f.UNIT_Z)){
    var a=camera.getScreenCoordinates(origin);var b=camera.getScreenCoordinates(origin.add(axis.mult(1.35f)));
    var drag=new AxisDrag(camera,origin,axis,new Vector2f(a.x,a.y));float delta=drag.delta(new Vector2f(b.x,b.y));
    ok(Float.isFinite(delta) && Math.abs(delta-1.35f)<.006f,"ray/axis matches projected world distance "+delta+" yaw="+yaw+" elevation="+elevation);
   }
  }
  ok(Float.isNaN(AxisDrag.parameter(new Ray(new Vector3f(0,5,0),Vector3f.UNIT_Y),new Vector3f(),Vector3f.UNIT_Y)),"parallel axis safe fallback");
  System.out.println("AXIS_PROJECTION_PASS: 18 camera poses, 54 axis drags");
 }
 static void dragAxis(String axis,Vector3f direction,float meters)throws Exception {
  var world=handle("objectHandles","resizeAxis",axis);var from=screen(world);var to=screen(world.add(direction.mult(meters)));
  mouse((int)from.x,(int)from.y,true);ok(Objects.toString(field("resizeAxis")).equals(axis),"grab "+axis+" got "+field("resizeAxis"));
  mouse((int)to.x,(int)to.y,null);mouse((int)to.x,(int)to.y,false);
 }
 static String craftShape()throws Exception{return engine(()->{var f=GladeApp.class.getDeclaredField("content");f.setAccessible(true);var root=(Node)f.get(app);String[] shape={null};root.depthFirstTraversal(n->{String s=n.getUserData("craftShape");if(s!=null)shape[0]=s;});return shape[0];});}
 public static void main(String[] args)throws Exception {
  axisMath();app=new GladeApp();var settings=new AppSettings(true);settings.setResolution(1360,860);settings.setRenderer(AppSettings.LWJGL_OPENGL33);settings.setSamples(4);settings.setGammaCorrection(true);settings.setAudioRenderer(null);settings.setTitle("DreamHouse · Kiểm tra kéo chính xác");app.setSettings(settings);app.setShowSettings(false);app.setPauseOnLostFocus(false);app.start();
  try {engine(()->true);Thread.sleep(600);fixtureHouse();select(0);
   float h=item(0).height();dragAxis("HEIGHT",Vector3f.UNIT_Y,1.2f);ok(Math.abs(item(0).height()-h-1.2f)<.06f,"height follows pointer in world meters");
   float top=design().top(item(0));dragAxis("BOTTOM",Vector3f.UNIT_Y,.55f);ok(Math.abs(design().top(item(0))-top)<.01 && Math.abs(item(0).form().base()-.55f)<.06,"lower bound preserves upper bound");
   float base=item(0).form().base();dragAxis("ELEVATION",Vector3f.UNIT_Y,.65f);ok(Math.abs(item(0).form().base()-base-.65f)<.06,"center lift follows pointer");
   float w=item(0).width();dragAxis("WIDTH",Vector3f.UNIT_X,.7f);ok(Math.abs(item(0).width()-w-1.4f)<.07,"symmetric width follows actual handle");
   float bodyWidth=item(0).width();dragAxis("ROOF_RIGHT",Vector3f.UNIT_X,.8f);ok(Math.abs(item(0).form().roofRight()-.8f)<.07 && item(0).width()==bodyWidth,"roof edge independent and precise");
   var target=item(0);var rotated=new GladeDesign.Item(0,target.kind(),target.center(),target.width(),target.depth(),target.height(),target.pitched(),target.points(),37,target.mount(),target.form());fixture(new GladeDesign(List.of(rotated),2));select(0);
   var direction=new Quaternion().fromAngleAxis(37*FastMath.DEG_TO_RAD,Vector3f.UNIT_Y).mult(Vector3f.UNIT_X);w=item(0).width();dragAxis("WIDTH",direction,.5f);ok(Math.abs(item(0).width()-w-1)<.08,"rotated local axis correct");shot();
   System.out.println("PRECISE_GIZMO_PASS");
   for(int variant:new int[]{7,8,9}) {
    var form=new GladeDesign.Form(0,0,0,-1).finish(new GladeDesign.Finish("wood","","original","",variant));
    var craft=new GladeDesign.Item(0,GladeDesign.Kind.HOUSE,new GladeDesign.Point(0,0),3,2,variant==9?2.2f:.85f,false,List.of(),0,null,form);fixture(new GladeDesign(List.of(craft),2));select(0);
    String expected=variant==7?"Ghế gỗ":variant==8?"Bàn gỗ":"Giàn gỗ";ok(expected.equals(craftShape()),"craft geometry "+expected+" got "+craftShape());
    clickRaw("palette");clickRaw("paintMaterial:stone");ok("Khối đặc".equals(craftShape()),"changing material rebuilds solid craft");clickRaw("paintMaterial:wood");ok(expected.equals(craftShape()),"wood restores procedural prop");esc();select(0);
    dragAxis("HEIGHT",Vector3f.UNIT_Y,-4);ok(item(0).height()<=.12 && "Tấm phẳng".equals(craftShape()),"all templates flatten");
    dragAxis("HEIGHT",Vector3f.UNIT_Y,variant==9?2:1);ok(!"Tấm phẳng".equals(craftShape()),"template grows after flattening");
   }
   var props=new ArrayList<GladeDesign.Item>();for(int i=0;i<3;i++)props.add(new GladeDesign.Item(i,GladeDesign.Kind.HOUSE,new GladeDesign.Point(-5+i*5,0),3,2,i==2?2.2f:.85f,false,List.of(),0,null,new GladeDesign.Form(0,0,0,-1).finish(new GladeDesign.Finish("wood","","original","",7+i))));fixture(new GladeDesign(props,2));shot();
   for(var kind:List.of(GladeDesign.Kind.BENCH,GladeDesign.Kind.PERGOLA,GladeDesign.Kind.PLANTER,GladeDesign.Kind.DECK)){
    var object=new GladeDesign.Item(0,kind,new GladeDesign.Point(0,0),3,2,1,false,List.of());fixture(new GladeDesign(List.of(object),2));select(0);clickRaw("palette");clickRaw("paintMaterial:wood");clickRaw("paintColor:#86aaa0");
    ok(item(0).form().finish().color().equals("#86aaa0"),"prop accepts finish "+kind);ok(!ArchitectureCheck.buttons().containsKey("paintTarget:roof"),"no roof controls on roofless props");
    engine(()->{var f=GladeApp.class.getDeclaredField("content");f.setAccessible(true);var root=(Node)f.get(app);var count=new int[1];root.depthFirstTraversal(n->{if("#86aaa0".equals(n.getUserData("paintColor")))count[0]++;if(kind==GladeDesign.Kind.PLANTER && n.getName().equals("Tán"))ok(n.getUserData("paintColor")==null,"planter flowers retain color");});ok(count[0]>0,"prop material rendered "+kind);return true;});esc();
   }
   var points=new ArrayList<GladeDesign.Point>();for(int i=0;i<900;i++)points.add(new GladeDesign.Point(-4+i*.009f,FastMath.sin(i*.02f)));
   var water=new GladeDesign.Item(0,GladeDesign.Kind.WATER,new GladeDesign.Point(0,0),12,5,.65f,false,points);var file=Path.of("glade/classes/long-water.glade");new GladeDesign(List.of(water),2).save(file);ok(GladeDesign.load(file).items().getFirst().points().size()==900,"long stroke survives save/load");
   clickRaw("architecture");clickRaw("catalog:wood");shot();
   System.out.println("PRECISION_CRAFT_PASS: pointer-calibrated axes, rotated roofs, reversible craft families, prop materials, plant preservation, long strokes");
  } finally {app.stop();}
 }
}


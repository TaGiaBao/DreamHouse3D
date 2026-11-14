package vn.dreamhouse.glade;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.collision.CollisionResults;
import com.jme3.system.AppSettings;
import java.nio.file.*;
import java.util.*;
class ProceduralMechanicsCheck extends InteractionExpansionCheck {
 static GladeDesign.Item block(long id,float x,float z,float w,float d,float h,float roof){return new GladeDesign.Item(id,GladeDesign.Kind.HOUSE,new GladeDesign.Point(x,z),w,d,h,roof>0,List.of(),0,null,new GladeDesign.Form(0,roof,0,-1).finish(GladeCatalog.plain()));}
 static GladeDesign.Item source(long id,float x,float z){return new GladeDesign.Item(id,GladeDesign.Kind.WATER,new GladeDesign.Point(x,z),.5f,.5f,1,false,List.of(new GladeDesign.Point(x,z)),0,null,new GladeDesign.Form(0,0,0,-1).finish(GladeDesign.Finish.DEFAULT.variant(20)));}
 static GladeDesign.Item path(long id,float x1,float z1,float x2,float z2){var points=new ArrayList<GladeDesign.Point>();for(int i=0;i<=80;i++)points.add(new GladeDesign.Point(x1+(x2-x1)*i/80,z1+(z2-z1)*i/80));return new GladeDesign.Item(id,GladeDesign.Kind.PATH,points.getFirst(),1,1,1,false,points);}
 static boolean tagged(String tag,String value)throws Exception{return engine(()->{var f=GladeApp.class.getDeclaredField("content");f.setAccessible(true);var content=(Node)f.get(app);boolean[] found={false};content.depthFirstTraversal(n->{if(value.equals(Objects.toString(n.getUserData(tag))))found[0]=true;});return found[0];});}
 static void right(int x,int y,boolean pressed)throws Exception{engine(()->{var m=GladeApp.class.getDeclaredMethod("cameraButton",int.class,boolean.class,float.class,float.class);m.setAccessible(true);m.invoke(app,1,pressed,(float)x,(float)y);return true;});Thread.sleep(150);}
 static void scroll(int delta)throws Exception {engine(()->{var input=app.getInputManager();var gate=com.jme3.input.InputManager.class.getDeclaredField("eventsPermitted");gate.setAccessible(true);gate.setBoolean(input,true);try{input.onMouseMotionEvent(new com.jme3.input.event.MouseMotionEvent(800,440,0,0,delta,delta));}finally{gate.setBoolean(input,false);}return true;});Thread.sleep(150);}
 static void modelChecks()throws Exception {
  var forms=GladeCatalog.entries("architecture");
  ok(forms.stream().anyMatch(e->e.tool().equals("PENTAGON")) && forms.stream().allMatch(e->GladeCatalog.finish(e).variant()==18),"library offers neutral forms including a pentagon");
  var flat=new GladeDesign(List.of(source(0,0,0)),2);var water=WaterFlow.simulate(flat,flat.items().getFirst());ok(water.cells().size()>25 && water.cells().size()<200,"finite Minecraft-style spread");ok(water.at(.2f,.2f)!=null && water.at(10,10)==null,"flow from source, bounded lateral reach");
  var ridge=new TerrainField().brush(1.5f,0,1.5f,4,null);var scene=new GladeDesign(flat.items(),flat.land(),ridge);ok(WaterFlow.simulate(scene,scene.items().getFirst()).at(1.5f,0)==null,"water cannot climb high terrain");
  var hill=new TerrainField().brush(0,0,1.5f,2.8f,null);scene=new GladeDesign(flat.items(),flat.land(),hill);var downhill=WaterFlow.simulate(scene,scene.items().getFirst());ok(downhill.cells().values().stream().anyMatch(w->w.level()<1),"source runs down cliff and changes water level");
  var host=block(1,2.6f,0,2,2,2,0);scene=new GladeDesign(List.of(source(0,0,0),host),2);ok(WaterFlow.simulate(scene,scene.items().getFirst()).at(2.6f,0)==null,"water flows around solid building");
  var file=PlayerFiles.directory("checks").resolve("source-water-v9.glade");scene.save(file);ok(GladeDesign.load(file).items().equals(scene.items()),"source persists in v9");
  System.out.println("SOURCE_WATER_MODEL_PASS");
 }
 static void polygonChecks()throws Exception{
   fixture(new GladeDesign(List.of(),2));esc();BuildMorphCheck.choose("PENTAGON");
   click(new Vector3f(0,0,0));
   var pentagon=design().items().getFirst();ok(pentagon.kind()==GladeDesign.Kind.PENTAGON,"construction tray places a pentagon");
   ok(!SceneRules.contains(pentagon,pentagon.center().x()+pentagon.width()*.45f,
           pentagon.center().z()-pentagon.depth()*.45f,0),"rear clipped corner stays outside the footprint");
   var roofs=List.of(GladeDesign.Kind.ROOF_FLAT,GladeDesign.Kind.ROOF_PYRAMID,GladeDesign.Kind.ROOF_PENTAGON);
   for(int index=0;index<roofs.size();index++){
    var dock=ArchitectureCheck.buttons().get("architecture");
    mouse((int)dock.getCenterX(),(int)dock.getCenterY(),null);
    clickRaw("architecture");clickRaw("roofs");clickRaw("libraryPick:"+index);
    click(new Vector3f(pentagon.center().x(),1.4f,pentagon.center().z()+pentagon.depth()/2));
    ok(design().items().size()==2 && design().attachedRoof(pentagon.id()).kind()==roofs.get(index),"roof replaces its predecessor on the same host");
   }
   chord(com.jme3.input.KeyInput.KEY_Z);
   ok(design().attachedRoof(pentagon.id()).kind()==GladeDesign.Kind.ROOF_PYRAMID,"undo restores the previous roof");
   chord(com.jme3.input.KeyInput.KEY_Y);
   var polygonFile=PlayerFiles.directory("checks").resolve("polygon-roof.glade");design().save(polygonFile);
   ok(GladeDesign.load(polygonFile).items().equals(design().items()),"pentagon and attached roof survive reopening");
 }
 static void terrainButtonsCheck()throws Exception{
   fixture(new GladeDesign(List.of(),2));esc();clickRaw("terrain");
   var p=screen(new Vector3f(0,0,0));
   mouse((int)p.x,(int)p.y,true);mouse((int)p.x,(int)p.y,false);
   float raised=design().terrain().height(0,0);
   ok(raised>.05f,"left tap raises terrain without depending on frame rate");
   float yaw=(Float)field("yaw");
   p=screen(new Vector3f(0,raised,0));int x=Math.round(p.x),y=Math.round(p.y);
   engine(()->{
    var button=GladeApp.class.getDeclaredMethod("cameraButton",int.class,boolean.class,float.class,float.class);
    button.setAccessible(true);
    button.invoke(app,1,true,(float)x,(float)y);
    button.invoke(app,1,false,(float)x,(float)y);
    return true;
   });
   ok(design().terrain().height(0,0)<raised-.035f && (Float)field("yaw")==yaw,"right tap lowers terrain without rotating");
   chord(com.jme3.input.KeyInput.KEY_Z);
   ok(Math.abs(design().terrain().height(0,0)-raised)<.001f,"one undo restores the terrain before right tap");
   clickRaw("terrain");
 }
 public static void main(String[] args)throws Exception {
  boolean polygonOnly=Arrays.asList(args).contains("--polygon-only");
  modelChecks();app=new GladeApp();var settings=new AppSettings(true);settings.setResolution(polygonOnly?1000:1360,polygonOnly?680:860);settings.setRenderer(AppSettings.LWJGL_OPENGL33);settings.setSamples(polygonOnly?0:4);settings.setGammaCorrection(true);settings.setAudioRenderer(null);settings.setTitle("DreamHouse · Kiểm tra cơ chế phản ứng");app.setSettings(settings);app.setShowSettings(false);app.setPauseOnLostFocus(false);app.start();
  try {app.enqueue(()->true).get(90,java.util.concurrent.TimeUnit.SECONDS);Thread.sleep(500);if(polygonOnly){set("qualityLow",true);call("applyQuality");terrainButtonsCheck();polygonChecks();System.out.println("POLYGON_UI_PASS: terrain taps, roof placement, replacement, undo and save");return;}fixture(new GladeDesign(List.of(),2));float distance=(Float)field("distance");scroll(120);ok(Math.abs((Float)field("distance")-distance)<distance*.05f,"wheel notch changes zoom under five percent");set("distance",44f);call("updateCamera");
   var build=ArchitectureCheck.buttons().get("architecture");mouse((int)build.getCenterX(),(int)build.getCenterY(),null);clickRaw("architecture");ok(!(Boolean)field("libraryOpen") && ArchitectureCheck.buttons().keySet().stream().filter(k->k.startsWith("house:")).count()==6,"compact six-form tray, no prefab/material tabs");ok(!ArchitectureCheck.buttons().containsKey("catalog:wood"),"no material submenus");clickRaw("house:HOUSE");click(new Vector3f(0,0,0));ok(item(0).form().finish().variant()==18 && tagged("plainStructure","true"),"new house is bare shell");ok(!tagged("craftShape","Ghế gỗ"),"bare neutral initial material");esc();shot();
   fixture(new GladeDesign(List.of(),2));clickRaw("terrain");ok(field("tool").toString().equals("TERRAIN") && !(Boolean)field("libraryOpen"),"terrain enters directly");var edge=handle("landHandles","landEdge","LEFT");ok(edge!=null,"edge handles immediately visible");
   terrainButtonsCheck();
   var a=screen(handle("landHandles","landEdge","LEFT"));var b=screen(new Vector3f(-20,0,0));mouse((int)a.x,(int)a.y,true);mouse((int)b.x,(int)b.y,null);mouse((int)b.x,(int)b.y,false);ok(design().land().minX()<-19,"terrain mode also extends boundary");shot();esc();
   fixture(new GladeDesign(List.of(),2));set("distance",44f);call("updateCamera");clickRaw("water");ok(!(Boolean)field("libraryOpen"),"water is direct pouring mode, no lake library");click(new Vector3f(0,0,0));ok(WaterFlow.source(item(0)) && item(0).points().size()==1,"one click places source");Thread.sleep(1700);ok(tagged("waterSource","true"),"rendered source flow");shot();chord(com.jme3.input.KeyInput.KEY_Z);ok(design().items().isEmpty(),"undo removes source and flow");
   System.out.println("DIRECT_MODES_PASS");
   var house=block(0,0,0,4,3,2.6f,1.4f);fixture(new GladeDesign(List.of(house),2));esc();clickRaw("windows");clickRaw("libraryPick:0");click(new Vector3f(0,.3f,1.5f));ok(design().items().getLast().kind()==GladeDesign.Kind.DOOR_ARCH,"low window becomes door");
   click(new Vector3f(-.8f,3.4717f,.6f));ok(design().items().getLast().mount().lift()>=1 && tagged("proceduralFeature","dormer"),"roof window becomes dormer");shot();
   var first=new GladeDesign.Item(1,GladeDesign.Kind.WINDOW_ARCH,house.center(),.9f,.5f,1.08f,true,List.of(),0,new GladeDesign.Mount(0,-.1f,.5f,.55f));var second=new GladeDesign.Item(2,GladeDesign.Kind.WINDOW_ARCH,house.center(),.9f,.5f,1.08f,true,List.of(),0,new GladeDesign.Mount(0,.15f,.5f,.55f));fixture(new GladeDesign(List.of(house,first,second),2));ok(tagged("proceduralFeature","joined-balcony"),"neighbor windows join balcony");
   var flower=new GladeDesign.Item(3,GladeDesign.Kind.FLOWERS,new GladeDesign.Point(2.3f,0),1,1,.4f,false,List.of());fixture(new GladeDesign(List.of(house,flower),2));ok(tagged("ivy","true"),"flora near wall grows ivy");
   var hill=new TerrainField().brush(-1.7f,-1.2f,2,2.8f,null);fixture(new GladeDesign(List.of(house),LandBounds.level(2),hill));ok(tagged("autoSupports","true"),"cliff creates supports");
   fixture(new GladeDesign(List.of(block(1,2.6f,0,2,2,2,0),source(0,0,0)),2));ok(tagged("waterFoundation","true"),"water creates stone bank and moss");
   fixture(new GladeDesign(List.of(source(0,0,0),path(1,-5,0,5,0)),2));ok(tagged("proceduralFeature","bridge"),"path across water creates bridge");
   fixture(new GladeDesign(List.of(path(0,-5,0,5,0)),LandBounds.level(2),new TerrainField().brush(0,0,2.4f,2,null)));ok(tagged("proceduralFeature","stairs"),"steep path creates stairs");
   var points=List.of(new GladeDesign.Point(8,4),new GladeDesign.Point(12,4),new GladeDesign.Point(12,8),new GladeDesign.Point(8,8),new GladeDesign.Point(8,4));var fence=new GladeDesign.Item(0,GladeDesign.Kind.FENCE,points.getFirst(),1,1,1,false,points);var enclosed=new GladeDesign(List.of(fence,path(1,10,1,10,9)),2);
   var rules=SceneRules.of(enclosed);
   ok(rules.enclosed(10,6) && !rules.enclosed(0,0),"closed fence defines short-grass garden");fixture(enclosed);ok(tagged("proceduralFeature","gate"),"path opens fence gate");
   ok(engine(()->{var f=GladeApp.class.getDeclaredField("art");f.setAccessible(true);var art=(GladeArt)f.get(app);int[] counts=new int[2];var meadows=List.of(art.meadow(new GladeDesign(List.of(),2)),art.meadow(enclosed));for(int i=0;i<2;i++){final int k=i;meadows.get(i).depthFirstTraversal(n->{if(n instanceof Geometry g && n.getName().equals("Cỏ") && n.getCullHint()!=Spatial.CullHint.Always)counts[k]+=g.getMesh().getTriangleCount();});}return counts[1]<counts[0];}),"closed fence actually cuts grass geometry");
   var joined=new GladeDesign(List.of(block(0,0,0,4,3,2.6f,1.4f),block(1,3,0,4,3,2.6f,1.4f),block(2,0,2.5f,2,3,2.6f,1.4f)),2);fixture(joined);ok(new BlockFusion(joined).joined(0),"T-shaped roof merge");ok(engine(()->{var f=GladeApp.class.getDeclaredField("content");f.setAccessible(true);var root=(Node)f.get(app);var ray=new Ray(new Vector3f(0,1,0),Vector3f.UNIT_X);ray.setLimit(3.8f);var hits=new CollisionResults();root.collideWith(ray,hits);for(var hit:hits)if(hit.getDistance()<3.8f){System.out.println("INTERIOR_HIT "+hit.getGeometry().getName()+" "+hit.getDistance()+" "+hit.getContactPoint());return false;}return true;}),"interior shared walls removed in overlapping blocks");
   ok(SceneRules.snapFence(new GladeDesign(List.of(house),2),new GladeDesign.Point(2.3f,0)).distance(new GladeDesign.Point(2,0))<.01f,"fence snaps to building wall");
   fixture(new GladeDesign(List.of(house,path(1,0,-5,0,5)),2));ok(engine(()->{var f=GladeApp.class.getDeclaredField("content");f.setAccessible(true);var root=(Node)f.get(app);var hits=new CollisionResults();root.getChild(0).collideWith(new Ray(new Vector3f(0,1,5),new Vector3f(0,0,-1)),hits);return hits.size()==0;}),"path really cuts a passage through plain shell");shot();
   polygonChecks();
   System.out.println("PROCEDURAL_MECHANICS_PASS: terrain, water, passages, six neutral forms, pentagon placement, roof selection/replacement, undo and save");
  }finally{app.stop();}
 }
}

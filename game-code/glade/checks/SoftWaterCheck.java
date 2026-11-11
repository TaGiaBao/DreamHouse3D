package vn.dreamhouse.glade;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.system.AppSettings;
import java.nio.*;
import java.util.*;
class SoftWaterCheck extends ProceduralMechanicsCheck {
 public static void main(String[] args)throws Exception {
  modelChecks();app=new GladeApp();var settings=new AppSettings(true);settings.setResolution(1100,720);settings.setRenderer(AppSettings.LWJGL_OPENGL33);settings.setAudioRenderer(null);settings.setTitle("DreamHouse · Kiểm thử nước mềm");app.setSettings(settings);app.setShowSettings(false);app.setPauseOnLostFocus(false);app.start();
  try {engine(()->true);Thread.sleep(350);fixture(new GladeDesign(List.of(source(0,0,0)),2));Thread.sleep(1800);
   ok(tagged("softWater","true"),"continuous soft water generated");
   engine(()->{var f=GladeApp.class.getDeclaredField("content");f.setAccessible(true);var content=(Node)f.get(app);int[] totals={0,0};boolean[] offGrid={false},gradient={false};
    content.depthFirstTraversal(part->{if(part instanceof Geometry geo && Boolean.TRUE.equals(geo.getUserData("softSurface"))){
     totals[0]++;var mesh=geo.getMesh();var pos=(FloatBuffer)mesh.getBuffer(VertexBuffer.Type.Position).getData();var color=(FloatBuffer)mesh.getBuffer(VertexBuffer.Type.Color).getData();var normal=(FloatBuffer)mesh.getBuffer(VertexBuffer.Type.Normal).getData();
     for(int i=0;i<pos.limit();i+=3){ok(Float.isFinite(pos.get(i)) && Float.isFinite(pos.get(i+1)) && Float.isFinite(pos.get(i+2)),"finite water vertices");if(Math.abs(pos.get(i)/WaterFlow.STEP-Math.round(pos.get(i)/WaterFlow.STEP))>.05f)offGrid[0]=true;ok(normal.get(i+1)>.98f,"continuous surface without vertical cell walls");}
     for(int i=3;i<color.limit();i+=4)if(color.get(i)<.8f)gradient[0]=true;
     float before=pos.get(1);SoftWater.animate(geo,50);float after=pos.get(1);SoftWater.animate(geo,51);ok(Math.abs(pos.get(1)-after)<.04f,"gentle bounded wave amplitude");if(Math.abs(before-after)>.00001f)totals[1]++;
    }});
    ok(totals[0]>0 && totals[1]>0 && offGrid[0] && gradient[0],"smooth contour, transparent shore and animated surface");return true;
   });
   var hill=new TerrainField().brush(0,0,1.5f,2.8f,null);fixture(new GladeDesign(List.of(source(0,0,0)),LandBounds.level(2),hill));Thread.sleep(300);ok(tagged("softWater","true"),"soft surface also follows downhill flow");
   fixture(new GladeDesign(List.of(),2));clickRaw("water");click(new Vector3f(0,0,0));ok(WaterFlow.source(item(0)),"water tool still places sources");chord(com.jme3.input.KeyInput.KEY_Z);ok(design().items().isEmpty(),"undo removes water and soft surface");
   System.out.println("SOFT_WATER_PASS: bounded flow, terrain obstacles, continuous mesh, curved off-grid shore, transparency, gentle waves, source placement and undo; no screenshots");
  }finally {app.stop();}
 }
}

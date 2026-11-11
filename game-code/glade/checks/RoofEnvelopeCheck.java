package vn.dreamhouse.glade;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.scene.*;
import com.jme3.math.*;
import com.jme3.collision.CollisionResults;
import java.util.*;
class RoofEnvelopeCheck {
 static void ok(boolean yes,String why){if(!yes)throw new AssertionError(why);}
 static GladeDesign.Item house(long id,float x,float z,float w,float d,float angle){return new GladeDesign.Item(id,GladeDesign.Kind.HOUSE,new GladeDesign.Point(x,z),w,d,2.6f,true,List.of(),angle,null,new GladeDesign.Form(0,1.4f,0,-1).finish(GladeCatalog.plain()));}
 public static void main(String[]args){
  var art=new GladeArt(new DesktopAssetManager(true));var sample=GladeDesign.mechanicsSample();
  var scenes=List.of(sample,new GladeDesign(List.of(house(0,0,0,4,3,0),house(1,3,0,4,3,0),house(2,0,2.5f,2,3,0)),2),new GladeDesign(List.of(house(0,0,0,4,3,0),house(1,0,2.5f,3,3,0)),2));
  for(var scene:scenes){var fusion=new BlockFusion(scene);var all=new Node();var edges=new HashMap<RoofEnvelopeProbe.Edge,Integer>();var endpoints=new HashMap<RoofEnvelopeProbe.Edge,Vector3f[]>();int[] gableCount={0};
   for(var item:scene.items())if(fusion.joined(item.id())){
    var node=art.render(item,scene);all.attachChild(node);all.updateGeometricState();
    node.depthFirstTraversal(n->{if(!(n instanceof Geometry g) || (!Boolean.TRUE.equals(g.getUserData("roofSurface")) && !Boolean.TRUE.equals(g.getUserData("gableClosure"))))return;
     boolean gable=Boolean.TRUE.equals(g.getUserData("gableClosure"));if(gable){gableCount[0]++;ok(!Boolean.TRUE.equals(g.getUserData("roofSurface")),"gable must keep wall material");ok(Objects.equals(g.getUserData("paintMaterial"),"plaster"),"gable uses plaster rather than roof tiles");}
     var mesh=g.getMesh();var index=mesh.getIndexBuffer();var pos=(java.nio.FloatBuffer)mesh.getBuffer(VertexBuffer.Type.Position).getData();
     for(int i=0;i<index.size();i+=3){var vertices=new Vector3f[3];for(int k=0;k<3;k++){int id=index.get(i+k);vertices[k]=g.localToWorld(new Vector3f(pos.get(id*3),pos.get(id*3+1),pos.get(id*3+2)),null);}
      for(int k=0;k<3;k++){var e=RoofEnvelopeProbe.Edge.of(vertices[k],vertices[(k+1)%3]);edges.merge(e,1,Integer::sum);endpoints.put(e,new Vector3f[]{vertices[k],vertices[(k+1)%3]});}
      if(gable){var center=vertices[0].add(vertices[1]).addLocal(vertices[2]).divideLocal(3);var normal=vertices[1].subtract(vertices[0]).cross(vertices[2].subtract(vertices[0]));if(normal.lengthSquared()<1e-10)continue;
       float minZ=scene.items().stream().filter(it->fusion.joined(it.id())).map(it->it.center().z()-it.halfDepth()).min(Float::compare).orElseThrow(),maxZ=scene.items().stream().filter(it->fusion.joined(it.id())).map(it->it.center().z()+it.halfDepth()).max(Float::compare).orElseThrow();
       if(Math.abs(normal.z)>normal.length()*.8f && center.z>maxZ+.05f)ok(normal.z>0,"front gable faces outside, not into the house");if(Math.abs(normal.z)>normal.length()*.8f && center.z<minZ-.05f)ok(normal.z<0,"back gable faces outside");
      }
     }
    });
   }
   ok(gableCount[0]>=2,"joined roofs include actual closed gable walls");
   for(var e:edges.entrySet())if(e.getValue()==1){var p=endpoints.get(e.getKey());ok(!(p[0].y>2.61f && p[1].y>2.61f),"no open roof/gable seam above the wall top: "+e.getKey());}
  }
  var separated=new GladeDesign(List.of(house(0,0,0,4,1,45),house(1,2.8f,2.8f,4,1,45)),2);ok(!new BlockFusion(separated).joined(0),"overlapping AABBs cannot fuse separated rotated houses");
  var a=house(0,0,0,4,3,0);var b=house(1,3,0,4,3,0);var scene=new GladeDesign(List.of(a,b),2);var fusion=new BlockFusion(scene);ok(Math.abs(scene.roofLocalHeight(a,1.9f,0)-fusion.surfaceHeight(a,1.9f,0))<.001f,"attachment height comes from the shared roof envelope");
  System.out.println("ROOF_ENVELOPE_PASS: outward gable winding, wall material separated from roof, closed shared edges on sample/L/T roofs, rotated false fusion rejected, attachment envelope height; no screenshots");
 }
}

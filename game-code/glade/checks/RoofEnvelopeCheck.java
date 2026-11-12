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
  checkTerracotta(art,a);
  checkRoofProfiles(art,a);
  System.out.println("ROOF_ENVELOPE_PASS: closed shared roof edges, outward gables, attachment envelope, overlapping terracotta relief and mipmaps; no screenshots");
 }

 private static void checkRoofProfiles(GladeArt art,GladeDesign.Item host){
  for(var kind:List.of(GladeDesign.Kind.ROOF_FLAT,GladeDesign.Kind.ROOF_PYRAMID,GladeDesign.Kind.ROOF_PENTAGON)){
   var part=new GladeDesign.Item(1,kind,host.center(),host.width(),host.depth(),1.1f,true,List.of(),0,new GladeDesign.Mount(host.id(),0,0,0));
   var design=new GladeDesign(List.of(host,part),2);
   var mesh=art.render(part,design);mesh.updateGeometricState();
   float x=.71f,z=.42f;
   var hits=new CollisionResults();mesh.collideWith(new Ray(new Vector3f(x,10,z),Vector3f.UNIT_Y.negate()),hits);
   ok(hits.size()>0 && Math.abs(hits.getClosestCollision().getContactPoint().y-design.roofLocalHeight(host,x,z))<.065f,
       "roof detail stays close to the same chimney profile: "+kind);
   boolean[] detail={false};
   mesh.depthFirstTraversal(partNode->{
    if(partNode instanceof Geometry geometry && (partNode.getName().equals("Lát sân mái") || partNode.getName().equals("Hàng ngói cong")))
     detail[0]=geometry.getMesh().getTriangleCount()>30;
   });
   ok(detail[0],"each profile has physical tile or paving relief");
  }
 }

 private static void checkTerracotta(GladeArt art,GladeDesign.Item house){
  var mesh=new PlainStructure.Builder();
  TileRoof.course(mesh,new Vector3f(0,0,1),new Vector3f(1,0,1),
      new Vector3f(0,.4f,.48f),new Vector3f(1,.4f,.48f),1,false);
  TileRoof.course(mesh,new Vector3f(0,.4f,.5f),new Vector3f(1,.4f,.5f),
      new Vector3f(0,.8f,-.02f),new Vector3f(1,.8f,-.02f),1,false);
  var courses=mesh.geometry(art,"Terracotta course fixture","#c78150");
  courses.updateGeometricState();
  var hits=new CollisionResults();
  courses.collideWith(new Ray(new Vector3f(.43f,2,.49f),Vector3f.UNIT_Y.negate()),hits);
  ok(hits.size()==2 && hits.getFarthestCollision().getDistance()-hits.getClosestCollision().getDistance()>.01f,
      "adjacent clay courses overlap at different heights");
  var tiled=art.render(house,new GladeDesign(List.of(house),2));
  int[] textures={0};
  tiled.depthFirstTraversal(part->{
   if(part instanceof Geometry tile && "tile".equals(tile.getUserData("paintMaterial"))){
    var map=(com.jme3.texture.Texture)tile.getMaterial().getParam("DiffuseMap").getValue();
    ok(map.getImage().hasMipmaps(),"roof texture carries mipmaps");
    textures[0]++;
   }
  });
  ok(textures[0]>0,"four-sided roof uses painted clay in a normal save");
 }
}

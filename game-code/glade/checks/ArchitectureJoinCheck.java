package vn.dreamhouse.glade;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.math.*;
import com.jme3.scene.*;
import java.nio.FloatBuffer;
import java.util.*;

/** Roof/tower regressions using actual transformed mesh triangles, not saved dimensions alone. */
class ArchitectureJoinCheck {
    static void ok(boolean value,String message){if(!value)throw new AssertionError(message);}
    static int roofVertices(Node node){int[] count={0};node.depthFirstTraversal(s->{if(s instanceof Geometry g && Boolean.TRUE.equals(g.getUserData("roofSurface")) && g.getCullHint()!=Spatial.CullHint.Always)count[0]+=g.getMesh().getVertexCount();});return count[0];}
    static void noRoofInsideTower(Node house,GladeDesign.Item tower,GladeDesign scene){
        house.updateGeometricState();int[] checked={0};var placement=scene.placement(tower);
        house.depthFirstTraversal(s->{if(!(s instanceof Geometry g) || !Boolean.TRUE.equals(g.getUserData("roofSurface")) || g.getCullHint()==Spatial.CullHint.Always)return;
            var mesh=g.getMesh();var p=(FloatBuffer)mesh.getBuffer(VertexBuffer.Type.Position).getData();var index=mesh.getIndicesAsList();
            for(int i=0;i<index.size();i+=3){var center=new Vector3f();for(int j=0;j<3;j++){int k=index.get(i+j);var v=g.localToWorld(new Vector3f(p.get(k*3),p.get(k*3+1),p.get(k*3+2)),null);ok(Float.isFinite(v.x) && Float.isFinite(v.y) && Float.isFinite(v.z),"finite join geometry");center.addLocal(v);}center.divideLocal(3);var point=new GladeDesign.Point(center.x,center.z);float x=tower.localX(point)/(tower.width()/2),z=tower.localZ(point)/(tower.depth()/2);boolean buried=x*x+z*z<.985f && center.y>placement.elevation()+.02f && center.y<placement.elevation()+tower.height()-.02f;ok(!buried,"roof triangle left inside tower at "+center);checked[0]++;}
        });ok(checked[0]>0,"exposed roof preserved");
    }
    public static void main(String[] args){
        var art=new GladeArt(new DesktopAssetManager(true));int cases=0;
        for(float angle:new float[]{0,23,91})for(float rise:new float[]{.35f,1.5f,2.2f})for(float width:new float[]{2.1f,3.2f}){
            var house=new GladeDesign.Item(0,GladeDesign.Kind.HOUSE,new GladeDesign.Point(0,0),4,3,2.7f,true,List.of(),angle,null,new GladeDesign.Form(0,rise,0,-1));
            var tower=new GladeDesign.Item(1,GladeDesign.Kind.TOWER,new GladeDesign.Point(1.8f,.15f),width,2.3f,5.2f,true,List.of(),37,null,new GladeDesign.Form(0,1.5f,0,-1));
            var scene=new GladeDesign(List.of(house,tower),2);noRoofInsideTower(art.render(house,scene),tower,scene);cases++;
        }
        var scene=GardenScene.create();noRoofInsideTower(art.render(scene.items().getFirst(),scene),scene.items().get(2),scene);
        var cache=new SceneRenderer();var root=new Node();cache.update(root,art,scene);var prior=root.getChild(0);var removed=new GladeDesign(scene.items().stream().filter(i->i.id()!=2).toList(),scene.land(),scene.terrain());cache.update(root,art,removed);ok(root.getChild(0)!=prior,"removing tower restores neighboring roof");
        int cached=roofVertices((Node)root.getChild(0)),fresh=roofVertices(art.render(removed.items().getFirst(),removed));ok(cached==fresh,"cached roof equals fresh uncut roof after removal");
        var lone=new GladeDesign.Item(0,GladeDesign.Kind.HOUSE,new GladeDesign.Point(0,0),4,3,2.6f,true,List.of(),0,null,new GladeDesign.Form(0,1.4f,0,-1));var loneScene=new GladeDesign(List.of(lone),2);var actual=art.render(lone,loneScene);actual.updateGeometricState();var hits=new com.jme3.collision.CollisionResults();actual.collideWith(new Ray(new Vector3f(-.9f,10,0),Vector3f.UNIT_Y.negate()),hits);ok(hits.size()>0 && hits.getClosestCollision().getContactPoint().y>3.3f,"batched roof keeps actual nondegenerate triangles above wall");
        var roof=new Node();TileRoof.build(art,roof,4,3,0,false);int[] samples={0};roof.depthFirstTraversal(s->{if(s instanceof Geometry g && g.getName().equals("Ngói cong thủ công")){var n=(FloatBuffer)g.getMesh().getBuffer(VertexBuffer.Type.Normal).getData();for(int i=1;i<n.limit();i+=3){ok(n.get(i)>.1f,"tile normals face sky on both slopes");samples[0]++;}}});ok(samples[0]>500,"curved tile mesh checked");
        System.out.println("ARCHITECTURE_JOIN_PASS: "+cases+" rotated/resized roof-tower intersections, actual garden contact, cache restoration after tower removal and upward curved-tile normals; no screenshots");
    }
}

package vn.dreamhouse.glade;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.scene.*;
import com.jme3.math.*;
import java.util.*;
class RoofEnvelopeProbe {
 record Edge(String a,String b) {static Edge of(Vector3f a,Vector3f b){String x=key(a),y=key(b);return x.compareTo(y)<0?new Edge(x,y):new Edge(y,x);}static String key(Vector3f p){return Math.round(p.x*10000)+","+Math.round(p.y*10000)+","+Math.round(p.z*10000);}}
 public static void main(String[]args){var scene=GladeDesign.mechanicsSample();var art=new GladeArt(new DesktopAssetManager(true));var fusion=new BlockFusion(scene);var edges=new HashMap<Edge,Integer>();var points=new HashMap<Edge,Vector3f[]>();
 for(var item:scene.items())if(fusion.joined(item.id())){var node=fusion.mesh(art,item);node.rotate(0,item.rotation()*FastMath.DEG_TO_RAD,0);var place=scene.placement(item);node.setLocalTranslation(place.center().x(),place.elevation(),place.center().z());node.updateGeometricState();node.depthFirstTraversal(n->{if(n instanceof Geometry g){var mesh=g.getMesh();var index=mesh.getIndexBuffer();var p=(java.nio.FloatBuffer)mesh.getBuffer(VertexBuffer.Type.Position).getData();for(int i=0;i<index.size();i+=3){var v=new Vector3f[3];for(int k=0;k<3;k++){int id=index.get(i+k);v[k]=g.localToWorld(new Vector3f(p.get(id*3),p.get(id*3+1),p.get(id*3+2)),null);}for(int k=0;k<3;k++){var edge=Edge.of(v[k],v[(k+1)%3]);edges.merge(edge,1,Integer::sum);points.put(edge,new Vector3f[]{v[k],v[(k+1)%3]});}}}});}
 int count=0;for(var entry:edges.entrySet())if(entry.getValue()==1){var p=points.get(entry.getKey());if(p[0].y>2.61f && p[1].y>2.61f){if(count++<12)System.out.println("OPEN_EDGE "+Arrays.toString(p));}}System.out.println("OPEN_HIGH_EDGES="+count);
 }
}

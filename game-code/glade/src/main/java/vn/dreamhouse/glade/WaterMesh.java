package vn.dreamhouse.glade;
import com.jme3.scene.*;
import com.jme3.math.*;
import java.util.*;
/** A single continuous surface cut from the union of a painted water stroke. */
final class WaterMesh {
 static Geometry build(GladeArt art,GladeDesign.Item item,GladeDesign scene) {
  float radius=item.height(),step=Math.max(.15f,Math.max(item.width(),item.depth())/150);
  var cells=new HashSet<Long>();
  for(var p:item.points()) for(int x=(int)Math.floor((p.x()-radius)/step);x<=Math.ceil((p.x()+radius)/step);x++)
   for(int z=(int)Math.floor((p.z()-radius)/step);z<=Math.ceil((p.z()+radius)/step);z++)cells.add(((long)x<<32)|(z&0xffffffffL));
  var positions=new ArrayList<Float>();var colors=new ArrayList<Float>();
  var bins=new HashMap<Long,List<GladeDesign.Point>>();
  for(var p:item.points()){int x=(int)Math.floor(p.x()/radius),z=(int)Math.floor(p.z()/radius);bins.computeIfAbsent(((long)x<<32)|(z&0xffffffffL),k->new ArrayList<>()).add(p);}
  for(long cell:cells){int x=(int)(cell>>32),z=(int)cell;var corners=new Vector3f[]{new Vector3f(x*step,0,z*step),new Vector3f(x*step,0,(z+1)*step),new Vector3f((x+1)*step,0,z*step),new Vector3f((x+1)*step,0,(z+1)*step)};
   for(var point:corners)point.y=Math.max(signed(point.x,point.z,radius,bins),Math.max(Math.max(scene.land().minX()-point.x,point.x-scene.land().maxX()),Math.max(scene.land().minZ()-point.z,point.z-scene.land().maxZ())));
   for(int[] triangle:new int[][]{{0,1,2},{2,1,3}}){var poly=new ArrayList<Vector3f>();for(int i=0;i<3;i++){
    var a=corners[triangle[i]];var b=corners[triangle[(i+1)%3]];if(a.y<=0)poly.add(a);
    if((a.y<0)!=(b.y<0)){float t=a.y/(a.y-b.y);poly.add(new Vector3f(a.x+(b.x-a.x)*t,0,a.z+(b.z-a.z)*t));}
   }for(int i=1;i+1<poly.size();i++)for(var p:List.of(poly.get(0),poly.get(i),poly.get(i+1))){
    positions.add(p.x-item.center().x());positions.add(scene.terrain().height(p.x,p.z)-scene.terrain().height(item.center().x(),item.center().z())+.08f);positions.add(p.z-item.center().z());
    float edge=FastMath.clamp(-p.y/radius,0,1);var shallow=GladeArt.color("#aed7bf");var deep=GladeArt.color("#4fa4aa");
    float glint=(float)Math.pow(Math.max(0,Math.sin(p.x*3.8+p.z*2)*Math.sin(p.x*.8-p.z*3)),12)*.15f;
    colors.add(FastMath.interpolateLinear(edge,shallow.r,deep.r)+glint);colors.add(FastMath.interpolateLinear(edge,shallow.g,deep.g)+glint);colors.add(FastMath.interpolateLinear(edge,shallow.b,deep.b)+glint);colors.add(1f);
   }}
  }
  float[] p=new float[positions.size()],c=new float[colors.size()],n=new float[positions.size()];for(int i=0;i<p.length;i++){p[i]=positions.get(i);n[i]=i%3==1?1:0;}for(int i=0;i<c.length;i++)c[i]=colors.get(i);
  var mesh=new Mesh();mesh.setBuffer(VertexBuffer.Type.Position,3,p);mesh.setBuffer(VertexBuffer.Type.Normal,3,n);mesh.setBuffer(VertexBuffer.Type.Color,4,c);mesh.updateBound();
  var result=new Geometry("Nước vẽ tự do",mesh);var material=art.material("#ffffff").clone();material.setBoolean("UseVertexColor",true);material.setFloat("Shininess",85);material.setColor("Specular",new ColorRGBA(.48f,.56f,.51f,1));result.setMaterial(material);SoftWater.register(result,p);return result;
 }
 private static float signed(float x,float z,float radius,Map<Long,List<GladeDesign.Point>> bins){float best=radius*2;int bx=(int)Math.floor(x/radius),bz=(int)Math.floor(z/radius);
  for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)for(var p:bins.getOrDefault(((long)(bx+dx)<<32)|((bz+dz)&0xffffffffL),List.of()))best=Math.min(best,(float)Math.hypot(x-p.x(),z-p.z())-radius);return best;
 }
}


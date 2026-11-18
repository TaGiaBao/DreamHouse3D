package vn.dreamhouse.glade;
import com.jme3.scene.*;
import com.jme3.math.*;
import java.util.*;
final class WaterMesh {
 static Geometry build(GladeArt art,GladeDesign.Item item,GladeDesign scene) {
  float radius=item.height(),step=Math.max(Math.max(.15f,radius/20),Math.max(item.width(),item.depth())/150);
  float ground=scene.terrain().height(item.center().x(),item.center().z());
  var shallow=GladeArt.color("#aed7bf");var deep=GladeArt.color("#4fa4aa");
  var rows=new HashMap<Integer,BitSet>();
  int left=(int)Math.floor((scene.land().minX()-step)/step),right=(int)Math.ceil(scene.land().maxX()/step);
  int back=(int)Math.floor((scene.land().minZ()-step)/step),front=(int)Math.ceil(scene.land().maxZ()/step);
  for(var p:item.points()){
   int start=Math.max(left,(int)Math.floor((p.x()-radius)/step)),end=Math.min(right,(int)Math.ceil((p.x()+radius)/step));
   if(start>end)continue;
   int minZ=Math.max(back,(int)Math.floor((p.z()-radius)/step)),maxZ=Math.min(front,(int)Math.ceil((p.z()+radius)/step));
   for(int z=minZ;z<=maxZ;z++)rows.computeIfAbsent(z,key->new BitSet()).set(start-left,end-left+1);
  }
  var positions=new ArrayList<Float>();var colors=new ArrayList<Float>();
  var bins=new HashMap<Long,List<GladeDesign.Point>>();
  for(var p:item.points()){int x=(int)Math.floor(p.x()/radius),z=(int)Math.floor(p.z()/radius);bins.computeIfAbsent(((long)x<<32)|(z&0xffffffffL),k->new ArrayList<>()).add(p);}
  var samples=new HashMap<Long,Vector3f>();
  int[][] triangles={{0,1,2},{2,1,3}};
  for(var row:rows.entrySet())for(int bit=row.getValue().nextSetBit(0);bit>=0;bit=row.getValue().nextSetBit(bit+1)){
   int x=bit+left,z=row.getKey();
   var corners=new Vector3f[]{sample(x,z,step,radius,bins,samples,scene.land()),sample(x,z+1,step,radius,bins,samples,scene.land()),
     sample(x+1,z,step,radius,bins,samples,scene.land()),sample(x+1,z+1,step,radius,bins,samples,scene.land())};
   for(int[] triangle:triangles){var poly=new ArrayList<Vector3f>(4);for(int i=0;i<3;i++){
    var a=corners[triangle[i]];var b=corners[triangle[(i+1)%3]];if(a.y<=0)poly.add(a);
    if((a.y<0)!=(b.y<0)){float t=a.y/(a.y-b.y);poly.add(new Vector3f(a.x+(b.x-a.x)*t,0,a.z+(b.z-a.z)*t));}
   }for(int i=1;i+1<poly.size();i++)for(var p:List.of(poly.get(0),poly.get(i),poly.get(i+1))){
    positions.add(p.x-item.center().x());positions.add(scene.terrain().height(p.x,p.z)-ground+.08f);positions.add(p.z-item.center().z());
    float edge=FastMath.clamp(-p.y/radius,0,1);
    float glint=(float)Math.pow(Math.max(0,Math.sin(p.x*3.8+p.z*2)*Math.sin(p.x*.8-p.z*3)),12)*.15f;
    colors.add(FastMath.interpolateLinear(edge,shallow.r,deep.r)+glint);colors.add(FastMath.interpolateLinear(edge,shallow.g,deep.g)+glint);colors.add(FastMath.interpolateLinear(edge,shallow.b,deep.b)+glint);colors.add(1f);
   }}
  }
  float[] p=new float[positions.size()],c=new float[colors.size()],n=new float[positions.size()];for(int i=0;i<p.length;i++){p[i]=positions.get(i);n[i]=i%3==1?1:0;}for(int i=0;i<c.length;i++)c[i]=colors.get(i);
  var mesh=new Mesh();mesh.setBuffer(VertexBuffer.Type.Position,3,p);mesh.setBuffer(VertexBuffer.Type.Normal,3,n);mesh.setBuffer(VertexBuffer.Type.Color,4,c);mesh.updateBound();
  var result=new Geometry("Nước vẽ tự do",mesh);var material=art.material("#ffffff").clone();material.setBoolean("UseVertexColor",true);material.setFloat("Shininess",85);material.setColor("Specular",new ColorRGBA(.48f,.56f,.51f,1));result.setMaterial(material);
  SoftWater.register(result,p);
  return result;
 }

 static Geometry preview(GladeArt art,GladeDesign.Item item,GladeDesign scene) {
  var points=item.points();
  var mesh=new PlainStructure.Builder();
  float radius=item.height(),base=scene.placement(item).elevation();
  Vector3f left=null,right=null;
  for(int i=0;i<points.size();i++){
   var p=points.get(i);
   var previous=points.get(Math.max(0,i-1));var next=points.get(Math.min(points.size()-1,i+1));
   float dx=next.x()-previous.x(),dz=next.z()-previous.z();
   float length=(float)Math.hypot(dx,dz);
   if(length<.001f){dx=1;length=1;}
   var a=previewPoint(item,scene,base,p.x()-dz/length*radius,p.z()+dx/length*radius);
   var b=previewPoint(item,scene,base,p.x()+dz/length*radius,p.z()-dx/length*radius);
   if(i>0)mesh.quad(left,a,b,right);
   if(i==0 || i==points.size()-1){
    float angle=FastMath.atan2(dz,dx)+(i==0?FastMath.HALF_PI:-FastMath.HALF_PI);
    float span=points.size()==1?FastMath.TWO_PI:FastMath.PI;
    var center=previewPoint(item,scene,base,p.x(),p.z());
    var rim=previewPoint(item,scene,base,p.x()+FastMath.cos(angle)*radius,p.z()+FastMath.sin(angle)*radius);
    for(int edge=1;edge<=12;edge++){
     float t=angle+span*edge/12;
     var end=previewPoint(item,scene,base,p.x()+FastMath.cos(t)*radius,p.z()+FastMath.sin(t)*radius);
     mesh.triangle(center,end,rim);rim=end;
    }
   }
   left=a;right=b;
  }
  var geometry=mesh.geometry(art,"Nét nước xem trước","#76c6c9");
  var material=geometry.getMaterial().clone();
  var color=GladeArt.color("#76c6c9");color.a=.72f;
  material.setColor("Diffuse",color);
  var state=material.getAdditionalRenderState();
  state.setFaceCullMode(com.jme3.material.RenderState.FaceCullMode.Off);
  state.setBlendMode(com.jme3.material.RenderState.BlendMode.Alpha);
  state.setDepthWrite(false);
  geometry.setMaterial(material);
  geometry.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Transparent);
  geometry.setShadowMode(com.jme3.renderer.queue.RenderQueue.ShadowMode.Off);
  return geometry;
 }

 private static Vector3f previewPoint(GladeDesign.Item item,GladeDesign scene,float base,float x,float z){
  var land=scene.land();
  x=Math.clamp(x,land.minX(),land.maxX());z=Math.clamp(z,land.minZ(),land.maxZ());
  float height=scene.terrain().height(x,z);
  if(item.points().size()==1)height=Math.max(height,base);
  return new Vector3f(x-item.center().x(),height-base+.07f,z-item.center().z());
 }
 private static Vector3f sample(int ix,int iz,float step,float radius,Map<Long,List<GladeDesign.Point>> bins,Map<Long,Vector3f> samples,LandBounds land){
  return samples.computeIfAbsent(key(ix,iz),ignored->{
   float x=ix*step,z=iz*step,best=9*radius*radius;
   int bx=(int)Math.floor(x/radius),bz=(int)Math.floor(z/radius);
   for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)for(var p:bins.getOrDefault(key(bx+dx,bz+dz),List.of())){
    float px=x-p.x(),pz=z-p.z();best=Math.min(best,px*px+pz*pz);
   }
   float edge=Math.max(Math.max(land.minX()-x,x-land.maxX()),Math.max(land.minZ()-z,z-land.maxZ()));
   return new Vector3f(x,Math.max((float)Math.sqrt(best)-radius,edge),z);
  });
 }
 private static long key(int x,int z){return ((long)x<<32)|(z&0xffffffffL);}
}


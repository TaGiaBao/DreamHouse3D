package vn.dreamhouse.glade;
import com.jme3.asset.AssetManager;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.material.*;
import com.jme3.renderer.queue.RenderQueue;
import java.util.*;

/** A construction cage, never a saved structure and never part of world picking. */
final class BuildGhost extends Node {
 private final Material frame,footprint;
 private String last="";
 BuildGhost(AssetManager assets){super("Khung xem trước");frame=material(assets,new ColorRGBA(.82f,.66f,.40f,.78f));footprint=material(assets,new ColorRGBA(1,1,.89f,.95f));setShadowMode(RenderQueue.ShadowMode.Off);}
 private Material material(AssetManager assets,ColorRGBA color){var mat=new Material(assets,"Common/MatDefs/Misc/Unshaded.j3md");mat.setColor("Color",color);mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);mat.getAdditionalRenderState().setDepthWrite(false);mat.getAdditionalRenderState().setLineWidth(2);return mat;}
 void hide(){setCullHint(CullHint.Always);}
 void show(GladeDesign.Item item,GladeDesign scene){
  var p=scene.placement(item);String key=item.kind()+":"+item.width()+":"+item.depth()+":"+item.height()+":"+item.form().roof();
  if(!key.equals(last)){last=key;detachAllChildren();float w=item.width()/2,d=item.depth()/2,h=item.height(),roof=item.form().roof();var lines=new ArrayList<Float>();var base=new ArrayList<Float>();
   if(item.kind()==GladeDesign.Kind.PENTAGON)polygonFrame(item,lines,base);
   else if(item.kind()==GladeDesign.Kind.TOWER){for(int i=0;i<24;i++){float a=i*FastMath.TWO_PI/24,b=(i+1)*FastMath.TWO_PI/24;for(float y:new float[]{0,h})line(lines,new Vector3f(FastMath.cos(a)*w,y,FastMath.sin(a)*d),new Vector3f(FastMath.cos(b)*w,y,FastMath.sin(b)*d));if(i%6==0){line(lines,new Vector3f(FastMath.cos(a)*w,0,FastMath.sin(a)*d),new Vector3f(FastMath.cos(a)*w,h,FastMath.sin(a)*d));line(lines,new Vector3f(FastMath.cos(a)*w,h,FastMath.sin(a)*d),new Vector3f(0,h+roof,0));}}}
   else for(int side:new int[]{-1,1}){
    for(float y:new float[]{0,h}){line(lines,new Vector3f(-w,y,side*d),new Vector3f(w,y,side*d));line(lines,new Vector3f(side*w,y,-d),new Vector3f(side*w,y,d));}
    for(int end:new int[]{-1,1})line(lines,new Vector3f(side*w,0,end*d),new Vector3f(side*w,h,end*d));
    if(roof>.02f)for(int end:new int[]{-1,1})line(lines,new Vector3f(side*w,h,end*d),new Vector3f(0,h+roof,0));
    line(lines,new Vector3f(-w,h*.5f,side*d),new Vector3f(w,h*.5f,side*d));
   }
   if(item.kind()!=GladeDesign.Kind.PENTAGON)for(int side:new int[]{-1,1})for(int i=0;i<10;i++){float t=i/10f,u=(i+.55f)/10;line(base,new Vector3f(-w+2*w*t,.03f,side*d),new Vector3f(-w+2*w*u,.03f,side*d));line(base,new Vector3f(side*w,.03f,-d+2*d*t),new Vector3f(side*w,.03f,-d+2*d*u));}
   attachChild(geometry(lines,frame));attachChild(geometry(base,footprint));
  }
  setLocalTranslation(p.center().x(),p.elevation(),p.center().z());setLocalRotation(new Quaternion().fromAngleAxis(p.rotation()*FastMath.DEG_TO_RAD,Vector3f.UNIT_Y));setCullHint(CullHint.Inherit);
 }
 private void polygonFrame(GladeDesign.Item item,List<Float> lines,List<Float> base){
  float height=item.height(),rise=item.form().roof();
  for(var edge:Pentagon.edges(item.width(),item.depth())){
   var a=edge.from();var b=edge.to();
   var top=new Vector3f(a.x,height,a.y);
   line(lines,top,new Vector3f(b.x,height,b.y));
   line(lines,new Vector3f(a.x,0,a.y),top);
   if(rise>.02f)line(lines,top,new Vector3f(0,height+rise,0));
   int segments=Math.clamp((int)Math.ceil(a.distance(b)/.35f),2,24);
   for(int i=0;i<segments;i++){
    var start=a.clone().interpolateLocal(b,i/(float)segments);
    var end=a.clone().interpolateLocal(b,(i+.55f)/segments);
    line(base,new Vector3f(start.x,.03f,start.y),new Vector3f(end.x,.03f,end.y));
   }
  }
 }
 private void line(List<Float> out,Vector3f a,Vector3f b){for(var point:List.of(a,b)){out.add(point.x);out.add(point.y);out.add(point.z);}}
 private Geometry geometry(List<Float> values,Material material){float[] positions=new float[values.size()];for(int i=0;i<positions.length;i++)positions[i]=values.get(i);var mesh=new Mesh();mesh.setMode(Mesh.Mode.Lines);mesh.setBuffer(VertexBuffer.Type.Position,3,positions);mesh.updateBound();var geo=new Geometry("Khung dựng",mesh);geo.setMaterial(material);geo.setQueueBucket(RenderQueue.Bucket.Transparent);return geo;}
}

package vn.dreamhouse.glade;
import com.jme3.math.*;
import com.jme3.scene.*;

/** Four sloping roof faces derived continuously from footprint and roof rise. */
final class HipRoof {
 static void build(GladeArt art,Node parent,GladeDesign.Item item,float rise) {
  var form=item.form();float width=item.width()+.24f+form.roofLeft()+form.roofRight(),depth=item.depth()+.24f+form.roofFront()+form.roofBack();boolean swap=depth>width;
  float w=swap?depth:width,d=swap?width:depth,h=item.height(),ridge=(w-d)/2;
  var roof=art.roofGroup(parent);var fill=new PlainStructure.Builder();
  fill.quad(new Vector3f(-w/2,h,d/2),new Vector3f(w/2,h,d/2),new Vector3f(ridge,h+rise,0),new Vector3f(-ridge,h+rise,0));
  fill.quad(new Vector3f(w/2,h,-d/2),new Vector3f(-w/2,h,-d/2),new Vector3f(-ridge,h+rise,0),new Vector3f(ridge,h+rise,0));
  fill.triangle(new Vector3f(-w/2,h,-d/2),new Vector3f(-w/2,h,d/2),new Vector3f(-ridge,h+rise,0));
  fill.triangle(new Vector3f(w/2,h,d/2),new Vector3f(w/2,h,-d/2),new Vector3f(ridge,h+rise,0));
  roof.attachChild(fill.geometry(art,"Bốn mặt mái","#bd926a"));
  String material=form.finish().roofMaterial();
  if(material.equals("tile") || material.equals("slate") || material.equals("original")) {
   float slope=(float)Math.hypot(d/2,rise),angle=FastMath.atan2(rise,d/2);int courses=Math.max(3,Math.min(24,(int)Math.ceil(slope/.30f))),columns=Math.max(3,Math.min(48,(int)Math.ceil(w/.34f)));
   for(int side:new int[]{-1,1})for(int row=0;row<courses;row++) {
    float t=(row+.5f)/courses,span=w/2*(1-t)+ridge*t,z=side*d/2*(1-t),y=h+rise*t+.038f;
    for(int col=0;col<columns;col++){var tile=art.box(roof,Math.max(.035f,2*span/columns-.016f),.095f,slope/courses+.035f,-span+(col+.5f)*2*span/columns,y,z,"#c58b62");tile.rotate(side*angle,0,0);}
   }
   for(int side:new int[]{-1,1})for(int row=0;row<courses;row++){
    float t=(row+.5f)/courses,span=d/2*(1-t),x=side*(w/2-d/2*t),y=h+rise*t+.039f;int cols=Math.max(1,(int)Math.ceil(2*span/.34f));
    for(int col=0;col<cols;col++){var tile=art.box(roof,slope/courses+.035f,.095f,Math.max(.035f,2*span/cols-.016f),x,y,-span+(col+.5f)*2*span/cols,"#c58b62");tile.rotate(0,0,-side*angle);}
   }
   for(int end:new int[]{-1,1})for(int side:new int[]{-1,1})art.beam(roof,new Vector3f(end*w/2,h+.035f,side*d/2),new Vector3f(end*ridge,h+rise+.04f,0),.065f,"#b9845c");
   if(ridge>.02f)art.beam(roof,new Vector3f(-ridge,h+rise+.04f,0),new Vector3f(ridge,h+rise+.04f,0),.06f,"#b9845c");
  }
  roof.updateGeometricState();jme3tools.optimize.GeometryBatchFactory.optimize(roof,false);
  roof.depthFirstTraversal(part->{if(part instanceof Geometry)part.setUserData("roofSurface",true);});
  if(swap)roof.rotate(0,FastMath.HALF_PI,0);
  roof.move((form.roofRight()-form.roofLeft())/2,0,(form.roofFront()-form.roofBack())/2);roof.setUserData("hipRoof",true);
 }
}

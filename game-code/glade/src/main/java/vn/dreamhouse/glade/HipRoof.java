package vn.dreamhouse.glade;
import com.jme3.math.*;
import com.jme3.scene.*;

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
   float slope=(float)Math.hypot(d/2,rise);
   int courses=Math.max(3,Math.min(24,(int)Math.ceil(slope/.30f)));
   var tiles=new PlainStructure.Builder();boolean slate=material.equals("slate");
   for(int side:new int[]{-1,1})for(int row=0;row<courses;row++) {
    float t0=row/(float)courses,t1=Math.min(1,(row+1.035f)/courses);
    float outerZ=d/2*(1-t0),innerZ=d/2*(1-t1);
    float outerX=ridge+outerZ,innerX=ridge+innerZ,outerY=h+rise*t0,innerY=h+rise*t1;
    TileRoof.course(tiles,new Vector3f(-outerX,outerY,side*outerZ),new Vector3f(outerX,outerY,side*outerZ),
        new Vector3f(-innerX,innerY,side*innerZ),new Vector3f(innerX,innerY,side*innerZ),columns(outerX),slate);
    TileRoof.course(tiles,new Vector3f(side*outerX,outerY,-outerZ),new Vector3f(side*outerX,outerY,outerZ),
        new Vector3f(side*innerX,innerY,-innerZ),new Vector3f(side*innerX,innerY,innerZ),columns(outerZ),slate);
   }
   roof.attachChild(tiles.geometry(art,"Ngói cong mái bốn dốc",slate?"#7b9198":"#c58b62"));
   for(int end:new int[]{-1,1})for(int side:new int[]{-1,1})art.beam(roof,new Vector3f(end*w/2,h+.035f,side*d/2),new Vector3f(end*ridge,h+rise+.04f,0),.065f,"#b9845c");
   if(ridge>.02f)art.beam(roof,new Vector3f(-ridge,h+rise+.04f,0),new Vector3f(ridge,h+rise+.04f,0),.06f,"#b9845c");
  }
  roof.updateGeometricState();jme3tools.optimize.GeometryBatchFactory.optimize(roof,false);
  roof.depthFirstTraversal(part->{if(part instanceof Geometry)part.setUserData("roofSurface",true);});
  if(swap)roof.rotate(0,FastMath.HALF_PI,0);
  roof.move((form.roofRight()-form.roofLeft())/2,0,(form.roofFront()-form.roofBack())/2);roof.setUserData("hipRoof",true);
 }
 private static int columns(float halfWidth){return Math.clamp((int)Math.ceil(2*halfWidth/.34f),1,48);}
}

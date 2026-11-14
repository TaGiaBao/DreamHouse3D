package vn.dreamhouse.glade;
import com.jme3.scene.*;
import com.jme3.math.*;
import com.jme3.scene.shape.Cylinder;
final class GladeVariants {
 static boolean craft(GladeArt art,Node node,GladeDesign.Item item) {
  if(item.kind().stroke() || item.kind().mounted() || item.kind()==GladeDesign.Kind.WATER || item.kind()==GladeDesign.Kind.PENTAGON)return false;
  float w=item.width(),d=item.depth(),h=item.height();var finish=item.form().finish();
  boolean template=(finish.variant()>=7 && finish.variant()<=9 && item.kind().building()) || finish.variant()==18 && item.kind()==GladeDesign.Kind.DECK;
  if(h<=.12f){art.box(node,w,h,d,0,h/2,0,finish.material().equals("wood")?"#be9c70":"#c4baa2").setUserData("wallSurface",true);node.setUserData("craftShape","Tấm phẳng");return true;}
  if(template && !finish.material().equals("wood") && item.form().roof()<=.02f) {
   art.box(node,w,h,d,0,h/2,0,"#c4baa2").setUserData("wallSurface",true);node.setUserData("craftShape","Khối đặc");return true;
  }
  if(!item.kind().building() && item.kind()!=GladeDesign.Kind.DECK || !finish.material().equals("wood") || !template && h>1.2f || item.form().roof()>.02f)return false;
  boolean table=finish.variant()==8,pergola=finish.variant()==9 && h>.55f;
  float deck=pergola?h-.10f:table?h:h<=1.2f?Math.min(h,.65f):h-.55f;
  for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})art.box(node,.12f,Math.max(.04f,deck-.08f),.12f,x*w*.38f,(deck-.08f)/2,z*d*.34f,"#97754f");
  int slats=Math.max(3,Math.min(30,(int)(d/.18f)));
  for(int i=0;i<slats;i++) {
   float plankWidth=pergola?Math.min(.11f,d/slats*.55f):d/slats-.012f;
   art.box(node,w,.08f,plankWidth,0,deck-.04f,-d/2+(i+.5f)*d/slats,"#c9a578");
  }
  if(pergola) {
   for(int side:new int[]{-1,1})art.box(node,.13f,.12f,d+.12f,side*w*.4f,h-.06f,0,"#b79566");
   for(int i=0;i<6;i++){var leaf=art.sphere(node,.12f,FastMath.sin(i*2.1f)*w*.35f,h-.09f,FastMath.cos(i*1.7f)*d*.32f,"#a3b97f");leaf.setLocalScale(1,.45f,1);}
  } else if(!table && h>.74f && h<=1.2f) {
   for(int x:new int[]{-1,1})art.box(node,.09f,h,.09f,x*w*.4f,h/2,-d*.37f,"#97754f");
   for(int row=0;row<3;row++)art.box(node,w,Math.min(.09f,(h-deck)/4),.07f,0,deck+(h-deck)*(row+.6f)/3,-d*.37f,"#c9a578");
  } else if(!table && h>1.2f) {
   for(int side:new int[]{-1,1}) {
    art.box(node,.08f,h-deck,d,side*w*.47f,deck+(h-deck)/2,0,"#b79566");
    art.box(node,w,.08f,.08f,0,h-.04f,side*d*.47f,"#b79566");
   }
  }
  node.depthFirstTraversal(part->{if(part instanceof Geometry && part.getName().equals("Khối"))part.setUserData("wallSurface",true);});
  node.setUserData("craftShape",pergola?"Giàn gỗ":table?"Bàn gỗ":h<.55f?"Sàn thấp":h<=1.2f?"Ghế gỗ":"Sàn nâng / ban công");return true;
 }
 static void apply(GladeArt art,Node node,GladeDesign.Item item) {
  int variant=item.form().finish().variant();
  if(item.kind()==GladeDesign.Kind.TREE) {
   node.setLocalTranslation(0,0,0);
   node.detachAllChildren();float h=item.height(),w=item.width();String foliage=variant==4?"#dda9bd":variant==6?"#d1a05e":"#92ad69";
   art.cylinder(node,Math.min(.13f,w*.10f),h*.67f,0,h*.335f,0,variant==2?"#e2dccb":"#96754e");
   if(variant==1 || variant==5){for(int i=0;i<3;i++){float r=w*(variant==5?.24f:.48f)*(1-i*.17f),rise=h*.5f;var cone=new Geometry("Tán lá tầng",new Cylinder(2,24,.01f,r,rise,true,false));cone.rotate(-FastMath.HALF_PI,0,0);cone.setMaterial(art.material(i%2==0?"#779965":"#90ac79"));cone.setLocalTranslation(0,h*(.35f+i*.20f)+rise/2,0);node.attachChild(cone);}}
   else {
    node.detachAllChildren();ScenicDetails.canopy(art,node,w,h,foliage);
    if(variant==3)for(int i=0;i<11;i++)art.sphere(node,.08f,(float)Math.sin(i*2.3f)*w*.4f,h*.74f+i%3*.12f,(float)Math.cos(i*2.3f)*w*.4f,"#cf7964");
   }
   if(variant==7)art.box(node,w,.22f,w,0,.11f,0,"#ba9b6c");
   node.depthFirstTraversal(part->{if(part instanceof Geometry leaf && (leaf.getName().equals("Tán") || leaf.getName().equals("Tán lá tầng")))CanopyShading.apply(leaf);});
   node.updateGeometricState();jme3tools.optimize.GeometryBatchFactory.optimize(node,false);node.setUserData("softCanopy",true);
   node.setLocalScale(1,1,item.depth()/item.width());
  } else if(item.kind()==GladeDesign.Kind.FLOWERS){node.setLocalTranslation(0,0,0);node.detachAllChildren();String tint=variant==1?"#a18abc":variant==2?"#e7c56e":"#d790a3";
   ScenicDetails.blossoms(art,node,item.width(),item.depth(),item.height(),tint);
  } else if(item.kind().opening() && variant>0){node.depthFirstTraversal(s->{if(s instanceof Geometry g){var value=g.getMaterial().getParam("Diffuse");if(value!=null && value.getValue() instanceof ColorRGBA c && c.b<c.r+.03f)g.setMaterial(art.material(variant==1?"#eee2bd":variant==2?"#8fa787":"#b19ab7"));}});}
 }
}

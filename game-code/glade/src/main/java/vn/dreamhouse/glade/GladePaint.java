package vn.dreamhouse.glade;
import com.jme3.scene.*;
import com.jme3.texture.*;
import com.jme3.texture.plugins.AWTLoader;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
/** Locally generated material patterns; no downloaded textures or external service. */
final class GladePaint {
 private static final Map<String,Texture2D> textures=new HashMap<>();
 static void apply(GladeArt art,Node node,GladeDesign.Finish finish) {
  node.depthFirstTraversal(part->{if(!(part instanceof Geometry geo))return;
   boolean roof=Boolean.TRUE.equals(geo.getUserData("roofSurface"));
   boolean wall=Boolean.TRUE.equals(geo.getUserData("wallSurface"));if(!roof && !wall)return;
   String style=roof?finish.roofMaterial():finish.material(),tint=roof?finish.roofColor():finish.color();
   if(style.equals("original") && tint.isEmpty())return;
   if(tint.isEmpty())tint=switch(style){case "brick","tile"->"#c08369";case "stone"->"#a8afa4";case "wood"->"#be9c70";case "slate","metal"->"#849aa4";default->"#e8dbb6";};
   var mat=art.material(tint).clone();
   mat.getAdditionalRenderState().setFaceCullMode(geo.getMaterial().getAdditionalRenderState().getFaceCullMode());
   if(!style.equals("original") && geo.getMesh().getBuffer(VertexBuffer.Type.TexCoord)==null){
    var positions=(java.nio.FloatBuffer)geo.getMesh().getBuffer(VertexBuffer.Type.Position).getData();var normals=geo.getMesh().getBuffer(VertexBuffer.Type.Normal);var normal=normals==null?null:(java.nio.FloatBuffer)normals.getData();var uv=new float[geo.getMesh().getVertexCount()*2];for(int i=0;i<uv.length/2;i++){float nx=normal==null?0:Math.abs(normal.get(i*3)),ny=normal==null?1:Math.abs(normal.get(i*3+1));boolean top=ny>.65f;uv[i*2]=positions.get(i*3+(nx>.65f?2:0));uv[i*2+1]=positions.get(i*3+(top?2:1));}geo.getMesh().setBuffer(VertexBuffer.Type.TexCoord,2,uv);
   }
   if(!style.equals("original"))mat.setTexture("DiffuseMap",textures.computeIfAbsent(style,GladePaint::texture));
   mat.setFloat("Shininess",style.equals("metal")?55:style.equals("plaster")?5:style.equals("wood")?10:16);
   mat.setColor("Specular",style.equals("metal")?new com.jme3.math.ColorRGBA(.28f,.30f,.31f,1):new com.jme3.math.ColorRGBA(.06f,.06f,.05f,1));
   geo.setMaterial(mat);geo.setUserData("paintMaterial",style);geo.setUserData("paintColor",tint);
  });
 }
 private static Texture2D texture(String style) {
  var img=new BufferedImage(128,128,BufferedImage.TYPE_INT_RGB);var g=img.createGraphics();
  g.setColor(new Color(247,246,241));g.fillRect(0,0,128,128);var random=new Random(41);
  for(int i=0;i<1000;i++){int shade=235+random.nextInt(17);g.setColor(new Color(shade,shade,shade));g.fillRect(random.nextInt(128),random.nextInt(128),1,1);}
  g.setStroke(new BasicStroke(2));g.setColor(new Color(191,190,182));
  switch(style){
   case "brick","stone","tile","slate" -> {int step=style.equals("stone")?42:32;for(int y=0,row=0;y<128;y+=step,row++){
    g.drawLine(0,y,128,y);for(int x=(row%2)*32;x<128;x+=64)g.drawLine(x,y,x,y+step);
   }}
   case "wood" -> {g.setStroke(new BasicStroke(2));for(int x=0;x<128;x+=32)g.drawLine(x,0,x,128);g.setColor(new Color(188,185,169));
    for(int i=0;i<18;i++){int x=random.nextInt(128);g.drawArc(x-9,random.nextInt(100),18,50,60,220);}}
   case "plaster" -> {g.setStroke(new BasicStroke(.8f));g.setColor(new Color(229,228,221));for(int i=0;i<2;i++){int x=16+i*37,y=16+i*21;g.drawLine(x,y,x+5,y+11);g.drawLine(x+5,y+11,x+1,y+19);g.drawLine(x+5,y+11,x+13,y+16);}}
   case "metal" -> {g.drawLine(0,0,0,128);g.drawLine(64,0,64,128);g.setColor(new Color(255,255,248));g.drawLine(4,0,4,128);}
   default -> {}
  }
  g.dispose();var texture=new Texture2D(new AWTLoader().load(img,true));texture.setWrap(Texture.WrapMode.Repeat);texture.setMagFilter(Texture.MagFilter.Bilinear);texture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);return texture;
 }
}

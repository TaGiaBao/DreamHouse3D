package vn.dreamhouse.glade;
import com.jme3.scene.*;
import com.jme3.texture.*;
import com.jme3.texture.plugins.AWTLoader;
import com.jme3.util.MipMapGenerator;
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
   if(tint.isEmpty())tint=switch(style){case "brick","tile"->"#c08369";case "stone"->"#a8afa4";case "wood"->"#be9c70";case "slate","metal"->"#849aa4";case "dirt","concrete","gravel"->PathSurface.defaultColor(style);default->"#e8dbb6";};
   var mat=art.material(tint).clone();
   Float shade=geo.getUserData("roofShade");
   if(roof && shade!=null){
    var color=GladeArt.color(tint).mult(shade);
    color.a=1;
    mat.setColor("Diffuse",color);
    mat.setColor("Ambient",color);
   }
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
   case "dirt","concrete","gravel" -> {
    boolean gravel=style.equals("gravel"),dirt=style.equals("dirt");
    for(int i=0;i<(gravel?500:210);i++){
     int x=random.nextInt(128),y=random.nextInt(128),size=1+random.nextInt(gravel?7:dirt?4:2);
     int tone=(gravel?156:dirt?208:225)+random.nextInt(gravel?95:dirt?43:24);
     g.setColor(new Color(tone,tone,tone));
     for(int ox=-128;ox<=128;ox+=128)for(int oy=-128;oy<=128;oy+=128)g.fillOval(x+ox,y+oy,size+1,size);
    }
   }
   case "tile" -> {
    for(int y=0,row=0;y<128;y+=32,row++){
     int offset=(row%2)*32;
     g.setColor(new Color(188,181,174,94));g.drawLine(0,y,128,y);
     for(int x=offset;x<128;x+=64)g.drawLine(x,y,x,y+32);
     g.setColor(new Color(255,250,239,96));g.drawLine(0,y+2,128,y+2);
    }
   }
   case "brick","stone","slate" -> {int step=style.equals("stone")?42:32;for(int y=0,row=0;y<128;y+=step,row++){
    g.drawLine(0,y,128,y);for(int x=(row%2)*32;x<128;x+=64)g.drawLine(x,y,x,y+step);
   }}
   case "wood" -> {g.setStroke(new BasicStroke(2));for(int x=0;x<128;x+=32)g.drawLine(x,0,x,128);g.setColor(new Color(188,185,169));
    for(int i=0;i<18;i++){int x=random.nextInt(128);g.drawArc(x-9,random.nextInt(100),18,50,60,220);}}
   case "plaster" -> {
    for(int y=0;y<128;y++)for(int x=0;x<128;x++){
     double u=x*(2*Math.PI/128),v=y*(2*Math.PI/128);
     int tone=245+(int)Math.round(3*Math.sin(u+v)+2*Math.cos(2*u-v)+Math.sin(5*u+3*v))+random.nextInt(5)-2;
     img.setRGB(x,y,(tone<<16)|((tone-1)<<8)|(tone-4));
    }
    g.setStroke(new BasicStroke(.8f));g.setColor(new Color(230,227,219,145));
    g.drawLine(24,30,30,42);g.drawLine(30,42,26,49);g.drawLine(30,42,37,45);
   }
   case "metal" -> {g.drawLine(0,0,0,128);g.drawLine(64,0,64,128);g.setColor(new Color(255,255,248));g.drawLine(4,0,4,128);}
   default -> {}
  }
  g.dispose();var image=new AWTLoader().load(img,true);MipMapGenerator.generateMipMaps(image);
  var texture=new Texture2D(image);texture.setWrap(Texture.WrapMode.Repeat);texture.setMagFilter(Texture.MagFilter.Bilinear);texture.setMinFilter(Texture.MinFilter.Trilinear);return texture;
 }
}

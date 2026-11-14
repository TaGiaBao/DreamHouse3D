package vn.dreamhouse.glade;

import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.*;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;
import com.jme3.util.MipMapGenerator;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Random;

final class MeadowArt {
    private static Texture2D texture;
    private static final ColorRGBA OLIVE=GladeArt.color("#89a658"), HAY=GladeArt.color("#c2c780");

    static Material ground(GladeArt art){
        var material=art.material("#ffffff").clone();
        material.setBoolean("UseVertexColor",true);
        material.setColor("Specular",new ColorRGBA(.025f,.028f,.018f,1));
        if(texture==null)texture=texture();
        material.setTexture("DiffuseMap",texture);
        return material;
    }

    static ColorRGBA tint(float x,float z){
        float blend=.5f+.21f*(float)Math.sin(x*.39f+z*.24f)
                +.16f*(float)Math.cos(z*.51f-x*.14f)+.1f*(float)Math.sin(z*.72f+x*.63f);
        return OLIVE.clone().interpolateLocal(HAY,blend);
    }

    static Geometry grass(GladeArt art,GladeDesign scene){
        var land=scene.land();
        int patches=Math.clamp(Math.round(land.width()*land.depth()*6),1600,5400);
        int blades=patches*3;
        float[] positions=new float[blades*15],normals=new float[positions.length],colors=new float[blades*20];
        int[] indices=new int[blades*9];
        var random=new Random(631);
        var roots=GladeArt.color("#627c3b");
        var green=GladeArt.color("#a8c368");
        var gold=GladeArt.color("#d7ca81");
        for(int patch=0;patch<patches;patch++){
            float x=land.minX()+.14f+random.nextFloat()*(land.width()-.28f);
            float z=land.minZ()+.14f+random.nextFloat()*(land.depth()-.28f);
            float scale=.48f+random.nextFloat()*.63f;
            float direction=random.nextFloat()*6.283185f;
            for(int blade=0;blade<3;blade++){
                int index=patch*3+blade;
                float angle=direction+blade*2.0944f;
                float dx=(float)Math.cos(angle),dz=(float)Math.sin(angle);
                float bx=x+dx*.065f,bz=z+dz*.065f;
                float y=scene.terrain().height(bx,bz)+.016f;
                float h=(.12f+random.nextFloat()*.21f)*scale,w=(.017f+random.nextFloat()*.015f)*scale;
                var tip=green.clone().interpolateLocal(gold,random.nextFloat()*.6f);
                for(int v=0;v<5;v++){
                    int vertex=index*5+v;
                    float side=v==0?-w:v==1?w:v==2?-w*.60f:v==3?w*.60f:0;
                    float height=v<2?0:v<4?h*.54f:h;
                    float bend=v<2?0:v<4?h*.13f:h*.48f;
                    int p=vertex*3,c=vertex*4;
                    positions[p]=bx-land.centerX()+dx*bend-dz*side;positions[p+1]=y+height;
                    positions[p+2]=bz-land.centerZ()+dz*bend+dx*side;
                    normals[p]=dx*.22f;normals[p+1]=.95f;normals[p+2]=dz*.22f;
                    float t=v<2?0:v<4?.65f:1;
                    colors[c]=roots.r+(tip.r-roots.r)*t;colors[c+1]=roots.g+(tip.g-roots.g)*t;
                    colors[c+2]=roots.b+(tip.b-roots.b)*t;colors[c+3]=1;
                }
                int a=index*5,i=index*9;
                indices[i]=a;indices[i+1]=a+1;indices[i+2]=a+2;
                indices[i+3]=a+2;indices[i+4]=a+1;indices[i+5]=a+3;
                indices[i+6]=a+2;indices[i+7]=a+3;indices[i+8]=a+4;
            }
        }
        var mesh=new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position,3,positions);mesh.setBuffer(VertexBuffer.Type.Normal,3,normals);
        mesh.setBuffer(VertexBuffer.Type.Color,4,colors);mesh.setBuffer(VertexBuffer.Type.Index,3,indices);mesh.updateBound();
        var result=new Geometry("Cỏ",mesh);
        result.setUserData("indicesPerBlade",9);
        var material=art.material("#ffffff").clone();
        material.setBoolean("UseVertexColor",true);
        material.setColor("Specular",ColorRGBA.Black);
        material.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        result.setMaterial(material);
        result.setShadowMode(com.jme3.renderer.queue.RenderQueue.ShadowMode.Receive);
        return result;
    }

    private static Texture2D texture(){
        int size=256;
        var image=new BufferedImage(size,size,BufferedImage.TYPE_INT_RGB);
        var graphics=image.createGraphics();
        graphics.setColor(new Color(238,239,224));graphics.fillRect(0,0,size,size);
        var random=new Random(82);
        for(int i=0;i<3900;i++){
            int x=random.nextInt(size),y=random.nextInt(size),length=2+random.nextInt(7);
            int value=174+random.nextInt(82);
            graphics.setColor(new Color(value,Math.min(255,value+3),Math.max(0,value-12)));
            for(int dx=-size;dx<=size;dx+=size)for(int dy=-size;dy<=size;dy+=size)
                graphics.drawLine(x+dx,y+dy,x+dx+length/2,y+dy-length);
        }
        graphics.dispose();
        var data=new AWTLoader().load(image,true);MipMapGenerator.generateMipMaps(data);
        var result=new Texture2D(data);
        result.setWrap(Texture.WrapMode.Repeat);result.setMagFilter(Texture.MagFilter.Bilinear);
        result.setMinFilter(Texture.MinFilter.Trilinear);result.setAnisotropicFilter(4);
        return result;
    }
}

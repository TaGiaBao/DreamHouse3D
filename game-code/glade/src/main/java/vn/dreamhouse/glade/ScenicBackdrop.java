package vn.dreamhouse.glade;

import com.jme3.asset.AssetManager;
import com.jme3.material.*;
import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.scene.shape.Sphere;
import java.nio.FloatBuffer;

/** Noninteractive distant scenery. Never inserted into the editable/pickable content node. */
final class ScenicBackdrop {
    final Node node=new Node("Chân trời thung lũng");
    private final Geometry sky;
    private final Material skyMaterial;
    private final AssetManager assets;
    private ColorRGBA lastColor;
    ScenicBackdrop(AssetManager assets){
        this.assets=assets;
        sky=new Geometry("Bầu trời chuyển sắc",new Sphere(24,48,1400));
        skyMaterial=new Material(assets,"Common/MatDefs/Misc/Unshaded.j3md");skyMaterial.setBoolean("VertexColor",true);
        skyMaterial.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Front);
        skyMaterial.getAdditionalRenderState().setDepthWrite(false);sky.setMaterial(skyMaterial);
        sky.setQueueBucket(RenderQueue.Bucket.Sky);sky.setCullHint(Spatial.CullHint.Never);sky.setShadowMode(RenderQueue.ShadowMode.Off);
        node.attachChild(sky);colorSky(new ColorRGBA(.79f,.85f,.84f,1));
    }
    void colorSky(ColorRGBA ambient){
        var mesh=sky.getMesh();var p=(FloatBuffer)mesh.getBuffer(VertexBuffer.Type.Position).getData();var c=new float[mesh.getVertexCount()*4];
        float night=FastMath.clamp((ambient.r-.29f)/.5f,0,1);
        for(int i=0;i<mesh.getVertexCount();i++){
            float y=p.get(i*3+1)/1400,t=FastMath.clamp(y*1.6f,0,1);
            var horizon=ambient.clone().interpolateLocal(new ColorRGBA(.96f,.86f,.69f,1),.40f*night);
            var top=new ColorRGBA(ambient.r*.72f,ambient.g*.86f,ambient.b*1.03f,1);var color=horizon.interpolateLocal(top,t);
            float glow=(float)Math.pow(Math.max(0,(-p.get(i*3)-p.get(i*3+2)*.5f)/1600),12)*.20f*night;
            c[i*4]=color.r+glow;c[i*4+1]=color.g+glow*.6f;c[i*4+2]=color.b;c[i*4+3]=1;
        }mesh.setBuffer(VertexBuffer.Type.Color,4,c);
    }
    void syncColor(ColorRGBA color){if(lastColor==null || !lastColor.equals(color)){colorSky(color);lastColor=color.clone();for(var child:node.getChildren())if(child instanceof Geometry g && g!=sky && g.getMaterial().getMaterialDef().getMaterialParam("Color")!=null)g.getMaterial().setColor("Color",new ColorRGBA(.65f+color.r*.35f,.65f+color.g*.35f,.65f+color.b*.35f,1));}}
    void rebuild(GladeArt art,LandBounds land){
        for(var child:new java.util.ArrayList<>(node.getChildren()))if(child!=sky)child.removeFromParent();
        float radius=Math.max(land.width(),land.depth())*.8f+24;
        var woods=new Node("Rừng ven thung lũng");
        for(int i=0;i<46;i++){
            float a=i*2.399f,dist=radius*.59f+(i%4)*3.2f,x=FastMath.sin(a)*dist+land.centerX(),z=FastMath.cos(a)*dist+land.centerZ(),h=2.7f+(i%5)*.48f;
            art.cylinder(woods,.12f,h*.65f,x,h*.325f,z,"#8d7452");
            for(int j=0;j<7;j++){float t=j*2.399f;var leaf=art.sphere(woods,h*.25f,x+FastMath.sin(t)*h*.18f,h*(.6f+j%3*.08f),z+FastMath.cos(t)*h*.18f,i%4==0?"#b4ac6a":"#839666");CanopyShading.apply(leaf);}
        }
        woods.updateGeometricState();jme3tools.optimize.GeometryBatchFactory.optimize(woods,false);woods.setShadowMode(RenderQueue.ShadowMode.Off);node.attachChild(woods);
        for(int ring=0;ring<3;ring++){
            int count=144,rows=8;float[] p=new float[(count+1)*(rows+1)*3],n=new float[p.length],c=new float[(count+1)*(rows+1)*4];int[] index=new int[count*rows*6];int cursor=0;
            for(int r=0;r<=rows;r++)for(int j=0;j<=count;j++){
                float t=r/(float)rows,a=j*FastMath.TWO_PI/count,dist=radius+ring*38+t*100;
                float ridge=(float)(Math.sin(a*5+ring)*.5+Math.sin(a*9+1.7)*.23+.8);
                float y=-4+(float)Math.sin(t*Math.PI)*(5+ring*5)*ridge-ring*1.5f;
                int k=r*(count+1)+j;p[k*3]=land.centerX()+FastMath.sin(a)*dist;p[k*3+1]=y;p[k*3+2]=land.centerZ()+FastMath.cos(a)*dist;n[k*3+1]=1;
                var col=GladeArt.color(ring==0?"#849d69":ring==1?"#8fa99b":"#9db6b7");float shade=.86f+.14f*(float)Math.sin(a*3+t*4);c[k*4]=col.r*shade;c[k*4+1]=col.g*shade;c[k*4+2]=col.b*shade;c[k*4+3]=1;
                if(r<rows && j<count){int b=k+count+1;for(int v:new int[]{k,b,k+1,k+1,b,b+1})index[cursor++]=v;}
            }
            var mesh=new Mesh();mesh.setBuffer(VertexBuffer.Type.Position,3,p);mesh.setBuffer(VertexBuffer.Type.Normal,3,n);mesh.setBuffer(VertexBuffer.Type.Color,4,c);mesh.setBuffer(VertexBuffer.Type.Index,3,index);mesh.updateBound();
            var g=new Geometry("Đồi xa "+ring,mesh);var mat=new Material(assets,"Common/MatDefs/Misc/Unshaded.j3md");mat.setBoolean("VertexColor",true);mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);g.setMaterial(mat);g.setShadowMode(RenderQueue.ShadowMode.Off);node.attachChild(g);
        }
    }
    void follow(Vector3f camera){sky.setLocalTranslation(camera);}
}

package vn.dreamhouse.glade;

import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.scene.control.AbstractControl;
import com.jme3.material.RenderState;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.*;

/** Small-scale, dimension-driven details shared by the actual player catalog. */
final class ScenicDetails {
    static void smoke(GladeArt art,Node node,float height){
        var plume=new Node("Khói ống khói");plume.setShadowMode(RenderQueue.ShadowMode.Off);
        for(int i=0;i<6;i++){
            var puff=art.sphere(plume,.13f,0,height+.1f,0,"#f2e9d6");var mat=puff.getMaterial().clone();
            mat.setTransparent(true);mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);mat.getAdditionalRenderState().setDepthWrite(false);puff.setMaterial(mat);puff.setQueueBucket(RenderQueue.Bucket.Transparent);
        }
        plume.addControl(new AbstractControl(){float time;protected void controlUpdate(float dt){time+=Math.min(dt,.1f);for(int i=0;i<6;i++){float t=(time*.20f+i/6f)%1;var p=plume.getChild(i);p.setLocalTranslation(t*.35f+FastMath.sin(time*.8f+i)*.04f,height+.1f+t*1.65f,t*.12f);p.setLocalScale(.65f+t*1.35f);var mat=((Geometry)p).getMaterial();var color=GladeArt.color("#f2e9d6");color.a=.48f*(1-t);mat.setColor("Diffuse",color);}}protected void controlRender(RenderManager rm,ViewPort vp){}});
        node.attachChild(plume);
    }
    static void canopy(GladeArt art,Node root,float w,float h,String tint){
        art.cylinder(root,Math.min(.16f,w*.075f),h*.65f,0,h*.325f,0,"#876641");
        for(int i=0;i<5;i++){
            float a=i*2.4f,x=FastMath.sin(a)*w*.25f,z=FastMath.cos(a)*w*.25f,y=h*(.62f+i%3*.085f);
            art.beam(root,new Vector3f(0,h*.34f,0),new Vector3f(x,y,z),.055f,"#876641");
        }
        // Main volume plus smaller overlapping lobes: silhouette reads as leaves, not five balls.
        for(int i=0;i<35;i++){
            float a=i*2.399963f,ring=i<7?.19f:.34f,r=w*(i<7?.29f:.115f),y=h*(.62f+(i%5)*.058f);
            var leaf=art.sphere(root,r,FastMath.sin(a)*w*ring,y,FastMath.cos(a)*w*ring,tint);
            leaf.setLocalScale(1,i<7?1.12f:.82f,1);CanopyShading.apply(leaf);
        }
    }
    static void blossoms(GladeArt art,Node node,float w,float d,float height,String tint){
        var leaves=new PlainStructure.Builder();
        var petals=new PlainStructure.Builder();
        var centers=new PlainStructure.Builder();
        for(int i=0;i<9;i++){
            float a=i*2.399f,x=FastMath.sin(a)*w*.34f,z=FastMath.cos(a)*d*.34f,h=height*(.65f+i%3*.13f);
            for(int side=0;side<5;side++){
                float t=side*FastMath.TWO_PI/5,u=(side+1)*FastMath.TWO_PI/5;
                var p=new Vector3f(x+FastMath.cos(t)*.013f,0,z+FastMath.sin(t)*.013f);
                var q=new Vector3f(x+FastMath.cos(u)*.013f,0,z+FastMath.sin(u)*.013f);
                leaves.quad(p,p.add(0,h,0),q.add(0,h,0),q);
            }
            blossomPart(leaves,x+.035f,h*.45f,z,.075f,.045f,.018f);
            for(int j=0;j<5;j++){
                float t=j*FastMath.TWO_PI/5;
                blossomPart(petals,x+FastMath.sin(t)*.066f,h,z+FastMath.cos(t)*.066f,.06f,.08f,.023f);
            }
            blossomPart(centers,x,h+.016f,z,.032f,.032f,.016f);
        }
        node.attachChild(leaves.geometry(art,"Lá và thân hoa","#6f8848"));
        node.attachChild(petals.geometry(art,"Cánh hoa",tint));
        node.attachChild(centers.geometry(art,"Nhụy hoa","#e8bf55"));
    }

    private static void blossomPart(PlainStructure.Builder mesh,float x,float y,float z,float w,float d,float h){
        var top=new Vector3f(x,y+h,z);
        var bottom=new Vector3f(x,y-h,z);
        for(int i=0;i<10;i++){
            float a=i*FastMath.TWO_PI/10,b=(i+1)*FastMath.TWO_PI/10;
            var rimA=new Vector3f(x+FastMath.sin(a)*w,y,z+FastMath.cos(a)*d);
            var rimB=new Vector3f(x+FastMath.sin(b)*w,y,z+FastMath.cos(b)*d);
            var upperA=new Vector3f(x+FastMath.sin(a)*w*.63f,y+h*.76f,z+FastMath.cos(a)*d*.63f);
            var upperB=new Vector3f(x+FastMath.sin(b)*w*.63f,y+h*.76f,z+FastMath.cos(b)*d*.63f);
            mesh.triangle(top,upperA,upperB);
            mesh.quad(upperA,rimA,rimB,upperB);
            mesh.triangle(bottom,rimB,rimA);
        }
    }
    static void house(GladeArt art,Node root,GladeDesign.Item item,GladeDesign scene){
        if(item.height()<1.5f || item.form().finish().variant()==18 || item.kind()==GladeDesign.Kind.PENTAGON)return;
        var details=new Node("Chi tiết thủ công");
        float w=item.width(),d=item.depth(),h=item.height();
        for(int side:new int[]{-1,1}){
            for(int k=0;k<Math.min(16,(int)(w/.45f));k++){
                float x=-w/2+.24f+k*.45f;if(StructureShell.coveredDetail(item,scene,new Vector3f(x,.13f,side*d/2)))continue;
                art.box(details,.42f,.22f,.16f,x,.13f,side*(d/2+.012f),k%3==0?"#b6ab8e":"#c5b99a");
            }
            float x=Math.min(w*.32f,1.55f),y=Math.min(h*.67f,1.7f),z=side*(d/2+.16f);
            if(!StructureShell.coveredDetail(item,scene,new Vector3f(x,y,z))){
                art.box(details,.21f,.30f,.18f,x,y,z,"#69543a");
                var glass=art.box(details,.145f,.22f,.19f,x,y,z,"#ffe1a0");
                var mat=glass.getMaterial().clone();mat.setColor("GlowColor",new ColorRGBA(.45f,.22f,.055f,1));glass.setMaterial(mat);
                art.box(details,.27f,.07f,.25f,x,y+.18f,z,"#79603e");
                art.box(details,.27f,.05f,.25f,x,y-.17f,z,"#79603e");
            }
        }
        details.updateGeometricState();jme3tools.optimize.GeometryBatchFactory.optimize(details,false);root.attachChild(details);
    }
}

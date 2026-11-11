package vn.dreamhouse.glade;

import com.jme3.scene.*;
import java.nio.FloatBuffer;

/** Soft baked leaf variation, independent of sunlight and without extra textures or draw calls. */
final class CanopyShading {
    static void apply(Geometry leaf){
        var mesh=leaf.getMesh();var buffer=mesh.getBuffer(VertexBuffer.Type.Position);if(buffer==null)return;
        var p=(FloatBuffer)buffer.getData();float low=Float.POSITIVE_INFINITY,high=Float.NEGATIVE_INFINITY;
        for(int i=0;i<mesh.getVertexCount();i++){float y=p.get(i*3+1);low=Math.min(low,y);high=Math.max(high,y);}
        float[] colors=new float[mesh.getVertexCount()*4];
        for(int i=0;i<mesh.getVertexCount();i++){
            float height=(p.get(i*3+1)-low)/Math.max(.001f,high-low),shade=.76f+.24f*height;
            colors[i*4]=shade;colors[i*4+1]=shade;colors[i*4+2]=shade*(.91f+.09f*(1-height));colors[i*4+3]=1;
        }
        mesh.setBuffer(VertexBuffer.Type.Color,4,colors);var material=leaf.getMaterial().clone();material.setBoolean("UseVertexColor",true);material.setFloat("Shininess",6);leaf.setMaterial(material);leaf.setUserData("softCanopy",true);
    }
}

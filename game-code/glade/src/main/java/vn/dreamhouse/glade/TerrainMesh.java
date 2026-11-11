package vn.dreamhouse.glade;

import com.jme3.math.Vector3f;
import com.jme3.scene.*;
import java.util.*;

final class TerrainMesh {
    static Node build(GladeArt art,GladeDesign design) {
        var land=design.land();var field=design.terrain();
        int nx=Math.max(8,Math.min(192,(int)Math.ceil(land.width()/.5f)));
        int nz=Math.max(6,Math.min(192,(int)Math.ceil(land.depth()/.5f)));
        float[] positions=new float[(nx+1)*(nz+1)*3],normals=new float[positions.length],colors=new float[(nx+1)*(nz+1)*4];
        for(int z=0;z<=nz;z++) for(int x=0;x<=nx;x++) {
            int i=z*(nx+1)+x;float wx=land.minX()+x*land.width()/nx,wz=land.minZ()+z*land.depth()/nz;
            float y=field.height(wx,wz);
            positions[i*3]=wx-land.centerX();positions[i*3+1]=y+.015f;positions[i*3+2]=wz-land.centerZ();
            var n=new Vector3f(field.height(wx-.15f,wz)-field.height(wx+.15f,wz),.30f,
                    field.height(wx,wz-.15f)-field.height(wx,wz+.15f)).normalizeLocal();
            normals[i*3]=n.x;normals[i*3+1]=n.y;normals[i*3+2]=n.z;
            float shade=.96f+(float)Math.sin(wx*2.4+wz*1.7)*.035f;
            colors[i*4]=shade;colors[i*4+1]=shade;colors[i*4+2]=shade*.97f;colors[i*4+3]=1;
        }
        int[] indices=new int[nx*nz*6];int cursor=0;
        for(int z=0;z<nz;z++) for(int x=0;x<nx;x++) {
            int a=z*(nx+1)+x,b=a+1,c=a+nx+1,d=c+1;
            for(int index:new int[]{a,c,b,b,c,d}) indices[cursor++]=index;
        }
        var mesh=new Mesh();mesh.setBuffer(VertexBuffer.Type.Position,3,positions);mesh.setBuffer(VertexBuffer.Type.Normal,3,normals);
        mesh.setBuffer(VertexBuffer.Type.Color,4,colors);mesh.setBuffer(VertexBuffer.Type.Index,3,indices);mesh.updateBound();
        var top=new Geometry("Mặt đất",mesh);var mat=art.material("#9db577").clone();mat.setBoolean("UseVertexColor",true);top.setMaterial(mat);
        top.setUserData("terrainSurface",true);
        var root=new Node("Địa hình");root.attachChild(top);
        float bottom=-1;
        for(float value:field.cells().values()) bottom=Math.min(bottom,value-1);
        var sideP=new ArrayList<Float>();var sideN=new ArrayList<Float>();var sideI=new ArrayList<Integer>();
        for(int side=0;side<4;side++) {
            int count=side<2?nx:nz;
            for(int step=0;step<count;step++) {
                float t=(float)step/count,s=(float)(step+1)/count;
                float ax=side<2?land.minX()+t*land.width():(side==2?land.minX():land.maxX());
                float bx=side<2?land.minX()+s*land.width():ax;
                float az=side<2?(side==0?land.minZ():land.maxZ()):land.minZ()+t*land.depth();
                float bz=side<2?az:land.minZ()+s*land.depth();
                int base=sideP.size()/3;
                float[][] points={{ax,field.height(ax,az),az},{bx,field.height(bx,bz),bz},{ax,bottom,az},{bx,bottom,bz}};
                for(var p:points) {sideP.add(p[0]-land.centerX());sideP.add(p[1]);sideP.add(p[2]-land.centerZ());
                    sideN.add(side==2?-1f:side==3?1f:0f);sideN.add(0f);sideN.add(side==0?-1f:side==1?1f:0f);}
                for(int index:new int[]{0,1,2,1,3,2}) sideI.add(base+index);
            }
        }
        var sides=new Mesh();sides.setBuffer(VertexBuffer.Type.Position,3,floats(sideP));sides.setBuffer(VertexBuffer.Type.Normal,3,floats(sideN));
        sides.setBuffer(VertexBuffer.Type.Index,3,sideI.stream().mapToInt(Integer::intValue).toArray());sides.updateBound();
        var skirt=new Geometry("Lớp đất",sides);var soil=art.material("#ac8963").clone();
        soil.getAdditionalRenderState().setFaceCullMode(com.jme3.material.RenderState.FaceCullMode.Off);skirt.setMaterial(soil);root.attachChild(skirt);
        return root;
    }
    private static float[] floats(List<Float> values) {var result=new float[values.size()];for(int i=0;i<result.length;i++) result[i]=values.get(i);return result;}
}

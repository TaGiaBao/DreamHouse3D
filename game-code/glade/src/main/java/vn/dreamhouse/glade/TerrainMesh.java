package vn.dreamhouse.glade;

import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import java.util.*;

final class TerrainMesh {
    static Node build(GladeArt art,GladeDesign design) {
        var land=design.land();var field=design.terrain();
        int nx=Math.max(8,Math.min(192,(int)Math.ceil(land.width()/.5f)));
        int nz=Math.max(6,Math.min(192,(int)Math.ceil(land.depth()/.5f)));
        float[] positions=new float[(nx+1)*(nz+1)*3],normals=new float[positions.length],colors=new float[(nx+1)*(nz+1)*4],uv=new float[(nx+1)*(nz+1)*2];
        for(int z=0;z<=nz;z++) for(int x=0;x<=nx;x++) {
            int i=z*(nx+1)+x;float wx=land.minX()+x*land.width()/nx,wz=land.minZ()+z*land.depth()/nz;
            float y=field.height(wx,wz);
            positions[i*3]=wx-land.centerX();positions[i*3+1]=y+.015f;positions[i*3+2]=wz-land.centerZ();
            var n=new Vector3f(field.height(wx-.15f,wz)-field.height(wx+.15f,wz),.30f,
                    field.height(wx,wz-.15f)-field.height(wx,wz+.15f)).normalizeLocal();
            normals[i*3]=n.x;normals[i*3+1]=n.y;normals[i*3+2]=n.z;
            var tint=MeadowArt.tint(wx,wz);
            colors[i*4]=tint.r;colors[i*4+1]=tint.g;colors[i*4+2]=tint.b;colors[i*4+3]=1;
            uv[i*2]=wx*.29f;uv[i*2+1]=wz*.29f;
        }
        int[] indices=new int[nx*nz*6];int cursor=0;
        for(int z=0;z<nz;z++) for(int x=0;x<nx;x++) {
            int a=z*(nx+1)+x,b=a+1,c=a+nx+1,d=c+1;
            for(int index:new int[]{a,c,b,b,c,d}) indices[cursor++]=index;
        }
        var mesh=new Mesh();mesh.setBuffer(VertexBuffer.Type.Position,3,positions);mesh.setBuffer(VertexBuffer.Type.Normal,3,normals);
        mesh.setBuffer(VertexBuffer.Type.Color,4,colors);mesh.setBuffer(VertexBuffer.Type.TexCoord,2,uv);mesh.setBuffer(VertexBuffer.Type.Index,3,indices);mesh.updateBound();
        var top=new Geometry("Mặt đất",mesh);var mat=MeadowArt.ground(art);top.setMaterial(mat);
        top.setUserData("terrainSurface",true);
        var root=new Node("Địa hình");root.attachChild(top);
        var edge=new ArrayList<Integer>();
        for(int x=0;x<nx;x++)edge.add(nz*(nx+1)+x);
        for(int z=nz;z>0;z--)edge.add(z*(nx+1)+nx);
        for(int x=nx;x>0;x--)edge.add(x);
        for(int z=0;z<nz;z++)edge.add(z*(nx+1));
        root.attachChild(meadow(land,edge,positions,normals,colors,mat));
        var outline=new ArrayList<Vector3f>();
        for(int i:edge)outline.add(new Vector3f(positions[i*3],positions[i*3+1],positions[i*3+2]));
        root.attachChild(boundary(art,outline,"#f8ecc7"));
        return root;
    }

    private static Geometry meadow(LandBounds land,List<Integer> edge,float[] top,float[] topNormals,float[] topColors,com.jme3.material.Material material){
        float max=Math.max(land.width(),land.depth());
        float[] rings={0,1,2.5f,5,8,12,20,32,max*1.6f+48};
        int count=edge.size();
        float[] p=new float[count*rings.length*3],n=new float[p.length],c=new float[count*rings.length*4],uv=new float[count*rings.length*2];
        int[] triangles=new int[count*(rings.length-1)*6];
        int cursor=0;
        for(int row=0;row<rings.length;row++)for(int i=0;i<count;i++){
            int from=edge.get(i),index=row*count+i,at=index*3;
            float distance=rings[row],t=Math.min(1,distance/20),fade=1-t*t*(3-2*t);
            p[at]=top[from*3]*(1+distance*2/land.width());
            p[at+1]=.015f+(top[from*3+1]-.015f)*fade;
            p[at+2]=top[from*3+2]*(1+distance*2/land.depth());
            var normal=new Vector3f(topNormals[from*3]*fade,1+(topNormals[from*3+1]-1)*fade,topNormals[from*3+2]*fade).normalizeLocal();
            n[at]=normal.x;n[at+1]=normal.y;n[at+2]=normal.z;
            float wx=p[at]+land.centerX(),wz=p[at+2]+land.centerZ();
            var tint=MeadowArt.tint(wx,wz);
            c[index*4]=topColors[from*4]*fade+tint.r*(1-fade);
            c[index*4+1]=topColors[from*4+1]*fade+tint.g*(1-fade);
            c[index*4+2]=topColors[from*4+2]*fade+tint.b*(1-fade);
            c[index*4+3]=1;
            uv[index*2]=wx*.29f;uv[index*2+1]=wz*.29f;
            if(row==rings.length-1)continue;
            int next=row*count+(i+1)%count;
            triangles[cursor++]=index;triangles[cursor++]=index+count;triangles[cursor++]=next;
            triangles[cursor++]=next;triangles[cursor++]=index+count;triangles[cursor++]=next+count;
        }
        var mesh=new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position,3,p);mesh.setBuffer(VertexBuffer.Type.Normal,3,n);
        mesh.setBuffer(VertexBuffer.Type.Color,4,c);mesh.setBuffer(VertexBuffer.Type.TexCoord,2,uv);mesh.setBuffer(VertexBuffer.Type.Index,3,triangles);mesh.updateBound();
        var result=new Geometry("Đồng cỏ ngoài khu xây",mesh);
        result.setMaterial(material);result.setShadowMode(RenderQueue.ShadowMode.Receive);
        return result;
    }

    static Geometry boundary(GladeArt art,List<Vector3f> outline,String tint){
        var builder=new PlainStructure.Builder();
        var sides=new ArrayList<Vector3f>();
        for(int i=0;i<outline.size();i++){
            var point=outline.get(i);
            var before=outline.get(Math.floorMod(i-1,outline.size()));
            var after=outline.get((i+1)%outline.size());
            var a=new Vector3f(before.z-point.z,0,point.x-before.x).normalizeLocal();
            var b=new Vector3f(point.z-after.z,0,after.x-point.x).normalizeLocal();
            sides.add(a.add(b).multLocal(.065f/(1+a.dot(b))));
        }
        for(int i=0;i<outline.size();i++){
            int j=(i+1)%outline.size();
            var start=outline.get(i).add(0,.045f,0);var end=outline.get(j).add(0,.045f,0);
            builder.quad(start.subtract(sides.get(i)),start.add(sides.get(i)),end.add(sides.get(j)),end.subtract(sides.get(j)));
        }
        var edge=builder.geometry(art,"Viền khu xây dựng",tint);
        edge.setShadowMode(RenderQueue.ShadowMode.Off);
        return edge;
    }

    static Geometry draftBoundary(GladeArt art,GladeDesign design){
        var land=design.land();
        float[][] corners={{land.minX(),land.maxZ()},{land.maxX(),land.maxZ()},{land.maxX(),land.minZ()},{land.minX(),land.minZ()}};
        var points=new ArrayList<Vector3f>();
        for(int i=0;i<corners.length;i++){
            var a=corners[i];var b=corners[(i+1)%corners.length];
            int samples=Math.clamp((int)Math.ceil(Math.hypot(a[0]-b[0],a[1]-b[1])/.5f),1,192);
            for(int j=0;j<samples;j++){
                float t=(float)j/samples,x=a[0]+(b[0]-a[0])*t,z=a[1]+(b[1]-a[1])*t;
                points.add(new Vector3f(x,design.terrain().height(x,z)+.015f,z));
            }
        }
        return boundary(art,points,"#fff1a4");
    }
}

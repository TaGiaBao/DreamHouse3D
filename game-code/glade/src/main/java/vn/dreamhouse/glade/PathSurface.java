package vn.dreamhouse.glade;

import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer.Type;
import java.util.List;

final class PathSurface {
    static final List<String> MATERIALS=List.of("dirt","concrete","brick","gravel");
    static final List<String> COLORS=List.of("#a57955","#bcb8ac","#b87157","#dfc18e","#84918d","#756358");
    static final int VARIABLE_WIDTH=4;

    static GladeDesign.Finish finish(String material,String color) {
        return new GladeDesign.Finish(material,color,"original","",VARIABLE_WIDTH);
    }

    static String defaultColor(String material) {
        return switch(material) {
            case "dirt" -> "#b48c61";
            case "concrete" -> "#c6c3b5";
            case "brick" -> "#c38369";
            case "gravel" -> "#b8b3a0";
            default -> "#c9c0a4";
        };
    }

    static boolean ribbon(GladeDesign.Item item) {
        var finish=item.form().finish();
        return finish.variant()==VARIABLE_WIDTH;
    }

    static float width(GladeDesign.Item item) {
        return ribbon(item)?item.width():1;
    }

    static void render(GladeArt art,Node root,GladeDesign.Item item,GladeDesign scene,boolean[] flat) {
        var points=item.points();
        if(points.isEmpty())return;
        float radius=width(item)/2;
        int across=Math.max(2,(int)Math.ceil(radius*8)),columns=across+1;
        var mesh=new Mesh();
        float[] position=new float[points.size()*columns*3],normal=new float[position.length],uv=new float[points.size()*columns*2];
        int[] triangles=new int[Math.max(0,points.size()-1)*across*6];
        int count=0;
        float travel=0;
        for(int i=0;i<points.size();i++){
            var point=points.get(i);
            var before=points.get(Math.max(0,i-1));
            var after=points.get(Math.min(points.size()-1,i+1));
            float dx=after.x()-before.x(),dz=after.z()-before.z(),length=(float)Math.hypot(dx,dz);
            if(length<.001f){dx=after.x()-point.x();dz=after.z()-point.z();length=(float)Math.hypot(dx,dz);}
            if(length<.001f){dz=1;length=1;}
            if(i>0)travel+=point.distance(before);
            for(int j=0;j<columns;j++){
                int vertex=i*columns+j;
                float offset=radius*(2f*j/across-1);
                write(scene,point.x()+dz/length*offset,point.z()-dx/length*offset,vertex,position,normal);
                uv[vertex*2]=(offset+radius)*2;uv[vertex*2+1]=travel*2;
                if(i==0 || j==0 || !flat[i] || !flat[i-1])continue;
                triangles[count++]=vertex-columns-1;triangles[count++]=vertex-1;triangles[count++]=vertex;
                triangles[count++]=vertex-columns-1;triangles[count++]=vertex;triangles[count++]=vertex-columns;
            }
        }
        if(count>0)add(art,root,item,mesh,position,normal,uv,java.util.Arrays.copyOf(triangles,count));
        if(flat[0])cap(art,root,item,scene,points.getFirst(),points.size()==1?null:points.get(1),radius,0,true);
        if(points.size()>1 && flat[flat.length-1])cap(art,root,item,scene,points.getLast(),points.get(points.size()-2),radius,travel,false);
    }

    private static void cap(GladeArt art,Node root,GladeDesign.Item item,GladeDesign scene,GladeDesign.Point point,GladeDesign.Point inner,float radius,float travel,boolean first) {
        int edges=16;
        float angle=inner==null?0:(float)Math.atan2(point.z()-inner.z(),point.x()-inner.x());
        float span=inner==null?(float)(2*Math.PI):(float)Math.PI;
        float[] pos=new float[(edges+2)*3],normals=new float[pos.length],uv=new float[(edges+2)*2];
        int[] index=new int[edges*3];
        write(scene,point.x(),point.z(),0,pos,normals);uv[0]=radius*2;uv[1]=travel*2;
        float tx=(float)Math.cos(angle)*(first?-1:1),tz=(float)Math.sin(angle)*(first?-1:1);
        for(int i=0;i<=edges;i++){
            float direction=angle-span/2+span*i/edges;
            float x=(float)Math.cos(direction)*radius,z=(float)Math.sin(direction)*radius;
            write(scene,point.x()+x,point.z()+z,i+1,pos,normals);
            uv[(i+1)*2]=(radius+x*tz-z*tx)*2;uv[(i+1)*2+1]=(travel+x*tx+z*tz)*2;
            if(i<edges){index[i*3]=0;index[i*3+1]=i+2;index[i*3+2]=i+1;}
        }
        add(art,root,item,new Mesh(),pos,normals,uv,index);
    }

    private static void write(GladeDesign scene,float x,float z,int vertex,float[] pos,float[] normals) {
        x=Math.max(scene.land().minX(),Math.min(scene.land().maxX(),x));
        z=Math.max(scene.land().minZ(),Math.min(scene.land().maxZ(),z));
        var field=scene.terrain();
        int at=vertex*3;pos[at]=x;pos[at+1]=field.height(x,z)+.035f;pos[at+2]=z;
        var n=new Vector3f(field.height(x-.12f,z)-field.height(x+.12f,z),.24f,
                field.height(x,z-.12f)-field.height(x,z+.12f)).normalizeLocal();
        normals[at]=n.x;normals[at+1]=n.y;normals[at+2]=n.z;
    }

    private static void add(GladeArt art,Node root,GladeDesign.Item item,Mesh mesh,float[] pos,float[] normal,float[] uv,int[] index) {
        mesh.setBuffer(Type.Position,3,pos);mesh.setBuffer(Type.Normal,3,normal);
        mesh.setBuffer(Type.TexCoord,2,uv);mesh.setBuffer(Type.Index,3,index);mesh.updateBound();
        var surface=new Geometry("Mặt đường",mesh);
        surface.move(0,(item.id()%31)*.0006f,0);
        surface.setMaterial(art.material(defaultColor(item.form().finish().material())));
        surface.setUserData("worldHeight",true);surface.setUserData("wallSurface",true);
        root.attachChild(surface);
    }
}

package vn.dreamhouse.glade;

import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.bounding.BoundingBox;
import java.nio.FloatBuffer;
import java.util.*;

/** Subtract hidden surfaces at real building intersections, preserving each editable item ID. */
final class ArchitectureJoin {
    private record Plane(Vector3f n,float d){float distance(Vector3f p){return n.dot(p)+d;}}
    private record Vertex(Vector3f p,Vector3f n,Vector2f uv,ColorRGBA color){
        Vertex mix(Vertex b,float t){return new Vertex(p.clone().interpolateLocal(b.p,t),n.clone().interpolateLocal(b.n,t).normalizeLocal(),uv.clone().interpolateLocal(b.uv,t),color.clone().interpolateLocal(b.color,t));}
    }
    private record Volume(List<Plane> planes,BoundingBox bounds){}
    static void apply(Node root,GladeDesign.Item host,GladeDesign scene){
        if(!host.kind().building() || host.kind()==GladeDesign.Kind.PAVILION)return;
        var volumes=new ArrayList<Volume>();
        for(var other:scene.items())if(other.id()!=host.id() && other.kind().building() && other.kind()!=GladeDesign.Kind.PAVILION){
            float reachX=host.halfWidth()+other.halfWidth()+.8f,reachZ=host.halfDepth()+other.halfDepth()+.8f;
            if(Math.abs(host.center().x()-other.center().x())>reachX || Math.abs(host.center().z()-other.center().z())>reachZ)continue;
            volumes.add(body(other,scene));
            if(scene.roofRise(other)>.02f && other.form().finish().variant()!=18 && !scene.customRoof(other.id()))volumes.add(roof(other,scene));
        }
        if(volumes.isEmpty())return;root.updateGeometricState();
        var geometry=new ArrayList<Geometry>();root.depthFirstTraversal(s->{if(s instanceof Geometry g && g.getMesh().getMode()==Mesh.Mode.Triangles && !Boolean.TRUE.equals(g.getUserData("structuralShell")))geometry.add(g);});
        for(var geo:geometry){var active=volumes.stream().filter(v->geo.getWorldBound()!=null && geo.getWorldBound().intersects(v.bounds)).toList();if(!active.isEmpty())clip(geo,active);}
    }
    private static Plane localPlane(GladeDesign.Item item,GladeDesign scene,Vector3f n,float d){
        var placement=scene.placement(item);var q=new Quaternion().fromAngleAxis(placement.rotation()*FastMath.DEG_TO_RAD,Vector3f.UNIT_Y);var normal=q.mult(n);
        return new Plane(normal,d-normal.dot(new Vector3f(placement.center().x(),placement.elevation(),placement.center().z())));
    }
    private static Volume volume(GladeDesign.Item item,GladeDesign scene,List<Vector4f> local,float top){
        var p=new ArrayList<Plane>();for(var v:local)p.add(localPlane(item,scene,new Vector3f(v.x,v.y,v.z),v.w));
        var at=scene.placement(item);return new Volume(p,new BoundingBox(new Vector3f(at.center().x(),at.elevation()+top/2,at.center().z()),item.halfWidth()+.35f,top/2+.02f,item.halfDepth()+.35f));
    }
    private static Volume body(GladeDesign.Item item,GladeDesign scene){
        var p=new ArrayList<Vector4f>();p.add(new Vector4f(0,-1,0,-.003f));p.add(new Vector4f(0,1,0,-item.height()-.003f));
        if(item.kind()==GladeDesign.Kind.TOWER)for(int i=0;i<48;i++){float a=(i+.5f)*FastMath.TWO_PI/48;p.add(new Vector4f(FastMath.cos(a)/(item.width()/2),0,FastMath.sin(a)/(item.depth()/2),-1.002f));}
        else {p.add(new Vector4f(1,0,0,-item.width()/2-.003f));p.add(new Vector4f(-1,0,0,-item.width()/2-.003f));p.add(new Vector4f(0,0,1,-item.depth()/2-.003f));p.add(new Vector4f(0,0,-1,-item.depth()/2-.003f));}
        return volume(item,scene,p,item.height());
    }
    private static Volume roof(GladeDesign.Item item,GladeDesign scene){
        float h=item.height(),rise=scene.roofRise(item),w=item.width()/2+.32f,d=item.depth()/2+.32f;var p=new ArrayList<Vector4f>();
        p.add(new Vector4f(0,-1,0,h-.003f));
        if(item.kind()==GladeDesign.Kind.TOWER){for(int i=0;i<48;i++){float a=(i+.5f)*FastMath.TWO_PI/48;p.add(new Vector4f(FastMath.cos(a)/(item.width()/2+.26f),1/rise,FastMath.sin(a)/(item.depth()/2+.26f),-1-h/rise));}}
        else {p.add(new Vector4f(0,0,1,-d));p.add(new Vector4f(0,0,-1,-d));p.add(new Vector4f(rise/w,1,0,-h-rise-.003f));p.add(new Vector4f(-rise/w,1,0,-h-rise-.003f));}
        return volume(item,scene,p,h+rise);
    }
    private static void clip(Geometry geo,List<Volume> volumes){
        var mesh=geo.getMesh();var position=(FloatBuffer)mesh.getBuffer(VertexBuffer.Type.Position).getData();var nb=mesh.getBuffer(VertexBuffer.Type.Normal);var uvb=mesh.getBuffer(VertexBuffer.Type.TexCoord);var cb=mesh.getBuffer(VertexBuffer.Type.Color);
        var normals=nb==null?null:(FloatBuffer)nb.getData();var uv=uvb==null?null:(FloatBuffer)uvb.getData();var color=cb==null?null:(FloatBuffer)cb.getData();var indices=mesh.getIndicesAsList();var out=new ArrayList<Vertex>();boolean changed=false;
        for(int i=0;i<indices.size();i+=3){var poly=new ArrayList<Vertex>();for(int j=0;j<3;j++){int k=indices.get(i+j);var pos=geo.localToWorld(new Vector3f(position.get(k*3),position.get(k*3+1),position.get(k*3+2)),null);poly.add(new Vertex(pos,normals==null?Vector3f.UNIT_Y.clone():new Vector3f(normals.get(k*3),normals.get(k*3+1),normals.get(k*3+2)),uv==null?new Vector2f():new Vector2f(uv.get(k*2),uv.get(k*2+1)),color==null?ColorRGBA.White.clone():new ColorRGBA(color.get(k*4),color.get(k*4+1),color.get(k*4+2),color.get(k*4+3))));}
            List<List<Vertex>> pieces=new ArrayList<>();pieces.add(poly);
            for(var volume:volumes){var next=new ArrayList<List<Vertex>>();for(var piece:pieces){boolean outside=false;for(var plane:volume.planes){if(piece.stream().allMatch(v->plane.distance(v.p)>.00001f)){outside=true;break;}}if(outside){next.add(piece);continue;}
                var inside=piece;for(var plane:volume.planes){if(inside.isEmpty())break;var yes=new ArrayList<Vertex>();var no=new ArrayList<Vertex>();for(int k=0;k<inside.size();k++){var a=inside.get(k);var b=inside.get((k+1)%inside.size());float da=plane.distance(a.p),db=plane.distance(b.p);if(da<=0)yes.add(a);else no.add(a);if((da<0)!=(db<0) && Math.abs(da-db)>.0000001f){var v=a.mix(b,da/(da-db));yes.add(v);no.add(v);}}if(no.size()>=3)next.add(no);inside=yes;}changed=true;
            }pieces=next;}
            for(var piece:pieces)for(int j=1;j+1<piece.size();j++){out.add(piece.getFirst());out.add(piece.get(j));out.add(piece.get(j+1));}
        }
        if(!changed)return;if(out.isEmpty()){geo.setCullHint(Spatial.CullHint.Always);return;}
        float[] p=new float[out.size()*3],n=new float[p.length],u=new float[out.size()*2],c=new float[out.size()*4];
        for(int i=0;i<out.size();i++){var v=out.get(i);var local=geo.worldToLocal(v.p,null);p[i*3]=local.x;p[i*3+1]=local.y;p[i*3+2]=local.z;n[i*3]=v.n.x;n[i*3+1]=v.n.y;n[i*3+2]=v.n.z;u[i*2]=v.uv.x;u[i*2+1]=v.uv.y;c[i*4]=v.color.r;c[i*4+1]=v.color.g;c[i*4+2]=v.color.b;c[i*4+3]=v.color.a;}
        var clipped=new Mesh();clipped.setBuffer(VertexBuffer.Type.Position,3,p);if(normals!=null)clipped.setBuffer(VertexBuffer.Type.Normal,3,n);if(uv!=null)clipped.setBuffer(VertexBuffer.Type.TexCoord,2,u);if(color!=null)clipped.setBuffer(VertexBuffer.Type.Color,4,c);clipped.updateBound();geo.setMesh(clipped);geo.setUserData("joinedSurface",true);
    }
}

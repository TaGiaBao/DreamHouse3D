package vn.dreamhouse.glade;

import com.jme3.math.*;
import com.jme3.scene.*;
import java.util.*;

/** A shared roof envelope for touching rectangular blocks at the same storey. Blocks retain their IDs. */
final class BlockFusion {
    private final GladeDesign design;
    private final Map<Long,List<GladeDesign.Item>> groups=new HashMap<>();
    BlockFusion(GladeDesign design) {
        this.design=design;
        var roofs=design.items().stream().filter(item->item.kind().roofHost() && item.kind()!=GladeDesign.Kind.PAVILION
                && item.form().roofLeft()==0 && item.form().roofRight()==0 && item.form().roofFront()==0 && item.form().roofBack()==0 && design.roofRise(item)>.02f && !design.customRoof(item.id())).toList();
        int[] parents=new int[roofs.size()];for(int i=0;i<parents.length;i++) parents[i]=i;
        var bins=new HashMap<String,List<Integer>>();var checked=new HashSet<Long>();
        for(int i=0;i<roofs.size();i++) {
            var a=roofs.get(i);
            for(int x=(int)Math.floor((a.center().x()-a.halfWidth()-.1f)/8);x<=Math.floor((a.center().x()+a.halfWidth()+.1f)/8);x++)
                for(int z=(int)Math.floor((a.center().z()-a.halfDepth()-.1f)/8);z<=Math.floor((a.center().z()+a.halfDepth()+.1f)/8);z++) {
                    var bucket=bins.computeIfAbsent(x+":"+z,key->new ArrayList<>());
                    for(int j:bucket) if(checked.add(((long)i<<32)|j) && touches(a,roofs.get(j))) parents[find(parents,i)]=find(parents,j);
                    bucket.add(i);
                }
        }
        var components=new HashMap<Integer,List<GladeDesign.Item>>();
        for(int i=0;i<roofs.size();i++) components.computeIfAbsent(find(parents,i),key->new ArrayList<>()).add(roofs.get(i));
        for(var group:components.values()) if(group.size()>1) for(var item:group) groups.put(item.id(),group);
    }
    private static int find(int[] parents,int x) {while(parents[x]!=x) {parents[x]=parents[parents[x]];x=parents[x];}return x;}
    private boolean touches(GladeDesign.Item a,GladeDesign.Item b) {
        if(Math.abs(design.top(a)-design.top(b))>.08f) return false;
        // Test actual rotated footprints; AABBs alone join separated rotated houses.
        for(float angle:new float[]{a.rotation(),a.rotation()+90,b.rotation(),b.rotation()+90}){
            double t=Math.toRadians(angle);float ax=(float)Math.cos(t),az=-(float)Math.sin(t);
            float ra=projection(a,ax,az),rb=projection(b,ax,az);
            float separation=Math.abs((a.center().x()-b.center().x())*ax+(a.center().z()-b.center().z())*az);
            if(ra+rb-separation<-.06f)return false;
        }
        return true;
    }
    private static float projection(GladeDesign.Item item,float x,float z){double t=Math.toRadians(item.rotation());return (float)(Math.abs(x*Math.cos(t)-z*Math.sin(t))*item.width()/2+Math.abs(x*Math.sin(t)+z*Math.cos(t))*item.depth()/2);}
    float surfaceHeight(GladeDesign.Item item,float lx,float lz){
        var group=groups.get(item.id());double angle=Math.toRadians(item.rotation());float x=item.center().x()+(float)(Math.cos(angle)*lx+Math.sin(angle)*lz),z=item.center().z()+(float)(-Math.sin(angle)*lx+Math.cos(angle)*lz);
        return (group==null?height(item,x,z):envelope(group,x,z,height(item,x,z)))-design.placement(item).elevation();
    }
    boolean joined(long id) {return groups.containsKey(id);}
    private float height(GladeDesign.Item item,float x,float z) {
        double angle=Math.toRadians(item.rotation());float dx=x-item.center().x(),dz=z-item.center().z();
        float lx=(float)(Math.cos(angle)*dx-Math.sin(angle)*dz),lz=(float)(Math.sin(angle)*dx+Math.cos(angle)*dz);
        float span=item.width()/2+.20f;
        if(Math.abs(lx)>span || Math.abs(lz)>item.depth()/2+.20f) return Float.NEGATIVE_INFINITY;
        return design.top(item)+design.roofRise(item)*Math.max(0,1-Math.abs(lx)/span);
    }
    private GladeDesign.Item owner(List<GladeDesign.Item> group,float x,float z) {
        GladeDesign.Item owner=null;float y=Float.NEGATIVE_INFINITY;
        for(var item:group) {float h=height(item,x,z);if(h>y+.0001f || h==y && owner!=null && item.id()<owner.id()) {owner=item;y=h;}}
        return owner;
    }
    private float envelope(List<GladeDesign.Item> group,float x,float z,float fallback) {
        float y=Float.NEGATIVE_INFINITY;for(var item:group)y=Math.max(y,height(item,x,z));if(Float.isFinite(y))return y;
        // Every shared lattice vertex must get the same height, including just outside the eaves.
        // A per-cell fallback gives adjacent triangles different endpoints and tears the roof open.
        float closest=Float.POSITIVE_INFINITY,result=fallback;
        var point=new GladeDesign.Point(x,z);
        for(var item:group){float lx=item.localX(point),lz=item.localZ(point),span=item.width()/2+.20f,depth=item.depth()/2+.20f;
            float cx=FastMath.clamp(lx,-span,span),cz=FastMath.clamp(lz,-depth,depth),distance=(lx-cx)*(lx-cx)+(lz-cz)*(lz-cz);
            float level=design.top(item)+design.roofRise(item)*Math.max(0,1-Math.abs(cx)/span);
            if(distance<closest-.000001f){closest=distance;result=level;}else if(Math.abs(distance-closest)<.000001f)result=Math.max(result,level);
        }
        return result;
    }
    Node mesh(GladeArt art,GladeDesign.Item item) {
        var group=groups.get(item.id());if(group==null) return null;
        var result=art.roofGroup(new Node());result.removeFromParent();
        var vertices=new ArrayList<Float>();var normals=new ArrayList<Float>();var indices=new ArrayList<Integer>();
        var gableVertices=new ArrayList<Float>();var gableNormals=new ArrayList<Float>();var gableIndices=new ArrayList<Integer>();
        float step=Math.max(.22f,Math.max(item.width(),item.depth())/100);
        // A common lattice avoids cracks where two roofs meet.
        step=Math.max(.22f,group.stream().map(roof->Math.max(roof.width(),roof.depth())).max(Float::compare).orElse(1f)/100);
        float base=design.placement(item).elevation();
        double angle=Math.toRadians(item.rotation());
        for(int iz=(int)Math.floor((item.center().z()-item.halfDepth()-.3f)/step);iz<=Math.ceil((item.center().z()+item.halfDepth()+.3f)/step);iz++)
            for(int ix=(int)Math.floor((item.center().x()-item.halfWidth()-.3f)/step);ix<=Math.ceil((item.center().x()+item.halfWidth()+.3f)/step);ix++) {
                float x=ix*step,z=iz*step;
                if(owner(group,x+step/2,z+step/2)!=item) continue;
                float fallback=height(item,x+step/2,z+step/2);
                var a=new Vector3f(x,envelope(group,x,z,fallback),z);
                var b=new Vector3f(x+step,envelope(group,x+step,z,fallback),z);
                var c=new Vector3f(x,envelope(group,x,z+step,fallback),z+step);
                var d=new Vector3f(x+step,envelope(group,x+step,z+step,fallback),z+step);
                quad(item,base,angle,a,c,b,d,vertices,normals,indices);
                for(int side=0;side<4;side++) {
                    float nx=x+step/2+(side==0?-step:side==1?step:0),nz=z+step/2+(side==2?-step:side==3?step:0);
                    if(owner(group,nx,nz)!=null) continue;
                    Vector3f p=side==0?a:side==1?d:side==2?b:c;
                    Vector3f q=side==0?c:side==1?b:side==2?a:d;
                    // Strip order must face outward. The former p,q order pointed all gables inward.
                    quad(item,base,angle,q,p,new Vector3f(q.x,design.top(item),q.z),new Vector3f(p.x,design.top(item),p.z),gableVertices,gableNormals,gableIndices);
                }
            }
        if(vertices.isEmpty()) return result;
        var mesh=new Mesh();mesh.setBuffer(VertexBuffer.Type.Position,3,floats(vertices));mesh.setBuffer(VertexBuffer.Type.Normal,3,floats(normals));
        float[] colors=new float[vertices.size()/3*4];
        boolean slate=item.kind()==GladeDesign.Kind.BRICK_HOUSE || item.kind()==GladeDesign.Kind.LOG_CABIN;
        for(int i=0;i<vertices.size()/3;i++) {
            boolean gable=Math.abs(normals.get(i*3+1))<.12f;
            var tint=GladeArt.color(gable?"#e2cca5":slate?"#7e969e":"#c58254");
            int tile=i/4;float shade=gable?1f:.87f+Math.floorMod(tile*37+13,5)*.04f;
            colors[i*4]=tint.r*shade;colors[i*4+1]=tint.g*shade;colors[i*4+2]=tint.b*shade;colors[i*4+3]=1;
        }
        mesh.setBuffer(VertexBuffer.Type.Color,4,colors);
        mesh.setBuffer(VertexBuffer.Type.Index,3,indices.stream().mapToInt(Integer::intValue).toArray());mesh.updateBound();
        var geometry=new Geometry("Mái ghép liên tục",mesh);
        var mat=art.material("#ffffff").clone();mat.setBoolean("UseVertexColor",true);
        mat.getAdditionalRenderState().setFaceCullMode(com.jme3.material.RenderState.FaceCullMode.Off);geometry.setMaterial(mat);geometry.setUserData("roofSurface",true);result.attachChild(geometry);
        if(!gableVertices.isEmpty()){
            var walls=new Mesh();walls.setBuffer(VertexBuffer.Type.Position,3,floats(gableVertices));walls.setBuffer(VertexBuffer.Type.Normal,3,floats(gableNormals));walls.setBuffer(VertexBuffer.Type.Index,3,gableIndices.stream().mapToInt(Integer::intValue).toArray());walls.updateBound();
            var gable=new Geometry("Tường đầu hồi khép kín",walls);gable.setMaterial(art.material("#eadbb4"));gable.setUserData("wallSurface",true);gable.setUserData("gableClosure",true);result.attachChild(gable);
        }
        return result;
    }
    private static void quad(GladeDesign.Item item,float base,double angle,Vector3f a,Vector3f b,Vector3f c,Vector3f d,
            List<Float> vertices,List<Float> normals,List<Integer> indices) {
        int index=vertices.size()/3;var normal=b.subtract(a).cross(c.subtract(a)).normalizeLocal();
        for(var p:new Vector3f[]{a,b,c,d}) {
            float dx=p.x-item.center().x(),dz=p.z-item.center().z();
            vertices.add((float)(Math.cos(angle)*dx-Math.sin(angle)*dz));vertices.add(p.y-base);vertices.add((float)(Math.sin(angle)*dx+Math.cos(angle)*dz));
            normals.add((float)(Math.cos(angle)*normal.x-Math.sin(angle)*normal.z));normals.add(normal.y);normals.add((float)(Math.sin(angle)*normal.x+Math.cos(angle)*normal.z));
        }
        for(int offset:new int[]{0,1,2,1,3,2}) indices.add(index+offset);
    }
    private static float[] floats(List<Float> values) {var result=new float[values.size()];for(int i=0;i<result.length;i++) result[i]=values.get(i);return result;}
}

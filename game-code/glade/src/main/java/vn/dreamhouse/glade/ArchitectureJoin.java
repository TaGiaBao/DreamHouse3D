package vn.dreamhouse.glade;

import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.bounding.BoundingBox;
import java.nio.FloatBuffer;
import java.util.*;

final class ArchitectureJoin {
    private record Plane(Vector3f n,float d){float distance(Vector3f p){return n.dot(p)+d;}}
    private record Vertex(Vector3f p,Vector3f n,Vector2f uv,ColorRGBA color){
        Vertex mix(Vertex b,float t){return new Vertex(p.clone().interpolateLocal(b.p,t),n.clone().interpolateLocal(b.n,t).normalizeLocal(),uv.clone().interpolateLocal(b.uv,t),color.clone().interpolateLocal(b.color,t));}
    }
    private record Volume(List<Plane> planes,BoundingBox bounds){}
    static void apply(Node root,GladeDesign.Item host,GladeDesign scene){
        boolean shaped=RoofProfiles.shaped(host.kind());
        if(!shaped && (!host.kind().building() || host.kind()==GladeDesign.Kind.PAVILION))return;
        long owner=shaped?host.mount().hostId():host.id();
        root.updateGeometricState();
        var bounds=root.getWorldBound();
        if(bounds==null)return;
        var shapedRoofs=new HashMap<Long,GladeDesign.Item>();
        for(var item:scene.items())if(RoofProfiles.shaped(item.kind()))shapedRoofs.put(item.mount().hostId(),item);
        var volumes=new ArrayList<Volume>();
        for(var other:scene.items())if(other.id()!=owner && other.kind().building() && other.kind()!=GladeDesign.Kind.PAVILION){
            float margin=.8f+other.width()*.15f+other.depth()*.15f;
            var form=other.form();
            margin+=form.roofFront()+form.roofBack()+form.roofLeft()+form.roofRight();
            var attached=shapedRoofs.get(other.id());
            if(attached!=null)margin+=(float)Math.hypot(attached.mount().u()*other.width(),attached.mount().v()*other.depth());
            float xExtent=other.halfWidth()+margin,zExtent=other.halfDepth()+margin;
            if(bounds instanceof BoundingBox box && (Math.abs(box.getCenter().x-other.center().x())>box.getXExtent()+xExtent
                    || Math.abs(box.getCenter().z-other.center().z())>box.getZExtent()+zExtent))continue;
            var placement=scene.placement(other);
            boolean hillside=scene.hillside(other) && attached==null && !scene.customRoof(other.id()) && scene.roofRise(other)>.02f;
            var anchor=new BoundingBox(new Vector3f(placement.center().x(),placement.elevation()+other.height()/2,placement.center().z()),
                    xExtent,other.height()/2+.003f,zExtent);
            if(bounds.intersects(anchor))volumes.add(body(other,scene));
            if(hillside || attached!=null && !scene.hasUpper(other.id()) && other.height()>.12f){
                var at=hillside?placement:scene.placement(attached);
                boolean priority=(hillside?other.id():attached.id())<host.id();
                if(hillside || attached.kind()==GladeDesign.Kind.ROOF_CURVED){
                    float height=hillside?scene.roofRise(other):attached.height();
                    float base=hillside?other.height():0;
                    for(int band=0;band<RoofProfiles.CURVED_BANDS;band++){
                        var candidate=profile(other,GladeDesign.Kind.ROOF_CURVED,height,at,base,priority,band);
                        if(bounds.intersects(candidate.bounds()))volumes.add(candidate);
                    }
                }else{
                    var candidate=profile(other,attached.kind(),attached.height(),at,0,priority);
                    if(bounds.intersects(candidate.bounds()))volumes.add(candidate);
                }
            }else if(scene.roofRise(other)>.02f && !scene.customRoof(other.id())){
                if(other.kind()==GladeDesign.Kind.PENTAGON){
                    var candidate=profile(other,GladeDesign.Kind.ROOF_PENTAGON,scene.roofRise(other),placement,other.height(),other.id()<host.id());
                    if(bounds.intersects(candidate.bounds()))volumes.add(candidate);
                }else if(other.form().finish().variant()!=18)volumes.add(roof(other,scene));
            }
        }
        if(volumes.isEmpty())return;
        var geometry=new ArrayList<Geometry>();root.depthFirstTraversal(s->{if(s instanceof Geometry g && g.getMesh().getMode()==Mesh.Mode.Triangles && !Boolean.TRUE.equals(g.getUserData("structuralShell")))geometry.add(g);});
        for(var geo:geometry){var active=volumes.stream().filter(v->geo.getWorldBound()!=null && geo.getWorldBound().intersects(v.bounds)).toList();if(!active.isEmpty())clip(geo,active);}
    }
    private static List<Plane> worldPlanes(GladeDesign.Placement placement,List<Vector4f> local){
        var q=new Quaternion().fromAngleAxis(placement.rotation()*FastMath.DEG_TO_RAD,Vector3f.UNIT_Y);
        var origin=new Vector3f(placement.center().x(),placement.elevation(),placement.center().z());
        return local.stream().map(v->{var normal=q.mult(new Vector3f(v.x,v.y,v.z));return new Plane(normal,v.w-normal.dot(origin));}).toList();
    }
    private static Volume volume(GladeDesign.Item item,GladeDesign scene,List<Vector4f> local,float top){
        var at=scene.placement(item);return new Volume(worldPlanes(at,local),new BoundingBox(new Vector3f(at.center().x(),at.elevation()+top/2,at.center().z()),item.halfWidth()+.35f,top/2+.02f,item.halfDepth()+.35f));
    }
    private static Volume profile(GladeDesign.Item owner,GladeDesign.Kind kind,float height,GladeDesign.Placement placement,float base,boolean priority){
        return profile(owner,kind,height,placement,base,priority,-1);
    }
    private static Volume profile(GladeDesign.Item owner,GladeDesign.Kind kind,float height,GladeDesign.Placement placement,float base,boolean priority,int band){
        var outline=RoofProfiles.outline(kind,owner);
        var form=owner.form();
        float x=(form.roofRight()-form.roofLeft())/2,z=(form.roofFront()-form.roofBack())/2;
        float rise=RoofProfiles.rise(kind,height),bias=priority?.002f:-.002f;
        float detailClearance=priority?RoofDetail.MAX_RISE:0;
        boolean flat=kind==GladeDesign.Kind.ROOF_FLAT;
        var shed=kind==GladeDesign.Kind.ROOF_SHED?RoofProfiles.shed(outline,rise):null;
        var planes=new ArrayList<Vector4f>();
        float slopeRise=rise,eave=0,low=0,high=rise;
        if(band>=0){
            low=RoofProfiles.curvedHeight(band,rise);
            high=RoofProfiles.curvedHeight(band+1,rise);
            slopeRise=(high-low)*RoofProfiles.CURVED_BANDS;
            eave=low-slopeRise*band/RoofProfiles.CURVED_BANDS;
            detailClearance=Math.min(detailClearance,.035f);
            planes.add(new Vector4f(0,1,0,-base-high-.002f-detailClearance));
        }
        planes.add(new Vector4f(0,-1,0,base+low-(band>0?.002f:bias)));
        if(flat)planes.add(new Vector4f(0,1,0,-base-rise-bias-detailClearance));
        if(shed!=null)planes.add(new Vector4f(0,1,shed.slope(),-base-(shed.front()+z)*shed.slope()-bias-detailClearance));
        float radius=0;
        for(int i=0;i<outline.size();i++){
            var a=outline.get(i);
            var edge=new Pentagon.Edge(a,outline.get((i+1)%outline.size()));
            var n=edge.normal();float distance=n.dot(a);
            float slope=flat || shed!=null?0:distance/slopeRise;
            planes.add(new Vector4f(n.x,slope,n.y,-distance-n.x*x-n.y*z-slope*(base+eave+detailClearance)-bias));
            radius=Math.max(radius,(float)Math.hypot(a.x+x,a.y+z));
        }
        var center=new Vector3f(placement.center().x(),placement.elevation()+base+(low+high)/2,placement.center().z());
        return new Volume(worldPlanes(placement,planes),new BoundingBox(center,radius+.025f,(high-low)/2+RoofDetail.MAX_RISE,radius+.025f));
    }
    private static Volume body(GladeDesign.Item item,GladeDesign scene){
        var p=new ArrayList<Vector4f>();p.add(new Vector4f(0,-1,0,-.003f));p.add(new Vector4f(0,1,0,-item.height()-.003f));
        if(item.kind()==GladeDesign.Kind.PENTAGON){
            for(var edge:Pentagon.edges(item.width(),item.depth())){
                var normal=edge.normal();p.add(new Vector4f(normal.x,0,normal.y,-normal.dot(edge.from())-.003f));
            }
        }
        else if(item.kind()==GladeDesign.Kind.TOWER)for(int i=0;i<48;i++){float a=(i+.5f)*FastMath.TWO_PI/48;p.add(new Vector4f(FastMath.cos(a)/(item.width()/2),0,FastMath.sin(a)/(item.depth()/2),-1.002f));}
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
        if(!changed)return;if(out.isEmpty()){geo.removeFromParent();return;}
        float[] p=new float[out.size()*3],n=new float[p.length],u=new float[out.size()*2],c=new float[out.size()*4];
        for(int i=0;i<out.size();i++){var v=out.get(i);var local=geo.worldToLocal(v.p,null);p[i*3]=local.x;p[i*3+1]=local.y;p[i*3+2]=local.z;n[i*3]=v.n.x;n[i*3+1]=v.n.y;n[i*3+2]=v.n.z;u[i*2]=v.uv.x;u[i*2+1]=v.uv.y;c[i*4]=v.color.r;c[i*4+1]=v.color.g;c[i*4+2]=v.color.b;c[i*4+3]=v.color.a;}
        var clipped=new Mesh();clipped.setBuffer(VertexBuffer.Type.Position,3,p);if(normals!=null)clipped.setBuffer(VertexBuffer.Type.Normal,3,n);if(uv!=null)clipped.setBuffer(VertexBuffer.Type.TexCoord,2,u);if(color!=null)clipped.setBuffer(VertexBuffer.Type.Color,4,c);clipped.updateBound();geo.setMesh(clipped);geo.setUserData("joinedSurface",true);
    }
}

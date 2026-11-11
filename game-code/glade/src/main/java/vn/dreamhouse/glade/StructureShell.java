package vn.dreamhouse.glade;
import com.jme3.math.*;
import com.jme3.scene.*;
import java.util.*;

/** Exposed structural shell with real arched openings and clipped shared walls/floors. */
final class StructureShell {
    private static final float THICKNESS=.10f;
    private record Face(Vector3f origin,Vector3f tangent,Vector3f normal,float span) {
        Vector3f point(Vector2f p){return origin.add(tangent.mult(p.x)).addLocal(0,p.y,0);}
    }
    static boolean solid(GladeDesign.Item item){
        if(item.kind()==GladeDesign.Kind.PAVILION)return false;
        if(!item.kind().building() && item.kind()!=GladeDesign.Kind.DECK)return false;
        if(item.height()<=.12f)return true;
        return !(item.form().finish().material().equals("wood") && item.form().roof()<=.02f && (item.kind()==GladeDesign.Kind.DECK || item.height()<=1.2f));
    }
    static Geometry build(GladeArt art,GladeDesign.Item item,GladeDesign scene){
        var mesh=new PlainStructure.Builder();float w=item.width()/2,d=item.depth()/2,h=item.height();
        var faces=new ArrayList<Face>();
        if(item.kind()==GladeDesign.Kind.TOWER){
            for(int i=0;i<48;i++){float a=i*FastMath.TWO_PI/48,b=(i+1)*FastMath.TWO_PI/48;var p=new Vector3f(FastMath.sin(a)*w,0,FastMath.cos(a)*d);var q=new Vector3f(FastMath.sin(b)*w,0,FastMath.cos(b)*d);var direction=q.subtract(p);float span=direction.length();var tangent=direction.divide(span);faces.add(new Face(p.add(q).multLocal(.5f),tangent,new Vector3f(-tangent.z,0,tangent.x),span));}
        }else{
            faces.add(new Face(new Vector3f(0,0,d),Vector3f.UNIT_X,Vector3f.UNIT_Z,w*2));
            faces.add(new Face(new Vector3f(w,0,0),Vector3f.UNIT_Z.negate(),Vector3f.UNIT_X,d*2));
            faces.add(new Face(new Vector3f(0,0,-d),Vector3f.UNIT_X.negate(),Vector3f.UNIT_Z.negate(),w*2));
            faces.add(new Face(new Vector3f(-w,0,0),Vector3f.UNIT_Z,Vector3f.UNIT_X.negate(),d*2));
        }
        for(var face:faces)wall(mesh,item,scene,face,h);
        cap(mesh,item,scene,true);cap(mesh,item,scene,false);
        var result=mesh.geometry(art,"Vỏ khối hợp nhất","#ded6c0");result.setUserData("wallSurface",true);result.setUserData("structuralShell",true);return result;
    }
    private static Vector3f world(GladeDesign.Item host,GladeDesign scene,Vector3f p){var placement=scene.placement(host);return new Quaternion().fromAngleAxis(placement.rotation()*FastMath.DEG_TO_RAD,Vector3f.UNIT_Y).mult(p).addLocal(placement.center().x(),placement.elevation(),placement.center().z());}
    private static Vector3f rotate(GladeDesign.Item host,Vector3f p){return new Quaternion().fromAngleAxis(host.rotation()*FastMath.DEG_TO_RAD,Vector3f.UNIT_Y).mult(p);}
    private static void wall(PlainStructure.Builder mesh,GladeDesign.Item item,GladeDesign scene,Face face,float h){
        var bounds=PolyCuts.rectangle(-face.span/2,0,face.span/2,h);var cuts=new ArrayList<List<Vector2f>>();
        var origin=world(item,scene,face.origin);var tangent=rotate(item,face.tangent);var normal=rotate(item,face.normal);
        for(var other:scene.items())if(other.id()!=item.id() && solid(other)){
            float low=scene.placement(other).elevation()-scene.placement(item).elevation(),high=low+other.height();if(high<=0 || low>=h)continue;
            var interval=footprintInterval(other,origin.add(normal.mult(.012f)),tangent);
            if(interval==null && other.id()<item.id())interval=footprintInterval(other,origin,tangent);
            if(interval!=null && interval[1]-interval[0]>.0001f)cuts.add(PolyCuts.rectangle(interval[0],Math.max(0,low),interval[1],Math.min(h,high)));
        }
        int shared=cuts.size();
        for(var opening:scene.items())if(opening.kind().opening() && opening.mount()!=null && opening.mount().hostId()==item.id() && opening.mount().lift()<1 && !AttachmentRules.hidden(opening,scene)){
            float angle=opening.rotation()*FastMath.DEG_TO_RAD;var facing=new Vector3f(FastMath.sin(angle),0,FastMath.cos(angle));float dot=facing.dot(face.normal);if(dot<.70f)continue;
            var center=new Vector3f(opening.mount().u()*item.width(),0,opening.mount().v()*item.depth());if(Math.abs(center.subtract(face.origin).dot(face.normal))>.22f)continue;
            float u=center.subtract(face.origin).dot(face.tangent),width=opening.width()*.72f/dot;boolean door=opening.kind()==GladeDesign.Kind.DOOR_ARCH || opening.kind()==GladeDesign.Kind.DOOR_DOUBLE;
            float height=opening.height()*(door?.86f:.75f),bottom=door?opening.mount().lift()*h:opening.mount().lift()*h-height/2;
            if(opening.kind()==GladeDesign.Kind.WINDOW_ROUND)cuts.add(ellipse(u,opening.mount().lift()*h,opening.width()*.38f/dot,opening.height()*.38f));
            else cuts.add(arch(u,bottom,width,height));
        }
        var entrances=new ArrayList<Float>();
        for(var path:scene.items())if(path.kind()==GladeDesign.Kind.PATH)for(int i=1;i<path.points().size();i++){
            var a=path.points().get(i-1);var b=path.points().get(i);float da=(a.x()-origin.x)*normal.x+(a.z()-origin.z)*normal.z,db=(b.x()-origin.x)*normal.x+(b.z()-origin.z)*normal.z;
            float length=a.distance(b);if(length<.001f || Math.abs(db-da)/length<.25f)continue;
            float t;if(da*db<=0)t=da/(da-db);else if(Math.min(Math.abs(da),Math.abs(db))<.55f)t=Math.abs(da)<Math.abs(db)?0:1;else continue;
            float x=a.x()+(b.x()-a.x())*t,z=a.z()+(b.z()-a.z())*t,u=(x-origin.x)*tangent.x+(z-origin.z)*tangent.z;
            float walkLevel=scene.terrain().height(x,z)+.03f;Float water=SceneRules.of(scene).water(x+normal.x*.25f,z+normal.z*.25f);if(water!=null)walkLevel=Math.max(walkLevel,water+.18f);
            if(Math.abs(scene.placement(item).elevation()-walkLevel)>.65f)continue;
            if(Math.abs(u)>face.span/2+.55f || entrances.stream().anyMatch(old->Math.abs(old-u)<.30f))continue;entrances.add(u);cuts.add(arch(u,0,1.12f,Math.min(2.1f,h*.9f)));
        }
        List<List<Vector2f>> pieces=new ArrayList<>();pieces.add(bounds);for(var cut:cuts)pieces=PolyCuts.subtract(pieces,cut);
        var inward=face.normal.mult(-THICKNESS);
        for(var polygon:pieces)for(int i=1;i<polygon.size()-1;i++){
            var a=face.point(polygon.getFirst());var b=face.point(polygon.get(i));var c=face.point(polygon.get(i+1));mesh.triangle(a,b,c);mesh.triangle(c.add(inward),b.add(inward),a.add(inward));
        }
        // Close only opening reveals, not the partitions generated by polygon subtraction.
        for(int index=shared;index<cuts.size();index++){
            var cut=PolyCuts.ccw(cuts.get(index));
            for(int i=0;i<cut.size();i++){
                var a=cut.get(i);var b=cut.get((i+1)%cut.size());if(a.y<=.001f && b.y<=.001f)continue;
                var visible=PolyCuts.interval(a,b,bounds);if(visible==null)continue;
                var ranges=new ArrayList<float[]>();ranges.add(visible);
                for(int j=0;j<cuts.size();j++)if(j!=index){var hidden=PolyCuts.interval(a,b,cuts.get(j));if(hidden==null)continue;var next=new ArrayList<float[]>();for(var range:ranges){if(hidden[1]<=range[0]+.00001f || hidden[0]>=range[1]-.00001f){next.add(range);continue;}if(hidden[0]>range[0])next.add(new float[]{range[0],hidden[0]});if(hidden[1]<range[1])next.add(new float[]{hidden[1],range[1]});}ranges=next;}
                for(var range:ranges)if(range[1]-range[0]>.00001f){var p=face.point(new Vector2f(a.x+(b.x-a.x)*range[0],a.y+(b.y-a.y)*range[0]));var q=face.point(new Vector2f(a.x+(b.x-a.x)*range[1],a.y+(b.y-a.y)*range[1]));mesh.quad(p,q,q.add(inward),p.add(inward));}
            }
        }
    }
    static boolean coveredDetail(GladeDesign.Item host,GladeDesign scene,Vector3f local){
        var p=world(host,scene,local);for(var other:scene.items())if(other.id()!=host.id() && solid(other) && SceneRules.contains(other,p.x,p.z,.07f)){float low=scene.placement(other).elevation();if(p.y>=low-.005f && p.y<=low+other.height()+.005f)return true;}return false;
    }
    private static float[] footprintInterval(GladeDesign.Item item,Vector3f origin,Vector3f direction){
        var center=new GladeDesign.Point(origin.x,origin.z);float x=item.localX(center),z=item.localZ(center);double angle=Math.toRadians(item.rotation());float dx=(float)(Math.cos(angle)*direction.x-Math.sin(angle)*direction.z),dz=(float)(Math.sin(angle)*direction.x+Math.cos(angle)*direction.z);float w=item.width()/2,d=item.depth()/2;
        if(item.kind()==GladeDesign.Kind.TOWER){float a=dx*dx/(w*w)+dz*dz/(d*d),b=2*(x*dx/(w*w)+z*dz/(d*d)),c=x*x/(w*w)+z*z/(d*d)-1;if(a<.000001f)return c<=0?new float[]{-10000,10000}:null;float disc=b*b-4*a*c;if(disc<0)return null;float root=FastMath.sqrt(disc);return new float[]{(-b-root)/(2*a),(-b+root)/(2*a)};}
        float low=-10000,high=10000;for(float[] axis:new float[][]{{x,dx,w},{z,dz,d}}){if(Math.abs(axis[1])<.000001f){if(Math.abs(axis[0])>axis[2]+.00001f)return null;}else{float a=(-axis[2]-axis[0])/axis[1],b=(axis[2]-axis[0])/axis[1];low=Math.max(low,Math.min(a,b));high=Math.min(high,Math.max(a,b));if(low>=high)return null;}}return new float[]{low,high};
    }
    private static List<Vector2f> arch(float center,float bottom,float width,float height){
        float radius=Math.min(width/2,height*.48f);var p=new ArrayList<Vector2f>();p.add(new Vector2f(center-width/2,bottom));p.add(new Vector2f(center+width/2,bottom));
        for(int i=0;i<=20;i++){float t=i*FastMath.PI/20;p.add(new Vector2f(center+FastMath.cos(t)*width/2,bottom+height-radius+FastMath.sin(t)*radius));}return p;
    }
    private static List<Vector2f> ellipse(float x,float y,float w,float h){var p=new ArrayList<Vector2f>();for(int i=0;i<32;i++){float a=i*FastMath.TWO_PI/32;p.add(new Vector2f(x+FastMath.cos(a)*w,y+FastMath.sin(a)*h));}return p;}
    private static List<Vector2f> footprint(GladeDesign.Item item){
        if(item.kind()!=GladeDesign.Kind.TOWER)return PolyCuts.rectangle(-item.width()/2,-item.depth()/2,item.width()/2,item.depth()/2);
        var polygon=new ArrayList<Vector2f>();for(int i=0;i<48;i++){float a=i*FastMath.TWO_PI/48;polygon.add(new Vector2f(FastMath.cos(a)*item.width()/2,FastMath.sin(a)*item.depth()/2));}return polygon;
    }
    private static void cap(PlainStructure.Builder mesh,GladeDesign.Item host,GladeDesign scene,boolean top){
        float y=top?host.height():0,worldY=scene.placement(host).elevation()+y;List<List<Vector2f>> fragments=new ArrayList<>();fragments.add(footprint(host));
        for(var other:scene.items())if(other.id()!=host.id() && solid(other)){
            float low=scene.placement(other).elevation(),high=low+other.height();boolean interior=worldY>low+.001f && worldY<high-.001f;
            boolean stacked=top?other.form().support()==host.id() && Math.abs(worldY-low)<.002f:host.form().support()==other.id() && Math.abs(worldY-high)<.002f;
            boolean duplicate=other.id()<host.id() && Math.abs(worldY-(top?high:low))<.002f;
            if(!interior && !stacked && !duplicate)continue;
            var cut=new ArrayList<Vector2f>();for(var p:footprint(other)){var wp=world(other,scene,new Vector3f(p.x,0,p.y));var point=new GladeDesign.Point(wp.x,wp.z);cut.add(new Vector2f(host.localX(point),host.localZ(point)));}fragments=PolyCuts.subtract(fragments,cut);
        }
        for(var polygon:fragments)for(int i=1;i<polygon.size()-1;i++){var a=polygon.getFirst();var b=polygon.get(i);var c=polygon.get(i+1);var aa=new Vector3f(a.x,y,a.y);var bb=new Vector3f(b.x,y,b.y);var cc=new Vector3f(c.x,y,c.y);if(top)mesh.triangle(aa,cc,bb);else mesh.triangle(aa,bb,cc);}
    }
}

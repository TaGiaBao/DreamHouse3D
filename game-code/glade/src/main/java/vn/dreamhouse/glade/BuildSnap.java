package vn.dreamhouse.glade;
import com.jme3.math.*;
import java.util.*;

/** Gridless, nearest-neighbor alignment. Preview and committed placement share this calculation. */
final class BuildSnap {
    record Line(Vector3f a,Vector3f b){}
    record Result(GladeDesign.Point center,long neighbor,Float x,Float z,List<Line> lines){}
    record Anchor(float center,float guide){}
    static Result resolve(GladeDesign scene,GladeDesign.Item item,float tolerance,Result previous){
        var plain=new Result(item.center(),-1,null,null,List.of());
        if(!item.kind().building() && item.kind()!=GladeDesign.Kind.DECK || item.form().support()>=0)return plain;
        var excluded=scene.items().stream().anyMatch(i->i.id()==item.id())?scene.assemblyIds(item.id()):Set.<Long>of();
        Result best=plain;float bestScore=Float.POSITIVE_INFINITY;
        for(var neighbor:scene.items()){
            if(excluded.contains(neighbor.id()) || neighbor.mount()!=null || !neighbor.kind().building() && neighbor.kind()!=GladeDesign.Kind.DECK)continue;
            // Rectangular edge alignment only. A round tower must remain freely placeable.
            if(item.kind()==GladeDesign.Kind.TOWER || neighbor.kind()==GladeDesign.Kind.TOWER)continue;
            float turn=Math.abs(FastMath.normalize(item.rotation()-neighbor.rotation(),-180,180)),quarter=Math.round(turn/90);
            if(Math.abs(turn-quarter*90)>1)continue;
            float w=(quarter%2==0?item.width():item.depth())/2,d=(quarter%2==0?item.depth():item.width())/2,nw=neighbor.width()/2,nd=neighbor.depth()/2;
            float x=neighbor.localX(item.center()),z=neighbor.localZ(item.center());
            Anchor ax=null,az=null;
            if(Math.abs(z)<nd+d+tolerance)ax=nearest(x,nw,w,tolerance,previous!=null && previous.neighbor()==neighbor.id()?previous.x():null);
            if(Math.abs(x)<nw+w+tolerance)az=nearest(z,nd,d,tolerance,previous!=null && previous.neighbor()==neighbor.id()?previous.z():null);
            if(ax==null && az==null)continue;
            float sx=ax==null?x:ax.center(),sz=az==null?z:az.center();var center=world(neighbor,sx,sz);
            var candidate=copy(item,center);if(!fitsAssembly(scene,item,candidate))continue;
            float score=(sx-x)*(sx-x)+(sz-z)*(sz-z);if(previous!=null && previous.neighbor()==neighbor.id())score*=.7f;
            if(score>=bestScore)continue;bestScore=score;
            float y=Math.max(scene.placement(neighbor).elevation(),scene.placement(candidate).elevation())+.07f;
            var lines=new ArrayList<Line>();
            if(ax!=null)lines.add(line(neighbor,ax.guide(),Math.min(-nd,sz-d)-.25f,ax.guide(),Math.max(nd,sz+d)+.25f,y));
            if(az!=null)lines.add(line(neighbor,Math.min(-nw,sx-w)-.25f,az.guide(),Math.max(nw,sx+w)+.25f,az.guide(),y));
            best=new Result(center,neighbor.id(),ax==null?null:ax.center(),az==null?null:az.center(),List.copyOf(lines));
        }
        return best;
    }
    private static Anchor nearest(float value,float neighbor,float own,float radius,Float locked){
        var anchors=List.of(new Anchor(-neighbor-own,-neighbor),new Anchor(neighbor+own,neighbor),new Anchor(0,0),new Anchor(-neighbor+own,-neighbor),new Anchor(neighbor-own,neighbor));
        if(locked!=null && Math.abs(value-locked)<radius*1.55f)for(var a:anchors)if(Math.abs(a.center()-locked)<.001f)return a;
        Anchor best=null;float gap=radius;for(var a:anchors){float delta=Math.abs(value-a.center());if(delta<gap){gap=delta;best=a;}}return best;
    }
    static GladeDesign.Item copy(GladeDesign.Item item,GladeDesign.Point center){return new GladeDesign.Item(item.id(),item.kind(),center,item.width(),item.depth(),item.height(),item.pitched(),item.points(),item.rotation(),item.mount(),item.form());}
    private static boolean fitsAssembly(GladeDesign scene,GladeDesign.Item original,GladeDesign.Item candidate){
        var saved=scene.items().stream().filter(i->i.id()==original.id()).findFirst().orElse(original);
        float dx=candidate.center().x()-saved.center().x(),dz=candidate.center().z()-saved.center().z();
        if(!inside(scene,candidate.center().x(),candidate.center().z(),candidate.halfWidth(),candidate.halfDepth()))return false;
        if(scene.items().stream().noneMatch(i->i.id()==original.id()))return true;
        var ids=scene.assemblyIds(original.id());
        for(var child:scene.items())if(ids.contains(child.id()) && child.id()!=original.id() && child.mount()==null && child.kind().footprint() && !inside(scene,child.center().x()+dx,child.center().z()+dz,child.halfWidth(),child.halfDepth()))return false;
        return true;
    }
    private static boolean inside(GladeDesign scene,float x,float z,float w,float d){var land=scene.land();return x-w>=land.minX()-.001f && x+w<=land.maxX()+.001f && z-d>=land.minZ()-.001f && z+d<=land.maxZ()+.001f;}
    private static GladeDesign.Point world(GladeDesign.Item item,float x,float z){double t=Math.toRadians(item.rotation());return new GladeDesign.Point(item.center().x()+(float)(Math.cos(t)*x+Math.sin(t)*z),item.center().z()+(float)(-Math.sin(t)*x+Math.cos(t)*z));}
    private static Line line(GladeDesign.Item item,float x,float z,float xx,float zz,float y){var a=world(item,x,z);var b=world(item,xx,zz);return new Line(new Vector3f(a.x(),y,a.z()),new Vector3f(b.x(),y,b.z()));}
}

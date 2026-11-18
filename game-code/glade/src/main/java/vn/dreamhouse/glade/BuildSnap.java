package vn.dreamhouse.glade;
import com.jme3.math.*;
import java.util.*;

final class BuildSnap {
    record Line(Vector3f a,Vector3f b){}
    record Result(GladeDesign.Point center,long neighbor,Float x,Float z,List<Line> lines,EdgePair edges){}
    private record EdgePair(int target,int moving){}
    record Anchor(float center,float guide){}
    static Result resolve(GladeDesign scene,GladeDesign.Item item,float tolerance,Result previous){
        var plain=new Result(item.center(),-1,null,null,List.of(),null);
        if(!item.kind().building() && item.kind()!=GladeDesign.Kind.DECK || item.form().support()>=0)return plain;
        var excluded=scene.items().stream().anyMatch(i->i.id()==item.id())?scene.assemblyIds(item.id()):Set.<Long>of();
        Result best=plain;float bestScore=Float.POSITIVE_INFINITY;
        List<Pentagon.Edge> movingEdges=null;
        for(var neighbor:scene.items()){
            if(excluded.contains(neighbor.id()) || neighbor.mount()!=null || !neighbor.kind().building() && neighbor.kind()!=GladeDesign.Kind.DECK)continue;
            if(item.kind()==GladeDesign.Kind.TOWER || neighbor.kind()==GladeDesign.Kind.TOWER)continue;
            if(item.kind()==GladeDesign.Kind.PENTAGON || neighbor.kind()==GladeDesign.Kind.PENTAGON){
                if(Math.abs(item.center().x()-neighbor.center().x())>item.halfWidth()+neighbor.halfWidth()+tolerance*1.55f
                        || Math.abs(item.center().z()-neighbor.center().z())>item.halfDepth()+neighbor.halfDepth()+tolerance*1.55f)continue;
                if(movingEdges==null)movingEdges=worldEdges(item);
                var aligned=matchEdges(scene,item,neighbor,movingEdges,tolerance,previous);
                if(aligned==null)continue;
                float distance=aligned.center().distance(item.center());
                float score=distance*distance*(previous!=null && previous.neighbor()==neighbor.id()?.7f:1f);
                if(score<bestScore){best=aligned;bestScore=score;}
                continue;
            }
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
            best=new Result(center,neighbor.id(),ax==null?null:ax.center(),az==null?null:az.center(),List.copyOf(lines),null);
        }
        return best;
    }

    private static Result matchEdges(GladeDesign scene,GladeDesign.Item item,GladeDesign.Item neighbor,
            List<Pentagon.Edge> movingEdges,float tolerance,Result previous){
        var targetEdges=worldEdges(neighbor);
        Result best=null;
        float bestGap=Float.POSITIVE_INFINITY;
        for(int target=0;target<targetEdges.size();target++){
            var fixed=targetEdges.get(target);
            var normal=fixed.normal();
            var tangent=fixed.to().subtract(fixed.from());
            float length=tangent.length();
            tangent.divideLocal(length);
            for(int moving=0;moving<movingEdges.size();moving++){
                var edge=movingEdges.get(moving);
                if(normal.dot(edge.normal())>-.9999f)continue;
                var start=edge.from().subtract(fixed.from());
                var end=edge.to().subtract(fixed.from());
                float min=Math.min(start.dot(tangent),end.dot(tangent));
                float max=Math.max(start.dot(tangent),end.dot(tangent));
                float overlap=Math.min(length,max)-Math.max(0,min);
                if(overlap<Math.min(.16f,Math.min(length,max-min)*.2f))continue;
                float offset=-Math.min(start.dot(normal),end.dot(normal));
                var pair=new EdgePair(target,moving);
                boolean held=previous!=null && previous.neighbor()==neighbor.id() && pair.equals(previous.edges());
                float gap=Math.abs(offset);
                if(gap>(held?tolerance*1.55f:tolerance) || gap*(held?.7f:1f)>=bestGap)continue;
                float x=item.center().x()+normal.x*offset,z=item.center().z()+normal.y*offset;
                if(!inside(scene,x,z,item.halfWidth(),item.halfDepth()))continue;
                var center=new GladeDesign.Point(x,z);
                var candidate=copy(item,center);
                if(!fitsAssembly(scene,item,candidate))continue;
                float y=Math.max(scene.placement(neighbor).elevation(),scene.placement(candidate).elevation())+.07f;
                var a=fixed.from().add(tangent.mult(Math.max(0,min)));
                var b=fixed.from().add(tangent.mult(Math.min(length,max)));
                best=new Result(center,neighbor.id(),null,null,List.of(new Line(new Vector3f(a.x,y,a.y),new Vector3f(b.x,y,b.y))),pair);
                bestGap=gap*(held?.7f:1f);
            }
        }
        return best;
    }

    private static List<Pentagon.Edge> worldEdges(GladeDesign.Item item){
        float w=item.width()/2,d=item.depth()/2;
        var local=item.kind()==GladeDesign.Kind.PENTAGON?Pentagon.edges(item.width(),item.depth())
                : List.of(new Pentagon.Edge(new Vector2f(-w,d),new Vector2f(w,d)),
                        new Pentagon.Edge(new Vector2f(w,d),new Vector2f(w,-d)),
                        new Pentagon.Edge(new Vector2f(w,-d),new Vector2f(-w,-d)),
                        new Pentagon.Edge(new Vector2f(-w,-d),new Vector2f(-w,d)));
        float angle=item.rotation()*FastMath.DEG_TO_RAD,c=FastMath.cos(angle),s=FastMath.sin(angle);
        return local.stream().map(edge->new Pentagon.Edge(edgePoint(item,edge.from(),c,s),edgePoint(item,edge.to(),c,s))).toList();
    }

    private static Vector2f edgePoint(GladeDesign.Item item,Vector2f point,float cos,float sin){
        return new Vector2f(item.center().x()+point.x*cos+point.y*sin,item.center().z()-point.x*sin+point.y*cos);
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

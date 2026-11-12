package vn.dreamhouse.glade;

import com.jme3.math.Vector2f;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Local XZ perimeter, beginning with the straight front façade. */
final class Pentagon {
    record Edge(Vector2f from,Vector2f to) {
        Vector2f normal(){var d=to.subtract(from).normalizeLocal();return new Vector2f(-d.y,d.x);}
        float distance(Vector2f point){return normal().dot(point.subtract(from));}
        Vector2f closest(Vector2f point){
            var delta=to.subtract(from);
            float t=Math.clamp(point.subtract(from).dot(delta)/delta.lengthSquared(),0,1);
            return from.add(delta.multLocal(t));
        }
    }

    static List<Edge> edges(float width,float depth,float backCorner){
        float w=width/2,d=depth/2;
        var points=List.of(new Vector2f(-w,d),new Vector2f(w,d),new Vector2f(w,backCorner*d),
                new Vector2f(0,-d),new Vector2f(-w,backCorner*d));
        var result=new ArrayList<Edge>(5);
        for(int i=0;i<points.size();i++)result.add(new Edge(points.get(i),points.get((i+1)%points.size())));
        return result;
    }

    static List<Edge> edges(float width,float depth){return edges(width,depth,-.35f);}

    static Edge nearestEdge(float width,float depth,Vector2f point){
        return edges(width,depth).stream().min(Comparator.comparingDouble(edge->edge.closest(point).distanceSquared(point))).orElseThrow();
    }
}

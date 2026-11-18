package vn.dreamhouse.glade;
import com.jme3.math.Vector2f;
import java.util.*;

final class PolyCuts {
    private static final float EDGE_EPSILON=.00001f;
    static List<Vector2f> rectangle(float x0,float y0,float x1,float y1){return List.of(new Vector2f(x0,y0),new Vector2f(x1,y0),new Vector2f(x1,y1),new Vector2f(x0,y1));}
    static float side(Vector2f a,Vector2f b,Vector2f p){return (b.x-a.x)*(p.y-a.y)-(b.y-a.y)*(p.x-a.x);}
    static float area(List<Vector2f> p){
        if(p.size()<3)return 0;
        double area=0;
        var origin=p.getFirst();
        for(int i=1;i<p.size()-1;i++){
            var a=p.get(i);var b=p.get(i+1);
            area+=((double)a.x-origin.x)*((double)b.y-origin.y)-((double)a.y-origin.y)*((double)b.x-origin.x);
        }
        return (float)(area/2);
    }
    static List<Vector2f> ccw(List<Vector2f> p){if(area(p)>=0)return p;var copy=new ArrayList<>(p);Collections.reverse(copy);return copy;}
    static List<Vector2f> half(List<Vector2f> polygon,Vector2f a,Vector2f b,boolean inside){
        if(polygon.size()<3)return List.of();
        float length=a.distance(b);
        if(length<=EDGE_EPSILON)return inside?polygon:List.of();
        float tolerance=length*EDGE_EPSILON;
        var result=new ArrayList<Vector2f>();
        for(int i=0;i<polygon.size();i++){
            var p=polygon.get(i);var q=polygon.get((i+1)%polygon.size());
            float sp=side(a,b,p)*(inside?1:-1),sq=side(a,b,q)*(inside?1:-1);
            if(Math.abs(sp)<=tolerance)sp=0;
            if(Math.abs(sq)<=tolerance)sq=0;
            if(sp>=0)result.add(p);
            if(sp<0 && sq>0 || sp>0 && sq<0){
                float t=sp/(sp-sq);
                result.add(new Vector2f(p.x+(q.x-p.x)*t,p.y+(q.y-p.y)*t));
            }
        }
        return result.size()>=3 && Math.abs(area(result))>.000001f?result:List.of();
    }
    static List<List<Vector2f>> subtract(List<List<Vector2f>> fragments,List<Vector2f> polygon){
        var cut=ccw(polygon);var result=new ArrayList<List<Vector2f>>();
        for(var fragment:fragments){
            if(intersection(fragment,cut).isEmpty()){result.add(fragment);continue;}
            var remainder=fragment;for(int i=0;i<cut.size() && !remainder.isEmpty();i++){
            var a=cut.get(i);var b=cut.get((i+1)%cut.size());var outside=half(remainder,a,b,false);if(!outside.isEmpty())result.add(outside);remainder=half(remainder,a,b,true);
        }}return result;
    }
    static List<Vector2f> intersection(List<Vector2f> polygon,List<Vector2f> clipping){var result=polygon;var cut=ccw(clipping);for(int i=0;i<cut.size() && !result.isEmpty();i++)result=half(result,cut.get(i),cut.get((i+1)%cut.size()),true);return result;}
    static float[] interval(Vector2f a,Vector2f b,List<Vector2f> polygon){
        float low=0,high=1;var cut=ccw(polygon);
        for(int i=0;i<cut.size();i++){var p=cut.get(i);var q=cut.get((i+1)%cut.size());float start=side(p,q,a),end=side(p,q,b),slope=end-start;
            if(Math.abs(slope)<.000001f){if(start<-.00001f)return null;continue;}
            float t=-start/slope;if(slope>0)low=Math.max(low,t);else high=Math.min(high,t);if(low>high)return null;
        }return new float[]{Math.max(0,low),Math.min(1,high)};
    }
}

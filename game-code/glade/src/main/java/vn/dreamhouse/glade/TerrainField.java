package vn.dreamhouse.glade;

import java.util.*;

/** Sparse, immutable sculpting data. Each brush stroke remains one undoable design edit. */
final class TerrainField {
    static final float STEP=.75f;
    record Cell(int x,int z) {}
    private final Map<Cell,Float> heights;
    TerrainField() {this(Map.of());}
    TerrainField(Map<Cell,Float> values) {
        for(var e:values.entrySet()) if(!Float.isFinite(e.getValue()) || Math.abs(e.getValue())>32
                || Math.abs(e.getKey().x()*STEP)>LandBounds.LIMIT_X+4 || Math.abs(e.getKey().z()*STEP)>LandBounds.LIMIT_Z+4)
            throw new IllegalArgumentException("Độ cao đất không hợp lệ.");
        heights=Map.copyOf(values);
    }
    Map<Cell,Float> cells() {return heights;}
    float height(float x,float z) {
        float gx=x/STEP,gz=z/STEP;int ix=(int)Math.floor(gx),iz=(int)Math.floor(gz);
        float tx=gx-ix,tz=gz-iz;
        return (get(ix,iz)*(1-tx)+get(ix+1,iz)*tx)*(1-tz)+(get(ix,iz+1)*(1-tx)+get(ix+1,iz+1)*tx)*tz;
    }
    private float get(int x,int z) {return heights.getOrDefault(new Cell(x,z),0f);}
    TerrainField brush(float x,float z,float radius,float amount,Float level) {
        var next=new HashMap<>(heights);
        for(int iz=(int)Math.floor((z-radius)/STEP);iz<=Math.ceil((z+radius)/STEP);iz++)
            for(int ix=(int)Math.floor((x-radius)/STEP);ix<=Math.ceil((x+radius)/STEP);ix++) {
                float distance=(float)Math.hypot(ix*STEP-x,iz*STEP-z);if(distance>radius) continue;
                float weight=1-distance/radius;weight=weight*weight*(3-2*weight);
                float old=get(ix,iz),value=level==null?old+amount*weight:old+(level-old)*Math.min(1,Math.abs(amount))*weight;
                value=Math.max(-16,Math.min(24,value));
                if(Math.abs(value)<.001f) next.remove(new Cell(ix,iz));else next.put(new Cell(ix,iz),value);
            }
        return new TerrainField(next);
    }
    String encode() {
        var result=new StringBuilder();
        heights.entrySet().stream().sorted(Comparator.comparingInt((Map.Entry<Cell,Float> e)->e.getKey().x()).thenComparingInt(e->e.getKey().z()))
                .forEach(e->{if(!result.isEmpty()) result.append(';');result.append(e.getKey().x()).append(',').append(e.getKey().z()).append(',').append(e.getValue());});
        return result.toString();
    }
    static TerrainField decode(String source) {
        if(source.length()>4*1024*1024 || source.chars().filter(c->c==';').count()>100000)throw new IllegalArgumentException("Địa hình trong tệp quá lớn.");
        var values=new HashMap<Cell,Float>();
        if(!source.isBlank()) for(String point:source.split(";")) {
            var v=point.split(",");if(v.length!=3) throw new IllegalArgumentException("Dữ liệu đất không hợp lệ.");
            values.put(new Cell(Integer.parseInt(v[0]),Integer.parseInt(v[1])),Float.parseFloat(v[2]));
        }
        return new TerrainField(values);
    }
}

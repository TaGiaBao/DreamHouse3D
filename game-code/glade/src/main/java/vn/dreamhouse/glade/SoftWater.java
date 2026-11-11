package vn.dreamhouse.glade;

import com.jme3.material.RenderState;
import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import java.nio.FloatBuffer;
import java.util.*;

/** Continuous shoreline extracted from the flood field, independent of its simulation cells. */
final class SoftWater {
    private static final float STEP=WaterFlow.STEP/3, EDGE=.42f;
    private static final ColorRGBA SHALLOW=GladeArt.color("#a3d2c6"),DEEP=GladeArt.color("#69b5bb");
    private static final Map<Mesh,float[]> REST=Collections.synchronizedMap(new WeakHashMap<>());
    static void register(Geometry geometry,float[] positions){REST.put(geometry.getMesh(),positions.clone());geometry.setUserData("softSurface",true);}
    private record Sample(float x,float z,float coverage,float level,float arrival) {
        Sample mix(Sample b,float t){return new Sample(FastMath.interpolateLinear(t,x,b.x),FastMath.interpolateLinear(t,z,b.z),EDGE,FastMath.interpolateLinear(t,level,b.level),FastMath.interpolateLinear(t,arrival,b.arrival));}
    }
    private static final class Batch {
        final List<Float> positions=new ArrayList<>(),colors=new ArrayList<>();
        void vertex(Sample p,GladeDesign.Item source,GladeDesign scene,float base){
            float ground=scene.terrain().height(p.x,p.z),height=Math.max(ground+.022f,p.level);
            positions.add(p.x-source.center().x());positions.add(height-base);positions.add(p.z-source.center().z());
            float depth=FastMath.clamp((height-ground)/.46f,0,1),edge=FastMath.clamp((p.coverage-EDGE)/.35f,0,1);
            colors.add(FastMath.interpolateLinear(depth,SHALLOW.r,DEEP.r));colors.add(FastMath.interpolateLinear(depth,SHALLOW.g,DEEP.g));colors.add(FastMath.interpolateLinear(depth,SHALLOW.b,DEEP.b));colors.add(.08f+.80f*edge);
        }
    }
    static Node render(GladeArt art,GladeDesign.Item source,GladeDesign scene,WaterFlow.Result water){
        var root=new Node("Mặt nước mềm");root.setUserData("softWater",true);
        if(water.cells().isEmpty())return root;
        // Sparse sampling limits work to the neighborhood of wet cells, even on a very large plot.
        var squares=new HashSet<WaterFlow.Cell>();
        for(var cell:water.cells().keySet())for(int x=-2;x<=4;x++)for(int z=-2;z<=4;z++)squares.add(new WaterFlow.Cell(cell.x()*3+x,cell.z()*3+z));
        var samples=new HashMap<WaterFlow.Cell,Sample>();var batches=new TreeMap<Integer,Batch>();float base=scene.placement(source).elevation();
        for(var square:squares){int x=square.x(),z=square.z();
            var a=sample(samples,water,x,z);var b=sample(samples,water,x,z+1);var c=sample(samples,water,x+1,z+1);var d=sample(samples,water,x+1,z);
            clip(a,b,c,batches,source,scene,base);clip(a,c,d,batches,source,scene,base);
        }
        for(var entry:batches.entrySet()){
            var batch=entry.getValue();float[] positions=array(batch.positions),normals=new float[positions.length];for(int i=1;i<normals.length;i+=3)normals[i]=1;
            var mesh=new Mesh();mesh.setBuffer(VertexBuffer.Type.Position,3,positions);mesh.setBuffer(VertexBuffer.Type.Normal,3,normals);mesh.setBuffer(VertexBuffer.Type.Color,4,array(batch.colors));mesh.setDynamic();mesh.updateBound();REST.put(mesh,positions);
            var geo=new Geometry("Nước liền mặt",mesh);var material=art.material("#ffffff").clone();material.setBoolean("UseVertexColor",true);material.setColor("Specular",new ColorRGBA(.6f,.7f,.67f,1));material.setFloat("Shininess",96);material.setTransparent(true);
            material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);material.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);material.getAdditionalRenderState().setDepthWrite(false);
            geo.setMaterial(material);geo.setQueueBucket(RenderQueue.Bucket.Transparent);geo.setShadowMode(RenderQueue.ShadowMode.Receive);geo.setUserData("softSurface",true);geo.setUserData("flowDelay",entry.getKey()*.045f);root.attachChild(geo);
        }
        return root;
    }
    private static Sample sample(Map<WaterFlow.Cell,Sample> cache,WaterFlow.Result water,int ix,int iz){
        return cache.computeIfAbsent(new WaterFlow.Cell(ix,iz),key->{float x=ix*STEP,z=iz*STEP,total=0,wetWeight=0,height=0,arrival=0;int cx=(int)Math.floor(x/WaterFlow.STEP),cz=(int)Math.floor(z/WaterFlow.STEP);
            for(int i=cx-1;i<=cx+1;i++)for(int j=cz-1;j<=cz+1;j++){
                var cell=new WaterFlow.Cell(i,j);float dx=(x-cell.wx())/WaterFlow.STEP,dz=(z-cell.wz())/WaterFlow.STEP;float weight=(float)Math.exp(-(dx*dx+dz*dz)*2.1f);total+=weight;
                var wet=water.cells().get(cell);if(wet!=null){wetWeight+=weight;height+=wet.level()*weight;arrival+=wet.arrival()*weight;}
            }
            return new Sample(x,z,wetWeight/total,wetWeight>0?height/wetWeight:0,wetWeight>0?arrival/wetWeight:0);
        });
    }
    private static void clip(Sample a,Sample b,Sample c,Map<Integer,Batch> batches,GladeDesign.Item source,GladeDesign scene,float base){
        var polygon=new ArrayList<Sample>(4);var vertices=List.of(a,b,c);
        for(int i=0;i<3;i++){var p=vertices.get(i);var q=vertices.get((i+1)%3);boolean inside=p.coverage>=EDGE,next=q.coverage>=EDGE;if(inside)polygon.add(p);if(inside!=next)polygon.add(p.mix(q,(EDGE-p.coverage)/(q.coverage-p.coverage)));}
        if(polygon.size()<3)return;
        int band=Math.min(22,Math.round((a.arrival+b.arrival+c.arrival)/3));var batch=batches.computeIfAbsent(band,key->new Batch());
        for(int i=1;i<polygon.size()-1;i++)for(var vertex:List.of(polygon.getFirst(),polygon.get(i),polygon.get(i+1)))batch.vertex(vertex,source,scene,base);
    }
    static void animate(Spatial part,float time){
        if(!(part instanceof Geometry geo) || !Boolean.TRUE.equals(geo.getUserData("softSurface")))return;
        Float last=geo.getUserData("waterTime");if(last!=null && time-last<.08f)return;geo.setUserData("waterTime",time);
        var mesh=geo.getMesh();var rest=REST.get(mesh);if(rest==null)return;
        var position=(FloatBuffer)mesh.getBuffer(VertexBuffer.Type.Position).getData();var normals=(FloatBuffer)mesh.getBuffer(VertexBuffer.Type.Normal).getData();position.clear();normals.clear();
        for(int i=0;i<rest.length;i+=3){float x=rest[i],z=rest[i+2],a=x*1.3f+z*.7f+time*.85f,b=z*1.8f-x*.4f-time*.65f;
            float wave=FastMath.sin(a)*.010f+FastMath.sin(b)*.006f,dx=FastMath.cos(a)*.013f-FastMath.cos(b)*.0024f,dz=FastMath.cos(a)*.007f+FastMath.cos(b)*.0108f;
            position.put(x).put(rest[i+1]+wave).put(z);float length=FastMath.sqrt(1+dx*dx+dz*dz);normals.put(-dx/length).put(1/length).put(-dz/length);
        }
        position.flip();normals.flip();mesh.getBuffer(VertexBuffer.Type.Position).updateData(position);mesh.getBuffer(VertexBuffer.Type.Normal).updateData(normals);
    }
    private static float[] array(List<Float> list){var values=new float[list.size()];for(int i=0;i<values.length;i++)values[i]=list.get(i);return values;}
}


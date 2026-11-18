package vn.dreamhouse.glade;
import com.jme3.scene.*;
import com.jme3.scene.shape.Cylinder;
import com.jme3.math.*;
import java.util.*;
final class PlainStructure {
    static boolean render(GladeArt art,Node node,GladeDesign.Item item,GladeDesign scene){
        if(item.kind()!=GladeDesign.Kind.PENTAGON && !scene.hillside(item) && !item.stonePlatform()
                && (item.form().finish().variant()!=18 || !item.kind().building() && item.kind()!=GladeDesign.Kind.DECK))return false;
        float w=item.width(),d=item.depth(),h=item.height();
        var body=StructureShell.build(art,item,scene);if(body.getMesh().getVertexCount()>0)node.attachChild(body);
        float rise=scene.roofRise(item);
        if(item.kind().building() && rise>.02f && !scene.customRoof(item.id()) && !art.joinedRoof(item,scene)){
            if(item.kind()==GladeDesign.Kind.PENTAGON || scene.hillside(item)){
                var peak=new Node("Dáng mái");peak.setLocalTranslation(0,h,0);node.attachChild(peak);
                RoofProfiles.build(art,peak,scene.hillside(item)?GladeDesign.Kind.ROOF_CURVED:GladeDesign.Kind.ROOF_PENTAGON,item,rise);
            }
            else if(item.kind()!=GladeDesign.Kind.TOWER)HipRoof.build(art,node,item,rise);
            else {var roof=art.roofGroup(node);var cone=new Geometry("Mái tháp",new Cylinder(2,32,.001f,w/2+.12f,rise,true,false));cone.rotate(-FastMath.HALF_PI,0,0);cone.setLocalScale(1,d/w,1);cone.setLocalTranslation(0,h+rise/2,0);cone.setMaterial(art.material("#c58b62"));cone.setUserData("roofSurface",true);roof.attachChild(cone);}
        }
        trim(art,node,item,scene,rise);
        int floors=Math.max(1,(int)Math.ceil(h/2.8f));node.setUserData("storeys",floors);
        node.setUserData("plainStructure",true);return true;
    }
    private static void trim(GladeArt art,Node node,GladeDesign.Item item,GladeDesign scene,float rise) {
        if(!item.kind().building() || item.kind()==GladeDesign.Kind.PENTAGON)return;
        float w=item.width(),d=item.depth(),h=item.height();boolean wood=item.form().finish().material().equals("wood");String stone=wood?"#a58b62":item.form().finish().material().equals("stone")?"#abb3a6":"#cfc09c";
        if(item.kind()!=GladeDesign.Kind.TOWER && !item.form().finish().material().equals("plaster")){
            int rows=Math.max(2,Math.min(24,(int)(h/.34f)));
            for(int x:new int[]{-1,1})for(int z:new int[]{-1,1}){
                if(wood){if(!StructureShell.coveredDetail(item,scene,new Vector3f(x*(w/2+.018f),h/2,z*(d/2+.018f))))art.box(node,.12f,h,.12f,x*(w/2+.018f),h/2,z*(d/2+.018f),stone);}
                else for(int row=0;row<rows;row++)if(!StructureShell.coveredDetail(item,scene,new Vector3f(x*w/2,(row+.5f)*h/rows,z*d/2)))art.box(node,row%2==0?.25f:.16f,h/rows-.018f,row%2==0?.16f:.25f,x*(w/2-.045f),(row+.5f)*h/rows,z*(d/2-.045f),stone);
            }
        }
        if(scene.hasUpper(item.id()) || scene.customRoof(item.id()) || rise>=.30f || h<=.25f)return;
        float amount=1-rise/.30f;node.setUserData("battlements",true);
        if(item.kind()==GladeDesign.Kind.TOWER){
            float radius=w/2;for(int i=0;i<20;i++){float angle=i*FastMath.TWO_PI/20;if(!rimVisible(item,scene,FastMath.sin(angle)*radius,FastMath.cos(angle)*d/2,new Vector3f(FastMath.sin(angle),0,FastMath.cos(angle))))continue;var cap=art.box(node,.22f,.24f*amount,.18f,FastMath.sin(angle)*radius,h+.12f*amount,FastMath.cos(angle)*d/2,stone);cap.rotate(0,angle,0);}
        }else for(int side:new int[]{-1,1}){
            int alongX=Math.min(256,Math.max(1,(int)Math.ceil(w/.35f))),alongZ=Math.min(256,Math.max(1,(int)Math.ceil(d/.35f)));
            for(int i=0;i<alongX;i++){float x=-w/2+(i+.5f)*w/alongX;if(rimVisible(item,scene,x,side*d/2,new Vector3f(0,0,side)))art.box(node,w/alongX+.006f,.18f*amount,.13f,x,h+.09f*amount,side*d/2,stone);}
            for(int i=0;i<alongZ;i++){float z=-d/2+(i+.5f)*d/alongZ;if(rimVisible(item,scene,side*w/2,z,new Vector3f(side,0,0)))art.box(node,.13f,.18f*amount,d/alongZ+.006f,side*w/2,h+.09f*amount,z,stone);}
            int nx=Math.max(2,Math.min(96,(int)(w/.55f))),nz=Math.max(2,Math.min(96,(int)(d/.55f)));
            for(int i=0;i<=nx;i++)if(rimVisible(item,scene,-w/2+w*i/nx,side*d/2,new Vector3f(0,0,side)))art.box(node,.24f,.24f*amount,.18f,-w/2+w*i/nx,h+.28f*amount,side*d/2,stone);
            for(int i=1;i<nz;i++)if(rimVisible(item,scene,side*w/2,-d/2+d*i/nz,new Vector3f(side,0,0)))art.box(node,.18f,.24f*amount,.24f,side*w/2,h+.28f*amount,-d/2+d*i/nz,stone);
        }
    }

    private static boolean rimVisible(GladeDesign.Item item,GladeDesign scene,float x,float z,Vector3f normal){
        if(StructureShell.coveredDetail(item,scene,new Vector3f(x,item.height()+.15f,z)))return false;
        var rotation=new Quaternion().fromAngleAxis(item.rotation()*FastMath.DEG_TO_RAD,Vector3f.UNIT_Y);var p=rotation.mult(new Vector3f(x,0,z)).addLocal(item.center().x(),0,item.center().z());var out=rotation.mult(normal).multLocal(.075f).addLocal(p);
        for(var other:scene.items())if(other.id()!=item.id() && StructureShell.solid(other) && Math.abs(scene.top(other)-scene.top(item))<.08f){
            if(SceneRules.contains(other,out.x,out.z,0) || other.id()<item.id() && SceneRules.contains(other,p.x,p.z,.001f))return false;
        }
        return true;
    }

    static void terraceRail(GladeArt art,Node node,GladeDesign.Item item,GladeDesign scene){
        if(!item.kind().roofHost() || item.height()<=.25f || !scene.hasUpper(item.id()))return;
        var rail=new Node("Lan can sân thượng");
        var outline=item.kind()==GladeDesign.Kind.PENTAGON
                ?Pentagon.edges(item.width(),item.depth())
                :List.of(new Pentagon.Edge(new Vector2f(-item.width()/2,item.depth()/2),new Vector2f(item.width()/2,item.depth()/2)),
                        new Pentagon.Edge(new Vector2f(item.width()/2,item.depth()/2),new Vector2f(item.width()/2,-item.depth()/2)),
                        new Pentagon.Edge(new Vector2f(item.width()/2,-item.depth()/2),new Vector2f(-item.width()/2,-item.depth()/2)),
                        new Pentagon.Edge(new Vector2f(-item.width()/2,-item.depth()/2),new Vector2f(-item.width()/2,item.depth()/2)));
        for(var edge:outline){
            terraceEdge(art,rail,item,scene,edge);
        }
        if(rail.getQuantity()==0)return;
        rail.updateGeometricState();
        jme3tools.optimize.GeometryBatchFactory.optimize(rail,false);
        node.attachChild(rail);
    }

    private static void terraceEdge(GladeArt art,Node rail,GladeDesign.Item item,GladeDesign scene,Pentagon.Edge edge){
        var direction=edge.to().subtract(edge.from());
        float length=direction.length();
        var outward=edge.normal();
        var normal=new Vector3f(outward.x,0,outward.y);
        int panels=Math.min(80,Math.max(1,(int)Math.ceil(length/.75f)));
        float span=length/panels;
        for(int i=0;i<panels;i++){
            var start=edge.from().add(direction.mult((i+.08f)/panels));
            var end=edge.from().add(direction.mult((i+.92f)/panels));
            var center=start.add(end).multLocal(.5f);
            if(!rimVisible(item,scene,center.x,center.y,normal) || !rimVisible(item,scene,start.x,start.y,normal)
                    || !rimVisible(item,scene,end.x,end.y,normal) || stairLanding(item,scene,center,normal))continue;
            float y=item.height();
            boolean wood=scene.hillside(item);
            String color=wood?"#594035":"#393c3a";
            art.box(rail,span,wood?.10f:.045f,wood?.09f:.045f,center.x,y+.47f,center.y,color).rotate(0,-FastMath.atan2(direction.y,direction.x),0);
            int bars=wood?1:Math.max(2,(int)Math.ceil(span/.18f));
            float barWidth=wood?.085f:.032f;
            for(int bar=0;bar<bars;bar++){
                var at=start.add(end.subtract(start).mult((bar+.5f)/bars));
                art.box(rail,barWidth,.44f,barWidth,at.x,y+.22f,at.y,color);
            }
        }
    }

    private static boolean stairLanding(GladeDesign.Item host,GladeDesign scene,Vector2f edge,Vector3f normal){
        var q=new Quaternion().fromAngleAxis(host.rotation()*FastMath.DEG_TO_RAD,Vector3f.UNIT_Y);
        var p=q.mult(new Vector3f(edge.x,0,edge.y)).addLocal(host.center().x(),0,host.center().z());
        var out=q.mult(normal).multLocal(.18f).addLocal(p);
        for(var part:scene.items()){
            if(part.kind()!=GladeDesign.Kind.STAIRS_STONE && part.kind()!=GladeDesign.Kind.STAIRS_WOOD)continue;
            if(!SceneRules.contains(part,out.x,out.z,.12f) || Math.abs(scene.top(part)-scene.top(host))>.16f)continue;
            var probe=new GladeDesign.Point(out.x,out.z);
            if(Math.abs(part.localZ(probe)+part.depth()/2)<.42f)return true;
        }
        return false;
    }

    static final class Builder {
        final List<Float> positions=new ArrayList<>(),normals=new ArrayList<>(),uv=new ArrayList<>();
        void triangle(Vector3f a,Vector3f b,Vector3f c){var normal=b.subtract(a).cross(c.subtract(a));if(normal.lengthSquared()<1e-12f)return;normal.normalizeLocal();for(var point:List.of(a,b,c)){positions.add(point.x);positions.add(point.y);positions.add(point.z);normals.add(normal.x);normals.add(normal.y);normals.add(normal.z);uv.add(Math.abs(normal.x)>.5f?point.z:point.x);uv.add(Math.abs(normal.y)>.5f?point.z:point.y);}}
        void quad(Vector3f a,Vector3f b,Vector3f c,Vector3f d){triangle(a,b,c);triangle(a,c,d);}
        Geometry geometry(GladeArt art,String name,String color){var mesh=new Mesh();mesh.setBuffer(VertexBuffer.Type.Position,3,array(positions));mesh.setBuffer(VertexBuffer.Type.Normal,3,array(normals));mesh.setBuffer(VertexBuffer.Type.TexCoord,2,array(uv));int[] indices=new int[positions.size()/3];for(int i=0;i<indices.length;i++)indices[i]=i;mesh.setBuffer(VertexBuffer.Type.Index,3,indices);if(positions.isEmpty())mesh.setBound(new com.jme3.bounding.BoundingBox(new Vector3f(),0,0,0));else mesh.updateBound();var geo=new Geometry(name,mesh);geo.setMaterial(art.material(color));return geo;}
        private float[] array(List<Float> list){var values=new float[list.size()];for(int i=0;i<values.length;i++)values[i]=list.get(i);return values;}
    }
}

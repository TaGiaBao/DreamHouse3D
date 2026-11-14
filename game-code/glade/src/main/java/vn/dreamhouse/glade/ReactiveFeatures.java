package vn.dreamhouse.glade;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.scene.VertexBuffer.Type;
import java.util.*;

final class ReactiveFeatures {
    static void path(GladeArt art,Node root,GladeDesign.Item item,GladeDesign scene){
        var rules=SceneRules.of(scene);int index=0;
        String style=item.form().finish().material();boolean masonry=style.equals("stone") || style.equals("brick");
        String masonryColor=style.equals("brick")?"#bf937c":"#b8bbaa";
        boolean ribbon=PathSurface.ribbon(item);
        boolean[] flat=new boolean[item.points().size()];
        float width=PathSurface.width(item);
        var buildings=scene.items().stream().filter(part->part.kind().building())
                .map(part->Map.entry(part,scene.placement(part))).toList();
        for(var p:item.points()){
            float ground=rules.walkSurface(p.x(),p.z());
            boolean inside=false;for(var entry:buildings)if(SceneRules.contains(entry.getKey(),p.x(),p.z(),0)){
                var house=entry.getKey();
                float base=entry.getValue().elevation();
                if(ground>=base-.10f && ground<base+house.height()-.05f){inside=true;break;}
            }if(inside){index++;continue;}
            var next=item.points().get(Math.min(item.points().size()-1,index+1));var prev=item.points().get(Math.max(0,index-1));
            float dx=next.x()-prev.x(),dz=next.z()-prev.z(),length=(float)Math.hypot(dx,dz);if(length<.01f){dx=0;dz=1;length=1;}
            float yaw=FastMath.atan2(dx,dz);Float water=rules.water(p.x(),p.z());
            float slope=Math.abs(rules.walkSurface(next.x(),next.z())-rules.walkSurface(prev.x(),prev.z()))/Math.max(.1f,length);
            if(ribbon && (water==null || water<=ground+.025f) && slope<=.32f){flat[index++]=true;continue;}
            var group=new Node("Đường thích ứng");group.setUserData("worldHeight",true);root.attachChild(group);
            if(water!=null && water>ground+.025f){
                float y=water+.18f;var board=art.box(group,.88f*width,masonry?.16f:.10f,.24f,p.x(),y-(masonry?.08f:.05f),p.z(),masonry?masonryColor:"#c5a574");board.rotate(0,yaw,0);board.setUserData("wallSurface",true);board.setUserData("walkFloor",true);group.setUserData("proceduralFeature","bridge");group.setUserData("bridgeMaterial",masonry?"stone":"wood");
                if(index%6==0)for(int side:new int[]{-1,1}){float x=p.x()+FastMath.cos(yaw)*side*.43f*width,z=p.z()-FastMath.sin(yaw)*side*.43f*width;float floor=scene.terrain().height(x,z);art.box(group,masonry?.20f:.10f,Math.max(.08f,y-floor),masonry?.20f:.10f,x,(y+floor)/2,z,masonry?masonryColor:"#9a805c").setUserData("wallSurface",true);if(!masonry)art.box(group,.09f,.60f,.09f,x,y+.30f,z,"#bda276").setUserData("wallSurface",true);}
                if(masonry)for(int side:new int[]{-1,1}){float x=p.x()+FastMath.cos(yaw)*side*.46f*width,z=p.z()-FastMath.sin(yaw)*side*.46f*width;var rail=art.box(group,.13f,.40f,.25f,x,y+.20f,z,masonryColor);rail.rotate(0,yaw,0);rail.setUserData("wallSurface",true);}
            }else{
                if(slope>.32f){float level=(float)Math.ceil(ground/.16f)*.16f+.03f;var step=art.box(group,.85f*width,.17f,.28f,p.x(),level-.085f,p.z(),style.equals("wood")?"#c5a574":"#c6bda7");step.rotate(0,yaw,0);step.setUserData("wallSurface",true);step.setUserData("walkFloor",true);group.setUserData("proceduralFeature","stairs");group.setUserData("stepMaterial",style.equals("wood")?"wood":"stone");}
                else for(int side:new int[]{-1,1}){var stone=art.sphere(group,.19f,p.x()+FastMath.cos(yaw)*side*.18f,ground+.025f,p.z()-FastMath.sin(yaw)*side*.18f,item.form().finish().variant()==1?"#c3987c":item.form().finish().variant()==2?"#a3ada8":"#c9c0a4");stone.setLocalScale(1.1f,.16f,.8f);stone.rotate(0,yaw,0);stone.setUserData("wallSurface",true);}
            }index++;
        }
        if(ribbon)PathSurface.render(art,root,item,scene,flat);
    }
    static void decorate(GladeArt art,Node node,GladeDesign.Item item,GladeDesign scene){
        if(item.kind().building() || item.kind()==GladeDesign.Kind.DECK)TerrainFoundation.build(art,node,item,scene);
        if(item.kind()==GladeDesign.Kind.PENTAGON)FacadeDetails.build(art,node,item,scene);
        else if(item.kind().building() || item.kind()==GladeDesign.Kind.DECK){foundation(art,node,item,scene);FacadeDetails.build(art,node,item,scene);ivy(art,node,item,scene);basePlants(art,node,item,scene);}
        if(item.kind().opening() && item.mount()!=null){
            if(item.mount().lift()>=1){float w=item.width(),h=item.height();var dormer=new Node("Cửa mái");art.box(dormer,w+.18f,h+.12f,.5f,0,0,-.23f,"#ded1b3").setUserData("wallSurface",true);art.roof(dormer,w+.2f,.55f,h/2+.05f,false,false);var host=scene.items().stream().filter(part->part.id()==item.mount().hostId()).findFirst().orElseThrow();GladePaint.apply(art,dormer,host.form().finish());node.attachChild(dormer);node.setUserData("proceduralFeature","dormer");}
            else if(item.kind()==GladeDesign.Kind.WINDOW_ARCH || item.kind()==GladeDesign.Kind.WINDOW_ROUND)balcony(art,node,item,scene);
        }
        if(item.kind()==GladeDesign.Kind.FENCE)gate(art,node,item,scene);
    }
    private static void basePlants(GladeArt art,Node node,GladeDesign.Item item,GladeDesign scene) {
        if(!item.kind().building() || scene.placement(item).elevation()-scene.terrain().height(item.center().x(),item.center().z())>.55f)return;
        for(int i=0;i<6;i++){float x=(i%2==0?-1:1)*(item.width()/2+.10f),z=(i/2-1)*item.depth()*.30f;if(StructureShell.coveredDetail(item,scene,new Vector3f(x,.08f,z)))continue;var leaf=art.sphere(node,.13f,x,.08f,z,i%2==0?"#91aa70":"#a5b982");leaf.setLocalScale(1.5f,.65f,1);if(i%3==0)art.sphere(node,.035f,x,.19f,z,"#e5d7a5");}
    }
    private static void foundation(GladeArt art,Node node,GladeDesign.Item item,GladeDesign scene){
        if(item.form().support()>=0)return;
        var placement=scene.placement(item);double angle=Math.toRadians(placement.rotation());var rules=SceneRules.of(scene);boolean stone=false;
        for(int sx:new int[]{-1,1})for(int sz:new int[]{-1,1}){
            float x=sx*item.width()*.44f,z=sz*item.depth()*.44f,wx=placement.center().x()+(float)(Math.cos(angle)*x+Math.sin(angle)*z),wz=placement.center().z()+(float)(-Math.sin(angle)*x+Math.cos(angle)*z);
            Float water=rules.water(wx+sx*.25f,wz+sz*.25f);if(water!=null && water>scene.terrain().height(wx,wz))stone=true;
        }
        for(float[] sample:new float[][]{{-item.width()/2-.5f,0},{item.width()/2+.5f,0},{0,-item.depth()/2-.5f},{0,item.depth()/2+.5f}}){
            float wx=placement.center().x()+(float)(Math.cos(angle)*sample[0]+Math.sin(angle)*sample[1]),wz=placement.center().z()+(float)(-Math.sin(angle)*sample[0]+Math.cos(angle)*sample[1]);
            if(rules.water(wx,wz)!=null)stone=true;
        }
        if(stone){float w=item.width()+.18f,d=item.depth()+.18f;for(int side:new int[]{-1,1}){
            art.box(node,w,.45f,.18f,0,.08f,side*d/2,"#a6aaa0");art.box(node,.18f,.45f,d,side*w/2,.08f,0,"#a6aaa0");
            for(int i=0;i<5;i++){var moss=art.sphere(node,.10f,-w/2+(i+.5f)*w/5,.11f,side*(d/2+.08f),"#91a778");moss.setLocalScale(1.6f,.3f,.5f);}
        }node.setUserData("waterFoundation",true);}
    }
    private static void ivy(GladeArt art,Node node,GladeDesign.Item host,GladeDesign scene){
        int count=0;float w=host.width()/2,d=host.depth()/2;
        for(var flora:scene.items())if((flora.kind()==GladeDesign.Kind.FLOWERS || flora.kind()==GladeDesign.Kind.TREE || flora.kind()==GladeDesign.Kind.HEDGE) && count<5){
            float x=host.localX(flora.center()),z=host.localZ(flora.center());if(Math.max(Math.abs(x)-w,Math.abs(z)-d)>1 || Math.abs(x)<w-.4f && Math.abs(z)<d-.4f)continue;
            boolean side=Math.abs(w-Math.abs(x))<Math.abs(d-Math.abs(z));x=Math.max(-w,Math.min(w,x));z=Math.max(-d,Math.min(d,z));if(side)x=Math.copySign(w+.06f,x);else z=Math.copySign(d+.06f,z);
            float rise=scene.roofRise(host),height=host.height()+Math.min(.4f,rise*.4f);
            if(!side)height=Math.min(height,host.height()+rise*Math.max(0,1-Math.abs(x)/(w+.12f)));
            height=Math.min(5,height);art.beam(node,new Vector3f(x,.1f,z),new Vector3f(x,Math.min(height,host.height()),z),.018f,"#8c9c68");
            for(int i=0;i<12;i++){float t=(i+.5f)/12,y=t*height,offset=FastMath.sin(i*2.4f)*.14f;float xx=side && y>host.height() && rise>.01f?Math.copySign((w+.12f)*(1-(y-host.height())/rise),x):x;var leaf=art.sphere(node,.11f,xx+(side?0:offset),y,z+(side?offset:0),i%3==0?"#aec187":"#879f69");leaf.setLocalScale(side?.3f:1,.65f,side?1:.3f);}node.setUserData("ivy",true);count++;
        }
    }
    private static void balcony(GladeArt art,Node node,GladeDesign.Item window,GladeDesign scene){
        var placement=scene.placement(window);var neighbors=new ArrayList<GladeDesign.Item>();
        var frontier=new ArrayDeque<GladeDesign.Item>();frontier.add(window);var visited=new HashSet<Long>();visited.add(window.id());
        while(!frontier.isEmpty()){var anchor=frontier.removeFirst();var ap=scene.placement(anchor);
            for(var other:scene.items())if(!visited.contains(other.id()) && (other.kind()==GladeDesign.Kind.WINDOW_ARCH || other.kind()==GladeDesign.Kind.WINDOW_ROUND) && other.mount()!=null && other.mount().hostId()==window.mount().hostId() && other.mount().lift()<1 && Math.abs(other.rotation()-window.rotation())<2){
                var p=scene.placement(other);if(p.center().distance(ap.center())<1.65f && Math.abs(p.elevation()-ap.elevation())<.35f){visited.add(other.id());neighbors.add(other);frontier.add(other);}
            }
        }
        if(neighbors.isEmpty() || neighbors.stream().anyMatch(other->other.id()<window.id()))return;
        double angle=Math.toRadians(placement.rotation());float low=-window.width()/2,high=window.width()/2;
        for(var neighbor:neighbors){var p=scene.placement(neighbor);float x=(float)(Math.cos(angle)*(p.center().x()-placement.center().x())-Math.sin(angle)*(p.center().z()-placement.center().z()));low=Math.min(low,x-neighbor.width()/2);high=Math.max(high,x+neighbor.width()/2);}
        float width=high-low+.25f,cx=(low+high)/2,y=-window.height()*.55f-.12f;art.box(node,width,.12f,.7f,cx,y,.33f,"#c4bca6");
        for(int i=0;i<=8;i++)art.box(node,.035f,.5f,.035f,low+(high-low)*i/8,y+.28f,.64f,"#939988");art.box(node,width,.05f,.04f,cx,y+.55f,.64f,"#b5bba7");node.setUserData("proceduralFeature","joined-balcony");
    }
    private static void gate(GladeArt art,Node node,GladeDesign.Item fence,GladeDesign scene){
        GladeDesign.Point previousGate=null;for(int i=1;i<fence.points().size();i++){var a=fence.points().get(i-1);var b=fence.points().get(i);float x=(a.x()+b.x())/2,z=(a.z()+b.z())/2;
            if(!scene.nearPath(x,z,.5f) || previousGate!=null && previousGate.distance(new GladeDesign.Point(x,z))<1.5f)continue;previousGate=new GladeDesign.Point(x,z);
            var gate=new Node("Cổng hàng rào");gate.setUserData("worldHeight",true);gate.setUserData("proceduralFeature","gate");float h=Math.max(1,fence.height());
            for(int side:new int[]{-1,1})art.box(gate,.12f,h,.12f,side*.48f,h/2,0,"#b2a78c");art.box(gate,1.1f,.12f,.15f,0,h+.06f,0,"#c4b99d");
            var wing=new Node("Cánh cổng mở");wing.setLocalTranslation(-.45f,0,0);wing.rotate(0,1.05f,0);for(int j=0;j<4;j++)art.box(wing,.055f,h*.65f,.06f,j*.13f,h*.38f,0,"#b9a984");gate.attachChild(wing);
            gate.rotate(0,FastMath.atan2(b.z()-a.z(),b.x()-a.x()),0);gate.setLocalTranslation(x,scene.terrain().height(x,z),z);node.attachChild(gate);
        }
    }
    static void trimGrass(Node meadow,GladeDesign scene){
        var rules=SceneRules.of(scene);
        meadow.depthFirstTraversal(part->{if(!(part instanceof Geometry geo) || !part.getName().equals("Cỏ"))return;
            Mesh mesh=geo.getUserData("grassSource");
            if(mesh==null){mesh=geo.getMesh();geo.setUserData("grassSource",mesh);}
            var p=(java.nio.FloatBuffer)mesh.getBuffer(Type.Position).getData();
            var index=mesh.getIndexBuffer();var kept=new int[index.size()];int count=0;
            Integer group=geo.getUserData("indicesPerBlade");int stride=group==null?3:group;
            for(int i=0;i<index.size();i+=stride){
                float x=0,z=0;for(int j=0;j<3;j++){int v=index.get(i+j);x+=p.get(v*3)/3;z+=p.get(v*3+2)/3;}
                x+=scene.land().centerX();z+=scene.land().centerZ();
                if(rules.enclosed(x,z) || rules.water(x,z)!=null)continue;
                for(int j=0;j<stride;j++)kept[count++]=index.get(i+j);
            }
            geo.setCullHint(count==0?Spatial.CullHint.Always:Spatial.CullHint.Inherit);
            if(count==0 || count==index.size()){geo.setMesh(mesh);return;}
            var trimmed=new Mesh();
            trimmed.setBuffer(mesh.getBuffer(Type.Position));trimmed.setBuffer(mesh.getBuffer(Type.Normal));
            var color=mesh.getBuffer(Type.Color);if(color!=null)trimmed.setBuffer(color);
            trimmed.setBuffer(Type.Index,3,Arrays.copyOf(kept,count));
            trimmed.updateBound();geo.setMesh(trimmed);
        });
    }
}

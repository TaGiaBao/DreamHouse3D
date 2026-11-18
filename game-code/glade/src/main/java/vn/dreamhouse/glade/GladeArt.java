package vn.dreamhouse.glade;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.scene.shape.*;

import java.util.*;

final class GladeArt {
    private final AssetManager assets;
    private final Map<String,Material> materials=new LinkedHashMap<>(64,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String,Material> eldest){return size()>512;}
    };
    private final Map<String,Mesh> boxes=new LinkedHashMap<>() {
        @Override protected boolean removeEldestEntry(Map.Entry<String,Mesh> eldest) { return size()>384; }
    };
    private final Random random=new Random(48);
    boolean joinedRoof(GladeDesign.Item item,GladeDesign design) {
        return design.roofFusion().joined(item.id());
    }

    GladeArt(AssetManager assets) { this.assets=assets; }

    Material material(String color) {
        return materials.computeIfAbsent(color,key -> {
            var mat=new Material(assets,"Common/MatDefs/Light/Lighting.j3md");
            var tint=color(key);
            mat.setBoolean("UseMaterialColors",true);
            mat.setColor("Diffuse",tint);
            mat.setColor("Ambient",tint);
            mat.setColor("Specular",new ColorRGBA(0.12f,0.11f,0.08f,1));
            mat.setFloat("Shininess",18);
            return mat;
        });
    }

    static ColorRGBA color(String hex) {
        int rgb=Integer.parseInt(hex.replace("#",""),16);
        return new ColorRGBA().setAsSrgb(((rgb>>16)&255)/255f,((rgb>>8)&255)/255f,(rgb&255)/255f,1);
    }

    Node meadow(GladeDesign design) {
        var land=meadow(design.halfWidth(),design.halfDepth());
        for(var child:land.getChildren()) {
            var p=child.getLocalTranslation();
            child.move(0,design.terrain().height(p.x+design.land().centerX(),p.z+design.land().centerZ()),0);
        }
        var scenery=new Node("Trang trí thung lũng");
        for(var child:new ArrayList<>(land.getChildren())){child.removeFromParent();scenery.attachChild(child);}
        scenery.updateGeometricState();jme3tools.optimize.GeometryBatchFactory.optimize(scenery,false);land.attachChild(scenery);
        land.attachChild(MeadowArt.grass(this,design));
        ReactiveFeatures.trimGrass(land,design);
        var mesh=TerrainMesh.build(this,design);land.attachChild(mesh);
        land.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);return land;
    }

    private Node meadow(float halfWidth,float halfDepth) {
        random.setSeed(48);
        float scaleX=halfWidth/9;
        float scaleZ=halfDepth/6.5f;
        var land=new Node("Thung lũng");
        for(var p:List.of(new Vector3f(-8,0,-4),new Vector3f(8,0,-4),
                new Vector3f(-8.5f,0,3),new Vector3f(8,0,4),new Vector3f(-5,0,-6))) {
            var tree=tree(1.3f);
            tree.setLocalTranslation(p.x*scaleX,0,p.z*scaleZ);
            land.attachChild(tree);
        }
        int posts=(int)Math.ceil(halfWidth*2/1.15f);
        for(int i=0;i<=posts;i++) {
            float x=-halfWidth+i*halfWidth*2/posts;
            box(land,0.11f,0.80f,0.11f,x,0.4f,-halfDepth+0.1f,"#b38b5a");
        }
        box(land,halfWidth*2,0.10f,0.09f,0,0.29f,-halfDepth+0.1f,"#ddc695");
        box(land,halfWidth*2,0.10f,0.09f,0,0.62f,-halfDepth+0.1f,"#ddc695");
        for(int i=0;i<65;i++) {
            float angle=i*2.399f;
            float x=FastMath.cos(angle)*(halfWidth-0.4f+random.nextFloat()*0.8f);
            float z=FastMath.sin(angle)*(halfDepth-0.6f+random.nextFloat()*0.5f);
            var pebble=sphere(land,0.10f+random.nextFloat()*0.16f,x,0.07f,z,"#b4b29b");
            pebble.setLocalScale(1,0.55f,0.8f);
            if(i%3==0) {
                var flowers=flowers();
                flowers.setLocalTranslation(x,0,z);
                land.attachChild(flowers);
            }
        }
        land.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        return land;
    }

    Node render(GladeDesign.Item item,GladeDesign design) {
        if(item.kind()==GladeDesign.Kind.PATH)return renderPath(item,design);
        var node=new Node(item.kind().name());
        node.setUserData("itemId",item.id());
        if(AttachmentRules.hidden(item,design)){node.setUserData("hiddenAttachment",true);return node;}
        boolean crafted=GladeVariants.craft(this,node,item);
        boolean plain=!crafted && PlainStructure.render(this,node,item,design);
        if(!crafted && !plain) switch(item.kind()) {
            case HOUSE, MANOR -> house(node,item,design);
            case CANAL_HOUSE, BRICK_HOUSE -> urbanHouse(node,item,design,item.kind()==GladeDesign.Kind.BRICK_HOUSE);
            case TOWER -> tower(node,item,design);
            case PAVILION -> pavilion(node,item,design);
            case BRIDGE -> bridge(node,item,design);
            case WALL -> wall(node,item,design);
            case FENCE -> fence(node,item,design);
            case TREE -> {
                node.attachChild(tree(1));
                node.setLocalTranslation(item.center().x(),0,item.center().z());
            }
            case FLOWERS -> {
                node.attachChild(flowers());
                node.setLocalTranslation(item.center().x(),0,item.center().z());
            }
            case WATER -> {
                if(WaterFlow.source(item)){
                    var rules=SceneRules.of(design);
                    node.attachChild(WaterFlow.render(this,item,design,rules.flows.get(item.id()),rules.takeSurface(item.id())));
                }
                else if(item.points().isEmpty()){var water=sphere(node,1,0,0.01f,0,"#79b4b0");water.setLocalScale(item.width()/2,0.035f,item.depth()/2);}
                else {node.attachChild(WaterMesh.build(this,item,design));WaterBank.decorate(this,node,item,design);node.setUserData("paintedWater",true);}
                node.setLocalTranslation(item.center().x(),0,item.center().z());
            }
            default -> new GladeParts(this).render(node,item,design);
        }
        if(!crafted) GladeVariants.apply(this,node,item);
        boolean hasJoinedRoof=item.kind().roofHost() && joinedRoof(item,design);
        if(hasJoinedRoof) {
            var joined=design.roofFusion().mesh(this,item);
            if(joined!=null){
                joined.depthFirstTraversal(part->{if(part instanceof Geometry && !Boolean.TRUE.equals(part.getUserData("wallSurface")))part.setUserData("roofSurface",true);});
                node.attachChild(joined);
            }
        }
        if(item.kind()==GladeDesign.Kind.WALL || item.kind()==GladeDesign.Kind.BRICK_WALL || item.kind()==GladeDesign.Kind.TIMBER_WALL || item.kind()==GladeDesign.Kind.FENCE)
            node.depthFirstTraversal(part->{if(part instanceof Geometry)part.setUserData("wallSurface",true);});
        if(Set.of(GladeDesign.Kind.DECK,GladeDesign.Kind.PERGOLA,GladeDesign.Kind.BENCH,GladeDesign.Kind.STAIRS_WOOD,GladeDesign.Kind.STAIRS_STONE).contains(item.kind()))
            node.depthFirstTraversal(part->{if(part instanceof Geometry && part.getName().equals("Khối"))part.setUserData("wallSurface",true);});
        var finish=item.form().finish();
        if(item.kind().roof() && item.mount()!=null) finish=design.items().stream().filter(host->host.id()==item.mount().hostId()).findFirst().orElseThrow().form().finish();
        if(item.kind().building())ScenicDetails.house(this,node,item,design);
        GladePaint.apply(this,node,finish);
        var attachedRoof=item.kind().roofHost()?design.attachedRoof(item.id()):null;
        if((design.hillside(item) || attachedRoof!=null && attachedRoof.kind()==GladeDesign.Kind.ROOF_CURVED)
                && item.kind()!=GladeDesign.Kind.PENTAGON)
            curvedRoofFrame(node,item,design);
        else if(streetWindows(item,design))
            streetFacade(node,item,design);
        ReactiveFeatures.decorate(this,node,item,design);
        PlainStructure.terraceRail(this,node,item,design);
        if(item.kind().chimney())ScenicDetails.smoke(this,node,item.height());
        if(item.kind().roof() || item.kind().chimney()) node.setUserData("roofPart",true);
        if(!item.kind().stroke()) {
            var p=design.placement(item);node.setLocalTranslation(p.center().x(),p.elevation(),p.center().z());
            if((item.kind().building() || item.kind()==GladeDesign.Kind.DECK) && item.form().stilts()>.02f) {
                float lift=item.form().stilts();
                for(int x:new int[]{-1,1}) for(int z:new int[]{-1,1}) {
                    float localX=x*item.width()*.36f,localZ=z*item.depth()*.36f;
                    float floor=item.form().support()>=0?-lift:TerrainFoundation.ground(design,p,localX,localZ);
                    if(floor>=-.02f)continue;
                    box(node,.20f,-floor+.05f,.20f,localX,floor/2,localZ,"#a27d52");
                    box(node,.32f,.12f,.32f,localX,floor+.04f,localZ,"#c4b18e");
                }
            }
        } else if(item.kind()!=GladeDesign.Kind.BRIDGE) {
            for(var child:node.getChildren()) {if(Boolean.TRUE.equals(child.getUserData("worldHeight")))continue;var p=child.getLocalTranslation();child.move(0,design.terrain().height(p.x,p.z),0);}
        }
        if(item.kind().footprint()) node.rotate(0,design.placement(item).rotation()*FastMath.DEG_TO_RAD,0);
        ArchitectureJoin.apply(node,item,design);
        node.setShadowMode(item.kind()==GladeDesign.Kind.FLOWERS
            ?RenderQueue.ShadowMode.Receive:RenderQueue.ShadowMode.CastAndReceive);
        return node;
    }

    private void curvedRoofFrame(Node node,GladeDesign.Item item,GladeDesign design){
        if(item.height()<=.4f)return;
        float x=item.width()/2,z=item.depth()/2,h=item.height();
        for(int side:new int[]{-1,1}){
            for(int end:new int[]{-1,1}){
                float px=side*x,pz=end*z;
                if(!StructureShell.coveredDetail(item,design,new Vector3f(px,h/2,pz)))
                    box(node,.14f,h,.14f,px,h/2,pz,"#4c342a");
            }
            if(!StructureShell.coveredDetail(item,design,new Vector3f(0,h-.1f,side*z)))
                box(node,item.width(),.16f,.17f,0,h-.1f,side*z,"#594033");
            if(!StructureShell.coveredDetail(item,design,new Vector3f(side*x,h-.1f,0)))
                box(node,.17f,.16f,item.depth(),side*x,h-.1f,0,"#594033");
        }
        if(design.hillside(item) && h>1.6f){
            int bays=Math.clamp((int)(item.width()/2f),1,5);
            for(int i=0;i<bays;i++){
                float px=((i+.5f)/bays-.5f)*item.width();
                var hook=new Vector3f(px,h-.18f,z+.12f);
                if(StructureShell.coveredDetail(item,design,hook))continue;
                box(node,.026f,.14f,.026f,px,h-.17f,z+.18f,"#4c342a");
                var lantern=sphere(node,.095f,px,h-.31f,z+.18f,"#ce492c");
                lantern.setLocalScale(1,1.18f,1);
                var lit=lantern.getMaterial().clone();
                lit.setColor("GlowColor",new ColorRGBA(.54f,.09f,.025f,1));
                lantern.setMaterial(lit);
            }
        }
    }

    static boolean streetWindows(GladeDesign.Item item,GladeDesign design){
        var roof=design.attachedRoof(item.id());
        return item.kind()==GladeDesign.Kind.HOUSE && item.height()>=1.55f
                && item.form().finish().variant()==18 && !design.hillside(item)
                && (roof==null || roof.kind()!=GladeDesign.Kind.ROOF_CURVED)
                && (design.roofFusion().joined(item.id()) || design.hasUpper(item.id()) || item.form().support()>=0);
    }
    static boolean streetWindowCovers(GladeDesign.Item item,GladeDesign design,Vector3f point,float clearance){
        if(!streetWindows(item,design) || Math.abs(Math.abs(point.z)-item.depth()/2)>.1f+clearance
                || Math.abs(point.y-item.height()*.60f)>.36f+clearance)return false;
        int bays=Math.clamp((int)(item.width()/.95f),1,16);
        for(int i=0;i<bays;i++){
            float x=((i+.5f)/bays-.5f)*item.width();
            if(Math.abs(point.x-x)>.31f+clearance)continue;
            float z=Math.copySign(item.depth()/2+.025f,point.z),y=item.height()*.60f;
            if(!StructureShell.coveredDetail(item,design,new Vector3f(x,y,z))
                    && !design.blocksOpening(item,x,z,y,.31f))return true;
        }
        return false;
    }

    private void streetFacade(Node node,GladeDesign.Item item,GladeDesign design){
        if(item.height()<1.55f)return;
        var facade=new Node("Mặt tiền Vieille Ville");
        int bays=Math.clamp((int)(item.width()/.95f),1,16);
        float y=item.height()*.60f,z=item.depth()/2+.025f;
        var shutters=new String[]{"#6b7562","#6d7b82","#837363","#627a73"};
        String color=shutters[(int)Math.floorMod(item.id(),shutters.length)];
        for(int side:new int[]{-1,1})for(int i=0;i<bays;i++){
            float x=((i+.5f)/bays-.5f)*item.width();
            if(StructureShell.coveredDetail(item,design,new Vector3f(x,y,side*z))
                    || design.blocksOpening(item,x,side*z,y,.31f))continue;
            box(facade,.34f,.58f,.055f,x,y,side*z,"#4a5153");
            box(facade,.24f,.44f,.065f,x,y,side*z,"#92acb0");
            for(int leaf:new int[]{-1,1})box(facade,.12f,.55f,.057f,x+leaf*.23f,y,side*z,color);
            box(facade,.53f,.07f,.15f,x,y-.32f,side*z,"#aa9a83");
        }
        if(facade.getQuantity()==0)return;
        facade.updateGeometricState();
        jme3tools.optimize.GeometryBatchFactory.optimize(facade,false);
        node.attachChild(facade);
    }

    private void house(Node node,GladeDesign.Item item,GladeDesign design) {
        float w=item.width(),d=item.depth(),h=item.height();
        box(node,w,h,d,0,h/2,0,"#e6d1a9").setUserData("wallSurface",true);
        box(node,w+0.12f,0.22f,d+0.12f,0,0.11f,0,"#b3a186");
        for(int x:new int[]{-1,1}) for(int z:new int[]{-1,1})
            box(node,0.15f,h,0.15f,x*(w/2-0.04f),h/2,z*(d/2-0.04f),"#8c6139");
        for(int side:new int[]{-1,1}) {
            box(node,w,0.14f,0.13f,0,h-0.10f,side*d/2,"#9b7045");
            box(node,0.13f,0.14f,d,side*w/2,h-0.10f,0,"#9b7045");
            facade(node,item,design,side);
            int floors=Math.max(1,Math.min(3,(int)(h/1.8f)));
            for(int floor=0;floor<floors;floor++) {
                if(design.blocksOpening(item,side*w/2,0,(floor+.55f)*h/floors,.45f)) continue;
                var window=window();
                window.setLocalScale(Math.min(1,Math.min(h/floors/2.1f,d*0.6f)));
                window.setLocalTranslation(side*(w/2+0.025f),(floor+0.55f)*h/floors,0);
                window.rotate(0,side*FastMath.HALF_PI,0);
                node.attachChild(window);
            }
            for(int floor=1;floor<floors;floor++) {
                box(node,w,0.10f,0.12f,0,h*floor/floors,side*d/2,"#9b7045");
                box(node,0.12f,0.10f,d,side*w/2,h*floor/floors,0,"#9b7045");
            }
        }
        if(!design.customRoof(item.id()) && !joinedRoof(item,design) && design.roofRise(item)>.02f) buildingRoof(node,item,design,true,false);
        else if(!design.customRoof(item.id()) && !joinedRoof(item,design)) {
            var top=roofGroup(node);
            box(top,w+0.18f,0.18f,d+0.18f,0,h+0.04f,0,"#aa7952");
            box(top,w+0.12f,0.07f,d+0.12f,0,h+0.17f,0,"#d4b991");
        }
        node.setLocalTranslation(item.center().x(),0,item.center().z());
    }

    private void urbanHouse(Node node,GladeDesign.Item item,GladeDesign design,boolean brick) {
        float w=item.width(),d=item.depth(),h=item.height();
        String[] pastel={"#d8a16d","#88b7ae","#c99a96","#b8bc86"};
        String wall=brick?"#ac745d":pastel[(int)(item.id()%pastel.length)];
        String trim=brick?"#d9ccb3":"#f1e1bc";
        box(node,w,h,d,0,h/2,0,wall).setUserData("wallSurface",true);
        box(node,w+0.16f,0.28f,d+0.16f,0,0.14f,0,brick?"#8e8c7c":"#b6ac94");
        int floors=Math.max(1,Math.min(4,(int)(h/1.6f)));
        for(int floor=1;floor<=floors;floor++) {
            float y=h*floor/floors-0.05f;
            box(node,w+0.12f,0.11f,d+0.12f,0,y,0,trim);
        }
        if(brick) {
            int rows=Math.max(2,Math.min(20,(int)(h/0.26f)));
            for(int row=1;row<rows;row++) for(int side:new int[]{-1,1}) {
                box(node,w,0.018f,0.018f,0,row*h/rows,side*(d/2+0.01f),"#c49a7c");
                box(node,0.018f,0.018f,d,side*(w/2+0.01f),row*h/rows,0,"#c49a7c");
            }
            for(int side:new int[]{-1,1}) for(int end:new int[]{-1,1})
                for(int row=0;row<Math.min(14,rows);row++)
                    box(node,0.25f+(row%2)*0.10f,h/rows-0.03f,0.12f,
                            side*(w/2-0.10f),(row+0.5f)*h/rows,end*d/2,trim);
        }
        for(int side=0;side<4;side++) {
            float span=side%2==0?w:d;
            var face=new Node("Mặt phố");
            int count=Math.max(1,Math.min(16,(int)(span/1.25f)));
            float spacing=span/count;
            float openingWidth=Math.min(0.69f,spacing*0.56f);
            float openingHeight=Math.min(0.90f,h/floors*0.52f);
            for(int floor=0;floor<floors;floor++) for(int column=0;column<count;column++) {
                float x=(column+0.5f)*spacing-span/2;
                float y=(floor+0.60f)*h/floors;
                float faceAngle=side*FastMath.HALF_PI;
                float fx=FastMath.cos(faceAngle)*x+FastMath.sin(faceAngle)*(side%2==0?d/2:w/2);
                float fz=-FastMath.sin(faceAngle)*x+FastMath.cos(faceAngle)*(side%2==0?d/2:w/2);
                if(design.blocksOpening(item,fx,fz,y,.40f)) continue;
                if(side==0 && floor==0 && column==count/2) continue;
                box(face,openingWidth+0.18f,openingHeight+0.16f,0.10f,x,y,0.025f,trim);
                arch(face,openingWidth,openingHeight,0.06f,x,y,0.105f,"#67929e");
                box(face,0.035f,openingHeight,0.05f,x,y,0.15f,trim);
                box(face,openingWidth,0.035f,0.05f,x,y-0.04f,0.15f,trim);
                box(face,openingWidth+0.24f,0.08f,0.23f,x,y-openingHeight/2-0.10f,0.10f,trim);
                if(!brick) for(int shutter:new int[]{-1,1}) {
                    float sx=x+shutter*(openingWidth/2+0.17f);
                    box(face,0.20f,openingHeight,0.07f,sx,y,0.08f,"#527f70");
                    for(int slat=0;slat<4;slat++) box(face,0.19f,0.028f,0.035f,sx,
                            y-openingHeight*.36f+slat*openingHeight*.24f,0.13f,"#79a18a");
                }
            }
            if(side==0) {
                float doorX=(count/2+0.5f)*spacing-span/2;
                if(!design.blocksOpening(item,doorX,d/2,.85f,.5f)) {
                    var entrance=door();entrance.setLocalScale(Math.min(0.85f,Math.min(h/2.4f,span/1.6f)));
                    entrance.setLocalTranslation(doorX,0,0.05f);face.attachChild(entrance);
                }
                if(brick && floors>1) {
                    float balconyWidth=Math.min(w-0.3f,1.9f),by=h/floors;
                    box(face,balconyWidth,0.12f,0.62f,0,by,0.23f,trim);
                    for(int rail=0;rail<=6;rail++) box(face,0.035f,0.42f,0.035f,
                            (rail/6f-0.5f)*balconyWidth,by+0.24f,0.49f,"#536468");
                    box(face,balconyWidth,0.045f,0.04f,0,by+0.46f,0.49f,"#536468");
                }
            }
            float yaw=side*FastMath.HALF_PI;
            face.rotate(0,yaw,0);
            face.setLocalTranslation(FastMath.sin(yaw)*w/2,0,FastMath.cos(yaw)*d/2);
            node.attachChild(face);
        }
        var roofParts=roofGroup(node);
        if(!design.customRoof(item.id()) && !joinedRoof(item,design) && design.roofRise(item)>.02f) {
            buildingRoof(roofParts,item,design,true,brick);
            if(!brick && design.roofRise(item)>w*.25f) {
                int steps=5;
                for(int end:new int[]{-1,1}) for(int step=0;step<steps;step++) {
                    float sw=(w+0.12f)*(1-step/(float)steps);
                    float sh=(w/2+0.32f)*0.69f/steps;
                    float y=h+(step+0.5f)*sh;
                    box(roofParts,sw,sh+0.025f,0.18f,0,y,end*(d/2+0.35f),wall);
                    box(roofParts,sw+0.09f,0.055f,0.23f,0,y+sh/2,end*(d/2+0.35f),trim);
                }
            } else if(brick && design.roofRise(item)>w*.25f) {
                float dw=Math.min(1.05f,d*0.40f),scale=Math.min(1,w/2);
                var dormer=new Node("Cửa áp mái");
                box(dormer,dw,0.80f,0.70f,0,0.40f,-0.18f,trim);
                arch(dormer,dw*0.60f,0.54f,0.08f,0,0.42f,0.20f,"#6b939e");
                box(dormer,0.035f,0.53f,0.04f,0,0.42f,0.26f,trim);
                float span=dw/2+0.08f,rise=0.32f;
                for(int side:new int[]{-1,1}) {
                    var panel=box(dormer,(float)Math.hypot(span,rise),0.08f,0.90f,
                            side*span/2,0.80f+rise/2,-0.18f,"#637c87");
                    panel.rotate(0,0,-side*FastMath.atan2(rise,span));
                }
                dormer.rotate(0,-FastMath.HALF_PI,0);dormer.setLocalScale(scale);
                dormer.setLocalTranslation(-w*0.30f,h+(w/2+0.32f)*0.62f*0.40f,0);
                roofParts.attachChild(dormer);
            }
        } else if(!design.customRoof(item.id()) && !joinedRoof(item,design)) {
            box(roofParts,w+0.20f,0.16f,d+0.20f,0,h+0.05f,0,trim);
            for(int side:new int[]{-1,1}) {
                box(roofParts,w+0.12f,0.25f,0.12f,0,h+0.20f,side*d/2,trim);
                box(roofParts,0.12f,0.25f,d,side*w/2,h+0.20f,0,trim);
            }
        }
        node.setLocalTranslation(item.center().x(),0,item.center().z());
    }

    private void tower(Node node,GladeDesign.Item item,GladeDesign design) {
        float radius=item.width()/2;
        float h=item.height();
        cylinder(node,radius,h,0,h/2,0,"#b7ad96");
        cylinder(node,radius+0.12f,0.18f,0,0.09f,0,"#c7bda5");
        int rows=Math.max(2,Math.min(12,(int)(h/0.32f)));
        int columns=Math.max(12,Math.min(28,(int)(radius*14)));
        for(int row=0;row<rows;row++) for(int column=0;column<columns;column++) {
            float angle=(column+(row%2)*0.5f)*FastMath.TWO_PI/columns;
            var brick=box(node,FastMath.TWO_PI*radius/columns-0.025f,h/rows-0.025f,0.07f,
                    FastMath.sin(angle)*radius,(row+0.5f)*h/rows,FastMath.cos(angle)*radius,
                    (row+column)%4==0?"#c1b69d":"#ada58e");
            brick.setUserData("wallSurface",true);brick.rotate(0,angle,0);
        }
        int floors=Math.max(1,(int)(h/2));
        boolean hasDoor=design.items().stream().filter(stroke->stroke.kind()==GladeDesign.Kind.PATH)
                .flatMap(stroke->stroke.points().stream()).anyMatch(point->
                        Math.abs(item.localZ(point)-item.depth()/2)<0.65f
                                && Math.abs(item.localX(point))<item.width()*0.3f);
        for(int floor=0;floor<floors;floor++) for(int side=0;side<4;side++) {
            if(hasDoor && floor==0 && side==0) continue;
            float angle=side*FastMath.HALF_PI;
            var opening=window();
            opening.setLocalScale(Math.min(0.85f,Math.min(radius,h/floors/1.8f)));
            opening.setLocalTranslation(FastMath.sin(angle)*(radius+0.05f),(floor+0.6f)*h/floors,
                    FastMath.cos(angle)*(radius+0.05f));
            opening.rotate(0,angle,0);
            node.attachChild(opening);
        }
        var towerRoof=roofGroup(node);
        if(design.roofRise(item)>.02f) {
            float roofHeight=design.roofRise(item);
            var cone=new Geometry("Mái tháp",new Cylinder(2,32,0.005f,radius+0.26f,roofHeight,true,false));
            cone.setMaterial(material("#668494"));cone.setUserData("roofSurface",true);
            cone.rotate(-FastMath.HALF_PI,0,0);
            cone.setLocalTranslation(0,h+roofHeight/2,0);
            towerRoof.attachChild(cone);
            for(int ring=0;ring<5;ring++) {
                float t=ring/5f;
                var band=new Geometry("Viền ngói",new Torus(32,8,0.025f,(radius+0.26f)*(1-t)));
                band.setMaterial(material("#819ba4"));
                band.rotate(-FastMath.HALF_PI,0,0);
                band.setLocalTranslation(0,h+t*roofHeight,0);
                towerRoof.attachChild(band);
            }
        } else {
            cylinder(towerRoof,radius+0.18f,0.16f,0,h+0.08f,0,"#c7bda5");
        }
        if(hasDoor) {
            var entrance=door();
            entrance.setLocalScale(Math.min(1,h/2.1f));
            entrance.setLocalTranslation(0,0,radius+0.03f);
            node.attachChild(entrance);
        }
        node.setLocalScale(1,1,item.depth()/item.width());
        node.setLocalTranslation(item.center().x(),0,item.center().z());
    }

    void cylinder(Node node,float radius,float height,float x,float y,float z,String tint) {
        var geometry=new Geometry("Trụ",new Cylinder(2,32,radius,height,true));
        geometry.rotate(-FastMath.HALF_PI,0,0);
        geometry.setMaterial(material(tint));
        geometry.setLocalTranslation(x,y,z);
        node.attachChild(geometry);
    }

    private void pavilion(Node node,GladeDesign.Item item,GladeDesign design) {
        float w=item.width(),d=item.depth(),h=item.height();
        box(node,w+0.18f,0.18f,d+0.18f,0,0.09f,0,"#b29b75");
        for(int side:new int[]{-1,1}) for(int end:new int[]{-1,1}) {
            box(node,0.16f,h,0.16f,side*(w/2-0.12f),h/2,end*(d/2-0.12f),"#a77c4d");
        }
        for(int end:new int[]{-1,1})
            box(node,w,0.16f,0.14f,0,h-0.08f,end*(d/2-0.12f),"#a77c4d");
        if(design.roofRise(item)>.02f) buildingRoof(node,item,design,false,false);
        else box(roofGroup(node),w+0.30f,0.16f,d+0.30f,0,h+0.04f,0,"#b58b59");
        box(node,Math.max(0.6f,w-0.7f),0.10f,0.48f,0,0.55f,-d/2+0.43f,"#cba572");
        node.setLocalTranslation(item.center().x(),0,item.center().z());
    }

    private void bridge(Node node,GladeDesign.Item item,GladeDesign design) {
        var a=item.points().getFirst();
        var b=item.points().getLast();
        float length=Math.max(0.30f,a.distance(b));
        float yaw=FastMath.atan2(b.x()-a.x(),b.z()-a.z());
        int planks=Math.max(3,(int)Math.ceil(length/0.23f));
        float width=Math.max(1,Math.min(3,item.width()));
        var rules=SceneRules.of(design);
        float start=rules.walkSurface(a.x(),a.z()),end=rules.walkSurface(b.x(),b.z());
        Vector3f previousLeft=null,previousRight=null;
        for(int i=0;i<=planks;i++) {
            float t=(float)i/planks;
            float y=FastMath.interpolateLinear(t,start,end)+0.16f+FastMath.sin(t*FastMath.PI)*item.height()*0.45f;
            box(node,width,0.10f,length/planks+0.025f,0,y,t*length,"#b58a5a").setUserData("walkFloor",true);
            if(i%3==0 || i==planks) {
                var left=new Vector3f(-width/2,y+0.68f,t*length);
                var right=new Vector3f(width/2,y+0.68f,t*length);
                for(int side:new int[]{-1,1})
                    box(node,0.09f,0.78f,0.09f,side*width/2,y+0.35f,t*length,"#936b41");
                if(previousLeft!=null) {
                    beam(node,previousLeft,left,0.045f,"#c6a371");
                    beam(node,previousRight,right,0.045f,"#c6a371");
                }
                previousLeft=left;
                previousRight=right;
            }
        }
        node.rotate(0,yaw,0);
        node.setLocalTranslation(a.x(),0,a.z());
    }

    private void fence(Node node,GladeDesign.Item item,GladeDesign design) {
        float h=Math.min(1.8f,item.height());
        var posts=new ArrayList<GladeDesign.Point>();
        posts.add(item.points().getFirst());
        for(int i=1;i<item.points().size();i++) {
            var a=item.points().get(i-1);
            var b=item.points().get(i);
            int steps=Math.max(1,(int)Math.ceil(a.distance(b)/0.85f));
            for(int step=1;step<=steps;step++) {
                float t=(float)step/steps;
                var point=new GladeDesign.Point(FastMath.interpolateLinear(t,a.x(),b.x()),
                        FastMath.interpolateLinear(t,a.z(),b.z()));
                if(posts.getLast().distance(point)>0.7f) posts.add(point);
            }
        }
        var end=item.points().getLast();
        if(posts.getLast().distance(end)>0.05f) posts.add(end);
        for(int i=0;i<posts.size();i++) {
            var point=posts.get(i);
            box(node,0.11f,h,0.11f,point.x(),h/2,point.z(),"#a77b49");
            if(i>0) for(float level:new float[]{0.35f,0.75f}) {
                var midpoint=point;var earlier=posts.get(i-1);
                if(design.nearPath((point.x()+earlier.x())/2,(point.z()+earlier.z())/2,.64f))continue;
                var previous=posts.get(i-1);
                beam(node,new Vector3f(previous.x(),h*level,previous.z()),
                        new Vector3f(point.x(),h*level,point.z()),0.05f,"#d4b889");
            }
        }
    }

    private void facade(Node node,GladeDesign.Item item,GladeDesign design,int side) {
        float front=side*item.depth()/2;
        Float doorX=null;
        for(var stroke:design.items()) if(stroke.kind()==GladeDesign.Kind.PATH) {
            for(var point:stroke.points()) if(Math.abs(item.localZ(point)-front)<0.50f
                    && Math.abs(item.localX(point))<item.width()/2-0.55f) {
                doorX=item.localX(point)*side;
                break;
            }
        }
        var facade=new Node("Mặt tiền");
        if(doorX!=null && !design.blocksOpening(item,doorX*side,front,.85f,.45f)) {
            var entrance=door();
            entrance.setLocalScale(Math.min(1,item.height()/2.1f));
            entrance.setLocalTranslation(doorX,0,0.03f);
            facade.attachChild(entrance);
        }
        int windows=Math.max(1,Math.min(16,(int)(item.width()/1.55f)));
        int floors=Math.max(1,Math.min(3,(int)(item.height()/1.8f)));
        for(int floor=0;floor<floors;floor++) for(int i=0;i<windows;i++) {
            float x=(i+0.5f)*item.width()/windows-item.width()/2;
            if(design.blocksOpening(item,x*side,front,(floor+.56f)*item.height()/floors,.45f)) continue;
            if(floor==0 && doorX!=null && Math.abs(x-doorX)<0.95f) continue;
            var window=window();
            window.setLocalScale(Math.min(1,Math.min(item.height()/floors/2.1f,item.width()*0.6f)));
            window.setLocalTranslation(x,(floor+0.56f)*item.height()/floors,0.04f);
            facade.attachChild(window);
        }
        facade.setLocalTranslation(0,0,side*(item.depth()/2+0.02f));
        if(side<0) facade.rotate(0,FastMath.PI,0);
        node.attachChild(facade);
    }

    Node door() {
        var node=new Node("Cửa tự tạo");
        arch(node,1.12f,1.80f,0.14f,0,0.90f,0.01f,"#936334");
        arch(node,0.87f,1.57f,0.10f,0,0.785f,0.11f,"#4d8980");
        box(node,0.035f,1.05f,0.04f,0,0.55f,0.18f,"#356c66");
        sphere(node,0.055f,0.29f,0.73f,0.21f,"#d4a74f");
        box(node,1.2f,0.13f,0.45f,0,0.065f,0.22f,"#c9bea4");
        return node;
    }

    Node window() {
        var node=new Node("Cửa sổ");
        arch(node,0.89f,1.08f,0.12f,0,0,0.02f,"#9d713e");
        arch(node,0.65f,0.85f,0.10f,0,0,0.10f,"#82b6bd");
        box(node,0.045f,0.85f,0.06f,0,-0.01f,0.17f,"#d4b277");
        box(node,0.65f,0.05f,0.06f,0,-0.05f,0.17f,"#d4b277");
        box(node,1.0f,0.17f,0.30f,0,-0.63f,0.14f,"#9b6b38");
        box(node,0.89f,0.06f,0.27f,0,-0.51f,0.14f,"#6b8b4e");
        for(int i=0;i<5;i++) sphere(node,0.065f,(i-2)*0.16f,-0.45f,0.20f,i%2==0?"#eaa895":"#e4bc67");
        return node;
    }

    private void roof(Node node,float width,float depth,float height,boolean chimney) {
        roof(node,width,depth,height,chimney,false);
    }

    Node roofGroup(Node parent) {
        var group=new Node("Bộ mái");group.setUserData("roofPart",true);parent.attachChild(group);return group;
    }

    void buildingRoof(Node parent,GladeDesign.Item item,GladeDesign design,boolean chimney,boolean slate) {
        var form=item.form();
        roof(parent,item.width()+form.roofLeft()+form.roofRight(),item.depth()+form.roofFront()+form.roofBack(),item.height(),chimney,slate,design.roofRise(item));
        parent.getChild(parent.getQuantity()-1).move((form.roofRight()-form.roofLeft())/2,0,(form.roofFront()-form.roofBack())/2);
    }

    void roof(Node parent,float width,float depth,float height,boolean chimney,boolean slate,float rise) {
        var shaped=roofGroup(parent);roof(shaped,width,depth,0,chimney,slate);
        shaped.setLocalScale(1,rise/((width/2+.32f)*.62f),1);shaped.setLocalTranslation(0,height,0);
    }

    void roof(Node parent,float width,float depth,float height,boolean chimney,boolean slate) {
        var node=roofGroup(parent);
        float span=width/2+0.32f;
        float run=depth/2+0.32f;
        float rise=span*0.62f;
        TileRoof.build(this,node,width,depth,height,slate);
        if(chimney) {
            box(node,0.34f,0.85f,0.38f,span*0.52f,height+rise*0.65f,0.35f,"#b6a590");
            box(node,0.45f,0.12f,0.49f,span*0.52f,height+rise*0.65f+0.44f,0.35f,"#cab89f");
        }
        node.updateGeometricState();jme3tools.optimize.GeometryBatchFactory.optimize(node,false);
        node.depthFirstTraversal(part->{if(part instanceof Geometry geo && geo.getMesh().getBuffer(VertexBuffer.Type.Normal)!=null)part.setUserData("roofSurface",true);});
    }

    Node renderPath(GladeDesign.Item item,GladeDesign design) {
        var node=new Node(item.kind().name());
        node.setUserData("itemId",item.id());
        ReactiveFeatures.path(this,node,item,design);
        GladePaint.apply(this,node,item.form().finish());
        node.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        return node;
    }

    private void wall(Node node,GladeDesign.Item item,GladeDesign design) {
        if(item.points().size()<2) return;
        for(int part=1;part<item.points().size();part++) {
            var first=item.points().get(part-1);
            var last=item.points().get(part);
            float length=first.distance(last);
            if(length<0.001f) continue;
            int segments=Math.max(1,(int)Math.ceil(length/0.36f));
            float yaw=FastMath.atan2(last.x()-first.x(),last.z()-first.z());
            for(int i=0;i<segments;i++) {
                float t=(i+0.5f)/segments;
                float x=FastMath.interpolateLinear(t,first.x(),last.x());
                float z=FastMath.interpolateLinear(t,first.z(),last.z());
                boolean gate=design.nearPath(x,z,0.55f);
                float h=gate?0.22f:item.height();
                var segment=box(node,0.32f,h,length/segments+0.025f,x,
                        gate?item.height()-0.11f:item.height()/2,z,"#b6b099");
                segment.rotate(0,yaw,0);
                var cap=box(node,0.41f,0.10f,length/segments+0.04f,x,item.height()+0.05f,z,"#d0c5a8");
                cap.rotate(0,yaw,0);
            }
        }
    }

    private Node tree(float scale) {
        var node=new Node("Cây");
        ScenicDetails.canopy(this,node,1.7f,2.7f,"#8fa957");
        node.updateGeometricState();jme3tools.optimize.GeometryBatchFactory.optimize(node,false);
        node.setUserData("softCanopy",true);
        node.setLocalScale(scale);
        return node;
    }

    private Node flowers() {
        var node=new Node("Hoa");
        ScenicDetails.blossoms(this,node,.7f,.7f,.33f,"#f1e7ba");
        return node;
    }

    Geometry box(Node node,float w,float h,float d,float x,float y,float z,String color) {
        String key=w+":"+h+":"+d;
        var geometry=new Geometry("Khối",boxes.computeIfAbsent(key,ignored -> rounded(w,h,d)));
        geometry.setMaterial(material(color));
        geometry.setLocalTranslation(x,y,z);
        node.attachChild(geometry);
        return geometry;
    }

    Geometry sphere(Node node,float radius,float x,float y,float z,String color) {
        var shape=new Geometry("Tán",new Sphere(12,18,radius));
        shape.setMaterial(material(color));
        shape.setLocalTranslation(x,y,z);
        node.attachChild(shape);
        return shape;
    }

    void beam(Node node,Vector3f a,Vector3f b,float radius,String color) {
        var shape=new Geometry("Thanh",new Cylinder(10,12,radius,a.distance(b),true));
        shape.setMaterial(material(color));
        shape.setLocalTranslation(a.add(b).multLocal(0.5f));
        shape.setLocalRotation(new Quaternion().lookAt(b.subtract(a),Vector3f.UNIT_Y));
        node.attachChild(shape);
    }

    void arch(Node node,float width,float height,float depth,float x,float y,float z,String color) {
        var outline=new ArrayList<Vector2f>();
        outline.add(new Vector2f(-width/2,-height/2));
        outline.add(new Vector2f(width/2,-height/2));
        for(int i=0;i<=16;i++) {
            float angle=i*FastMath.PI/16;
            outline.add(new Vector2f(FastMath.cos(angle)*width/2,height/2-width/2+FastMath.sin(angle)*width/2));
        }
        var vertices=new ArrayList<Float>();
        var normals=new ArrayList<Float>();
        var indices=new ArrayList<Integer>();
        for(var point:outline) {
            vertices.add(point.x);vertices.add(point.y);vertices.add(depth/2);
            normals.add(0f);normals.add(0f);normals.add(1f);
        }
        vertices.add(0f);vertices.add(0f);vertices.add(depth/2);
        normals.add(0f);normals.add(0f);normals.add(1f);
        for(int i=0;i<outline.size();i++) { indices.add(outline.size());indices.add(i);indices.add((i+1)%outline.size()); }
        var mesh=new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position,3,toFloats(vertices));
        mesh.setBuffer(VertexBuffer.Type.Normal,3,toFloats(normals));
        mesh.setBuffer(VertexBuffer.Type.Index,3,indices.stream().mapToInt(Integer::intValue).toArray());
        mesh.updateBound();
        var geometry=new Geometry("Vòm",mesh);
        geometry.setMaterial(material(color));
        geometry.setLocalTranslation(x,y,z);
        node.attachChild(geometry);
    }

    private static float[] toFloats(List<Float> values) {
        float[] array=new float[values.size()];
        for(int i=0;i<array.length;i++) array[i]=values.get(i);
        return array;
    }

    private static Mesh rounded(float w,float h,float d) {
        float[] half={w/2,h/2,d/2};
        float radius=Math.min(0.065f,Math.min(half[0],Math.min(half[1],half[2]))*0.42f);
        int[][] normal={{0,0,1},{0,0,-1},{1,0,0},{-1,0,0},{0,1,0},{0,-1,0}};
        int[][] u={{1,0,0},{-1,0,0},{0,0,-1},{0,0,1},{1,0,0},{1,0,0}};
        int[][] v={{0,1,0},{0,1,0},{0,1,0},{0,1,0},{0,0,-1},{0,0,1}};
        var points=new ArrayList<Float>();
        var normals=new ArrayList<Float>();
        var indices=new ArrayList<Integer>();
        for(int face=0;face<6;face++) {
            for(int row=0;row<4;row++) for(int column=0;column<4;column++) {
                float[] p=new float[3],center=new float[3];
                float length=0;
                for(int axis=0;axis<3;axis++) {
                    float[] steps={-half[axis],-half[axis]+radius,half[axis]-radius,half[axis]};
                    p[axis]=normal[face][axis]*half[axis]+u[face][axis]*steps[column]+v[face][axis]*steps[row];
                    center[axis]=FastMath.clamp(p[axis],-half[axis]+radius,half[axis]-radius);
                    length+=(p[axis]-center[axis])*(p[axis]-center[axis]);
                }
                length=FastMath.sqrt(length);
                for(int axis=0;axis<3;axis++) {
                    float n=(p[axis]-center[axis])/length;
                    points.add(center[axis]+n*radius);
                    normals.add(n);
                }
            }
            for(int row=0;row<3;row++) for(int col=0;col<3;col++) {
                int a=face*16+row*4+col,b=a+1,c=a+5,e=a+4;
                Collections.addAll(indices,a,b,c,a,c,e);
            }
        }
        var uv=new float[6*16*2];
        for(int face=0;face<6;face++) for(int row=0;row<4;row++) for(int col=0;col<4;col++) {
            float horizontal=face<2?w:face<4?d:w,vertical=face<4?h:d;
            int index=(face*16+row*4+col)*2;uv[index]=col/3f*horizontal;uv[index+1]=row/3f*vertical;
        }
        var mesh=new Mesh();mesh.setBuffer(VertexBuffer.Type.TexCoord,2,uv);
        mesh.setBuffer(VertexBuffer.Type.Position,3,toFloats(points));
        mesh.setBuffer(VertexBuffer.Type.Normal,3,toFloats(normals));
        mesh.setBuffer(VertexBuffer.Type.Index,3,indices.stream().mapToInt(Integer::intValue).toArray());
        mesh.updateBound();
        return mesh;
    }
}

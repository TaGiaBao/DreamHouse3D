package vn.dreamhouse.glade;

import com.jme3.math.*;
import com.jme3.scene.Node;

final class GladeParts {
    private final GladeArt art;
    GladeParts(GladeArt art) { this.art=art; }

    void render(Node node,GladeDesign.Item item,GladeDesign design) {
        var p=design.placement(item);
        float w=p.width(),d=p.depth(),h=item.height();
        switch(item.kind()) {
            case LOG_CABIN -> cabin(node,item,design);
            case BRICK_WALL,TIMBER_WALL,HEDGE -> wall(node,item);
            case ROOF_TILE,ROOF_SLATE -> {
                if(!design.hasUpper(item.mount().hostId())) {
                    var shape=new Node("Dáng mái độc lập");
                    var host=design.items().stream().filter(part->part.id()==item.mount().hostId()).findFirst().orElseThrow();
                    float front=host.form().roofFront(),back=host.form().roofBack();
                    art.roof(shape,w+host.form().roofLeft()+host.form().roofRight(),d+front+back,0,false,item.kind()==GladeDesign.Kind.ROOF_SLATE);
                    shape.setLocalTranslation((host.form().roofRight()-host.form().roofLeft())/2,0,(front-back)/2);
                    shape.setLocalScale(1,h/.5f,1);node.attachChild(shape);
                }
            }
            case ROOF_FLAT,ROOF_PYRAMID,ROOF_PENTAGON,ROOF_SHED,ROOF_CURVED -> {
                var host=design.items().stream().filter(part->part.id()==item.mount().hostId()).findFirst().orElseThrow();
                if(!design.hasUpper(host.id()))RoofProfiles.build(art,node,item.kind(),host,h);
            }
            case CHIMNEY_BRICK,CHIMNEY_STONE -> chimney(node,w,d,h,item.kind()==GladeDesign.Kind.CHIMNEY_BRICK);
            case WINDOW_ARCH -> {
                var window=art.window();window.setLocalScale(w/0.89f,h/1.08f,1);node.attachChild(window);
            }
            case WINDOW_ROUND -> {
                art.cylinder(node,w/2,0.13f,0,0,0,"#d3ba85");
                node.getChild(0).rotate(FastMath.HALF_PI,0,0);
                node.getChild(0).setLocalScale(1,h/w,1);
                var glass=art.sphere(node,1,0,0,0.08f,"#7eacb4");glass.setLocalScale(w*.38f,h*.38f,0.06f);
                art.box(node,0.04f,h*0.77f,0.05f,0,0,0.15f,"#ead4a6");
                art.box(node,w*0.77f,0.04f,0.05f,0,0,0.15f,"#ead4a6");
            }
            case DOOR_ARCH -> {
                var door=art.door();door.setLocalScale(w/1.12f,h/1.80f,1);node.attachChild(door);
            }
            case DOOR_DOUBLE -> {
                art.arch(node,w,h,0.16f,0,h/2,0.01f,"#d5b98c");
                art.arch(node,w*0.84f,h*0.89f,0.12f,0,h*.445f,0.12f,"#876344");
                art.box(node,0.04f,h*.83f,0.04f,0,h*.42f,0.20f,"#c6a574");
                for(int side:new int[]{-1,1}) {
                    for(int panel=0;panel<2;panel++) art.box(node,w*.30f,h*.26f,0.04f,
                            side*w*.21f,h*(.23f+panel*.34f),0.20f,"#9e7a50");
                    art.sphere(node,0.045f,side*.09f,h*.45f,0.24f,"#ddb458");
                }
            }
            case STAIRS_STONE,STAIRS_WOOD -> stairs(node,w,d,h,item.kind()==GladeDesign.Kind.STAIRS_WOOD);
            case DECK -> deck(node,w,d,h);
            case PERGOLA -> pergola(node,w,d,h);
            case PLANTER -> planter(node,w,d,h,item.form().finish().variant());
            case WELL -> well(node,w,d,h);
            case BENCH -> bench(node,w,d,h);
            default -> throw new IllegalArgumentException("Chưa có mô hình: "+item.kind());
        }
        if(!item.kind().stroke()) node.setLocalTranslation(p.center().x(),p.elevation(),p.center().z());
    }

    private void cabin(Node node,GladeDesign.Item item,GladeDesign design) {
        float w=item.width(),d=item.depth(),h=item.height();
        art.box(node,w+0.15f,0.23f,d+0.15f,0,.115f,0,"#a5a28b");
        art.box(node,w,h,d,0,h/2,0,"#96704c");
        var wall=new com.jme3.scene.Node("Tường gỗ");node.attachChild(wall);
        var style=item.form().finish();
        if(!style.material().equals("original") && !style.material().equals("wood")) {
            art.box(wall,w,h,d,0,h/2,0,"#b9956b").setUserData("wallSurface",true);
        } else {
        int rows=Math.max(3,Math.min(24,(int)(h/.23f)));
        for(int row=0;row<rows;row++) for(int side:new int[]{-1,1}) {
            float y=(row+.5f)*h/rows;
            art.beam(wall,new Vector3f(-w/2-.09f,y,side*d/2),new Vector3f(w/2+.09f,y,side*d/2),
                    h/rows*.48f,row%3==0?"#be9564":"#a87c50");
            art.beam(wall,new Vector3f(side*w/2,y,-d/2-.09f),new Vector3f(side*w/2,y,d/2+.09f),
                    h/rows*.48f,row%3==0?"#b88e5b":"#a1764b");
        }
        }
        wall.depthFirstTraversal(part->{if(part instanceof com.jme3.scene.Geometry)part.setUserData("wallSurface",true);});
        if(!design.blocksOpening(item,0,d/2,.85f,.5f)) {
            var door=art.door();door.setLocalScale(Math.min(1,h/2));door.setLocalTranslation(0,0,d/2+.14f);node.attachChild(door);
        }
        for(int side:new int[]{-1,1}) {
            if(design.blocksOpening(item,side*w/2,0,h*.58f,.5f)) continue;
            var window=art.window();window.setLocalScale(Math.min(0.8f,h/2));
            window.rotate(0,side*FastMath.HALF_PI,0);window.setLocalTranslation(side*(w/2+.14f),h*.58f,0);node.attachChild(window);
        }
        if(!design.customRoof(item.id()) && !art.joinedRoof(item,design)) {
            if(design.roofRise(item)>.02f) art.buildingRoof(node,item,design,false,true);
            else art.box(art.roofGroup(node),w+.25f,.18f,d+.25f,0,h+.06f,0,"#8b7759");
        }
    }

    private void chimney(Node node,float w,float d,float h,boolean brick) {
        art.box(node,w,h,d,0,h/2,0,brick?"#b37b60":"#aaa58e");
        int rows=Math.max(2,Math.min(18,(int)(h/.22f)));
        for(int row=1;row<rows;row++) {
            art.box(node,w+.015f,.018f,d+.015f,0,row*h/rows,0,brick?"#d8b497":"#d4c9ac");
            for(int side:new int[]{-1,1}) art.box(node,.018f,h/rows-.02f,.018f,
                    (row%2==0?-.18f:.18f)*w,(row-.5f)*h/rows,side*(d/2+.01f),"#d4bea0");
        }
        art.box(node,w+.16f,.13f,d+.16f,0,h,0,"#d4c6aa");
        art.box(node,w*.70f,.015f,d*.70f,0,h+.073f,0,"#4f534b");
        for(int side:new int[]{-1,1}) {
            art.box(node,w+.05f,.13f,.09f,0,h+.15f,side*d/2,"#ab9377");
            art.box(node,.09f,.13f,d,side*w/2,h+.15f,0,"#ab9377");
        }
    }

    private void stairs(Node node,float w,float d,float h,boolean wood) {
        int steps=WalkController.stairSteps(h);
        for(int i=0;i<steps;i++) {
            float top=(i+1)*h/steps,z=d/2-(i+.5f)*d/steps;
            art.box(node,w,wood?.12f:top,d/steps+.025f,0,wood?top-.06f:top/2,z,wood?"#c29a67":"#b9b49b");
            art.box(node,w+.04f,.035f,.065f,0,top+.012f,z+d/steps/2,wood?"#dec095":"#d8d0b2");
            if(wood && i%3==0) for(int side:new int[]{-1,1})
                art.box(node,.11f,top,.11f,side*(w/2-.10f),top/2,z,"#916c49");
        }
        if(wood) for(int side:new int[]{-1,1}) {
            for(int end:new int[]{-1,1}) {
                float y=end>0?h+.7f:.7f,z=end>0?-d/2:d/2;
                art.box(node,.10f,.8f,.10f,side*w/2,y-.35f,z,"#986f49");
            }
            art.beam(node,new Vector3f(side*w/2,.7f,d/2),new Vector3f(side*w/2,h+.7f,-d/2),.04f,"#c9a575");
        }
    }

    private void deck(Node node,float w,float d,float h) {
        int planks=Math.max(2,Math.min(256,(int)Math.ceil(d/.24f)));
        for(int i=0;i<planks;i++) art.box(node,w,.13f,d/planks-.014f,0,h,-d/2+(i+.5f)*d/planks,i%3==0?"#cda674":"#b78c5b");
        for(int x:new int[]{-1,1}) for(int z:new int[]{-1,1})
            art.box(node,.15f,h,.15f,x*(w/2-.13f),h/2,z*(d/2-.13f),"#99724c");
        for(int side:new int[]{-1,1}) {
            art.box(node,w,.16f,.13f,0,h-.18f,side*d/2,"#9c764d");
            art.box(node,.13f,.16f,d,side*w/2,h-.18f,0,"#9c764d");
        }
    }

    private void pergola(Node node,float w,float d,float h) {
        for(int x:new int[]{-1,1}) for(int z:new int[]{-1,1}) {
            art.box(node,.16f,h,.16f,x*(w/2-.12f),h/2,z*(d/2-.12f),"#b18a5b");
            art.box(node,.24f,.15f,.24f,x*(w/2-.12f),.08f,z*(d/2-.12f),"#c8b68f");
        }
        for(int side:new int[]{-1,1}) art.box(node,w+.25f,.18f,.15f,0,h-.10f,side*(d/2-.12f),"#a77f52");
        int beams=Math.max(3,Math.min(128,(int)Math.ceil(w/.48f)));
        for(int i=0;i<=beams;i++) art.box(node,.11f,.13f,d+.4f,-w/2+i*w/beams,h+.06f,0,"#c8a477");
        for(int i=0;i<10;i++) {
            float x=-w/2+(i/9f)*w,z=(float)Math.sin(i*1.8)*d*.35f;
            var leaf=art.sphere(node,.22f,x,h+.13f,z,i%2==0?"#88a76a":"#a1bc77");leaf.setLocalScale(1,.4f,1);
        }
    }

    private void planter(Node node,float w,float d,float h,int variant) {
        float top=Math.min(h,.75f);
        art.box(node,w,top,d,0,top/2,0,"#b88b61").setUserData("wallSurface",true);
        art.box(node,w-.12f,.06f,d-.12f,0,top+.02f,0,"#73644a");
        int count=Math.max(3,Math.min(96,(int)(w*d*5)));
        for(int i=0;i<count;i++) {
            float x=(float)Math.sin(i*2.39)*w*.38f,z=(float)Math.cos(i*3.1)*d*.35f;
            art.beam(node,new Vector3f(x,top,z),new Vector3f(x,top+.24f,z),.018f,"#79945c");
            for(int petal=0;petal<5;petal++) art.sphere(node,.055f,x+(float)Math.sin(petal*1.257)*.065f,
                    top+.25f,z+(float)Math.cos(petal*1.257)*.065f,variant==3?"#e5a1a1":variant==1?"#b49aca":i%2==0?"#e5a1a1":"#e4c26b");
        }
    }

    private void well(Node node,float w,float d,float h) {
        float radius=Math.min(w,d)*.40f,wallHeight=h*.38f;
        art.cylinder(node,radius,wallHeight,0,wallHeight/2,0,"#b6ad95");
        var water=art.sphere(node,1,0,wallHeight+.01f,0,"#5b8e98");water.setLocalScale(radius*.83f,.02f,radius*.83f);
        for(int i=0;i<12;i++) {
            float angle=i*FastMath.TWO_PI/12;
            var stone=art.box(node,radius*.50f,.18f,.20f,FastMath.sin(angle)*radius,wallHeight+.06f,
                    FastMath.cos(angle)*radius,"#d0c4a6");stone.rotate(0,angle,0);
        }
        for(int side:new int[]{-1,1}) art.box(node,.13f,h,.13f,side*radius,h/2,0,"#9f7b52");
        art.beam(node,new Vector3f(-radius,h*.72f,0),new Vector3f(radius,h*.72f,0),.065f,"#b79463");
        art.beam(node,new Vector3f(0,h*.72f,0),new Vector3f(0,wallHeight+.2f,0),.015f,"#d1bd92");
        art.roof(node,radius*2,.8f,h,false,false);
    }

    private void bench(Node node,float w,float d,float h) {
        for(int side:new int[]{-1,1}) {
            art.box(node,.13f,h*.65f,d*.85f,side*w*.34f,h*.325f,0,"#9d784f");
            art.box(node,.10f,h,.10f,side*w*.34f,h*.5f,-d*.33f,"#9d784f");
        }
        for(int plank=0;plank<3;plank++) {
            art.box(node,w,.07f,d/3-.015f,0,h*.52f,-d/2+(plank+.5f)*d/3,"#c5a174");
            art.box(node,w,h*.11f,.07f,0,h*(.70f+plank*.13f),-d*.36f,"#c5a174");
        }
    }

    private void wall(Node node,GladeDesign.Item item) {
        boolean hedge=item.kind()==GladeDesign.Kind.HEDGE,timber=item.kind()==GladeDesign.Kind.TIMBER_WALL;
        float h=item.height();
        for(int part=1;part<item.points().size();part++) {
            var start=item.points().get(part-1);var end=item.points().get(part);
            float length=start.distance(end);if(length<.001f) continue;
            int count=Math.max(1,(int)Math.ceil(length/.50f));
            float yaw=FastMath.atan2(end.x()-start.x(),end.z()-start.z());
            for(int step=0;step<count;step++) {
                float t=(step+.5f)/count,x=FastMath.interpolateLinear(t,start.x(),end.x()),z=FastMath.interpolateLinear(t,start.z(),end.z());
                var panel=new Node("Đoạn cấu kiện");
                float span=length/count+.025f;
                if(hedge) {
                    var shrub=art.sphere(panel,1,0,h/2,0,"#809b59");shrub.setLocalScale(.34f,h*.55f,Math.max(.20f,span*.70f));
                } else if(timber) {
                    art.box(panel,.14f,h,span,0,h/2,0,"#ad8858");
                    for(int row=0;row<5;row++) art.box(panel,.16f,.025f,span,0,(row+.5f)*h/5,0,"#d0ae7a");
                    art.box(panel,.22f,h+.10f,.10f,0,h/2,span/2,"#896a46");
                } else {
                    art.box(panel,.31f,h,span,0,h/2,0,"#b17b61");
                    int rows=Math.max(2,Math.min(16,(int)(h/.22f)));
                    for(int row=1;row<rows;row++) art.box(panel,.33f,.018f,span,0,row*h/rows,0,"#d6b497");
                    art.box(panel,.41f,.11f,span+.025f,0,h+.035f,0,"#d9c5a0");
                }
                panel.rotate(0,yaw,0);panel.setLocalTranslation(x,0,z);node.attachChild(panel);
            }
        }
    }
}

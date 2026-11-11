package vn.dreamhouse.glade;
import com.jme3.scene.*;
import com.jme3.math.*;

/** Sparse natural bank details follow the painted stroke and keep paths open. */
final class WaterBank {
    static void decorate(GladeArt art,Node root,GladeDesign.Item item,GladeDesign scene){
        if(item.points().size()<3)return;
        var bank=new Node("Bờ suối");float base=scene.terrain().height(item.center().x(),item.center().z());
        int stride=Math.max(3,item.points().size()/22);
        for(int i=1;i<item.points().size()-1;i+=stride){
            var p=item.points().get(i);var a=item.points().get(i-1);var b=item.points().get(i+1);
            float dx=b.x()-a.x(),dz=b.z()-a.z(),len=(float)Math.hypot(dx,dz);if(len<.001f)continue;
            for(int side:new int[]{-1,1}){
                float x=p.x()-side*dz/len*(item.height()+.13f),z=p.z()+side*dx/len*(item.height()+.13f);
                if(x<scene.land().minX()+.3f || x>scene.land().maxX()-.3f || z<scene.land().minZ()+.3f || z>scene.land().maxZ()-.3f || scene.nearPath(x,z,.85f))continue;
                float h=scene.terrain().height(x,z)-base;
                for(int j=0;j<2;j++){
                    float r=.14f+(i%3)*.035f;var rock=art.sphere(bank,r,x-item.center().x()+j*.22f,h+.07f,z-item.center().z(),i%3==0?"#aaa48c":"#b9b298");rock.setLocalScale(1,.65f,.85f);
                }
                if(i%2==1){var leaves=new Node("Cỏ bờ nước");ScenicDetails.blossoms(art,leaves,.38f,.32f,.24f,"#e8dba0");leaves.setLocalTranslation(x-item.center().x(),h,z-item.center().z());bank.attachChild(leaves);}
            }
        }
        bank.updateGeometricState();jme3tools.optimize.GeometryBatchFactory.optimize(bank,false);bank.setShadowMode(com.jme3.renderer.queue.RenderQueue.ShadowMode.CastAndReceive);root.attachChild(bank);
    }
}

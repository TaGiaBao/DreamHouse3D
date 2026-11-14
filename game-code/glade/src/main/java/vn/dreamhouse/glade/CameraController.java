package vn.dreamhouse.glade;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

final class CameraController {
    static Vector3f orbitOffset(float yaw,float elevation,float distance){
        float horizontal=distance*FastMath.cos(elevation);
        return new Vector3f(horizontal*FastMath.sin(yaw),distance*FastMath.sin(elevation),horizontal*FastMath.cos(yaw));
    }

    static Vector3f pan(Vector3f left,Vector3f forward,float distance,float dx,float dy){
        var horizontalLeft=left.clone();
        var horizontalForward=forward.clone();
        horizontalLeft.y=0;
        horizontalForward.y=0;
        return horizontalLeft.normalizeLocal().multLocal(dx*distance*.00075f)
                .addLocal(horizontalForward.normalizeLocal().multLocal(-dy*distance*.00075f));
    }

    static void constrain(Vector3f target,LandBounds land){
        target.x=FastMath.clamp(target.x,land.minX()-40,land.maxX()+40);
        target.z=FastMath.clamp(target.z,land.minZ()-40,land.maxZ()+40);
    }
}

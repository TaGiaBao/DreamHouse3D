package vn.dreamhouse.glade;

import com.jme3.math.*;
import com.jme3.renderer.Camera;

/** Closest ray/axis drag, calibrated at the exact grab point rather than by camera distance. */
final class AxisDrag {
    private final Camera camera;
    private final Vector3f origin,axis;
    private final Vector2f press,screenAxis;
    private final float initial;

    AxisDrag(Camera camera,Vector3f origin,Vector3f axis,Vector2f press) {
        this.camera=camera;this.origin=origin.clone();this.axis=axis.normalize();this.press=press.clone();
        var a=camera.getScreenCoordinates(origin);
        var b=camera.getScreenCoordinates(origin.add(this.axis));
        screenAxis=new Vector2f(b.x-a.x,b.y-a.y);
        initial=parameter(ray(press),this.origin,this.axis);
    }
    float delta(Vector2f pointer) {
        float current=parameter(ray(pointer),origin,axis);
        if(Float.isFinite(initial) && Float.isFinite(current)) return current-initial;
        float length=screenAxis.lengthSquared();
        return length<1?0:pointer.subtract(press).dot(screenAxis)/length;
    }
    private Ray ray(Vector2f point) {
        var start=camera.getWorldCoordinates(point,0);
        return new Ray(start,camera.getWorldCoordinates(point,1).subtractLocal(start).normalizeLocal());
    }
    static float parameter(Ray ray,Vector3f origin,Vector3f axis) {
        float b=axis.dot(ray.getDirection()),denominator=1-b*b;
        if(denominator<.00001f) return Float.NaN;
        var offset=origin.subtract(ray.getOrigin());
        return (b*ray.getDirection().dot(offset)-axis.dot(offset))/denominator;
    }
}

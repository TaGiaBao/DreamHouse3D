package vn.dreamhouse.glade;

import com.jme3.light.*;
import com.jme3.math.*;
import com.jme3.renderer.ViewPort;
import com.jme3.post.filters.FogFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;

/** A bounded, reversible lighting transition; no changes to scene geometry or player data. */
final class CozyAtmosphere {
    record Look(ColorRGBA sunlight,ColorRGBA ambient,ColorRGBA sky,Vector3f direction,float shadow){}
    private static final Look[] LOOKS={
        new Look(new ColorRGBA(1.13f,1.02f,.83f,1),new ColorRGBA(.37f,.44f,.54f,1),new ColorRGBA(.72f,.83f,.88f,1),new Vector3f(-.76f,-.86f,-.42f).normalizeLocal(),.53f),
        new Look(new ColorRGBA(1.25f,.88f,.60f,1),new ColorRGBA(.43f,.44f,.55f,1),new ColorRGBA(.87f,.76f,.72f,1),new Vector3f(.82f,-.57f,-.48f).normalizeLocal(),.52f),
        new Look(new ColorRGBA(.45f,.53f,.76f,1),new ColorRGBA(.34f,.40f,.52f,1),new ColorRGBA(.30f,.39f,.56f,1),new Vector3f(-.35f,-.80f,.55f).normalizeLocal(),.32f)
    };
    private final DirectionalLight sun;
    private final AmbientLight ambient;
    private final ViewPort viewport;
    private final DirectionalLightShadowRenderer shadow;
    final FogFilter haze=new FogFilter(LOOKS[0].sky().clone(),.48f,160);
    private Look start,current=copy(LOOKS[0]);
    private int target;
    private float elapsed=1.4f;
    CozyAtmosphere(DirectionalLight sun,AmbientLight ambient,ViewPort viewport,DirectionalLightShadowRenderer shadow){
        this.sun=sun;this.ambient=ambient;this.viewport=viewport;this.shadow=shadow;start=copy(current);apply();
    }
    static String label(int mode){return switch(Math.floorMod(mode,3)){case 1->"Nắng chiều";case 2->"Chạng vạng";default->"Ban ngày";};}
    static ColorRGBA sunlight(int mode){return LOOKS[Math.floorMod(mode,3)].sunlight().clone();}
    void select(int mode){target=Math.floorMod(mode,3);start=copy(current);elapsed=0;}
    void update(float dt,float cameraDistance){
        if(!Float.isFinite(dt) || dt<0)return;
        float fogDistance=FastMath.clamp(cameraDistance*5,100,1600);
        if(haze.getFogDistance()!=fogDistance)haze.setFogDistance(fogDistance);
        if(elapsed>=1.4f)return;
        elapsed=Math.min(1.4f,elapsed+dt);float t=elapsed/1.4f;t=t*t*(3-2*t);var end=LOOKS[target];
        var direction=start.direction().clone().interpolateLocal(end.direction(),t).normalizeLocal();
        current=new Look(mix(start.sunlight(),end.sunlight(),t),mix(start.ambient(),end.ambient(),t),mix(start.sky(),end.sky(),t),direction,FastMath.interpolateLinear(t,start.shadow(),end.shadow()));
        apply();
    }
    private void apply(){sun.setColor(current.sunlight().clone());sun.setDirection(current.direction().clone());ambient.setColor(current.ambient().clone());viewport.setBackgroundColor(current.sky().clone());haze.setFogColor(current.sky().clone());shadow.setShadowIntensity(current.shadow());}
    private static ColorRGBA mix(ColorRGBA a,ColorRGBA b,float t){return a.clone().interpolateLocal(b,t);}
    private static Look copy(Look look){return new Look(look.sunlight().clone(),look.ambient().clone(),look.sky().clone(),look.direction().clone(),look.shadow());}
}

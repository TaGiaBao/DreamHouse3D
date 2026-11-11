package vn.dreamhouse.glade;
import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.*;
import com.jme3.system.AppSettings;
import com.jme3.util.BufferUtils;
import java.util.*;
import java.util.concurrent.*;

/** Read a few rendered roof pixels in memory; creates no image file or displayed screenshot. */
class RoofColorCheck extends ProceduralMechanicsCheck {
    public static void main(String[] args)throws Exception{
        app=new GladeApp();var settings=new AppSettings(true);settings.setResolution(1100,720);settings.setSamples(4);settings.setGammaCorrection(true);settings.setRenderer(AppSettings.LWJGL_OPENGL33);settings.setAudioRenderer(null);settings.setVSync(true);app.setSettings(settings);app.setShowSettings(false);app.setPauseOnLostFocus(false);app.start();
        try{
            engine(()->true);var item=new GladeDesign.Item(0,GladeDesign.Kind.HOUSE,new GladeDesign.Point(0,0),4,3,2.6f,true,List.of(),0,null,new GladeDesign.Form(0,1.4f,0,-1).finish(new GladeDesign.Finish("plaster","#eedbb2","tile","#c77b4b")));fixture(new GladeDesign(List.of(item),2));set("yaw",-.65f);set("elevation",.65f);set("distance",18f);var focus=(Vector3f)field("focus");engine(()->{focus.set(0,1.8f,0);return true;});call("updateCamera");Thread.sleep(800);
            engine(()->{app.getRootNode().depthFirstTraversal(v->{if(v instanceof com.jme3.scene.Geometry g && Boolean.TRUE.equals(g.getUserData("roofSurface")))ok(g.getWorldLightList().size()>=2,"batched roof inherits sunlight and ambient light");});return true;});var done=new CompletableFuture<float[]>();engine(()->{app.getStateManager().attach(new BaseAppState(){protected void initialize(Application a){}protected void cleanup(Application a){}protected void onEnable(){}protected void onDisable(){}public void postRender(){try{int w=app.getCamera().getWidth(),h=app.getCamera().getHeight();var data=BufferUtils.createByteBuffer(w*h*4);app.getRenderer().readFrameBuffer(null,data);float r=0,g=0,b=0;int count=0;for(float z:new float[]{-.6f,0,.6f}){float x=-.9f,y=2.6f+1.4f*(1-Math.abs(x)/2.32f)+.09f;var s=app.getCamera().getScreenCoordinates(new Vector3f(x,y,z));for(int dy=-3;dy<=3;dy++)for(int dx=-3;dx<=3;dx++){int px=Math.round(s.x)+dx,py=Math.round(s.y)+dy;if(px<0 || px>=w || py<0 || py>=h)continue;int k=(py*w+px)*4;r+=Byte.toUnsignedInt(data.get(k));g+=Byte.toUnsignedInt(data.get(k+1));b+=Byte.toUnsignedInt(data.get(k+2));count++;}}done.complete(new float[]{r/count,g/count,b/count});}catch(Throwable e){done.completeExceptionally(e);}finally{app.getStateManager().detach(this);}}});return true;});
            var rgb=done.get(30,TimeUnit.SECONDS);System.out.println("ROOF_PIXEL_RGB="+Arrays.toString(rgb));ok(rgb[0]>rgb[1]*1.10f && rgb[0]>rgb[2]*1.20f && rgb[0]>70,"terracotta roof renders warm rather than black");System.out.println("ROOF_COLOR_PASS: actual rendered terracotta pixel samples, no screenshot saved or displayed");
        }finally{app.stop();}
    }
}

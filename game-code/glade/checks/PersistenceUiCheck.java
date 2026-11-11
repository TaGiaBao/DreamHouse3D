package vn.dreamhouse.glade;
import com.jme3.system.AppSettings;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
class PersistenceUiCheck extends ProceduralMechanicsCheck {
    public static void main(String[]args)throws Exception{
        app=new GladeApp();var settings=new AppSettings(true);settings.setResolution(1000,680);settings.setRenderer(AppSettings.LWJGL_OPENGL33);settings.setAudioRenderer(null);settings.setTitle("DreamHouse · Trạng thái tự lưu");app.setSettings(settings);app.setShowSettings(false);app.setPauseOnLostFocus(false);app.start();
        var firstEntered=new CountDownLatch(1);var releaseFirst=new CountDownLatch(1);var lastEntered=new CountDownLatch(1);var releaseLast=new CountDownLatch(1);var writes=new AtomicInteger();
        var store=new SessionStore(PlayerFiles.directory("ui-autosave"),(snapshot,target,sequence)->{
            boolean first=writes.getAndIncrement()==0;var entered=first?firstEntered:lastEntered;var release=first?releaseFirst:releaseLast;entered.countDown();try{if(!release.await(15,TimeUnit.SECONDS))throw new IOException("test latch timeout");}catch(InterruptedException error){throw new IOException(error);}snapshot.saveRecovery(target,sequence);
        });
        try{
            engine(()->true);set("sessionStore",store);set("managedSession",false);fixture(SessionPersistenceCheck.scene(1));call("queueAutosave");ok(firstEntered.await(5,TimeUnit.SECONDS),"first write active");
            fixture(SessionPersistenceCheck.scene(2));call("queueAutosave");var current=SessionPersistenceCheck.scene(3);fixture(current);call("queueAutosave");engine(()->true);
            ok((Boolean)field("autosavePending"),"cancelled superseded request cannot clear newest pending flag");releaseFirst.countDown();ok(lastEntered.await(5,TimeUnit.SECONDS),"latest pending write starts");engine(()->true);
            ok((Boolean)field("autosavePending") && field("autosavedDesign")!=current,"older completion cannot claim newest state is saved");releaseLast.countDown();
            long deadline=System.nanoTime()+10_000_000_000L;while((Boolean)field("autosavePending") && System.nanoTime()<deadline)Thread.sleep(20);
            ok(!(Boolean)field("autosavePending") && field("autosavedDesign")==current,"newest successful completion clears pending and records correct snapshot");ok(store.recover().orElseThrow().design().items().equals(current.items()),"recovery matches visible state");
            System.out.println("PERSISTENCE_UI_PASS: cancelled/old completions cannot clear newer autosave state; latest completion and recovered design match; no screenshots");
        }finally{releaseFirst.countDown();releaseLast.countDown();app.stop();}
    }
}

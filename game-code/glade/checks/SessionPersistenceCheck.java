package vn.dreamhouse.glade;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.*;

class SessionPersistenceCheck {
    static void ok(boolean value,String message){if(!value)throw new AssertionError(message);}
    static GladeDesign scene(int version){return new GladeDesign(List.of(new GladeDesign.Item(0,GladeDesign.Kind.HOUSE,new GladeDesign.Point(0,0),4,3,1+version*.01f,true,List.of(),0,null,new GladeDesign.Form(0,1.3f,0,-1).finish(GladeCatalog.plain()))),2);}
    static void rejectFile(Path file)throws Exception{try{GladeDesign.load(file);throw new AssertionError("bad save accepted");}catch(IOException|IllegalArgumentException expected){}}
    static Properties properties(Path file)throws Exception{var values=new Properties();try(var input=Files.newInputStream(file)){values.load(input);}return values;}
    static void store(Path file,Properties properties)throws Exception{try(var output=Files.newOutputStream(file)){properties.store(output,"fixture");}}
    static Process child(String mode,Path folder,int version,Path log)throws Exception{
        String encoded=Base64.getUrlEncoder().withoutPadding().encodeToString(folder.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","javaw.exe").toString(),"-cp",GameLaunchService.absoluteClassPath(),PersistenceChildProbe.class.getName(),mode,encoded,Integer.toString(version)).redirectErrorStream(true).redirectOutput(log.toFile()).start();
    }
    static void waitGood(Process process)throws Exception{if(!process.waitFor(15,TimeUnit.SECONDS)){process.destroyForcibly();throw new AssertionError("child timeout");}ok(process.exitValue()==0,"child exit zero");}
    public static void main(String[]args)throws Exception{
        var root=PlayerFiles.directory("persistence có dấu");var file=root.resolve("world.glade");var original=scene(1);original.save(file);var good=Files.readAllBytes(file);
        ok(GladeDesign.load(file).items().equals(original.items()),"checked save roundtrip");
        var bad=properties(file);bad.setProperty("count","0");var altered=root.resolve("altered.glade");store(altered,bad);rejectFile(altered);
        bad=properties(file);bad.setProperty("item.0.shape","0.0,0.0,4.0,3.0,2.5,true,0.0");store(altered,bad);rejectFile(altered);
        Files.writeString(altered,"#"+WorldFiles.HEADER+"\nversion=glade-9\nland=-9,9,-6.5,6.5\ncount=0\n");rejectFile(altered);
        for(int version=1;version<=9;version++){
            var legacy=properties(file);legacy.remove(WorldFiles.DIGEST);legacy.remove(WorldFiles.FORMAT);legacy.setProperty("version","glade-"+version);legacy.setProperty("landLevel","2");if(version<=2)legacy.setProperty("item.0.shape","0,0,4,3,1.01,true");
            legacy.setProperty("item.0.form",version<=6?"0,1.3,0,-1":version==7?"0,1.3,0,-1,0,0":"0,1.3,0,-1,0,0,0,0");store(altered,legacy);ok(GladeDesign.load(altered).items().size()==1,"legacy version "+version+" remains readable");
        }
        try{WorldFiles.replace(file,out->{out.write("partial write".getBytes());throw new IOException("injected disk write failure");},(t,d)->{});throw new AssertionError("write failure accepted");}catch(IOException expected){}
        ok(Arrays.equals(good,Files.readAllBytes(file)),"failed partial write preserves previous good bytes");
        try{WorldFiles.replace(file,out->out.write("invalid content".getBytes()),(t,d)->{});throw new AssertionError("invalid staged world accepted");}catch(IOException|IllegalArgumentException expected){}
        ok(Arrays.equals(good,Files.readAllBytes(file)),"invalid staged world cannot replace valid save");
        if(Files.getFileStore(file).supportsFileAttributeView("dos")){
            Files.setAttribute(file,"dos:readonly",true);boolean denied=false;
            try{scene(2).save(file);}catch(IOException expected){denied=true;}finally{Files.setAttribute(file,"dos:readonly",false);}
            ok(denied && Arrays.equals(good,Files.readAllBytes(file)),"real read-only replacement fails without damaging existing save");
        }
        var recovery=root.resolve("recovery");Path latest;
        try(var store=new SessionStore(recovery)){store.save(scene(1)).get(10,TimeUnit.SECONDS);store.save(scene(2)).get(10,TimeUnit.SECONDS);latest=store.save(scene(3)).get(10,TimeUnit.SECONDS);}
        for(var name:List.of("autosave-0.glade","autosave-1.glade","autosave-2.glade"))Files.setLastModifiedTime(recovery.resolve(name),FileTime.fromMillis(name.equals(latest.getFileName().toString())?1:4_000_000_000_000L));
        try(var store=new SessionStore(recovery)){ok(store.recover().orElseThrow().design().items().equals(scene(3).items()),"logical sequence wins over changed system/file timestamps");}
        bad=properties(latest);bad.setProperty("count","0");store(latest,bad);
        var older0=Files.readAllBytes(recovery.resolve("autosave-0.glade"));var older1=Files.readAllBytes(recovery.resolve("autosave-1.glade"));
        try(var store=new SessionStore(recovery)){ok(store.recover().orElseThrow().design().items().equals(scene(2).items()),"valid-looking corrupted newest snapshot falls back");var replacement=store.save(scene(4)).get(10,TimeUnit.SECONDS);ok(replacement.equals(latest),"corrupt slot repaired before overwriting good slots");}
        ok(Arrays.equals(older0,Files.readAllBytes(recovery.resolve("autosave-0.glade"))) && Arrays.equals(older1,Files.readAllBytes(recovery.resolve("autosave-1.glade"))),"repair retains other two good recovery versions");
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var writes=new java.util.concurrent.atomic.AtomicInteger();
        try(var store=new SessionStore(root.resolve("bounded"),(snapshot,target,sequence)->{if(writes.getAndIncrement()==0){entered.countDown();try{if(!release.await(10,TimeUnit.SECONDS))throw new IOException("test latch timeout");}catch(InterruptedException e){throw new IOException(e);}}snapshot.saveRecovery(target,sequence);})){var active=store.save(scene(10));ok(entered.await(5,TimeUnit.SECONDS),"writer entered");CompletableFuture<Path> last=null;for(int i=11;i<=510;i++)last=store.save(scene(i));var stats=store.stats();ok(stats.active()==1 && stats.pending()==1 && stats.coalesced()==499,"autosave queue retains only active and latest pending snapshot");release.countDown();active.get(10,TimeUnit.SECONDS);last.get(10,TimeUnit.SECONDS);ok(store.recover().orElseThrow().design().items().equals(scene(510).items()),"latest pending state survives coalescing");}
        var closed=new SessionStore(root.resolve("closed"));closed.close();try{closed.save(scene(1)).get();throw new AssertionError("closed writer accepted save");}catch(ExecutionException expected){ok(expected.getCause() instanceof IOException,"closed save returns future error");}
        var closeEntered=new CountDownLatch(1);var releaseClose=new CountDownLatch(1);
        var closing=new SessionStore(root.resolve("close-pending"),(snapshot,target,sequence)->{closeEntered.countDown();try{if(!releaseClose.await(5,TimeUnit.SECONDS))throw new IOException("close latch timeout");}catch(InterruptedException e){throw new IOException(e);}snapshot.saveRecovery(target,sequence);});
        var firstClose=closing.save(scene(20));ok(closeEntered.await(5,TimeUnit.SECONDS),"close test writer active");var lastClose=closing.save(scene(21));
        var releaser=new Thread(()->{try{Thread.sleep(250);}catch(InterruptedException e){Thread.currentThread().interrupt();}releaseClose.countDown();});releaser.start();closing.close();releaser.join();
        ok(firstClose.isDone() && lastClose.isDone() && closing.recover().orElseThrow().design().items().equals(scene(21).items()),"close drains active and latest pending save before returning");
        var restart=root.resolve("restart");Files.createDirectories(restart);waitGood(child("write",restart,55,root.resolve("write-child.log")));waitGood(child("recover",restart,55,root.resolve("recover-child.log")));
        var crash=root.resolve("crash");Files.createDirectories(crash);scene(9).saveRecovery(crash.resolve("autosave-0.glade"),1);var beforeCrash=Files.readAllBytes(crash.resolve("autosave-0.glade"));var process=child("interrupt",crash,99,root.resolve("interrupted-child.log"));
        try{long deadline=System.nanoTime()+15_000_000_000L;while(!Files.isRegularFile(crash.resolve("prepared.marker")) && process.isAlive() && System.nanoTime()<deadline)Thread.sleep(25);ok(Files.isRegularFile(crash.resolve("prepared.marker")),"child prepared validated temp before replacement");process.destroyForcibly();ok(process.waitFor(5,TimeUnit.SECONDS),"interrupted writer exited");}finally{if(process.isAlive())process.destroyForcibly();}
        ok(Arrays.equals(beforeCrash,Files.readAllBytes(crash.resolve("autosave-0.glade"))),"process death before replacement preserves old save bytes");waitGood(child("recover",crash,9,root.resolve("after-interrupt-recover.log")));
        try(var leftovers=Files.list(root)){ok(leftovers.noneMatch(p->p.getFileName().toString().startsWith("glade-") && p.toString().endsWith(".tmp")),"handled write errors clean up their own temporary files");}
        System.out.println("SESSION_PERSISTENCE_PASS: optional checksum and legacy 1-9, semantic corruption detection, staged validation, injected/real write errors, timestamp-independent recovery, damaged-slot repair, bounded latest queue, closed writer, actual process restart and interrupted pre-replace writer; no screenshots");
    }
}

package vn.dreamhouse.glade;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** At most one active and one latest pending autosave; each committed version has a logical order. */
final class SessionStore implements AutoCloseable {
    record Recovery(GladeDesign design,Path file){}
    record Stats(int active,int pending,long coalesced,long written){}
    @FunctionalInterface interface Sink {void write(GladeDesign design,Path target,long sequence)throws IOException;}
    private record Request(GladeDesign design,CompletableFuture<Path> future){}
    private record Slot(Path file,GladeDesign design,long sequence,long modified){}
    private final Path directory;
    private final Sink sink;
    private final Object monitor=new Object();
    private Request active,pending;
    private boolean closed;
    private long coalesced,written,lastSequence;
    private int nextSlot;
    private volatile Thread writerThread;
    private final ExecutorService writer=Executors.newSingleThreadExecutor(task->{var t=new Thread(task,"DreamHouse-autosave");t.setDaemon(true);return t;});
    SessionStore(Path directory)throws IOException{this(directory,GladeDesign::saveRecovery);}
    SessionStore(Path directory,Sink sink)throws IOException{
        this.directory=Files.createDirectories(directory);this.sink=sink;var all=readSlots();
        lastSequence=all.stream().filter(s->s.design()!=null).mapToLong(Slot::sequence).max().orElse(0);
        var candidate=all.stream().filter(s->s.design()==null).findFirst().orElseGet(()->all.stream().min(order()).orElseThrow());
        nextSlot=slots().indexOf(candidate.file());
    }
    CompletableFuture<Path> save(GladeDesign snapshot){
        synchronized(monitor){
            if(closed)return CompletableFuture.failedFuture(new IOException("Bộ tự lưu đã đóng."));
            if(pending!=null && pending.design()==snapshot)return pending.future();
            if(active!=null && active.design()==snapshot){if(pending!=null){var old=pending;pending=null;coalesced++;old.future().completeExceptionally(new CancellationException("Đã có bản tự lưu mới hơn."));}return active.future();}
            var request=new Request(snapshot,new CompletableFuture<>());
            if(active==null){active=request;writer.execute(()->drain(request));}
            else{var old=pending;pending=request;if(old!=null){coalesced++;old.future().completeExceptionally(new CancellationException("Đã có bản tự lưu mới hơn."));}}
            return request.future();
        }
    }
    private void drain(Request request){
        writerThread=Thread.currentThread();
        while(request!=null){
            Path target=slots().get(nextSlot);Exception failure=null;
            try{
                if(lastSequence==Long.MAX_VALUE)throw new IOException("Chỉ mục phục hồi vượt giới hạn.");
                sink.write(request.design(),target,lastSequence+1);lastSequence++;nextSlot=(nextSlot+1)%3;
                synchronized(monitor){written++;}
            }catch(Exception error){failure=error;}
            Request completed=request;
            synchronized(monitor){active=pending;pending=null;request=active;}
            if(failure==null)completed.future().complete(target);else completed.future().completeExceptionally(failure);
        }
    }
    private List<Path> slots(){return List.of(directory.resolve("autosave-0.glade"),directory.resolve("autosave-1.glade"),directory.resolve("autosave-2.glade"));}
    static boolean hasRecoveryFile(Path directory){
        return java.util.stream.IntStream.range(0,3).anyMatch(slot->Files.isRegularFile(directory.resolve("autosave-"+slot+".glade")));
    }
    static long modified(Path file){try{return Files.getLastModifiedTime(file).toMillis();}catch(IOException e){return Long.MIN_VALUE;}}
    private static Comparator<Slot> order(){return Comparator.comparingLong(Slot::sequence).thenComparingLong(Slot::modified);}
    private List<Slot> readSlots(){
        var result=new ArrayList<Slot>();
        for(var file:slots()){
            GladeDesign design=null;long sequence=-1;
            if(Files.isRegularFile(file))try{var values=WorldFiles.read(file);design=GladeDesign.decode(values);sequence=Long.parseLong(values.getProperty("recovery.sequence","0"));if(sequence<0)throw new IOException("Chỉ mục phục hồi không hợp lệ.");}
            catch(Exception error){design=null;sequence=-1;PlayerFiles.error("Bỏ qua bản tự lưu bị lỗi: "+file,error);}
            result.add(new Slot(file,design,sequence,modified(file)));
        }
        return result;
    }
    Optional<Recovery> recover(){return readSlots().stream().filter(s->s.design()!=null).max(order()).map(s->new Recovery(s.design(),s.file()));}
    Stats stats(){synchronized(monitor){return new Stats(active==null?0:1,pending==null?0:1,coalesced,written);}}
    public void close(){
        synchronized(monitor){if(!closed){closed=true;writer.shutdown();}}
        if(Thread.currentThread()==writerThread)return;
        try{if(!writer.awaitTermination(10,TimeUnit.SECONDS))PlayerFiles.error("Tự lưu chưa hoàn tất sau 10 giây; bản đã ghi trước đó vẫn được giữ",null);}catch(InterruptedException e){Thread.currentThread().interrupt();}
    }
}

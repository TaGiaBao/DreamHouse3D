package vn.dreamhouse.glade;

import javax.sound.sampled.*;
import java.util.concurrent.Executors;

/** Original PCM cues for the Swing launcher, played off its event thread. */
final class LauncherAudio implements AutoCloseable {
    private static final int RATE=22050;
    private static final AudioFormat FORMAT=new AudioFormat(RATE,16,1,true,false);
    private final java.util.concurrent.ExecutorService audio=Executors.newSingleThreadExecutor(task->{
        var thread=new Thread(task,"dreamhouse-launcher-audio");
        thread.setDaemon(true);
        return thread;
    });
    private final Clip[] clips=new Clip[3];
    private boolean enabled,playingMusic,unavailable,closed;
    private int volume;

    void configure(boolean enabled,int volume){
        submit(()->{this.enabled=enabled;this.volume=volume;refreshMusic();});
    }
    void music(boolean playing){submit(()->{playingMusic=playing;refreshMusic();});}
    void tap(){cue(1);}
    void launch(){cue(2);}

    private void refreshMusic(){
        if(!enabled || volume==0){
            for(var clip:clips)if(clip!=null)clip.stop();
        }else if(!playingMusic){
            if(clips[0]!=null)clips[0].stop();
        }else withClip(0,clip->{if(!clip.isRunning())clip.loop(Clip.LOOP_CONTINUOUSLY);});
    }

    private void cue(int index){
        submit(()->withClip(index,clip->{clip.stop();clip.setFramePosition(0);clip.start();}));
    }

    private void withClip(int index,java.util.function.Consumer<Clip> action){
        if(unavailable || !enabled || volume==0)return;
        try{
            if(clips[index]==null){
                var data=pcm(index);
                clips[index]=AudioSystem.getClip();
                clips[index].open(FORMAT,data,0,data.length);
            }
            var clip=clips[index];
            if(clip.isControlSupported(FloatControl.Type.MASTER_GAIN)){
                var gain=(FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN);
                float level=(float)(20*Math.log10(volume/100d));
                gain.setValue(Math.clamp(level,gain.getMinimum(),gain.getMaximum()));
            }
            action.accept(clip);
        }catch(LineUnavailableException | RuntimeException error){
            unavailable=true;
            release();
            PlayerFiles.error("Không bật được âm thanh launcher",error);
        }
    }

    private synchronized void submit(Runnable work){if(!closed)audio.execute(work);}

    @Override public synchronized void close(){
        if(closed)return;
        closed=true;
        audio.execute(this::release);
        audio.shutdown();
    }

    private void release(){for(var clip:clips)if(clip!=null)clip.close();}

    static byte[] pcm(int sound){
        int[] melody={72,76,79,76,74,77,81,77,76,79,83,79,74,77,79,71};
        int[] bass={48,50,52,43};
        int beat=RATE*3/5;
        int length=sound==0?beat*melody.length:sound==1?RATE/14:RATE*2/5;
        byte[] data=new byte[length*2];
        for(int i=0;i<length;i++){
            double sample;
            if(sound==0){
                int note=i/beat;
                sample=pluck(melody[note],i%beat,beat)*.064
                        +pluck(bass[note/4],i%(4*beat),4*beat)*.035;
            }else if(sound==1){
                sample=pluck(83,i,length)*.16;
            }else{
                int second=length/3;
                sample=pluck(72,i,length)*.13;
                if(i>=second)sample+=pluck(79,i-second,length-second)*.11;
            }
            int value=(int)(sample*32767);
            data[i*2]=(byte)value;
            data[i*2+1]=(byte)(value>>8);
        }
        return data;
    }

    private static double pluck(int note,int frame,int length){
        double t=frame/(double)RATE;
        double phase=2*Math.PI*440*Math.pow(2,(note-69)/12d)*t;
        double envelope=Math.min(1,frame/180d)*Math.min(1,(length-1-frame)/400d)*Math.exp(-3*t);
        return (Math.sin(phase)+.21*Math.sin(2*phase))*envelope;
    }
}

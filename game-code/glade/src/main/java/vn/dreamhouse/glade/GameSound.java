package vn.dreamhouse.glade;
import com.jme3.audio.*;
import com.jme3.util.BufferUtils;
import java.nio.*;
import java.util.Random;

/** Small original synthesized sounds; no external recordings or downloads. */
final class GameSound {
    private final AudioNode tap,place,wind;
    private float volume;
    GameSound(float volume){tap=tone(720,.08f,false);place=tone(460,.17f,false);wind=tone(0,4,true);setVolume(volume);}
    static AudioNode tone(float frequency,float duration,boolean noise){
        int rate=22050,samples=(int)(rate*duration);var pcm=BufferUtils.createByteBuffer(samples*2).order(ByteOrder.nativeOrder());var random=new Random(73);float filtered=0;
        for(int i=0;i<samples;i++){double t=(double)i/rate;double value;if(noise){filtered=filtered*.97f+(random.nextFloat()*2-1)*.03f;value=filtered*.14;}else{double envelope=Math.sin(Math.PI*i/samples)*Math.exp(-t*18);value=(Math.sin(t*frequency*2*Math.PI)+.22*Math.sin(t*frequency*3*Math.PI))*envelope*.28;}pcm.putShort((short)(value*Short.MAX_VALUE));}
        pcm.flip();var data=new AudioBuffer();data.setupFormat(1,16,rate);data.updateData(pcm);var node=new AudioNode(data,new AudioKey("generated-"+frequency+"-"+duration,false));node.setPositional(false);node.setReverbEnabled(false);node.setLooping(noise);return node;
    }
    void setVolume(float value){volume=Math.max(0,Math.min(1,value));tap.setVolume(volume*.35f);place.setVolume(volume*.4f);wind.setVolume(volume*.24f);}
    void tap(){if(volume>0)tap.playInstance();}
    void place(){if(volume>0)place.playInstance();}
    void ambient(boolean enabled){if(enabled && volume>0)wind.play();else wind.stop();}
    void stop(){tap.stop();place.stop();wind.stop();}
}

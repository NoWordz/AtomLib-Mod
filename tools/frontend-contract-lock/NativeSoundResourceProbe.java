import com.mojang.blaze3d.audio.OggAudioStream;
import java.nio.file.*;

/** Uses the actual 1.20.1 Ogg decoder; never creates a game or audio device. */
public final class NativeSoundResourceProbe {
    public static void main(String[] args) throws Exception {
        for(String arg:args) {
            Path path=Path.of(arg);
            String cue=path.getFileName().toString().replace(".ogg","");
            try(var decoded=new OggAudioStream(Files.newInputStream(path))) {
                var format=decoded.getFormat();int size=decoded.readAll().remaining();
                if(size<=0 || format.getChannels()<=0 || format.getSampleRate()<=0) throw new AssertionError("empty decoded sound");
                System.out.println(path+" decodedBytes="+size+" sampleRate="+format.getSampleRate());
            }
        }
    }
}

package kir.util.media;

import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.Executors;

public final class MediaPlayer {

    private final String source;
    private final JFrame jfContainer;
    private final JLabel jlImage;

    private volatile boolean isPlaying;
    private volatile long startTime = 0;

    private int screenWidth;
    private int screenHeight;

    public MediaPlayer(String address, int port) {
        this(String.format("udp://%s:%d?overrun_nonfatal=1&fifo_size=280000", address, port));
    }
    public MediaPlayer(String source) {
        this.source = source;
        jfContainer = new JFrame();
        jlImage = new JLabel();
        FFmpegLogCallback.setLevel(avutil.AV_LOG_QUIET);
        screenWidth = 1366;
        screenHeight = 768;
    }

    public void setScreenSize(int width, int height) {
        screenWidth = width;
        screenHeight = height;
    }

    public void play() {
        initWindow();
        try (var grabber = new FFmpegFrameGrabber(source)) {
            grabber.setOption("fflags", "discardcorrupt");
            grabber.start();

            jlImage.setSize(grabber.getImageWidth(), grabber.getImageHeight());
            jfContainer.setSize(grabber.getImageWidth(), grabber.getImageHeight());

            var audioFormat = new AudioFormat(grabber.getSampleRate(), 16, grabber.getAudioChannels(), true, true);
            var info = new DataLine.Info(SourceDataLine.class, audioFormat);
            var soundLine = (SourceDataLine) AudioSystem.getLine(info);
            soundLine.open(audioFormat);
            soundLine.start();

            var imageExecutor = Executors.newCachedThreadPool();
            var audioExecutor = Executors.newSingleThreadExecutor();

            final var converter = new Java2DFrameConverter();
            final var maxReadAheadBufferMicros = 1000 * 1000L;
            long lastImageTimestamp = -1L;
            isPlaying = true;
            while (isPlaying) {
                var frame = grabber.grab();
                if (frame.image != null) {
                    if (frame.timestamp < lastImageTimestamp) {
                        startTime = 0;
                    }
                    lastImageTimestamp = frame.timestamp;
                    final var imgFrame = frame.clone();
                    imageExecutor.execute(() -> {
                        if (startTime == 0) {
                            startTime = System.nanoTime() / 1000 - imgFrame.timestamp;
                        } else {
                            final var delay = imgFrame.timestamp - (System.nanoTime() / 1000 - startTime);
                            if (delay > 0) {
                                try {
                                    Thread.sleep(delay / 1000, (int) (delay % 1000) * 1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        imageExecutor.execute(() -> {
                            jlImage.setIcon(new ImageIcon(converter.convert(imgFrame)));
                            imgFrame.close();
                        });
                    });
                } else if (frame.samples != null) {
                    final var channelSamplesShortBuffer = (ShortBuffer) frame.samples[0];
                    channelSamplesShortBuffer.rewind();
                    final var outBuffer = ByteBuffer.allocate(channelSamplesShortBuffer.capacity() * 2);
                    for (int i = 0; i < channelSamplesShortBuffer.capacity(); i++) {
                        short val = channelSamplesShortBuffer.get(i);
                        outBuffer.putShort(val);
                    }
                    audioExecutor.execute(() -> {
                        soundLine.write(outBuffer.array(), 0, outBuffer.capacity());
                        outBuffer.clear();
                    });
                }
                final var timeStampDeltaMicros = frame.timestamp - (System.nanoTime() / 1000 - startTime);
                if (timeStampDeltaMicros > maxReadAheadBufferMicros) {
                    Thread.sleep((timeStampDeltaMicros - maxReadAheadBufferMicros) / 1000);
                }
            }
            converter.close();
        } catch (FrameGrabber.Exception | LineUnavailableException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException ignored) {}
    }

    private void initWindow() {
        jfContainer.setTitle("Playback");
        jfContainer.getContentPane().add(jlImage, BorderLayout.CENTER);
        jfContainer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jfContainer.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                super.windowClosed(e);
                isPlaying = false;
            }
        });
        jfContainer.setResizable(false);
        jfContainer.pack();
        jfContainer.setSize(screenWidth, screenHeight);
        jfContainer.setLocationRelativeTo(null);
        jfContainer.setVisible(true);
    }

}

package kir.util.media;

import kir.util.Printer;
import lombok.Getter;
import lombok.Setter;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class MediaStreamer {

    private final BlockingQueue<String> mediaQueue = new LinkedBlockingQueue<>(255);
    private final String multicastAddress;
    private final Resolution streamResolution;

    @Getter
    private String currentFileName = null;

    @Setter
    private String videoCodecName = null;

    public MediaStreamer(String host, int port) {
        this(host, port, Resolution.HD);
    }
    public MediaStreamer(String host, int port, Resolution resolution) {
        multicastAddress = String.format("udp://%s:%d", host, port);
        streamResolution = resolution;
        FFmpegLogCallback.setLevel(avutil.AV_LOG_QUIET);
    }

    public List<String> getQueue() {
        return new ArrayList<>(mediaQueue);
    }

    public void addVideoSourceSource(String source) {
        Path videoPath = Path.of(source);
        if (!Files.exists(videoPath)) {
            Printer.error("File not exists.");
            return;
        }
        if (isVideoFile(videoPath)) {
            Printer.error("Not a video file.");
            return;
        }
        if (!mediaQueue.offer(source)) {
            Printer.info("Queue is full.");
        }
    }
    public void addVideoDirectory(String directory) throws IOException {
        final var dirPath = Path.of(directory);
        if (!Files.isDirectory(dirPath)) {
            Printer.error("Not a directory.");
            return;
        }
        Files.walkFileTree(dirPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                // We don't want to read any subdirectories
                if (dir.toAbsolutePath().toString().equals(dirPath.toAbsolutePath().toString())) {
                    return FileVisitResult.CONTINUE;
                }
                return FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (isVideoFile(file)) {
                    if (!mediaQueue.offer(file.toString())) {
                        Printer.info("Queue is full.");
                        return FileVisitResult.TERMINATE;
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        Printer.success("Load success. Queue size: " + mediaQueue.size());
    }

    public boolean stream() throws FrameRecorder.Exception, FrameGrabber.Exception, InterruptedException {
        if (mediaQueue.isEmpty()) {
            return false;
        }
        final var currentVideo = mediaQueue.take();
        currentFileName = Path.of(currentVideo).getFileName().toString();
        try (var grabber = buildGrabber(currentVideo)) {
            grabber.start();
            try (var recorder = buildRecorder(grabber)) {
                recorder.start();
                Frame frame;
                while ((frame = grabber.grabAtFrameRate()) != null) {
                    recorder.setTimestamp(frame.timestamp);
                    recorder.record(frame);
                }
            }
        }
        return !mediaQueue.isEmpty();
    }

    private FFmpegFrameGrabber buildGrabber(String source) {
        return new FFmpegFrameGrabber(source);
    }
    private FFmpegFrameRecorder buildRecorder(FFmpegFrameGrabber grabber) {
//        var sw = streamResolution.getWidth() == -1 ? grabber.getImageWidth() : streamResolution.getWidth();
//        var sh = streamResolution.getHeight() == -1 ? grabber.getImageHeight() : streamResolution.getHeight();
        var recorder = new FFmpegFrameRecorder(multicastAddress, 1366, 768);
        // Add options here
        recorder.setFormat("mpegts");
        recorder.setFrameRate(grabber.getFrameRate());
        recorder.setMetadata(grabber.getMetadata());

        recorder.setVideoCodecName(videoCodecName == null ? "qsv" : videoCodecName);
        recorder.setVideoOption("preset", "ultrafast");
        recorder.setVideoOption("tune", "zerolatency");
        recorder.setVideoBitrate(grabber.getVideoBitrate());

        recorder.setAudioChannels(grabber.getAudioChannels());
        recorder.setAudioCodec(grabber.getAudioCodec());
        recorder.setAudioBitrate(grabber.getAudioBitrate());

        return recorder;
    }

    private boolean isVideoFile(Path path) {
        try {
            if (Files.probeContentType(path).startsWith("video")) {
                return true;
            }
        } catch (IOException ignored) {}
        return false;
    }
}

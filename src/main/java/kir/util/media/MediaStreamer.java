package kir.util.media;

import kir.util.Printer;
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
    private final int imageWidth;
    private final int imageHeight;

    private String currentFileName = null;
    private String videoCodecName = null;

    public MediaStreamer(String address, int port) {
        this(address, port, 1366, 768);
    }
    public MediaStreamer(String address, int port, int imageWidth, int imageHeight) {
        multicastAddress = String.format("udp://%s:%d", address, port);
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        FFmpegLogCallback.setLevel(avutil.AV_LOG_FATAL);
    }

    public String getCurrentFileName() {
        return currentFileName;
    }
    public void setVideoCodecName(String videoCodecName) {
        this.videoCodecName = videoCodecName;
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
        var recorder = new FFmpegFrameRecorder(multicastAddress, imageWidth, imageHeight);
        // Add options here
        recorder.setFormat("mpegts");
        recorder.setFrameRate(grabber.getFrameRate());
        recorder.setMetadata(grabber.getMetadata());

        recorder.setVideoCodecName(videoCodecName);
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

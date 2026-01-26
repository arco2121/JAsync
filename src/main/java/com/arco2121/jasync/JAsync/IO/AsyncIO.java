package com.arco2121.jasync.JAsync.IO;

import com.arco2121.jasync.JAsync.*;
import com.arco2121.jasync.JAsync.Collections.AsyncList;
import com.arco2121.jasync.JAsync.Collections.JSON;
import com.arco2121.jasync.JAsync.Collections.TOON;
import com.arco2121.jasync.JAsync.Running.Asyncable;
import com.arco2121.jasync.Types.Exceptions.CannotDeconstructONException;
import com.arco2121.jasync.Types.Exceptions.InvalidResourceException;
import com.arco2121.jasync.Types.Exceptions.NotONException;
import com.arco2121.jasync.Types.Interfaces.ObjectNotations.JSONable;
import com.arco2121.jasync.Types.Interfaces.ObjectNotations.TOONable;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Async IO operations
 */
public final class AsyncIO {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Scanner scanner = new Scanner(System.in);

    public enum Method {
        GET, POST, PUT
    }
    public static final class Resource {

        public final Object source;
        public final Method method;

        public Resource(Object source) throws InvalidResourceException {
            if(!(source instanceof byte[] || source instanceof ByteBuffer ||
                    source instanceof InputStream || source instanceof Path ||
                    source instanceof Process || source instanceof FileChannel))
                throw new InvalidResourceException("Resource not found: " + source.getClass().getName());
            this.source = source;
            this.method = null;
        }
        public Resource(String localPath) {
            this.source = Path.of(localPath);
            this.method = null;
        }
        public Resource(URI url, Method method) {
            this.source = url;
            this.method = method;
        }
        public Resource(DatagramSocket socket, String host, int PORT) {
            try {
                socket.bind(new InetSocketAddress(host, PORT));
            } catch (IOException ignore) {}
            this.source = socket;
            this.method = null;
        }
        public Resource(Socket socket, String host, int PORT) {
            try {
                socket.bind(new InetSocketAddress(host, PORT));
            } catch (IOException ignore) {}
            this.source = socket;
            this.method = null;
        }
    }

    private static InputStream getStream(Resource resource) throws IOException, InterruptedException, InvalidResourceException {
        Object source = resource.source;
        if (source instanceof byte[] bytes) return new ByteArrayInputStream(bytes);
        if (source instanceof ByteBuffer buffer) return new ByteArrayInputStream(buffer.array());
        if (source instanceof Socket s) return s.getInputStream();
        if (source instanceof InputStream is) return is;
        if (source instanceof URI s)
            return CLIENT.send(HttpRequest.newBuilder().uri(s).build(), HttpResponse.BodyHandlers.ofInputStream()).body();
        if(source instanceof Path s)
            return Files.newInputStream(s);
        if(source instanceof Process s)
            return s.getInputStream();
        if (source instanceof FileChannel channel) return Channels.newInputStream(channel);
        if (source instanceof DatagramSocket ds)
            return new InputStream() {
                @Override
                public int read() throws IOException {
                    byte[] b = new byte[1];
                    if (read(b) == -1) return -1;
                    return b[0] & 0xFF;
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    DatagramPacket packet = new DatagramPacket(b, off, len);
                    ds.receive(packet);
                    return packet.getLength();
                }
            };

        throw new InvalidResourceException("Resource not found: " + source.getClass().getName());
    }

    private static Stream<String> getText(Resource resource) throws IOException, InterruptedException, InvalidResourceException {
        InputStream is = getStream(resource);
        return new BufferedReader(new InputStreamReader(is)).lines();
    }

    private static OutputStream pushSteam(Resource destination) throws IOException, InvalidResourceException {
        Object res = destination.source;
        if (res instanceof byte[] bytes) return new ByteArrayOutputStream(bytes.length);
        if (res instanceof ByteBuffer bytes) return new ByteArrayOutputStream(bytes.array().length);
        if (res instanceof Socket s) return s.getOutputStream();
        if (res instanceof Path p) return Files.newOutputStream(p);
        if (res instanceof FileChannel channel) return Channels.newOutputStream(channel);
        if (res instanceof Process p) return p.getOutputStream();
        if (res instanceof URI) return null;
        if (res instanceof DatagramSocket) return null;

        throw new InvalidResourceException("Destination not supported: " + res.getClass().getName());
    }

    private static byte[] serialize(Object obj) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
            return bos.toByteArray();
        }
    }

    public static <T> AsyncList<T> memorize(AsyncQueue<T> queue) {
        AsyncList<T> result = new AsyncList<>();
        queue.forEach(result::add);
        return result;
    }

    public final static class Input {

        public static Asyncable<String> asyncIn() {
            return Async.async(() -> scanner.nextLine());
        }

        public static String awaitIn(int timeout) {
            return Async.await(() -> scanner.nextLine(), timeout);
        }
        public static String awaitIn() {
            return Async.await(() -> scanner.nextLine());
        }

        public static Asyncable<Object> fetch(Resource source) {
            return Async.async(() -> {
                try (InputStream is = getStream(source); ObjectInputStream out = new ObjectInputStream(is)) {
                    return out.readObject();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            });
        }
        public static <T> Asyncable<T> fetch(Resource source, Function<Object, T> change) throws ClassCastException {
            return Async.async(() -> {
                try (InputStream is = getStream(source); ObjectInputStream out = new ObjectInputStream(is)) {
                    return change.apply(out.readObject());
                } catch (ClassCastException e) {
                    throw new ClassCastException("Class not found: " + source.getClass().getName());
                }
            });
        }

        public static Asyncable<String> fetchText(Resource source) {
            return Async.async(() -> {
                try (Stream<String> lines = getText(source)) {
                    StringBuilder temp = new StringBuilder();
                    lines.forEach(temp::append);
                    return temp.toString();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            });
        }
        public static Asyncable<String> fetchText(Resource source, char lineSeparator) {
            return Async.async(() -> {
                try (Stream<String> lines = getText(source)) {
                    StringBuilder temp = new StringBuilder();
                    lines.forEach(line -> {
                        temp.append(line);
                        temp.append("%");
                        temp.append(lineSeparator);
                        temp.append("%");
                    });
                    return temp.toString();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            });
        }

        public static Asyncable<JSON> fetchJSON(Resource source) throws CannotDeconstructONException {
            return Async.async(() -> {
                try (Stream<String> lines = getText(source)) {
                    StringBuilder temp = new StringBuilder();
                    lines.forEach(temp::append);
                    String obj = temp.toString();
                    return JSON.fromNotation(obj);
                } catch (Exception e) {
                    throw new CannotDeconstructONException("Cannot derive from JSON");
                }
            });
        }

        public static Asyncable<TOON> fetchTOON(Resource source) throws CannotDeconstructONException {
            return Async.async(() -> {
                try (Stream<String> lines = getText(source)) {
                    StringBuilder temp = new StringBuilder();
                    lines.forEach(temp::append);
                    String obj = temp.toString();
                    return TOON.fromNotation(obj);
                } catch (Exception e) {
                    throw new CannotDeconstructONException("Cannot derive from JSON");
                }
            });
        }

        public static <T> Asyncable<T> fetchFromJSON(Resource source, Class<T> classTo) throws CannotDeconstructONException {
            return Async.async(() -> {
                try (Stream<String> lines = getText(source)) {
                    StringBuilder temp = new StringBuilder();
                    lines.forEach(temp::append);
                    String obj = temp.toString();
                    return JSONable.fromNotation(obj, classTo);
                } catch (Exception e) {
                    throw new CannotDeconstructONException("Cannot derive from JSON");
                }
            });
        }

        public static <T> AsyncQueue<T> fetchBinaries(Resource source, Function<Object, T> change) throws ClassCastException {
            AsyncQueue<T> queue = new AsyncQueue<>();
            Async.await(() -> {
                try (InputStream is = getStream(source);
                     ObjectInputStream ois = new ObjectInputStream(is)) {
                    while (true) {
                        try {
                            queue.add((T) ois.readObject());
                        } catch (EOFException e) { break; }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    queue.close();
                }
                return null;
            });
            return queue;
        }

        public static <T> AsyncQueue<T> fetchLines(Resource source, Function<String, T> transformation) throws ClassCastException {
            AsyncQueue<T> queue = new AsyncQueue<>();
            Async.await(() -> {
                try (Stream<String> lines = getText(source)) {
                    lines.forEach(line -> {
                        T obj = transformation.apply(line);
                        if (obj != null) queue.add(obj);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    queue.close();
                }
                return null;
            });
            return queue;
        }
    }

    public final static class Output {

        public static Asyncable<Void> asyncOut(String data, int timeout) {
            return Async.async(() -> {
                Async.timeout(timeout);
                System.out.println(data);
            });
        }

        public static void send(Resource destination, Object data) {
            Object res = destination.source;
            Async.async(() -> {
                if (res instanceof URI uri) {
                    byte[] serialized = serialize(data);
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(uri)
                            .header("Content-Type", "application/x-java-serialized-object")
                            .method(String.valueOf(destination.method), HttpRequest.BodyPublishers.ofByteArray(serialized))
                            .build();
                    CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
                }
                else if (res instanceof DatagramSocket socket) {
                    byte[] bytes = serialize(data);
                    DatagramPacket packet = new DatagramPacket(bytes, bytes.length, socket.getInetAddress(), socket.getPort());
                    socket.send(packet);
                }
                else {
                    try (OutputStream os = pushSteam(destination); ObjectOutputStream out = new ObjectOutputStream(os)) {
                        out.writeObject(data);
                    }
                }
                return null;
            });
        }

        public static void sendText(Resource destination, String data) {
            Object res = destination.source;
            Async.async(() -> {
                if (res instanceof URI uri) {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(uri)
                            .header("Content-Type", "text/plain")
                            .method(String.valueOf(destination.method), HttpRequest.BodyPublishers.ofString(data))
                            .build();
                    CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                }
                else
                    send(destination, data);
                return null;
            });
        }

        public static void sendJSON(Resource destination, Object data) throws NotONException {
            Object res = destination.source;
            Async.async(() -> {
                if (res instanceof URI uri && data instanceof JSONable jas) {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(uri)
                            .header("Content-Type", "application/json")
                            .method(String.valueOf(destination.method), HttpRequest.BodyPublishers.ofString(jas.toNotation()))
                            .build();
                    CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                }
                else
                    send(destination, data);
                return null;
            });
        }

        public static void sendTOON(Resource destination, Object data) throws NotONException {
            Object res = destination.source;
            Async.async(() -> {
                if (res instanceof URI uri && data instanceof TOONable jas) {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(uri)
                            .header("Content-Type", "application/toon")
                            .method(String.valueOf(destination.method), HttpRequest.BodyPublishers.ofString(jas.toNotation()))
                            .build();
                    CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                }
                else
                    send(destination, data);
                return null;
            });
        }
    }
}
package org.example;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {
    private final ExecutorService executorService;
    public static final String GET = "GET";
    public static final String POST = "POST";
    final List<String> allowedMethods = List.of(GET, POST);

    public Server(int countThreadsPool) {
        this.executorService = Executors.newFixedThreadPool(countThreadsPool);
    }


    public void listen(int port) {
        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {
                var clientSocket = serverSocket.accept();
                executorService.submit(() -> handleConnection(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public List<NameValuePair> getQueryParam(String name) throws URISyntaxException {

        List<NameValuePair> params = URLEncodedUtils.parse(new URI(name), StandardCharsets.UTF_8);
        return params;
    }

    public void getQueryParams(){

    }
    public void handleConnection(Socket socket) {
        try (
                socket;
                final var in = new BufferedInputStream(socket.getInputStream());
                final var out = new BufferedOutputStream(socket.getOutputStream())
        ) {

            for(NameValuePair a : getQueryParam("https://www.cian.ru/cat.php?currency=2&deal_type=sale&engine_version=2")) System.out.print(a + " ");




            // лимит на request line + заголовки
            final var limit = 4096;

            in.mark(limit);
            final var buffer = new byte[limit];
            final var read = in.read(buffer);


            // ищем request line
            final var requestLineDelimiter = new byte[]{'\r', '\n','\r', '\n'};
            final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
            if (requestLineEnd == -1) {
                badRequest(out);
                return;
            }

            // читаем request line
            final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");

            if (requestLine.length != 3) {
                badRequest(out);
                return;
            }

            final var method = requestLine[0];
            if (!allowedMethods.contains(method)) {
                badRequest(out);
                return;
            }
            System.out.println(method);

            final var path = requestLine[1];
            if (!path.startsWith("/")) {
                badRequest(out);
                return ;
            }
            System.out.println(path);

            // ищем заголовки
            final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
            final var headersStart = requestLineEnd + requestLineDelimiter.length;
            final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
            if (headersEnd == -1) {
                badRequest(out);
                return;
            }

            // отматываем на начало буфера
            in.reset();
            // пропускаем requestLine
            in.skip(headersStart);

            final var headersBytes = in.readNBytes(headersEnd - headersStart);
            final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));

            // для GET тела нет
            if (!method.equals(GET)) {
                in.skip(headersDelimiter.length);
                // вычитываем Content-Length, чтобы прочитать body
                final var contentLength = extractHeader(headers, "Content-Length");
                if (contentLength.isPresent()) {
                    final var length = Integer.parseInt(contentLength.get());
                    final var bodyBytes = in.readNBytes(length);

                    final var body = new String(bodyBytes);
                    System.out.println(body);
                }
            }


            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
        .filter(o -> o.startsWith(header))
        .map(o -> o.substring(o.indexOf(" ")))
        .map(String::trim)
        .findFirst();
        }

private static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
        "HTTP/1.1 400 Bad Request\r\n" +
        "Content-Length: 0\r\n" +
        "Connection: close\r\n" +
        "\r\n"
        ).getBytes());
        out.flush();
        }

// from google guava with modifications
private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
        for (int j = 0; j < target.length; j++) {
        if (array[i + j] != target[j]) {
        continue outer;
        }
        }
        return i;
        }
        return -1;
        }



}

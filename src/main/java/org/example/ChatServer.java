package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.login.LoginRequest;
import org.example.login.LoginResponse;
import org.example.messages.AbstractRequest;
import org.example.messages.BroadcastMessageRequest;
import org.example.messages.SendMessageRequest;
import org.example.messages.UsersListRequest;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
        try (ServerSocket server = new ServerSocket(8888)) {
            System.out.println("Сервер запущен");

            while (true) {
                System.out.println("Ждем подключения");
                Socket client = server.accept();
                ClientHandler clientHandler = new ClientHandler(client, clients);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Ошибка во время работы сервера: " + e.getMessage());
        }
    }


    private static class ClientHandler implements Runnable {

        private final Socket client;
        private final Scanner in;
        private final PrintWriter out;
        private final Map<String, ClientHandler> clients;
        private String clientLogin;

        public ClientHandler(Socket client, Map<String, ClientHandler> clients) throws IOException {
            this.client = client;
            this.clients = clients;
            this.in = new Scanner(client.getInputStream());
            this.out = new PrintWriter(client.getOutputStream(), true);
        }

        @Override
        public String toString() {
            return "(" +
                    "clientLogin=" + clientLogin + ")";
        }

        @Override
        public void run() {
            System.out.println("Подключен новый клиент");

            try {
                String loginRequest = in.nextLine();
                LoginRequest request = objectMapper.reader().readValue(loginRequest, LoginRequest.class);
                this.clientLogin = request.getLogin();
            } catch (IOException e) {
                System.err.println("Не удалось прочитать сообщение от [" + clientLogin + "]: " + e.getMessage());
                String unsuccessfulResponse = createLoginResponse(false);
                out.println(unsuccessfulResponse);
                doClose();
                return;
            }


            System.out.println("Запрос от клиента: " + clientLogin);

            if (clients.containsKey(clientLogin)) {
                String unsuccessfulResponse = createLoginResponse(false);
                out.println(unsuccessfulResponse);
                doClose();
                return;
            }

            clients.put(clientLogin, this);
            String successfulResponse = createLoginResponse(true);
            out.println(successfulResponse);

            while (true) {
                String msgFromClient = in.nextLine();
                final String type;
                try {
                    AbstractRequest abstractRequest =
                            objectMapper.reader().readValue(
                                    msgFromClient,
                                    AbstractRequest.class);
                    type = abstractRequest.getType();
                } catch (IOException e) {
                    System.err.println("Не удалось прочитать сообщение [while 1] от ["
                            + clientLogin + "]: "
                            + e.getMessage());
                    continue;
                }

                if (SendMessageRequest.TYPE.equals(type)) {
                    final SendMessageRequest request;
                    try {
                        request =
                                objectMapper.reader().readValue(
                                        msgFromClient,
                                        SendMessageRequest.class);
                    } catch (IOException e) {
                        System.err.println("Не удалось прочитать сообщение [while 2] от ["
                                + clientLogin + "]: "
                                + e.getMessage());
                        sendMessage("Не удалось прочитать сообщение: " + e.getMessage());
                        continue;
                    }

                    ClientHandler clientTo = clients.get(request.getRecipient());
                    if (clientTo == null) {
                        sendMessage("Клиент [" + request.getRecipient() + "] не найден");
                        continue;
                    }

                    clientTo.sendMessage(request);

                } else if (BroadcastMessageRequest.TYPE.equals(type)) {
                    final BroadcastMessageRequest broadcast;
                    try {
                        broadcast =
                                objectMapper.reader().readValue(
                                        msgFromClient,
                                        BroadcastMessageRequest.class);
                    } catch (IOException e) {
                        System.err.println("Ошибка отправки для всех");
                        sendMessage("Не удалось прочитать сообщение для всех: " + e.getMessage());
                        continue;
                    }

                    Collection<ClientHandler> clientList = clients.values();
                    for (ClientHandler clientTo : clientList) {
                        if (broadcast.getClientFrom().equals(clientTo.clientLogin)) {
                            continue;
                        }
                        clientTo.sendMessage(broadcast);
                    }

                } else if (UsersListRequest.TYPE.equals(type)) {
                    final UsersListRequest listRequest;
                    try {
                        listRequest =
                                objectMapper.reader().readValue(
                                        msgFromClient,
                                        UsersListRequest.class);
                    } catch (IOException e) {
                        System.err.println("Ошибка при попытке получения списка пользователей");
                        sendMessage("Ошибка получения списка пользователей: " + e.getMessage());
                        continue;
                    }

                    Set<ClientHandler> set = new HashSet<>(clients.values());

                    listRequest.setMessage(set.toString());

                    ClientHandler backTo = clients.get(listRequest.getRecipient());
                    backTo.sendMessage(listRequest);

                } else {
                    System.err.println("Неизвестный тип сообщения " + type);
                    sendMessage("Неизвестный тип сообщения: " + type);
                    break;
                }
            }
            doClose();
        }

        private void doClose() {
            try {
                in.close();
                out.close();
                client.close();
            } catch (IOException e) {
                System.err.println("Ошибка во время отключения клиента: " + e.getMessage());
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void sendMessage(AbstractRequest response) {
            try {
                String msgResponse = objectMapper.writeValueAsString(response);
                out.println(msgResponse);
            } catch (JsonProcessingException e) {
                System.out.println("ошибка в sendmessage 2");
            }
        }

        private String createLoginResponse(boolean success) {
            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setConnected(success);
            try {
                return objectMapper.writer().writeValueAsString(loginResponse);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Не удалось создать loginResponse: " + e.getMessage());
            }
        }
    }
}

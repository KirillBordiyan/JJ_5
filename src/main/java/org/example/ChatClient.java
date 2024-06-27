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
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Scanner console = new Scanner(System.in);
        String clientLogin = console.nextLine();


        try (Socket server = new Socket("127.0.0.1", 8888)
        /*localhost == 127.0.0.1, либо проверить на машине ее ip (ip config)
        на другой машине в хост пишется ip сервера, не машины, которая подключается, а "К КОТОРОЙ"
         */) {
            System.out.println("Успешно подключились к серверу");

            try (PrintWriter out = new PrintWriter(server.getOutputStream(), true)) {

                Scanner in = new Scanner(server.getInputStream()); // не в try() тк он закроется, а это не надо

                String loginRequest = createLoginRequest(clientLogin);
                out.println(loginRequest);

                String loginResponseString = in.nextLine();

                if (!checkLoginResponse(loginResponseString)) {
                    System.err.println("Не удалось подключится к серверу");
                    return;
                }

                new Thread(() -> {
                    while (true) {
                        String msgFromServer = in.nextLine();

                        try {
                            String requestType = objectMapper.reader()
                                    .readValue(msgFromServer, AbstractRequest.class).getType();

                            switch (requestType) {
                                case SendMessageRequest.TYPE -> {
                                    SendMessageRequest sendMessageRequest =
                                            objectMapper.reader().readValue(
                                                    msgFromServer,
                                                    SendMessageRequest.class);

                                    String clientFrom = sendMessageRequest.getClientFrom();

                                    showMessage(clientFrom, sendMessageRequest.getMessage());

                                }
                                case BroadcastMessageRequest.TYPE -> {
                                    BroadcastMessageRequest broadcast =
                                            objectMapper.reader().readValue(
                                                    msgFromServer,
                                                    BroadcastMessageRequest.class);

                                    String clientFrom = broadcast.getClientFrom();

                                    showMessage(clientFrom, broadcast.getMessage());
                                }
                                case UsersListRequest.TYPE -> {
                                    UsersListRequest listRequest =
                                            objectMapper.reader().readValue(
                                                    msgFromServer,
                                                    UsersListRequest.class);

                                    String list = listRequest.getMessage();
                                    String clientFrom = "сервер";

                                    showMessage(clientFrom, list);

                                }
                            }
                        } catch (IOException e) {
                            System.out.println("Что-то не так с входящими");
                        }
                    }
                }).start();


                while (true) {

                    System.out.println("Что можно сделать:");
                    System.out.println("1. Отправить сообщение другу");
                    System.out.println("2. Отправить сообщение всем");
                    System.out.println("3. Получить список логинов");

                    String action = console.nextLine();

                    switch (action) {
                        case "1" -> {
                            System.out.println("Формат ввода -> @login: message");
                            String[] message = console.nextLine().split(": ", 2);

                            SendMessageRequest request = new SendMessageRequest();
                            request.setRecipient(message[0].replace("@", "")); //указываем логин получателя

                            request.setClientFrom(clientLogin);
                            request.setMessage(message[1].trim());

                            String sendMsgRequest = objectMapper.writeValueAsString(request);
                            out.println(sendMsgRequest);
                        }
                        case "2" -> {

                            System.out.println("Введите сообщение для всех: ");
                            String message = console.nextLine();

                            BroadcastMessageRequest broad = new BroadcastMessageRequest();
                            broad.setClientFrom(clientLogin);
                            broad.setMessage(message);

                            String msgToAll = objectMapper.writeValueAsString(broad);
                            out.println(msgToAll);
                        }
                        case "3" -> {
                            UsersListRequest listRequest = new UsersListRequest();
                            listRequest.setRecipient(clientLogin);
                            String usersList = objectMapper.writeValueAsString(listRequest);
                            out.println(usersList);
                            System.out.println("Список всех пользователей: ");
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка во время подключения к серверу: " + e.getMessage());
        }
        System.out.println("Отключились от сервера");
    }

    private static void showMessage(String clientFrom, String message){
        System.out.println("Сообщение от ["
                + clientFrom + "]: "
                + message);
    }

    private static String createLoginRequest(String login) {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLogin(login);

        try {
            return objectMapper.writeValueAsString(loginRequest);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка JSON: " + e.getMessage());
        }
    }

    private static boolean checkLoginResponse(String loginResponse) {
        try {
            LoginResponse resp = objectMapper.reader().readValue(loginResponse, LoginResponse.class);
            return resp.isConnected();
        } catch (IOException e) {
            System.err.println("Ошибка чтения JSON: " + e.getMessage());
            return false;
        }
    }
}

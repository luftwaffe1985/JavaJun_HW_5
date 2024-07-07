import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

public class ChatClient {

    private static ObjectMapper objectMapper = new ObjectMapper();
    static Boolean threadInCondition = true;

    public static void main(String[] args) {
        try (Scanner console = new Scanner(System.in)) {
            String clientLogin = console.nextLine();
            String sendExit = "";
            try (Socket server = new Socket("localhost", 8888)) {
                System.out.println("Server connection successful");
                try (PrintWriter out = new PrintWriter(server.getOutputStream(), true)) {
                    try (Scanner in = new Scanner(server.getInputStream())) {
                        String loginRequest = createLoginRequest(clientLogin);
                        out.println(loginRequest);
                        String loginResponseString = in.nextLine();
                        if (!checkLoginResponse(loginResponseString)) {
                            System.out.println("Server connection failed");
                            return;
                        }
                        new Thread(() -> {
                            while (threadInCondition) {
                                String msgFromServer = in.nextLine();
                                System.out.println("Server msg: " + msgFromServer);
                            }
                        }).start();
                    }
                    while (true) {
                        System.out.println("What to do");
                        System.out.println("1. Send to a person");
                        System.out.println("2. Send to all");
                        System.out.println("3. Get a logins list");
                        System.out.println("4. Escape");
                        String type = console.nextLine();
                        if (type.equals("1")) {
                            SendMsgRequest request = new SendMsgRequest();
                            System.out.print("Enter a recipient's login: ");
                            request.setRecipient(console.nextLine());
                            System.out.print("\n" + "Enter a text: ");
                            request.setMessage(console.nextLine());
                            String sendMsgRequest = objectMapper.writeValueAsString(request);
                            out.println(sendMsgRequest);
                        } else if (type.equals("2")) {
                            SendBroadcastMessageRequest request = new SendBroadcastMessageRequest();
                            System.out.print("Enter a text for sending to all: ");
                            request.setMessage(console.nextLine());
                            String sendBroadcastMsgRequest = objectMapper.writeValueAsString(request);
                            out.println(sendBroadcastMsgRequest);
                        } else if (type.equals("3")) {
                            SendToGetUsersRequest request = new SendToGetUsersRequest();
                            String sendToGetUsersRequest = objectMapper.writeValueAsString(request);
                            System.out.println(sendToGetUsersRequest);
                            System.out.println("Chat recipients list: ");
                            out.println(sendToGetUsersRequest);
                        } else if (type.equals("4")) {
                            threadInCondition = false;
                            SendDisconnectionRequest request = new SendDisconnectionRequest();
                            sendExit = objectMapper.writeValueAsString(request);
                            break;
                        } else {
                            System.out.println("Unknown command, enter new data");
                        }
                    }
                    out.println(sendExit);
                }
            } catch (IOException e) {
                System.err.println("Server connection error: " + e.getMessage());
            }
        }
        System.out.println("Server disconnection");
    }

    private static String createLoginRequest(String login) {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLogin(login);
        try {
            return objectMapper.writeValueAsString(loginRequest);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON error: " + e.getMessage());
        }
    }

    private static boolean checkLoginResponse(String loginResponse) {
        try {
            LoginResponse resp = objectMapper.reader().readValue(loginResponse, LoginResponse.class);
            return resp.isConnected();
        } catch (IOException e) {
            System.err.println("JSON reading error: " + e.getMessage());
            return false;
        }
    }
}
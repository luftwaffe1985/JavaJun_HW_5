import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {

    private final static ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
        try (ServerSocket server = new ServerSocket(8888)) {
            System.out.println("Server is up and running");
            while (true) {
                System.out.println("Waiting a client's connection...");
                Socket client = server.accept();
                ClientHandler clientHandler = new ClientHandler(client, clients);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Server operation error: " + e.getMessage());
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
        public void run() {
            System.out.println("A new client connected");
            try {
                String loginRequest = in.nextLine();
                LoginRequest request = objectMapper.reader().readValue(loginRequest, LoginRequest.class);
                this.clientLogin = request.getLogin();
            } catch (IOException e) {
                System.err.println("Client's msg reading failed [" + clientLogin + "]: " + e.getMessage());
                String unsuccessfulResponse = createLoginResponse(false);
                out.println(unsuccessfulResponse);
                doClose();
                return;
            }

            System.out.println("Client's request: " + clientLogin);
            if (clients.containsKey(clientLogin)) {
                String unsuccessfulResponse = createLoginResponse(false);
                out.println(unsuccessfulResponse);
                doClose();
                return;
            }
            clients.put(clientLogin, this);
            String successfulLoginResponse = createLoginResponse(true);
            out.println(successfulLoginResponse);
            while (true) {
                String msgFromClient = in.nextLine();
                final String type;
                try {
                    AbstractRequest request = objectMapper.reader().readValue(msgFromClient, AbstractRequest.class);
                    type = request.getType();
                    if (SendMsgRequest.TYPE.equals(type)) {
                        final SendMsgRequest request1;
                        request1 = objectMapper.reader().readValue(msgFromClient, SendMsgRequest.class);
                        ClientHandler clientTo = clients.get(request1.getRecipient());
                        if (clientTo == null) {
                            sendMessage(
                                    "The Client with the login [" + request1.getRecipient() + "] has not been found");
                            continue;
                        }
                        clientTo.sendMessage(request1.getMessage());
                    } else if (SendBroadcastMessageRequest.TYPE.equals(type)) { // BroadcastRequest.TYPE.equals(type)
                        final SendBroadcastMessageRequest request2;
                        request2 = objectMapper.reader().readValue(msgFromClient, SendBroadcastMessageRequest.class);
                        if (clients.size() > 1) {
                            for (String clientName : clients.keySet()) {
                                if (!clientName.equals(clientLogin)) {
                                    ClientHandler clientTo = clients.get(clientName);
                                    clientTo.sendMessage(request2.getMessage());
                                }
                            }
                        }
                    } else if (SendToGetUsersRequest.TYPE.equals(type)) {
                        StringBuilder strUsers = new StringBuilder();
                        int count = 0;
                        for (String clientName : clients.keySet()) {
                            if (!clientName.equals(clientLogin)) {
                                strUsers.append(clientName + " ");
                                count++;
                            }
                        }
                        if (count == 0) {
                            sendMessage("You are the sole recipient in the Chat)");
                        } else {
                            sendMessage("Recipients in the Chat: " + strUsers);
                        }
                    } else if (SendDisconnectionRequest.TYPE.equals(type)) {
                        for (String clientName : clients.keySet()) {
                            if (!clientName.equals(clientLogin)) {
                                ClientHandler clientTo = clients.get(clientName);
                                clientTo.sendMessage(clientLogin + " has disconnected from the server");
                            }
                        }
                        System.out.println(SendDisconnectionRequest.TYPE);
                        break;
                    } else {
                        System.err.println("Unknown msg type: " + type);
                        sendMessage("Unknown msg type: " + type);
                        continue;
                    }
                } catch (IOException e) {
                    System.err.println(
                            "Client's msg reading failed [" + clientLogin + "]: " + e.getMessage());
                    sendMessage("Msg reading failed: " + e.getMessage());
                    continue;
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
                System.err.println("Client disconnection error: " + e.getMessage());
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        private String createLoginResponse(boolean success) {
            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setConnected(success);
            try {
                return objectMapper.writer().writeValueAsString(loginResponse);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to create a loginResponse: " + e.getMessage());
            }
        }
    }
}